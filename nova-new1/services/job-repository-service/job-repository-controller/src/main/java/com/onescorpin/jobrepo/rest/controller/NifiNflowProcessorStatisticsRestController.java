package com.onescorpin.jobrepo.rest.controller;

/*-
 * #%L
 * onescorpin-job-repository-controller
 * %%
 * Copyright (C) 2017 Onescorpin
 * %%
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
 * #L%
 */

import com.onescorpin.jobrepo.security.OperationsAccessControl;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowProcessorStatisticsProvider;
import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowProcessorStats;
import com.onescorpin.metadata.rest.jobrepo.nifi.NifiNflowProcessorStatsTransform;
import com.onescorpin.rest.model.LabelValue;
import com.onescorpin.security.AccessController;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api(tags = "Operations Manager - Nflows", produces = "application/json")
@Path("/v1/provenance-stats")
public class NifiNflowProcessorStatisticsRestController {

    @Inject
    private MetadataAccess metadataAccess;

    @Inject
    private AccessController accessController;

    @Autowired
    private NifiNflowProcessorStatisticsProvider statsProvider;

    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the provenance statistics for all nflows.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the provenance stats.", response = com.onescorpin.metadata.rest.jobrepo.nifi.NifiNflowProcessorStats.class, responseContainer = "List")
    )
    public Response findStats() {
        this.accessController.checkPermission(AccessController.SERVICES, OperationsAccessControl.ACCESS_OPS);
        return metadataAccess.read(() -> {
            List<? extends NifiNflowProcessorStats> list = statsProvider.findWithinTimeWindow(DateTime.now().minusDays(1), DateTime.now());
            List<com.onescorpin.metadata.rest.jobrepo.nifi.NifiNflowProcessorStats> model = NifiNflowProcessorStatsTransform.toModel(list);
            return Response.ok(model).build();
        });
    }

    @GET
    @Path("/{nflowName}/processor-duration/{timeframe}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the job duration for the specified nflow.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the job duration.", response = com.onescorpin.metadata.rest.jobrepo.nifi.NifiNflowProcessorStats.class, responseContainer = "List")
    )
    public Response findStats(@PathParam("nflowName") String nflowName, @PathParam("timeframe") @DefaultValue("THREE_MIN") NifiNflowProcessorStatisticsProvider.TimeFrame timeframe) {
        this.accessController.checkPermission(AccessController.SERVICES, OperationsAccessControl.ACCESS_OPS);
        return metadataAccess.read(() -> {
            List<? extends NifiNflowProcessorStats> list = statsProvider.findNflowProcessorStatisticsByProcessorName(nflowName, timeframe);
            List<com.onescorpin.metadata.rest.jobrepo.nifi.NifiNflowProcessorStats> model = NifiNflowProcessorStatsTransform.toModel(list);
            return Response.ok(model).build();
        });
    }

    @GET
    @Path("/{nflowName}/{timeframe}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the statistics for the specified nflow.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the nflow statistics.", response = com.onescorpin.metadata.rest.jobrepo.nifi.NifiNflowProcessorStats.class, responseContainer = "List")
    )
    public Response findNflowStats(@PathParam("nflowName") String nflowName, @PathParam("timeframe") @DefaultValue("THREE_MIN") NifiNflowProcessorStatisticsProvider.TimeFrame timeframe) {
        this.accessController.checkPermission(AccessController.SERVICES, OperationsAccessControl.ACCESS_OPS);
        return metadataAccess.read(() -> {

            List<? extends NifiNflowProcessorStats> list = statsProvider.findForNflowStatisticsGroupedByTime(nflowName, timeframe);
            List<com.onescorpin.metadata.rest.jobrepo.nifi.NifiNflowProcessorStats> model = NifiNflowProcessorStatsTransform.toModel(list);
            return Response.ok(model).build();
        });
    }

    @GET
    @Path("/time-frame-options")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the default time frame options.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the time frame options.", response = LabelValue.class, responseContainer = "List")
    )
    public Response getTimeFrameOptions() {
        List<LabelValue> vals = Arrays.stream(NifiNflowProcessorStatisticsProvider.TimeFrame.values())
            .map(timeFrame -> new LabelValue(timeFrame.getDisplayName(), timeFrame.name()))
            .collect(Collectors.toList());
        return Response.ok(vals).build();
    }
}
