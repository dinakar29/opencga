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

package org.opencb.opencga.catalog.monitor.daemons;

import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.commons.datastore.core.QueryResult;
import org.opencb.opencga.catalog.db.api.JobDBAdaptor;
import org.opencb.opencga.catalog.exceptions.CatalogDBException;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.managers.CatalogManager;
import org.opencb.opencga.core.models.Job;
import org.opencb.opencga.core.common.TimeUtils;

import java.util.Map;

/**
 * Created by imedina on 16/06/16.
 */
public class ExecutionDaemon extends MonitorParentDaemon {

    private int runningJobs;
    private String binHome;

    private JobDBAdaptor jobDBAdaptor;

    public ExecutionDaemon(int interval, String sessionId, CatalogManager catalogManager, String appHome) throws CatalogDBException {
        super(interval, sessionId, catalogManager);
        this.binHome = appHome + "/bin/";
        this.jobDBAdaptor = dbAdaptorFactory.getCatalogJobDBAdaptor();
    }

    @Override
    public void run() {

        Query runningJobsQuery = new Query()
                .append(JobDBAdaptor.QueryParams.STATUS_NAME.key(), Job.JobStatus.RUNNING)
                .append(JobDBAdaptor.QueryParams.TYPE.key(), "!=" + Job.Type.INDEX);
        Query queuedJobsQuery = new Query()
                .append(JobDBAdaptor.QueryParams.STATUS_NAME.key(), Job.JobStatus.QUEUED)
                .append(JobDBAdaptor.QueryParams.TYPE.key(), "!=" + Job.Type.INDEX);
        Query preparedJobsQuery = new Query()
                .append(JobDBAdaptor.QueryParams.STATUS_NAME.key(), Job.JobStatus.PREPARED)
                .append(JobDBAdaptor.QueryParams.TYPE.key(), "!=" + Job.Type.INDEX);
        // Sort jobs by creation date
        QueryOptions queryOptions = new QueryOptions()
                .append(QueryOptions.SORT, JobDBAdaptor.QueryParams.CREATION_DATE.key())
                .append(QueryOptions.ORDER, QueryOptions.ASCENDING);

        while (!exit) {
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                if (!exit) {
                    e.printStackTrace();
                }
            }
            logger.info("----- EXECUTION DAEMON -----", TimeUtils.getTimeMillis());

            /*
            RUNNING JOBS
             */
            try {
                QueryResult<Job> runningJobs = jobDBAdaptor.get(runningJobsQuery, queryOptions);
                logger.debug("Checking running jobs. {} running jobs found", runningJobs.getNumResults());
                for (Job job : runningJobs.getResult()) {
                    checkRunningJob(job);
                }
            } catch (CatalogException e) {
                e.printStackTrace();
            }

            /*
            QUEUED JOBS
             */
            try {
                QueryResult<Job> queuedJobs = jobDBAdaptor.get(queuedJobsQuery, queryOptions);
                logger.debug("Checking queued jobs. {} running jobs found", queuedJobs.getNumResults());
                for (Job job : queuedJobs.getResult()) {
                    checkQueuedJob(job);
                }
            } catch (CatalogException e) {
                e.printStackTrace();
            }

            /*
            PREPARED JOBS
             */
            try {
                QueryResult<Job> preparedJobs = jobDBAdaptor.get(preparedJobsQuery, queryOptions);
                logger.debug("Checking prepared jobs. {} running jobs found", preparedJobs.getNumResults());
                for (Job job : preparedJobs.getResult()) {
                    checkPreparedJob(job);
                }
            } catch (CatalogException e) {
                e.printStackTrace();
            }


        }
    }

    private void checkRunningJob(Job job) {
        logger.info("Updating job {} from {} to {}", job.getId(), Job.JobStatus.RUNNING, Job.JobStatus.READY);

        try {
            jobDBAdaptor.setStatus(job.getId(), Job.JobStatus.READY);
        } catch (CatalogException e) {
            logger.error("Could not update job {}. {}", job.getId(), e.getMessage());
            e.printStackTrace();
        }
    }

    private void checkQueuedJob(Job job) {
        logger.info("Updating job {} from {} to {}", job.getId(), Job.JobStatus.QUEUED, Job.JobStatus.RUNNING);

        try {
            logger.info("Running job {}" + job.getName());

            jobDBAdaptor.setStatus(job.getId(), Job.JobStatus.RUNNING);
        } catch (CatalogException e) {
            logger.error("Could not update job {}. {}", job.getId(), e.getMessage());
            e.printStackTrace();
        }
    }

    private void checkPreparedJob(Job job) {
        StringBuilder commandLine = new StringBuilder(binHome).append(job.getExecutable()).append(" ");
        for (Map.Entry<String, String> param : job.getParams().entrySet()) {
            commandLine
                    .append("--")
                    .append(param.getKey())
                    .append(" ")
                    .append(param.getValue())
                    .append(" ");
        }

        logger.info("Updating job {} from {} to {}", commandLine.toString(), Job.JobStatus.PREPARED, Job.JobStatus.QUEUED);

        try {
            jobDBAdaptor.setStatus(job.getId(), Job.JobStatus.QUEUED);
        } catch (CatalogException e) {
            logger.error("Could not update job {}. {}", job.getId(), e.getMessage());
            e.printStackTrace();
        }

    }



}
