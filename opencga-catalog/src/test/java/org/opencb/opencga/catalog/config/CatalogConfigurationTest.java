/*
 * Copyright 2015 OpenCB
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

package org.opencb.opencga.catalog.config;

import org.junit.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by imedina on 16/03/16.
 */
public class CatalogConfigurationTest {

    @Test
    public void testDefault() {
        CatalogConfiguration catalogConfiguration = new CatalogConfiguration();

        catalogConfiguration.setLogLevel("INFO");

        catalogConfiguration.setDataDir("/opt/opencga/sessions");
        catalogConfiguration.setTempJobsDir("/opt/opencga/sessions/jobs");

        catalogConfiguration.setAdmin("admin");
        catalogConfiguration.setPassword("admin");

        EmailServer emailServer = new EmailServer("localhost", "", "", "", "", false);
        catalogConfiguration.setEmailServer(emailServer);

        DatabaseCredentials databaseCredentials = new DatabaseCredentials(Arrays.asList("localhost"), "opencga_catalog", "admin", "");
        catalogConfiguration.setDatabase(databaseCredentials);
//        CellBaseConfiguration cellBaseConfiguration = new CellBaseConfiguration(Arrays.asList("localhost"), "v3", new DatabaseCredentials(Arrays.asList("localhost"), "user", "password"));
//        QueryServerConfiguration queryServerConfiguration = new QueryServerConfiguration(61976, Arrays.asList("localhost"));
//
//        catalogConfiguration.setDefaultStorageEngineId("mongodb");
//
//        catalogConfiguration.setCellbase(cellBaseConfiguration);
//        catalogConfiguration.setServer(queryServerConfiguration);
//
//        catalogConfiguration.getStorageEngines().add(storageEngineConfiguration1);
//        catalogConfiguration.getStorageEngines().add(storageEngineConfiguration2);

        try {
            catalogConfiguration.serialize(new FileOutputStream("/tmp/catalog-configuration-test.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testLoad() throws Exception {
        CatalogConfiguration catalogConfiguration = CatalogConfiguration.load(getClass().getResource("/catalog-configuration-test.yml").openStream());
        System.out.println("catalogConfiguration = " + catalogConfiguration);
    }
}