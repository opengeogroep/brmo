package nl.b3p;

import nl.b3p.brmo.loader.BrmoFramework;
import nl.b3p.brmo.loader.entity.Bericht;
import nl.b3p.brmo.loader.entity.LaadProces;
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
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.oracle.Oracle10DataTypeFactory;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 *
 * Draaien met:
 * {@code mvn -Dit.test=VerminderenStukdelenIntegrationTest -Dtest.onlyITs=true verify -Ppostgres > target/postgres.log}.
 *
 * @author mprins
 */
@Tag("skip-windows-java11")
public class VerminderenStukdelenIntegrationTest extends AbstractDatabaseIntegrationTest {

    private static final Log LOG = LogFactory.getLog(VerminderenStukdelenIntegrationTest.class);

    // bestand met "ontstaan" bericht
    private final String ontstaanBestand = "/verminderenstukdelen/MUTKX01-ASN00V2937-Bericht1.xml";
    private final String datumOntstaan = "2017-02-22";
    private final int brondocOntstaan = 57;

    // bestand met mutatie bericht
    private final String mutatieBestand = "/verminderenstukdelen/MUTKX01-ASN00V2937-Bericht2.xml";
    private final String datumMutatie = "2017-03-03";
    private final int brondocMutatie = 18;

    private BrmoFramework brmo;

    // dbunit
    private IDatabaseConnection staging;
    private IDatabaseConnection rsgb;
    private BasicDataSource dsRsgb;
    private BasicDataSource dsStaging;
    private final Lock sequential = new ReentrantLock(true);

    @BeforeEach
    @Override
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

        staging = new DatabaseDataSourceConnection(dsStaging);
        rsgb = new DatabaseDataSourceConnection(dsRsgb);

        if (this.isOracle) {
            staging = new DatabaseConnection(OracleConnectionUnwrapper.unwrap(dsStaging.getConnection()), params.getProperty("staging.user").toUpperCase());
            staging.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new Oracle10DataTypeFactory());
            staging.getConfig().setProperty(DatabaseConfig.FEATURE_SKIP_ORACLE_RECYCLEBIN_TABLES, true);

