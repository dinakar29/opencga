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

package org.opencb.opencga.app.cli.admin.executors;


import org.opencb.commons.datastore.core.DataStoreServerAddress;
import org.opencb.commons.datastore.mongodb.MongoDataStoreManager;
import org.opencb.opencga.analysis.demo.AnalysisDemo;
import org.opencb.opencga.app.cli.admin.AdminCliOptionsParser;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.managers.CatalogManager;
import org.opencb.opencga.catalog.monitor.MonitorService;
import org.opencb.opencga.catalog.utils.CatalogDemo;
import org.opencb.opencga.storage.core.exceptions.StorageEngineException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by imedina on 02/03/15.
 */
public class CatalogCommandExecutor extends AdminCommandExecutor {

    private AdminCliOptionsParser.CatalogCommandOptions catalogCommandOptions;

    public CatalogCommandExecutor(AdminCliOptionsParser.CatalogCommandOptions catalogCommandOptions) {
        super(catalogCommandOptions.commonOptions);
        this.catalogCommandOptions = catalogCommandOptions;
    }



    @Override
    public void execute() throws Exception {
        logger.debug("Executing variant command line");

        String subCommandString = catalogCommandOptions.getParsedSubCommand();
        switch (subCommandString) {
            case "demo":
                demo();
                break;
            case "install":
                install();
                break;
            case "delete":
                delete();
                break;
            case "index":
                index();
                break;
            case "export":
                export();
                break;
            case "import":
                importDatabase();
                break;
            case "daemon":
                daemons();
                break;
            default:
                logger.error("Subcommand not valid");
                break;
        }

    }

    private void demo() throws CatalogException, StorageEngineException, IOException, URISyntaxException {
        if (catalogCommandOptions.demoCatalogCommandOptions.prefix != null) {
            configuration.setDatabasePrefix(catalogCommandOptions.demoCatalogCommandOptions.prefix);
        } else {
            configuration.setDatabasePrefix("demo");
        }
        configuration.setOpenRegister(true);

        configuration.getAdmin().setPassword("demo");
        configuration.getAdmin().setSecretKey("demo");
        configuration.getAdmin().setAlgorithm("HS256");

        CatalogDemo.createDemoDatabase(configuration, catalogCommandOptions.demoCatalogCommandOptions.force);
        CatalogManager catalogManager = new CatalogManager(configuration);
        sessionId = catalogManager.getUserManager().login("user1", "user1_pass");
        AnalysisDemo.insertPedigreeFile(catalogManager, 6L, Paths.get(this.appHome).resolve("examples/20130606_g1k.ped"), sessionId);
    }

    private void export() throws CatalogException, IOException {
        AdminCliOptionsParser.ExportCatalogCommandOptions commandOptions = catalogCommandOptions.exportCatalogCommandOptions;
        validateConfiguration(commandOptions, commandOptions.commonOptions);

        CatalogManager catalogManager = new CatalogManager(configuration);
        String token = catalogManager.getUserManager().login("admin", configuration.getAdmin().getPassword());

        catalogManager.getProjectManager().exportReleases(commandOptions.project, commandOptions.release, commandOptions.outputDir, token);
    }

    private void importDatabase() throws CatalogException, IOException {
        AdminCliOptionsParser.ImportCatalogCommandOptions commandOptions = catalogCommandOptions.importCatalogCommandOptions;
        validateConfiguration(commandOptions, commandOptions.commonOptions);

        CatalogManager catalogManager = new CatalogManager(configuration);
        String token = catalogManager.getUserManager().login("admin", configuration.getAdmin().getPassword());

        catalogManager.getProjectManager().importReleases(commandOptions.owner, commandOptions.directory, token);
    }

    private void install() throws CatalogException {
        validateConfiguration(catalogCommandOptions.installCatalogCommandOptions, catalogCommandOptions.commonOptions);

        this.configuration.getAdmin().setSecretKey(this.catalogCommandOptions.installCatalogCommandOptions.secretKey);
        this.configuration.getAdmin().setAlgorithm("HS256");
//        this.configuration.getAdmin().setAlgorithm(this.catalogCommandOptions.installCatalogCommandOptions.algorithm);

        if (configuration.getAdmin().getPassword() == null || configuration.getAdmin().getPassword().isEmpty()) {
            throw new CatalogException("No admin password found. Please, insert your password.");
        }

        CatalogManager catalogManager = new CatalogManager(configuration);
        logger.info("\nInstalling database {} in {}\n", catalogManager.getCatalogDatabase(), configuration.getCatalog().getDatabase()
                .getHosts());
        catalogManager.installCatalogDB();
    }

