/*
 * Copyright 2015-2017 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.opencga.catalog.utils;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opencb.biodata.models.pedigree.Individual;
import org.opencb.biodata.models.pedigree.Pedigree;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.commons.datastore.core.QueryResult;
import org.opencb.commons.test.GenericTest;
import org.opencb.opencga.core.config.Configuration;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.managers.FileUtils;
import org.opencb.opencga.catalog.managers.CatalogManager;
import org.opencb.opencga.core.models.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class CatalogSampleAnnotationsLoaderTest extends GenericTest {

    private static final List<String> populations = Arrays.asList("ACB", "ASW", "BEB", "CDX", "CEU", "CHB", "CHS", "CLM", "ESN", "FIN",
            "GBR", "GIH", "GWD", "IBS", "ITU", "JPT", "KHV", "LWK", "MSL", "MXL", "PEL", "PJL", "PUR", "STU", "TSI", "YRI");
    private static CatalogSampleAnnotationsLoader loader;
    private static Pedigree pedigree;
    private static String sessionId;
    private static File pedFile;
    private static CatalogManager catalogManager;
    private static String userId;
    private static long studyId;

    @BeforeClass
    public static void beforeClass() throws IOException, CatalogException, URISyntaxException {
        Configuration configuration = Configuration.load(CatalogSampleAnnotationsLoaderTest.class.getClassLoader()
                .getClass().getResource("/configuration-test.yml").openStream());
        configuration.getAdmin().setSecretKey("dummy");
        configuration.getAdmin().setAlgorithm("HS256");
        catalogManager = new CatalogManager(configuration);
        catalogManager.deleteCatalogDB(true);
        catalogManager.installCatalogDB();
        loader = new CatalogSampleAnnotationsLoader(catalogManager);

        String pedFileName = "20130606_g1k.ped";
        URL pedFileURL = CatalogSampleAnnotationsLoader.class.getClassLoader().getResource(pedFileName);
        pedigree = loader.readPedigree(pedFileURL.getPath());

        userId = "user1";
        catalogManager.getUserManager().create(userId, userId, "asdasd@asd.asd", userId, "", -1L, Account.FULL, QueryOptions.empty(), null);
        sessionId = catalogManager.getUserManager().login(userId, userId);
        Project project = catalogManager.getProjectManager().create("default", "def", "", "ACME", "Homo sapiens",
                null, null, "GRCh38", new QueryOptions(), sessionId).getResult().get(0);
        Study study = catalogManager.getStudyManager().create(String.valueOf(project.getId()), "default", "def", Study.Type.FAMILY, null,
                "", null, null, null, null, null, null, null, null, sessionId).getResult().get(0);
        studyId = study.getId();
        pedFile = catalogManager.getFileManager().create(Long.toString(studyId), File.Type.FILE, File.Format.PED, File.Bioformat
                .OTHER_PED, "data/" + pedFileName, null, "", null, 0, -1, null, (long) -1, null, null, true, null, null, sessionId)
                .getResult().get(0);
        new FileUtils(catalogManager).upload(pedFileURL.toURI(), pedFile, null, sessionId, false, false, false, true, 10000000);
        pedFile = catalogManager.getFileManager().get(pedFile.getId(), null, sessionId).getResult().get(0);
    }

    @AfterClass
    public static void afterClass() throws CatalogException {
//        catalogManager.logout(userId, sessionId);
    }

    @Test
    public void testLoadPedigree_GeneratedVariableSet() throws Exception {
        URL pedFile = this.getClass().getClassLoader().getResource("20130606_g1k.ped");

        Pedigree pedigree = loader.readPedigree(pedFile.getPath());
        VariableSet variableSet = loader.getVariableSetFromPedFile(pedigree);

        validate(pedigree, variableSet);
    }

    @Test
    public void testLoadPedigree_GivenVariableSet() throws Exception {
        HashSet<Variable> variables = new HashSet<>();
        variables.add(new Variable("id", "", Variable.VariableType.DOUBLE, null, true, false, Collections.<String>emptyList(), 0, null,
                "", null, null));
        variables.add(new Variable("name", "", Variable.VariableType.TEXT, null, true, false, Collections.<String>emptyList(), 0, null,
                "", null, null));
        variables.add(new Variable("fatherId", "", Variable.VariableType.DOUBLE, null, false, false, Collections.<String>emptyList(), 0,
                null, "", null, null));
        variables.add(new Variable("Population", "", Variable.VariableType.CATEGORICAL, null, true, false, populations, 0, null, "",
                null, null));
        variables.add(new Variable("NonExistingField", "", Variable.VariableType.DOUBLE, "", false, false, Collections.emptyList(), 0, null, "",
                null, null));

        VariableSet variableSet = new VariableSet(5, "", false, false, "", variables, 1, null);

        validate(pedigree, variableSet);
    }

    @Test
    public void testLoadPedigreeCatalog() throws Exception {
        QueryResult<Sample> sampleQueryResult = loader.loadSampleAnnotations(pedFile, null, sessionId);
        long variableSetId = sampleQueryResult.getResult().get(0).getAnnotationSets().get(0).getVariableSetId();

//        Query query = new Query("variableSetId", variableSetId).append("annotation", "family:GB84");
        Query query = new Query("variableSetId", variableSetId)
                .append("annotation.family", "GB84");
        QueryOptions options = new QueryOptions("limit", 2);

        QueryResult<Sample> allSamples = catalogManager.getSampleManager().get(studyId, query, options, sessionId);
        Assert.assertNotEquals(0, allSamples.getNumResults());
        query.remove("annotation.family");

        query.put("annotation.sex", "2");
        query.put("annotation.Population","ITU");
        QueryResult<Sample> femaleIta = catalogManager.getSampleManager().get(studyId, query, options, sessionId);
        Assert.assertNotEquals(0, femaleIta.getNumResults());

        query.put("annotation.sex", "1");
        query.put("annotation.Population", "ITU");
        QueryResult<Sample> maleIta = catalogManager.getSampleManager().get(studyId, query, options, sessionId);
        Assert.assertNotEquals(0, maleIta.getNumResults());

        query.remove("annotation.sex");
        QueryResult<Sample> ita = catalogManager.getSampleManager().get(studyId, query, options, sessionId);
        Assert.assertNotEquals(0, ita.getNumResults());

        Assert.assertEquals("Fail sample query", ita.getNumTotalResults(), maleIta.getNumTotalResults() + femaleIta.getNumTotalResults());
    }


    private void validate(Pedigree pedigree, VariableSet variableSet) throws CatalogException {
        for (Map.Entry<String, Individual> entry : pedigree.getIndividuals().entrySet()) {
            Map<String, Object> annotation = loader.getAnnotation(entry.getValue(), null, variableSet, pedigree.getFields());
            HashSet<Annotation> annotationSet = new HashSet<>(annotation.size());
            for (Map.Entry<String, Object> annotationEntry : annotation.entrySet()) {
                annotationSet.add(new Annotation(annotationEntry.getKey(), annotationEntry.getValue()));
            }
            CatalogAnnotationsValidator.checkAnnotationSet(variableSet, new AnnotationSet("", variableSet.getId(), annotationSet, "", 1,
                    null), null);
        }
    }
}