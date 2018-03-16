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

import com.onescorpin.alerts.rest.model.AlertRange;
import com.onescorpin.alerts.rest.model.AlertSummaryGrouped;
import com.onescorpin.jobrepo.query.model.CheckDataJob;
import com.onescorpin.jobrepo.query.model.DataConfidenceSummary;
import com.onescorpin.jobrepo.query.model.NflowStatus;
import com.onescorpin.jobrepo.query.model.JobStatusCount;
import com.onescorpin.metadata.cache.CacheService;
import com.onescorpin.metadata.cache.CategoryNflowService;
import com.onescorpin.metadata.cache.Dashboard;
import com.onescorpin.metadata.cache.NflowHealthSummaryCache;
import com.onescorpin.rest.model.search.SearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;


@Api(tags = "Operations Manager - Dashboard", produces = "application/json")
@Path("/v1/dashboard")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    @Inject
    private CacheService cacheService;

    @Inject
    private CategoryNflowService categoryNflowService;


    @GET
    @Path("/data-confidence/summary")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the data confidence metrics.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the data confidence metrics.", response = DataConfidenceSummary.class)
    )
    public DataConfidenceSummary getDashbardDataConfidenceSummary() {
        List<CheckDataJob> checkDataJobs = cacheService.getUserDataConfidenceJobs();
        return new DataConfidenceSummary(checkDataJobs, 60);
    }


    @GET
    @Path("/nflows/nflow-name/{nflowName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Provides a detailed health status of every nflow.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the health.", response = NflowStatus.class)
    )
    public NflowStatus getNflowHealth(@PathParam("nflowName") String nflowName) {
        return cacheService.getUserNflowHealth(nflowName);
    }


    @GET
    @Path("/pageable-nflows")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Provides a detailed pageable response with the health status of every nflow.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the nflow health.", response = NflowStatus.class)
    )
    public SearchResult getPageableNflowHealth(@Context HttpServletRequest request, @QueryParam("sort") @DefaultValue("") String sort,
                                              @QueryParam("limit") @DefaultValue("10") Integer limit,
                                              @QueryParam("start") @DefaultValue("0") Integer start,
                                              @QueryParam("fixedFilter") String fixedFilter,
                                              @QueryParam("filter") String filter) {
        return cacheService.getUserNflowHealthWithFilter(new NflowHealthSummaryCache.NflowSummaryFilter(fixedFilter, filter, null, limit, start, sort));

    }

    @GET
    @Path("/nflow-health-counts")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Get a map of 'HEALTHY', 'UNHEALTHY' and the count of nflows in each group.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the health the nflows grouped by 'HEALTHY' and 'UNHEALTHY' as the keys to the returned Map.", response = Map.class)
    )
    public Map<String, Long> getNflowHealthCounts(@Context HttpServletRequest request) {
       return cacheService.getUserNflowHealthCounts();
    }

    @GET
    @Path("/running-jobs")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets running jobs for the last 10 seconds.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the stats.", response = JobStatusCount.class, responseContainer = "List")
    )
    public List<JobStatusCount> getRunningJobCounts() {
        return cacheService.getUserRunningJobs();
    }

    //TODO get Service status


    @GET
    @Path("/alerts")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Lists alerts")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns summary of the alerts grouped.", response = AlertRange.class)
    )
    public Collection<AlertSummaryGrouped> getAlertSummaryUnhandled() {
        return cacheService.getUserAlertSummary();
    }

    @GET
    @Path("/alerts/nflow-id/{nflowId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Get a summary of the unhandled alerts for a nflow by its id")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns summary of the unhandled alerts for a given nflow id", response = AlertSummaryGrouped.class, responseContainer = "Collection")
    )
    public Collection<AlertSummaryGrouped> getUserAlertSummaryForNflowId(@PathParam("nflowId") String nflowId) {
        return cacheService.getUserAlertSummaryForNflowId(nflowId);
    }

    @GET
    @Path("/alerts/nflow-name/{nflowName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Get a summary of the unhandled alerts for a nflow by its name (category.nflowname)")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns summary of the unhandled alerts for a given nflow name ", response = AlertSummaryGrouped.class, responseContainer = "Collection")
    )
    public Collection<AlertSummaryGrouped> getUserAlertSummaryForNflowName(@PathParam("nflowName") String nflowName) {
        return cacheService.getUserAlertSummaryForNflowName(nflowName);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Get dashboard containing service health,nflow health, data confidence, and unhandled alerts summary")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the dashboard.", response = Dashboard.class)
    )
    public Dashboard getDashboard(@Context HttpServletRequest request, @QueryParam("sort") @DefaultValue("") String sort,
                                  @QueryParam("limit") @DefaultValue("10") Integer limit,
                                  @QueryParam("start") @DefaultValue("0") Integer start,
                                  @QueryParam("fixedFilter") String fixedFilter,
                                  @QueryParam("filter") String filter) {
        return cacheService.getDashboard(new NflowHealthSummaryCache.NflowSummaryFilter(fixedFilter, filter, null, limit, start, sort));
    }


}