    /**
     * Checks if the database exists.
     *
     * @return true if exists.
     */
    private boolean checkDatabaseExists(String database) {
        List<DataStoreServerAddress> dataStoreServerAddresses = new ArrayList<>();
        for (String host : configuration.getCatalog().getDatabase().getHosts()) {
            if (host.contains(":")) {
                String[] split = host.split(":");
                Integer port = Integer.valueOf(split[1]);
                dataStoreServerAddresses.add(new DataStoreServerAddress(split[0], port));
            } else {
                dataStoreServerAddresses.add(new DataStoreServerAddress(host, 27017));
            }
        }
        MongoDataStoreManager mongoDataStoreManager = new MongoDataStoreManager(dataStoreServerAddresses);
        return mongoDataStoreManager.exists(database);
    }

    private void delete() throws CatalogException, URISyntaxException {
        setCatalogDatabaseCredentials(catalogCommandOptions.deleteCatalogCommandOptions.databaseHost,
                catalogCommandOptions.deleteCatalogCommandOptions.prefix, catalogCommandOptions.deleteCatalogCommandOptions.databaseUser,
                catalogCommandOptions.deleteCatalogCommandOptions.databasePassword,
                catalogCommandOptions.deleteCatalogCommandOptions.commonOptions.adminPassword);

        CatalogManager catalogManager = new CatalogManager(configuration);

        if (!checkDatabaseExists(catalogManager.getCatalogDatabase())) {
            throw new CatalogException("The database " + catalogManager.getCatalogDatabase() + " does not exist.");
        }
        logger.info("\nDeleting database {} from {}\n", catalogManager.getCatalogDatabase(), configuration.getCatalog().getDatabase()
                .getHosts());
        catalogManager.deleteCatalogDB(false);
    }

    private void index() throws CatalogException, IOException {
        setCatalogDatabaseCredentials(catalogCommandOptions.indexCatalogCommandOptions.databaseHost,
                catalogCommandOptions.indexCatalogCommandOptions.prefix, catalogCommandOptions.indexCatalogCommandOptions.databaseUser,
                catalogCommandOptions.indexCatalogCommandOptions.databasePassword,
                catalogCommandOptions.indexCatalogCommandOptions.commonOptions.adminPassword);

        CatalogManager catalogManager = new CatalogManager(configuration);
        if (!checkDatabaseExists(catalogManager.getCatalogDatabase())) {
            throw new CatalogException("The database " + catalogManager.getCatalogDatabase() + " does not exist.");
        }

        logger.info("\nChecking and installing non-existing indexes in {} in {}\n",
                catalogManager.getCatalogDatabase(), configuration.getCatalog().getDatabase().getHosts());
        String token = catalogManager.getUserManager().login("admin", configuration.getAdmin().getPassword());

        catalogManager.installIndexes(token);
    }

    private void daemons() throws Exception {
        if (catalogCommandOptions.daemonCatalogCommandOptions.start) {
            // Server crated and started
            MonitorService monitorService =
                    new MonitorService(catalogCommandOptions.daemonCatalogCommandOptions.commonOptions.adminPassword, configuration,
                            appHome);
            monitorService.start();
            monitorService.blockUntilShutdown();
            logger.info("Shutting down OpenCGA Storage REST server");
        }

        if (catalogCommandOptions.daemonCatalogCommandOptions.stop) {
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target("http://localhost:" + configuration.getMonitor().getPort())
                    .path("opencga")
                    .path("monitor")
                    .path("admin")
                    .path("stop");
            Response response = target.request().get();
            logger.info(response.toString());
        }
    }

    private void validateConfiguration(AdminCliOptionsParser.CatalogDatabaseCommandOptions catalogOptions,
                                       AdminCliOptionsParser.AdminCommonCommandOptions adminOptions) {
        if (catalogOptions.databaseUser != null) {
            configuration.getCatalog().getDatabase().setUser(catalogOptions.databaseUser);
        }
        if (catalogOptions.databasePassword != null) {
            configuration.getCatalog().getDatabase().setPassword(catalogOptions.databasePassword);
        }
        if (catalogOptions.prefix != null) {
            configuration.setDatabasePrefix(catalogOptions.prefix);
        }
        if (catalogOptions.databaseHost != null) {
            configuration.getCatalog().getDatabase().setHosts(Collections.singletonList(catalogOptions.databaseHost));
        }
        if (adminOptions.adminPassword != null) {
            configuration.getAdmin().setPassword(adminOptions.adminPassword);
        }
    }
}
