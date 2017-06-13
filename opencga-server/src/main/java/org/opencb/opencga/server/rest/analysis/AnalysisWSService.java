/*
 * Copyright 2015-2016 OpenCB
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

package org.opencb.opencga.server.rest.analysis;

import org.opencb.opencga.catalog.managers.api.IJobManager;
import org.opencb.opencga.catalog.models.Job;
import org.opencb.opencga.core.exception.VersionException;
import org.opencb.opencga.server.rest.OpenCGAWSServer;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

/**
 * Created by imedina on 17/08/16.
 */
@Path("/{version}/analysis")
@Produces(MediaType.APPLICATION_JSON)
public class AnalysisWSService extends OpenCGAWSServer {

    protected IJobManager jobManager;

    public AnalysisWSService(@Context UriInfo uriInfo, @Context HttpServletRequest httpServletRequest, @Context HttpHeaders httpHeaders)
            throws IOException, VersionException {
        this(uriInfo.getPathParameters().getFirst("version"), uriInfo, httpServletRequest, httpHeaders);
    }

    public AnalysisWSService(String version, @Context UriInfo uriInfo, @Context HttpServletRequest httpServletRequest, @Context HttpHeaders httpHeaders)
            throws IOException, VersionException {
        super(version, uriInfo, httpServletRequest, httpHeaders);

        this.jobManager = catalogManager.getJobManager();
    }

    protected Job createJob() {
        return null;
    }

}
