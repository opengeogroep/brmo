package nl.b3p;

import nl.b3p.brmo.loader.BrmoFramework;
import nl.b3p.brmo.test.util.database.dbunit.CleanUtil;
import nl.b3p.jdbc.util.converter.OracleConnectionUnwrapper;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.DatabaseDataSourceConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.xml.XmlDataSet;
import org.dbunit.ext.oracle.Oracle10DataTypeFactory;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Testcases voor mantis-14743 / GH #789; te grote waarde voor een int (laadprocesid)
 * Draaien met:
 * {@code mvn -Dit.test=GH789GroteWaardeIDIntegrationTest -Dtest.onlyITs=true verify -Poracle -pl brmo-loader > target/oracle.log}
 * voor bijvoorbeeld Oracle of
 * {@code mvn -Dit.test=GH789GroteWaardeIDIntegrationTest -Dtest.onlyITs=true verify -Ppostgresql -pl brmo-loader > target/postgresql.log}.
 *
 * @author mprins
 */
@Tag("skip-windows-java11")
public class GH789GroteWaardeIDIntegrationTest  extends AbstractDatabaseIntegrationTest {

    private static final Log LOG = LogFactory.getLog(GH789GroteWaardeIDIntegrationTest.class);

    private BrmoFramework brmo;

    private IDatabaseConnection rsgb;
    private IDatabaseConnection staging;
    private BasicDataSource dsRsgb;
    private BasicDataSource dsStaging;
    private final Lock sequential = new ReentrantLock();

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        dsStaging = new BasicDataSource();
        dsStaging.setUrl(params.getProperty("staging.jdbc.url"));
        dsStaging.setUsername(params.getProperty("staging.user"));
        dsStaging.setPassword(params.getProperty("staging.passwd"));
        dsStaging.setAccessToUnderlyingConnectionAllowed(true);

        dsRsgb = new BasicDataSource();
        dsRsgb.setUrl(params.getProperty("rsgb.jdbc.url"));
        dsRsgb.setUsername(params.getProperty("rsgb.user"));
        dsRsgb.setPassword(params.getProperty("rsgb.passwd"));
        dsRsgb.setAccessToUnderlyingConnectionAllowed(true);

        rsgb = new DatabaseDataSourceConnection(dsRsgb);
        staging = new DatabaseDataSourceConnection(dsStaging);

        if (this.isOracle) {
            rsgb = new DatabaseConnection(OracleConnectionUnwrapper.unwrap(dsRsgb.getConnection()), params.getProperty("rsgb.user").toUpperCase());
            rsgb.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new Oracle10DataTypeFactory());
            rsgb.getConfig().setProperty(DatabaseConfig.FEATURE_SKIP_ORACLE_RECYCLEBIN_TABLES, true);
            staging = new DatabaseConnection(OracleConnectionUnwrapper.unwrap(dsStaging.getConnection()), params.getProperty("staging.user").toUpperCase());
            staging.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new Oracle10DataTypeFactory());
            staging.getConfig().setProperty(DatabaseConfig.FEATURE_SKIP_ORACLE_RECYCLEBIN_TABLES, true);
        } else if (this.isPostgis) {
            // we hebben alleen nog postgres over
            rsgb.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new PostgresqlDataTypeFactory());
            staging.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new PostgresqlDataTypeFactory());
        }

        IDataSet stagingDataSet = new XmlDataSet(new FileInputStream(new File(GH789GroteWaardeIDIntegrationTest.class.getResource("/GH-789-grote-id/staging.xml").toURI())));

        sequential.lock();

        DatabaseOperation.CLEAN_INSERT.execute(staging, stagingDataSet);
        brmo = new BrmoFramework(dsStaging, dsRsgb);

        assumeTrue(1l == brmo.getCountBerichten(null, null, "brk", "STAGING_OK"),
                "Er zijn geen 1 STAGING_OK berichten");
        assumeTrue(1l == brmo.getCountLaadProcessen(null, null, "brk", "STAGING_OK"),
                "Er zijn geen 1 STAGING_OK laadproces");
    }

    @AfterEach
    public void cleanup() throws Exception {
        brmo.closeBrmoFramework();

        CleanUtil.cleanRSGB_BRK(rsgb, true);
        rsgb.close();
        dsRsgb.close();

        CleanUtil.cleanSTAGING(staging, false);
        staging.close();
        dsStaging.close();

        sequential.unlock();
    }

    /**
     * transformeer alle berichten en test of dat correct is gedaan.
     *
     * @throws Exception if any
     */
    @Test
    public void testTransformBerichten() throws Exception {
        brmo.setOrderBerichten(true);
        Thread t = brmo.toRsgb();
        t.join();

        assertEquals(0l, brmo.getCountBerichten(null, null, "brk", "STAGING_OK"),
                "Niet alle berichten zijn OK getransformeerd");
        assertEquals(1l, brmo.getCountBerichten(null, null, "brk", "RSGB_OK"),
                "Niet alle berichten zijn OK getransformeerd");
        assertEquals(0l, brmo.getCountBerichten(null, null, "brk", "RSGB_NOK"),
                "Er zijn berichten met status RSGB_NOK");

        ITable brondocument = rsgb.createDataSet().getTable("brondocument");
        assertTrue(brondocument.getRowCount() > 0, "Er zijn geen brondocumenten");

        ITable kad_onrrnd_zk = rsgb.createDataSet().getTable("kad_onrrnd_zk");
        assertEquals(1, kad_onrrnd_zk.getRowCount(), "Aantal actuele onroerende zaken is incorrect");

        ITable kad_perceel = rsgb.createDataSet().getTable("kad_perceel");
        assertEquals(1, kad_perceel.getRowCount(), "Aantal actuele percelen is incorrect");
        assertEquals("54690041070000", kad_perceel.getValue(0, "sc_kad_identif").toString(),
                "Perceel identif is incorrect");

//        // test of de records in de tabellen de juiste clazz hebben en volledig zijn
//        String[] tables = {"subject", "prs", "nat_prs"};
//        for (String table : tables) {
//            ITable tbl = rsgb.createDataSet().getTable(table);
//            assertEquals("Aantal rijen klopt niet in tabel " + table, 3, tbl.getRowCount());
//            for (int row=0;row < 3; row++) {
//                assertNotNull("'clazz' is null voor rij "+ row + 1 +", tabel " + table, tbl.getValue(0, "clazz"));
//                assertEquals("'clazz' klopt niet in tabel " + table, "INGESCHREVEN NATUURLIJK PERSOON", tbl.getValue(0, "clazz"));
//            }
//        }
//        // ingeschr_nat_prs
//        ITable ingeschr_nat_prs = rsgb.createDataSet().getTable("ingeschr_nat_prs");
//        assertEquals("Aantal ingeschr_nat_prs klopt niet.", 3, ingeschr_nat_prs.getRowCount());
//        for (int row=0;row < 3; row++) {
//            assertNull("'clazz' is niet null voor rij "+ row + 1 +", tabel ingeschr_nat_prs", ingeschr_nat_prs.getValue(row, "clazz"));
//
//        }
//        // niet_ingezetene
//        ITable niet_ingezetene = rsgb.createDataSet().getTable("niet_ingezetene");
//        assertEquals("Aantal niet_ingezetene klopt niet", 1, niet_ingezetene.getRowCount());
//        assertEquals("NL.KAD.Persoon.428924922", niet_ingezetene.getValue(0, "sc_identif"));
    }
}
