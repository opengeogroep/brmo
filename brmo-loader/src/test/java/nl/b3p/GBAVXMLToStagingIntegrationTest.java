/*
 * Copyright (C) 2018 B3Partners B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.b3p;

import nl.b3p.brmo.loader.BrmoFramework;
import nl.b3p.brmo.loader.entity.Bericht;
import nl.b3p.brmo.loader.util.BrmoLeegBestandException;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Draaien met:
 * {@code mvn -Dit.test=GBAVXMLToStagingIntegrationTest -Dtest.onlyITs=true verify -Ppostgresql  -pl brmo-loader > /tmp/postgresql.log}
 * voor bijvoorbeeld PostgreSQL.
 *
 * @author Mark Prins
 */
public class GBAVXMLToStagingIntegrationTest extends AbstractDatabaseIntegrationTest {

    static Stream<Arguments> argumentsProvider() {
        return Stream.of(
                // {"type","filename", aantalBerichten, aantalLaadProcessen, aantalSubject, aNummer, bsnNummer,
                // achterNaam, aandNaamgebruik, geslacht},
                arguments("gbav", "/nl/b3p/brmo/loader/xml/gbav-voorbeeld.xml", 1, 1, 2, "5054783237", "123459916",
                        "Kumari", "E", "V"),
                arguments("gbav", "/nl/b3p/brmo/loader/xml/fictieve-persoonslijst-brp.xml", 1, 1, 1, "1234567890",
                        "987654321", "Jansen", "E", "M")
        );
    }

    private static final Log LOG = LogFactory.getLog(GBAVXMLToStagingIntegrationTest.class);
    private final Lock sequential = new ReentrantLock();
    private BrmoFramework brmo;
    // dbunit
    private IDatabaseConnection staging;
    private IDatabaseConnection rsgb;
    private BasicDataSource dsStaging;
    private BasicDataSource dsRsgb;

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
        IDataSet stagingDataSet = fxdb.build(new FileInputStream(new File(GBAVXMLToStagingIntegrationTest.class.getResource("/staging-empty-flat.xml").toURI())));

        sequential.lock();

        DatabaseOperation.CLEAN_INSERT.execute(staging, stagingDataSet);
        assumeTrue(0L == brmo.getCountBerichten(null, null, "gbav", "STAGING_OK"),
                "Er zijn geen STAGING_OK berichten");
        assumeTrue(0L == brmo.getCountLaadProcessen(null, null, "gbav", "STAGING_OK"),
                "Er zijn geen STAGING_OK laadprocessen");
    }

    @AfterEach
    public void cleanup() throws Exception {
        brmo.closeBrmoFramework();
        CleanUtil.cleanSTAGING(staging, false);
        CleanUtil.cleanRSGB_BAG(rsgb, true);
        CleanUtil.cleanRSGB_BRP(rsgb);
        rsgb.close();
        dsRsgb.close();
        staging.close();
        dsStaging.close();
        sequential.unlock();
    }

    @DisplayName("insertOntbrekendeKlantAfgiftenummers")
    @ParameterizedTest(name = "{index}: type: {0}, bestand: {1}")
    @MethodSource("argumentsProvider")
    public void testGbavBerichtToStagingToRsgb(String bestandType, String bestandNaam, long aantalBerichten, long aantalProcessen, long aantalSubject,
            String aNummer, String bsnNummer, String achterNaam, String aandNaamgebruik, String geslacht) throws Exception {

        try {
            brmo.loadFromFile(bestandType, GBAVXMLToStagingIntegrationTest.class.getResource(bestandNaam).getFile(), null);
        } catch (BrmoLeegBestandException blbe) {
            LOG.debug("Er is een bestand zonder berichten geladen (kan voorkomen...).");
        }

        assertEquals(aantalBerichten, brmo.getCountBerichten(null, null, bestandType, "STAGING_OK"),
                "Verwacht aantal berichten");
        assertEquals(aantalProcessen, brmo.getCountLaadProcessen(null, null, bestandType, "STAGING_OK"),
                "Verwacht aantal laadprocessen");

        LOG.debug("Transformeren berichten naar rsgb DB.");
        brmo.setOrderBerichten(true);
        Thread t = brmo.toRsgb();
        t.join();

        assertEquals(aantalBerichten, brmo.getCountBerichten(null, null, bestandType, "RSGB_OK"),
                "Niet alle berichten zijn OK getransformeerd");

        for (Bericht b : brmo.listBerichten()) {
            Assertions.assertNotNull(b, "Bericht is 'null'");
            Assertions.assertNotNull(b.getDbXml(), "'db-xml' van bericht is 'null'");
        }

        ITable subject = rsgb.createDataSet().getTable("subject");
        assertEquals(aantalSubject, subject.getRowCount(), "Het aantal 'subject' klopt niet");

        ITable prs = rsgb.createDataSet().getTable("prs");
        assertEquals(aantalSubject, prs.getRowCount(), "Het aantal 'prs' klopt niet");

        ITable nat_prs = rsgb.createDataSet().getTable("nat_prs");
        assertEquals(aantalSubject, nat_prs.getRowCount(), "Het aantal 'nat_prs' klopt niet");
        int rowNum = (int) (aantalSubject - 1);
        assertEquals(aandNaamgebruik, nat_prs.getValue(rowNum, "aand_naamgebruik"),
                "Aanduiding naamgebruik komt niet overeen");
        assertEquals(geslacht, nat_prs.getValue(rowNum, "geslachtsaand"),
                "Geslachtsaanduiding komt niet overeen");
        assertEquals(achterNaam, nat_prs.getValue(rowNum, "nm_geslachtsnaam"),
                "Achternaam komt niet overeen");

        ITable ingeschr_nat_prs = rsgb.createDataSet().getTable("ingeschr_nat_prs");
        assertEquals(aantalSubject, ingeschr_nat_prs.getRowCount(),
                "Het aantal 'ingeschr_nat_prs' klopt niet");
        assertEquals(new BigDecimal(bsnNummer), ingeschr_nat_prs.getValue(rowNum, "bsn"),
                "BSN komt niet overeen");
        assertEquals(new BigDecimal(aNummer), ingeschr_nat_prs.getValue(rowNum, "a_nummer"),
                "A-nummer komt niet overeen");
    }
}
