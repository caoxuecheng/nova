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
import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowProcessorErrors;
import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowProcessorStatisticsProvider;
import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowProcessorStats;
import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowStatisticsProvider;
import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowStats;
import com.onescorpin.metadata.jobrepo.nifi.provenance.NifiStatsJmsReceiver;
import com.onescorpin.metadata.rest.jobrepo.nifi.NiFiNflowProcessorErrorsContainer;
import com.onescorpin.metadata.rest.jobrepo.nifi.NiFiNflowProcessorStatsContainer;
import com.onescorpin.metadata.rest.jobrepo.nifi.NifiNflowProcessorStatsTransform;
import com.onescorpin.rest.model.LabelValue;
import com.onescorpin.security.AccessController;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api(tags = "Operations Manager - Nflows", produces = "application/json")
@Path("/v2/provenance-stats")
public class NifiNflowProcessorStatisticsRestControllerV2 {

    /**
     * Maximum number of processor statistics to be returned to client, otherwise server would do
     * unnecessary aggregations and clients may get overloaded anyway
     **/
    private static final Integer MAX_DATA_POINTS = 6400;

    @Inject
    private MetadataAccess metadataAccess;

    @Inject
    private AccessController accessController;

    @Autowired
    private NifiNflowProcessorStatisticsProvider statsProvider;

    @Inject
    NifiNflowStatisticsProvider nifiNflowStatisticsProvider;


    @Inject
    NifiStatsJmsReceiver nifiStatsJmsReceiver;

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
    @Path("/{nflowName}/processor-duration")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Returns the list of stats for each processor within the given timeframe relative to now")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the list of stats for each processor within the given timeframe relative to now",
                     response = com.onescorpin.metadata.rest.jobrepo.nifi.NifiNflowProcessorStats.class, responseContainer = "List")
    )
    public Response findStats(@PathParam("nflowName") String nflowName, @QueryParam("from") Long fromMillis, @QueryParam("to") Long toMillis) {
        this.accessController.checkPermission(AccessController.SERVICES, OperationsAccessControl.ACCESS_OPS);
        final DateTime endTime = getToDateTime(toMillis);
        final DateTime startTime = getFromDateTime(fromMillis);
        return metadataAccess.read(() -> {
            NiFiNflowProcessorStatsContainer statsContainer = new NiFiNflowProcessorStatsContainer(startTime, endTime);
            List<? extends NifiNflowProcessorStats> list = statsProvider.findNflowProcessorStatisticsByProcessorName(nflowName, statsContainer.getStartTime(), statsContainer.getEndTime());
            List<? extends NifiNflowProcessorErrors> errors = statsProvider.findNflowProcessorErrors(nflowName, statsContainer.getStartTime(), statsContainer.getEndTime());
            List<com.onescorpin.metadata.rest.jobrepo.nifi.NifiNflowProcessorStats> model = NifiNflowProcessorStatsTransform.toModel(list);
            statsContainer.setStats(model);
            return Response.ok(statsContainer).build();
        });
    }


    @GET
    @Path("/{nflowName}/processor-errors")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Returns the list of stats for each processor within the given timeframe relative to now")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the list of stats for each processor within the given timeframe relative to now",
                     response = com.onescorpin.metadata.rest.jobrepo.nifi.NifiNflowProcessorStats.class, responseContainer = "List")
    )
    public Response findNflowProcessorErrors(@PathParam("nflowName") String nflowName, @QueryParam("from") Long fromMillis, @QueryParam("to") Long toMillis,
                                            @QueryParam("after") Long timestamp) {
        this.accessController.checkPermission(AccessController.SERVICES, OperationsAccessControl.ACCESS_OPS);

        final DateTime endTime = getToDateTime(toMillis);
        final DateTime startTime = getFromDateTime(fromMillis);

        NiFiNflowProcessorErrorsContainer container = new NiFiNflowProcessorErrorsContainer(startTime, endTime);
        List<? extends NifiNflowProcessorErrors> errors = null;
        if (nifiStatsJmsReceiver.isPersistErrors()) {
            errors = metadataAccess.read(() -> {
                if (timestamp != null && timestamp != 0L) {
                    return statsProvider.findNflowProcessorErrorsAfter(nflowName, new DateTime(timestamp));
                } else {
                    return statsProvider.findNflowProcessorErrors(nflowName, startTime, endTime);
                }
            });
        } else {
            if (timestamp != null && timestamp != 0L) {
                errors = nifiStatsJmsReceiver.getErrorsForNflow(nflowName, timestamp);
            } else {
                errors = nifiStatsJmsReceiver.getErrorsForNflow(nflowName, startTime.getMillis(), endTime.getMillis());
            }
        }
        List<com.onescorpin.metadata.rest.jobrepo.nifi.NifiNflowProcessorStatsErrors> errorsModel = NifiNflowProcessorStatsTransform.toErrorsModel(errors);
        container.setErrors(errorsModel);
        return Response.ok(container).build();


    }


    @GET
    @Path("/{nflowName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the statistics for the specified nflow.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the nflow statistics.", response = com.onescorpin.metadata.rest.jobrepo.nifi.NifiNflowProcessorStats.class, responseContainer = "List")
    )
    public Response findNflowStats(@PathParam("nflowName") String nflowName, @QueryParam("from") Long fromMillis, @QueryParam("to") Long toMillis) {
        this.accessController.checkPermission(AccessController.SERVICES, OperationsAccessControl.ACCESS_OPS);

        final DateTime endTime = getToDateTime(toMillis);
        final DateTime startTime = getFromDateTime(fromMillis);

        return metadataAccess.read(() -> {
            NiFiNflowProcessorStatsContainer statsContainer = new NiFiNflowProcessorStatsContainer(startTime, endTime);
            NifiNflowStats nflowStats = nifiNflowStatisticsProvider.findLatestStatsForNflow(nflowName);

            List<? extends NifiNflowProcessorStats> list = statsProvider.findForNflowStatisticsGroupedByTime(nflowName, statsContainer.getStartTime(), statsContainer.getEndTime());
            List<com.onescorpin.metadata.rest.jobrepo.nifi.NifiNflowProcessorStats> model = NifiNflowProcessorStatsTransform.toModel(list);

            statsContainer.setStats(model);
            if (nflowStats != null) {
                statsContainer.setRunningFlows(nflowStats.getRunningNflowFlows());
            } else {
                //calc diff from finished - started
                Long started = model.stream().mapToLong(s -> s.getJobsStarted()).sum();
                Long finished = model.stream().mapToLong(s -> s.getJobsFinished()).sum();
                Long running = started - finished;
                if (running < 0) {
                    running = 0L;
                }
                statsContainer.setRunningFlows(running);
            }
            return Response.ok(statsContainer).build();
        });
    }

    private DateTime getToDateTime(Long toMillis) {
        DateTime toDate;
        if (toMillis == null) {
            toDate = DateTime.now();
        } else {
            toDate = new DateTime(toMillis);
        }
        return toDate;
    }

    private DateTime getFromDateTime(Long fromMillis) {
        DateTime fromDate;
        if (fromMillis == null) {
            fromDate = DateTime.now().minusMinutes(5);
        } else {
            fromDate = new DateTime(fromMillis);
        }
        return fromDate;
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
            .map(timeFrame -> {
                LabelValue label = new LabelValue(timeFrame.getDisplayName(), timeFrame.name());
                Map<String, Object> properties = new HashMap<>(1);
                properties.put("millis", timeFrame.getMillis());
                label.setProperties(properties);
                return label;
            })
            .collect(Collectors.toList());
        return Response.ok(vals).build();
    }
}