            rsgb = new DatabaseConnection(OracleConnectionUnwrapper.unwrap(dsRsgb.getConnection()), params.getProperty("rsgb.user").toUpperCase());
            rsgb.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new Oracle10DataTypeFactory());
            rsgb.getConfig().setProperty(DatabaseConfig.FEATURE_SKIP_ORACLE_RECYCLEBIN_TABLES, true);
        } else if (this.isPostgis) {
            // we hebben alleen nog postgres over
            staging.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new PostgresqlDataTypeFactory());
            rsgb.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new PostgresqlDataTypeFactory());
        }

        brmo = new BrmoFramework(dsStaging, dsRsgb);

        FlatXmlDataSetBuilder fxdb = new FlatXmlDataSetBuilder();
        fxdb.setCaseSensitiveTableNames(false);
        IDataSet stagingDataSet = fxdb.build(new FileInputStream(new File(VerminderenStukdelenIntegrationTest.class.getResource("/staging-empty-flat.xml").toURI())));

        sequential.lock();

        DatabaseOperation.CLEAN_INSERT.execute(staging, stagingDataSet);

        assumeTrue(0l == brmo.getCountBerichten(null, null, BrmoFramework.BR_BRK, "STAGING_OK"),
                "Er zijn brk STAGING_OK berichten");
        assumeTrue(0l == brmo.getCountLaadProcessen(null, null, BrmoFramework.BR_BRK, "STAGING_OK"),
                "Er zijn brk STAGING_OK laadprocessen");
    }

    @AfterEach
    public void cleanup() throws Exception {
        brmo.closeBrmoFramework();

        CleanUtil.cleanSTAGING(staging, false);
        staging.close();
        dsStaging.close();

        CleanUtil.cleanRSGB_BRK(rsgb, true);
        rsgb.close();
        dsRsgb.close();

        sequential.unlock();
    }

    @Test
    public void testMinderStukdelenInMutatie() throws Exception {
        assumeFalse(null == VerminderenStukdelenIntegrationTest.class.getResource(ontstaanBestand), "Het ontstaan test bestand moet er zijn.");
        assumeFalse(null == VerminderenStukdelenIntegrationTest.class.getResource(mutatieBestand), "Het mutatie test bestand moet er zijn.");

        LOG.debug("laden van ontstaan bericht in staging DB.");
        brmo.loadFromFile(BrmoFramework.BR_BRK, VerminderenStukdelenIntegrationTest.class.getResource(ontstaanBestand).getFile(), null);

        List<Bericht> berichten = brmo.listBerichten();
        List<LaadProces> processen = brmo.listLaadProcessen();
        assertNotNull(berichten, "De verzameling berichten bestaat niet.");
        assertEquals(1, berichten.size(), "Het aantal berichten is niet als verwacht.");
        assertNotNull(processen, "De verzameling processen bestaat niet.");
        assertEquals(1, processen.size(), "Het aantal processen is niet als verwacht.");

        LOG.debug("Transformeren ontstaan bericht naar rsgb DB.");
        Thread t = brmo.toRsgb();
        t.join();

        assertEquals(1, brmo.getCountBerichten(null, null, BrmoFramework.BR_BRK, "RSGB_OK"),
                "Niet alle berichten zijn OK getransformeerd");

        // test inhoud van rsgb tabellen na transformatie ontstaan bericht
        ITable kad_onrrnd_zk = rsgb.createDataSet().getTable("kad_onrrnd_zk");
        assertEquals(1, kad_onrrnd_zk.getRowCount(), "Het aantal onroerende zaak records komt niet overeen");
        assertEquals(datumOntstaan, kad_onrrnd_zk.getValue(0, "dat_beg_geldh"),
                "Datum eerste record komt niet overeen");

        ITable brondocument = rsgb.createDataSet().getTable("brondocument");
        assertEquals(brondocOntstaan, brondocument.getRowCount(),
                "Het aantal brondocument records komt niet overeen");

        // mutatie laden
        brmo.loadFromFile(BrmoFramework.BR_BRK, VerminderenStukdelenIntegrationTest.class.getResource(mutatieBestand).getFile(), null);
        LOG.debug("klaar met laden van mutatie bericht in staging DB.");

        LOG.debug("Transformeren mutatie bericht naar rsgb DB.");
        t = brmo.toRsgb();
        t.join();

        // test staging inhoud
        assertEquals(2, brmo.listBerichten().size(), "Het aantal berichten is niet als verwacht.");
        assertEquals(2, brmo.listLaadProcessen().size(), "Het aantal processen is niet als verwacht.");
        assertEquals(2, brmo.getCountBerichten(null, null, BrmoFramework.BR_BRK, "RSGB_OK"),
                "Niet alle berichten zijn OK getransformeerd");
        for (Bericht b : brmo.listBerichten()) {
            assertNotNull(b, "Bericht is 'null'");
            assertNotNull(b.getDbXml(), "'db-xml' van bericht is 'null'");
        }

        // test inhoud van rsgb tabellen na transformatie mutatie bericht
        kad_onrrnd_zk = rsgb.createDataSet().getTable("kad_onrrnd_zk");
        assertEquals(1, kad_onrrnd_zk.getRowCount(), "Het aantal onroerende zaak records komt niet overeen");
        assertEquals(datumMutatie, kad_onrrnd_zk.getValue(0, "dat_beg_geldh"),
                "Datum eerste record komt niet overeen");

        ITable kad_onrrnd_zk_archief = rsgb.createDataSet().getTable("kad_onrrnd_zk_archief");
        assertEquals(1, kad_onrrnd_zk_archief.getRowCount(),
                "Het aantal onroerende zaak records komt niet overeen");
        assertEquals(datumMutatie, kad_onrrnd_zk_archief.getValue(0, "datum_einde_geldh"),
                "Einddatum eerste record komt niet overeen");
        assertEquals(datumOntstaan, kad_onrrnd_zk_archief.getValue(0, "dat_beg_geldh"),
                "Begindatum eerste record komt niet overeen");

        brondocument = rsgb.createDataSet().getTable("brondocument");
        assertEquals(brondocMutatie, brondocument.getRowCount(),
                "Het aantal brondocument records komt niet overeen");
    }
}
