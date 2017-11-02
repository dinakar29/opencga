package org.opencb.opencga.storage.core.variant.adaptors;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opencb.biodata.models.variant.StudyEntry;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.opencga.core.results.VariantQueryResult;
import org.opencb.opencga.storage.core.metadata.StudyConfiguration;
import org.opencb.opencga.storage.core.variant.VariantStorageBaseTest;
import org.opencb.opencga.storage.core.variant.VariantStorageEngine;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.opencb.biodata.models.variant.StudyEntry.FILTER;
import static org.opencb.opencga.storage.core.variant.adaptors.VariantMatchers.*;
import static org.opencb.opencga.storage.core.variant.adaptors.VariantQueryUtils.*;

/**
 * Created on 24/10/17.
 *
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
@Ignore
public abstract class VariantDBAdaptorMultiFileTest extends VariantStorageBaseTest {

    protected static boolean loaded = false;
    private VariantDBAdaptor dbAdaptor;
    private Query query;
    private QueryOptions options = new QueryOptions();
    private VariantQueryResult<Variant> queryResult;

    @Before
    public void before() throws Exception {
        dbAdaptor = variantStorageEngine.getDBAdaptor();
        if (!loaded) {
            super.before();

            VariantStorageEngine storageEngine = getVariantStorageEngine();
            ObjectMap options = getOptions();

            int maxStudies = 2;
            int studyId = 1;
            List<URI> inputFiles = new ArrayList<>();
            StudyConfiguration studyConfiguration = new StudyConfiguration(studyId, "S_" + studyId);
            for (int fileId = 12877; fileId <= 12893; fileId++) {
                String fileName = "1K.end.platinum-genomes-vcf-NA" + fileId + "_S1.genome.vcf.gz";
                URI inputFile = getResourceUri("platinum/" + fileName);
                inputFiles.add(inputFile);
                studyConfiguration.getFileIds().put(fileName, fileId);
                if (inputFiles.size() == 4) {
                    dbAdaptor.getStudyConfigurationManager().updateStudyConfiguration(studyConfiguration, null);
                    options.put(VariantStorageEngine.Options.STUDY_ID.key(), studyId);
                    storageEngine.getOptions().putAll(options);
                    storageEngine.index(inputFiles, outputUri, true, true, true);

                    studyId++;
                    studyConfiguration = new StudyConfiguration(studyId, "S_" + studyId);
                    inputFiles.clear();
                    if (studyId > maxStudies) {
                        break;
                    }
                }
            }
            loaded = true;
        }
    }

    protected ObjectMap getOptions() {
        return new ObjectMap();
    }

    @Test
    public void testIncludeStudies() throws Exception {
        query = new Query()
                .append(VariantQueryParam.RETURNED_STUDIES.key(), "S_1");
        queryResult = dbAdaptor.get(query, options);
        assertEquals(dbAdaptor.count(null).first().intValue(), queryResult.getNumResults());
        assertThat(queryResult, everyResult(allOf(withStudy("S_2", nullValue()), withStudy("S_3", nullValue()), withStudy("S_4", nullValue()))));
    }

    @Test
    public void testIncludeStudiesAll() throws Exception {
        query = new Query(VariantQueryParam.RETURNED_STUDIES.key(), ALL);
        queryResult = dbAdaptor.get(query, options);
        VariantQueryResult<Variant> allVariants = dbAdaptor.get(new Query(), options);

        assertThat(queryResult, everyResult(allVariants, notNullValue(Variant.class)));
    }

    @Test
    public void testIncludeStudiesNone() throws Exception {
        query = new Query(VariantQueryParam.RETURNED_STUDIES.key(), NONE);
        queryResult = dbAdaptor.get(query, options);

        assertEquals(dbAdaptor.count(null).first().intValue(), queryResult.getNumResults());
        assertThat(queryResult, everyResult(firstStudy(nullValue())));
    }

    @Test
    public void testIncludeFiles() throws Exception {
        query = new Query()
                .append(VariantQueryParam.RETURNED_STUDIES.key(), "S_1")
                .append(VariantQueryParam.RETURNED_FILES.key(), "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz");
        queryResult = dbAdaptor.get(query, options);
        assertEquals(dbAdaptor.count(null).first().intValue(), queryResult.getNumResults());
        for (Variant variant : queryResult.getResult()) {
            assertTrue(variant.getStudies().size() <= 1);
            StudyEntry s_1 = variant.getStudy("S_1");
            if (s_1 != null) {
                assertTrue(s_1.getFiles().size() <= 1);
                if (s_1.getFiles().size() == 1) {
                    assertNotNull(s_1.getFile("12877"));
                }
            }
            assertTrue(variant.getStudies().size() <= 1);
        }
        assertThat(queryResult, everyResult(allOf(not(withStudy("S_2")), not(withStudy("S_3")), not(withStudy("S_4")))));
    }

    @Test
    public void testGetByStudies() throws Exception {
        query = new Query()
                .append(VariantQueryParam.STUDIES.key(), "S_1");
        queryResult = dbAdaptor.get(query, options);
        VariantQueryResult<Variant> allVariants = dbAdaptor.get(new Query()
                .append(VariantQueryParam.RETURNED_STUDIES.key(), "S_1"), options);
        assertThat(queryResult, everyResult(allVariants, withStudy("S_1")));
    }

    @Test
    public void testGetByStudiesNegated() throws Exception {
        query = new Query()
                .append(VariantQueryParam.STUDIES.key(), "S_1" + AND + NOT + "S_2");
        queryResult = dbAdaptor.get(query, options);
        VariantQueryResult<Variant> allVariants = dbAdaptor.get(new Query()
                .append(VariantQueryParam.RETURNED_STUDIES.key(), "S_1,S_2"), options);
        assertThat(queryResult, everyResult(allVariants, allOf(withStudy("S_1"), not(withStudy("S_2")))));
    }

    @Test
    public void testGetByFileName() throws Exception {
        query = new Query()
                .append(VariantQueryParam.STUDIES.key(), "S_1")
                .append(VariantQueryParam.FILES.key(), "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz");
        queryResult = dbAdaptor.get(query, options);
        VariantQueryResult<Variant> allVariants = dbAdaptor.get(new Query()
                .append(VariantQueryParam.RETURNED_STUDIES.key(), "S_1")
                .append(VariantQueryParam.RETURNED_SAMPLES.key(), "NA12877")
                .append(VariantQueryParam.RETURNED_FILES.key(), "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz"), options);
        assertThat(queryResult, everyResult(allVariants, withStudy("S_1", withFileId("12877"))));

    }

    @Test
    public  void testGetByFileNamesOr() {
        query = new Query()
                .append(VariantQueryParam.STUDIES.key(), "S_1")
                .append(VariantQueryParam.FILES.key(),
                        "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz"
                                + VariantQueryUtils.OR +
                        "1K.end.platinum-genomes-vcf-NA12878_S1.genome.vcf.gz");
        queryResult = dbAdaptor.get(query, options);
        VariantQueryResult<Variant> allVariants = dbAdaptor.get(new Query()
                .append(VariantQueryParam.RETURNED_STUDIES.key(), "S_1")
                .append(VariantQueryParam.RETURNED_SAMPLES.key(), "NA12877,NA12878")
                .append(VariantQueryParam.RETURNED_FILES.key(), "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz,1K.end.platinum-genomes-vcf-NA12878_S1.genome.vcf.gz"), options);
        assertThat(queryResult, everyResult(allVariants, withStudy("S_1", anyOf(withFileId("12877"), withFileId("12878")))));
    }

    @Test
    public void testGetByFileNamesAnd() {
        query = new Query()
                .append(VariantQueryParam.STUDIES.key(), "S_1")
                .append(VariantQueryParam.FILES.key(),
                        "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz"
                                + AND +
                        "1K.end.platinum-genomes-vcf-NA12878_S1.genome.vcf.gz");
        queryResult = dbAdaptor.get(query, options);
        VariantQueryResult<Variant> allVariants = dbAdaptor.get(new Query()
                .append(VariantQueryParam.RETURNED_STUDIES.key(), "S_1")
                .append(VariantQueryParam.RETURNED_SAMPLES.key(), "NA12877,NA12878")
                .append(VariantQueryParam.RETURNED_FILES.key(), "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz,1K.end.platinum-genomes-vcf-NA12878_S1.genome.vcf.gz"), options);
        assertThat(queryResult, everyResult(allVariants, withStudy("S_1", allOf(withFileId("12877"), withFileId("12878")))));
    }

    @Test
    public void testGetByFileNamesAndNegated() {
        query = new Query()
                .append(VariantQueryParam.STUDIES.key(), "S_1")
                .append(VariantQueryParam.FILES.key(),
                        "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz"
                                + AND + NOT +
                                "1K.end.platinum-genomes-vcf-NA12878_S1.genome.vcf.gz")
                .append(VariantQueryParam.RETURNED_SAMPLES.key(), "NA12877,NA12878")
                .append(VariantQueryParam.RETURNED_FILES.key(), "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz");
        queryResult = dbAdaptor.get(query, options);
        VariantQueryResult<Variant> allVariants = dbAdaptor.get(new Query()
                .append(VariantQueryParam.RETURNED_STUDIES.key(), "S_1")
                .append(VariantQueryParam.RETURNED_SAMPLES.key(), "NA12877,NA12878")
                // Return file NA12878 to determine which variants must be discarded
                .append(VariantQueryParam.RETURNED_FILES.key(), "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz,1K.end.platinum-genomes-vcf-NA12878_S1.genome.vcf.gz"), options);
        assertThat(queryResult, everyResult(allVariants, withStudy("S_1", allOf(withFileId("12877"), not(withFileId("12878"))))));
    }

    @Test
    public void testGetByFileNamesMultiStudiesAnd() {
        query = new Query()
                .append(VariantQueryParam.STUDIES.key(), "S_1,S_2")
                .append(VariantQueryParam.FILES.key(),
                        "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz"
                                + AND +
                        "1K.end.platinum-genomes-vcf-NA12882_S1.genome.vcf.gz");
        queryResult = dbAdaptor.get(query, options);
        VariantQueryResult<Variant> allVariants = dbAdaptor.get(new Query()
                .append(VariantQueryParam.RETURNED_STUDIES.key(), "S_1,S_2")
                .append(VariantQueryParam.RETURNED_SAMPLES.key(), "NA12877,NA12882")
                .append(VariantQueryParam.RETURNED_FILES.key(), "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz,1K.end.platinum-genomes-vcf-NA12882_S1.genome.vcf.gz"), options);
        assertThat(queryResult, everyResult(allVariants, allOf(withStudy("S_1", withFileId("12877")), withStudy("S_2", withFileId("12882")))));
    }

    @Test
    public void testGetByFileNamesMultiStudiesOr() {
        query = new Query()
                .append(VariantQueryParam.STUDIES.key(), "S_1,S_2")
                .append(VariantQueryParam.FILES.key(),
                        "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz"
                                + VariantQueryUtils.OR +
                        "1K.end.platinum-genomes-vcf-NA12882_S1.genome.vcf.gz");
        queryResult = dbAdaptor.get(query, options);
        VariantQueryResult<Variant> allVariants = dbAdaptor.get(new Query()
                .append(VariantQueryParam.RETURNED_STUDIES.key(), "S_1,S_2")
                .append(VariantQueryParam.RETURNED_SAMPLES.key(), "NA12877,NA12882")
                .append(VariantQueryParam.RETURNED_FILES.key(), "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz,1K.end.platinum-genomes-vcf-NA12882_S1.genome.vcf.gz"), options);
        assertThat(queryResult, everyResult(allVariants, anyOf(withStudy("S_1", withFileId("12877")), withStudy("S_2", withFileId("12882")))));
    }

    @Test
    public void testGetByFileNamesMultiStudiesImplicitAnd() {
        query = new Query()
                .append(VariantQueryParam.FILES.key(),
                        "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz"
                                + AND +
                                "1K.end.platinum-genomes-vcf-NA12882_S1.genome.vcf.gz");
        queryResult = dbAdaptor.get(query, options);
        VariantQueryResult<Variant> allVariants = dbAdaptor.get(new Query()
                .append(VariantQueryParam.RETURNED_SAMPLES.key(), "NA12877,NA12882")
                .append(VariantQueryParam.RETURNED_FILES.key(), "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz,1K.end.platinum-genomes-vcf-NA12882_S1.genome.vcf.gz"), options);
        assertThat(queryResult, everyResult(allVariants, allOf(withStudy("S_1", withFileId("12877")), withStudy("S_2", withFileId("12882")))));
    }

    @Test
    public void testGetByFileNamesMultiStudiesImplicitOr() {
        query = new Query()
                .append(VariantQueryParam.FILES.key(),
                        "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz"
                                + OR +
                                "1K.end.platinum-genomes-vcf-NA12882_S1.genome.vcf.gz");
        queryResult = dbAdaptor.get(query, options);
        VariantQueryResult<Variant> allVariants = dbAdaptor.get(new Query()
                .append(VariantQueryParam.RETURNED_SAMPLES.key(), "NA12877,NA12882")
                .append(VariantQueryParam.RETURNED_FILES.key(), "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz,1K.end.platinum-genomes-vcf-NA12882_S1.genome.vcf.gz"), options);
        assertThat(queryResult, everyResult(allVariants, anyOf(withStudy("S_1", withFileId("12877")), withStudy("S_2", withFileId("12882")))));
    }

    @Test
    public void testGetByFilter() {
        VariantQueryResult<Variant> allVariants = dbAdaptor.get(new Query()
                .append(VariantQueryParam.RETURNED_SAMPLES.key(), "NA12877")
                .append(VariantQueryParam.RETURNED_FILES.key(), "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz"), options);

        query = new Query()
                .append(VariantQueryParam.FILTER.key(), "LowGQX;LowMQ;LowQD;TruthSensitivityTranche99.90to100.00")
                .append(VariantQueryParam.FILES.key(), "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz");
        queryResult = dbAdaptor.get(query, options);
        System.out.println("queryResult.getNumResults() = " + queryResult.getNumResults());
        assertThat(queryResult, everyResult(allVariants, withStudy("S_1", withFileId("12877",
                with(FILTER, fileEntry -> fileEntry.getAttributes().get(FILTER), allOf(
                        containsString("LowGQX"),
                        containsString("LowMQ"),
                        containsString("LowQD"),
                        containsString("TruthSensitivityTranche99.90to100.00")
                ))))));

        query = new Query()
                .append(VariantQueryParam.FILTER.key(), "LowGQX,LowMQ")
                .append(VariantQueryParam.FILES.key(), "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz");
        queryResult = dbAdaptor.get(query, options);
        System.out.println("queryResult.getNumResults() = " + queryResult.getNumResults());
        assertThat(queryResult, everyResult(allVariants, withStudy("S_1", withFileId("12877",
                with(FILTER, fileEntry -> fileEntry.getAttributes().get(FILTER), anyOf(
                        containsString("LowGQX"),
                        containsString("LowMQ")
                ))))));

        query = new Query()
                .append(VariantQueryParam.FILTER.key(), "\"LowGQX;LowMQ;LowQD;TruthSensitivityTranche99.90to100.00\"")
                .append(VariantQueryParam.FILES.key(), "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz");
        queryResult = dbAdaptor.get(query, options);
        System.out.println("queryResult.getNumResults() = " + queryResult.getNumResults());
        assertThat(queryResult, everyResult(allVariants, withStudy("S_1", withFileId("12877",
                with(FILTER, fileEntry -> fileEntry.getAttributes().get(FILTER), is("LowGQX;LowMQ;LowQD;TruthSensitivityTranche99.90to100.00"))))));

        query = new Query()
                .append(VariantQueryParam.FILTER.key(), "\"LowGQX;LowMQ;LowQD;TruthSensitivityTranche99.90to100.00\",\"LowGQX;LowQD;SiteConflict\"")
                .append(VariantQueryParam.FILES.key(), "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz");
        queryResult = dbAdaptor.get(query, options);
        System.out.println("queryResult.getNumResults() = " + queryResult.getNumResults());
        assertThat(queryResult, everyResult(allVariants, withStudy("S_1", withFileId("12877",
                with(FILTER, fileEntry -> fileEntry.getAttributes().get(FILTER), anyOf(
                        is("LowGQX;LowMQ;LowQD;TruthSensitivityTranche99.90to100.00"),
                        is("LowGQX;LowQD;SiteConflict")
                ))))));
    }

    @Test
    public void testGetByNegatedFilter() {
        VariantQueryResult<Variant> allVariants = dbAdaptor.get(new Query()
                .append(VariantQueryParam.RETURNED_SAMPLES.key(), "NA12877")
                .append(VariantQueryParam.RETURNED_FILES.key(), "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz"), options);

        query = new Query()
                .append(VariantQueryParam.FILTER.key(), "LowGQX" + AND + "LowMQ" + AND + NOT + "SiteConflict")
                .append(VariantQueryParam.FILES.key(), "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz");
        queryResult = dbAdaptor.get(query, options);
        System.out.println("queryResult.getNumResults() = " + queryResult.getNumResults());
        assertThat(queryResult, everyResult(allVariants, withStudy("S_1", withFileId("12877",
                with(FILTER, fileEntry -> fileEntry.getAttributes().get(FILTER), allOf(
                        containsString("LowGQX"),
                        containsString("LowMQ"),
                        not(containsString("SiteConflict"))
                ))))));

        query = new Query()
                .append(VariantQueryParam.FILTER.key(), "LowGQX" + AND + "LowQD" + AND + NOT + "\"LowGQX;LowQD;SiteConflict\"")
                .append(VariantQueryParam.FILES.key(), "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz");
        queryResult = dbAdaptor.get(query, options);
        System.out.println("queryResult.getNumResults() = " + queryResult.getNumResults());
        assertThat(queryResult, everyResult(allVariants, withStudy("S_1", withFileId("12877",
                with(FILTER, fileEntry -> fileEntry.getAttributes().get(FILTER), allOf(
                        containsString("LowGQX"),
                        containsString("LowQD"),
                        not(is("LowGQX;LowQD;SiteConflict"))
                ))))));

    }

    @Test
    public void testGetByFilterMultiFile() {
        VariantQueryResult<Variant> allVariants = dbAdaptor.get(new Query()
                .append(VariantQueryParam.RETURNED_SAMPLES.key(), "NA12877,NA12878")
                .append(VariantQueryParam.RETURNED_FILES.key(), "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz,1K.end.platinum-genomes-vcf-NA12878_S1.genome.vcf.gz"), options);


        query = new Query()
                .append(VariantQueryParam.FILTER.key(), "LowGQX;LowMQ")
                .append(VariantQueryParam.FILES.key(),
                        "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz"
                                + OR +
                        "1K.end.platinum-genomes-vcf-NA12878_S1.genome.vcf.gz");
        queryResult = dbAdaptor.get(query, options);

        assertThat(queryResult, everyResult(allVariants, withStudy("S_1", anyOf(
                withFileId("12877", with(FILTER, fileEntry -> fileEntry.getAttributes().get(FILTER), allOf(containsString("LowGQX"), containsString("LowMQ")))),
                withFileId("12878", with(FILTER, fileEntry -> fileEntry.getAttributes().get(FILTER), allOf(containsString("LowGQX"), containsString("LowMQ"))))
        ))));

        query = new Query()
                .append(VariantQueryParam.FILTER.key(), "LowGQX;LowMQ")
                .append(VariantQueryParam.FILES.key(),
                        "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz"
                                + AND +
                        "1K.end.platinum-genomes-vcf-NA12878_S1.genome.vcf.gz");
        queryResult = dbAdaptor.get(query, options);

        assertThat(queryResult, everyResult(allVariants, withStudy("S_1", allOf(
                withFileId("12877", with(FILTER, fileEntry -> fileEntry.getAttributes().get(FILTER), allOf(containsString("LowGQX"), containsString("LowMQ")))),
                withFileId("12878", with(FILTER, fileEntry -> fileEntry.getAttributes().get(FILTER), allOf(containsString("LowGQX"), containsString("LowMQ"))))
        ))));

        query = new Query()
                .append(VariantQueryParam.FILTER.key(), "LowGQX")
                .append(VariantQueryParam.FILES.key(),
                        "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz"
                                + AND +
                        "1K.end.platinum-genomes-vcf-NA12878_S1.genome.vcf.gz");
        queryResult = dbAdaptor.get(query, options);

        assertThat(queryResult, everyResult(allVariants, withStudy("S_1", allOf(
                withFileId("12877", with(FILTER, fileEntry -> fileEntry.getAttributes().get(FILTER), containsString("LowGQX"))),
                withFileId("12878", with(FILTER, fileEntry -> fileEntry.getAttributes().get(FILTER), containsString("LowGQX")))
        ))));
    }

    @Test
    public void testGetByFilterMultiFileNegatedFiles() {
        VariantQueryResult<Variant> allVariants = dbAdaptor.get(new Query()
                .append(VariantQueryParam.RETURNED_SAMPLES.key(), "NA12877")
                .append(VariantQueryParam.RETURNED_FILES.key(), "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz,1K.end.platinum-genomes-vcf-NA12878_S1.genome.vcf.gz"), options);


        query = new Query()
                .append(VariantQueryParam.FILTER.key(), "LowGQX;LowMQ")
                .append(VariantQueryParam.FILES.key(),
                        "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz"
                                + AND + NOT +
                        "1K.end.platinum-genomes-vcf-NA12878_S1.genome.vcf.gz");
        queryResult = dbAdaptor.get(query, options);

        assertThat(queryResult, everyResult(allVariants, withStudy("S_1", allOf(
                withFileId("12877", with(FILTER, fileEntry -> fileEntry.getAttributes().get(FILTER), allOf(containsString("LowGQX"), containsString("LowMQ")))),
                not(withFileId("12878"))
        ))));
    }

    @Test
    public void testGetByFilterMultiStudy() {
        query = new Query()
                .append(VariantQueryParam.FILTER.key(), "LowGQX;LowMQ")
                .append(VariantQueryParam.FILES.key(),
                        "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz"
                                + VariantQueryUtils.OR +
                         "1K.end.platinum-genomes-vcf-NA12882_S1.genome.vcf.gz");
        queryResult = dbAdaptor.get(query, options);
        VariantQueryResult<Variant> allVariants = dbAdaptor.get(new Query()
                .append(VariantQueryParam.RETURNED_SAMPLES.key(), "NA12877,NA12882")
                .append(VariantQueryParam.RETURNED_FILES.key(), "1K.end.platinum-genomes-vcf-NA12877_S1.genome.vcf.gz,1K.end.platinum-genomes-vcf-NA12882_S1.genome.vcf.gz"), options);
        assertThat(queryResult, everyResult(allVariants, anyOf(
                withStudy("S_1", withFileId("12877", with(FILTER, fileEntry -> fileEntry.getAttributes().get(FILTER), allOf(containsString("LowGQX"), containsString("LowMQ"))))),
                withStudy("S_2", withFileId("12882", with(FILTER, fileEntry -> fileEntry.getAttributes().get(FILTER), allOf(containsString("LowGQX"), containsString("LowMQ")))))
        )));
    }

}
