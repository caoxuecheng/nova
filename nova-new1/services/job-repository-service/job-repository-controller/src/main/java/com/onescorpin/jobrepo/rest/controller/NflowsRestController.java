package com.onescorpin.jobrepo.rest.controller;

import com.google.common.collect.Lists;
import com.onescorpin.DateTimeUtil;
import com.onescorpin.alerts.api.Alert;
import com.onescorpin.alerts.api.AlertCriteria;
import com.onescorpin.alerts.api.AlertProvider;
import com.onescorpin.alerts.api.AlertSummary;
import com.onescorpin.alerts.rest.AlertsModel;
import com.onescorpin.alerts.rest.model.AlertSummaryGrouped;
import com.onescorpin.jobrepo.query.model.ExecutedNflow;
import com.onescorpin.jobrepo.query.model.NflowHealth;
import com.onescorpin.jobrepo.query.model.NflowStatus;
import com.onescorpin.jobrepo.query.model.JobStatusCount;
import com.onescorpin.jobrepo.query.model.transform.NflowModelTransform;
import com.onescorpin.jobrepo.query.model.transform.JobStatusTransform;
import com.onescorpin.jobrepo.security.OperationsAccessControl;
import com.onescorpin.metadata.alerts.NovaEntityAwareAlertManager;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.metadata.api.nflow.OpsManagerNflowProvider;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobExecution;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobExecutionProvider;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreementDescription;
import com.onescorpin.metadata.api.sla.ServiceLevelAgreementDescriptionProvider;
import com.onescorpin.metadata.jpa.nflow.OpsNflowManagerNflowProvider;
import com.onescorpin.rest.model.RestResponseStatus;
import com.onescorpin.security.AccessController;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
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

@Api(tags = "Operations Manager - Nflows", produces = "application/json")
@Path("/v1/nflows")
public class NflowsRestController {

    @Inject
    OpsManagerNflowProvider opsNflowManagerNflowProvider;

    @Inject
    BatchJobExecutionProvider batchJobExecutionProvider;

    @Inject
    private AccessController accessController;

    @Inject
    private MetadataAccess metadataAccess;

    @Inject
    private ServiceLevelAgreementDescriptionProvider serviceLevelAgreementDescriptionProvider;


    @Inject
    private AlertProvider alertProvider;

    @Inject
    private NovaEntityAwareAlertManager novaEntityAwareAlertService;

    @Inject
    private AlertsModel alertsModel;

    @GET
    @Path("/{nflowName}/latest")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the latest execution of the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the latest execution.", response = ExecutedNflow.class),
                      @ApiResponse(code = 400, message = "The nflow does not exist or has no jobs.", response = RestResponseStatus.class)
                  })
    public ExecutedNflow findLatestNflowsByName(@PathParam("nflowName") String nflowName, @Context HttpServletRequest request) {
        accessController.checkPermission(AccessController.SERVICES, OperationsAccessControl.ACCESS_OPS);
        return metadataAccess.read(() -> {
            final BatchJobExecution latestJob = batchJobExecutionProvider.findLatestCompletedJobForNflow(nflowName);
            if (latestJob != null) {
                final OpsManagerNflow nflow = opsNflowManagerNflowProvider.findByName(nflowName);
                return NflowModelTransform.executedNflow(latestJob, nflow);
            } else {
                throw new NotFoundException();
            }
        });
    }

    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Provides a detailed health status of every nflow.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the health.", response = NflowStatus.class)
    )
    public NflowStatus getNflowHealth(@Context HttpServletRequest request) {

        this.accessController.checkPermission(AccessController.SERVICES, OperationsAccessControl.ACCESS_OPS);
        return metadataAccess.read(() -> {
            List<NflowHealth> nflowHealth = getNflowHealthCounts(request);
            NflowStatus status = NflowModelTransform.nflowStatus(nflowHealth);
            return status;
        });
    }

    @GET
    @Path("/health-count")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Provides a summarized health status of every nflow.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the health.", response = NflowHealth.class, responseContainer = "List")
    )
    public List<NflowHealth> getNflowHealthCounts(@Context HttpServletRequest request) {
        this.accessController.checkPermission(AccessController.SERVICES, OperationsAccessControl.ACCESS_OPS);
        return metadataAccess.read(() -> {
            List<? extends com.onescorpin.metadata.api.nflow.NflowHealth> nflowHealthList = opsNflowManagerNflowProvider.getNflowHealth();
            //Transform to list
            return NflowModelTransform.nflowHealth(nflowHealthList);
        });
    }

    @GET
    @Path("/health-count/{nflowName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets a health summary for the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the health.", response = NflowHealth.class),
                      @ApiResponse(code = 404, message = "The nflow does not exist.", response = RestResponseStatus.class)
                  })
    public NflowHealth getNflowHealthCounts(@Context HttpServletRequest request, @PathParam("nflowName") String nflowName) {
        this.accessController.checkPermission(AccessController.SERVICES, OperationsAccessControl.ACCESS_OPS);
        return metadataAccess.read(() -> {
            final com.onescorpin.metadata.api.nflow.NflowHealth nflowHealth = opsNflowManagerNflowProvider.getNflowHealth(nflowName);
            if (nflowHealth != null) {
                return NflowModelTransform.nflowHealth(nflowHealth);
            }

            final OpsManagerNflow nflow = opsNflowManagerNflowProvider.findByName(nflowName);
            if (nflow != null) {
                return NflowModelTransform.nflowHealth(nflow);
            } else {
                throw new NotFoundException();
            }
        });
    }

    @GET
    @Path("/health/{nflowName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the detailed health status of the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the health.", response = NflowStatus.class),
                      @ApiResponse(code = 404, message = "The nflow does not exist.", response = RestResponseStatus.class)
                  })
    public NflowStatus getNflowHealthForNflow(@Context HttpServletRequest request, @PathParam("nflowName") String nflowName) {

        this.accessController.checkPermission(AccessController.SERVICES, OperationsAccessControl.ACCESS_OPS);
        return metadataAccess.read(() -> {
            final NflowHealth nflowHealth = getNflowHealthCounts(request, nflowName);
            if (nflowHealth != null) {
                return NflowModelTransform.nflowStatus(Lists.newArrayList(nflowHealth));
            }

            final OpsManagerNflow nflow = opsNflowManagerNflowProvider.findByName(nflowName);
            if (nflow != null) {
                return NflowModelTransform.nflowStatus(nflow);
            } else {
                throw new NotFoundException();
            }
        });
    }

    @GET
    @Path("/{nflowName}/daily-status-count")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets a daily health summary for the specified nflow.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the health.", response = JobStatusCount.class, responseContainer = "List")
    )
    public List<JobStatusCount> findNflowDailyStatusCount(@PathParam("nflowName") String nflowName,
                                                         @QueryParam("period") String periodString) {
        this.accessController.checkPermission(AccessController.SERVICES, OperationsAccessControl.ACCESS_OPS);
        return metadataAccess.read(() -> {
            Period period = DateTimeUtil.period(periodString);
            List<com.onescorpin.metadata.api.jobrepo.job.JobStatusCount> counts = opsNflowManagerNflowProvider.getJobStatusCountByDateFromNow(nflowName, period);
            if (counts != null) {
                List<JobStatusCount> jobStatusCounts = counts.stream().map(c -> JobStatusTransform.jobStatusCount(c)).collect(Collectors.toList());
                JobStatusTransform.ensureDateFromPeriodExists(jobStatusCounts, period);
                return jobStatusCounts;
            } else {
                return Collections.emptyList();
            }
        });
    }

    @GET
    @Path("/names")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the name of every nflow.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the nflow names.", response = String.class, responseContainer = "List")
    )
    public List<String> getNflowNames() {
        this.accessController.checkPermission(AccessController.SERVICES, OperationsAccessControl.ACCESS_OPS);
        return metadataAccess.read(() -> opsNflowManagerNflowProvider.getNflowNames());
    }


    @GET
    @Path("/query/{nflowId}")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation("Gets the name of the nflow matching the nflowId.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the nflow name.", response = String.class)
    )
    public String getNflowName(@PathParam("nflowId") String nflowId) {
        return metadataAccess.read(() -> {
            String filter="id.uuid=="+nflowId;
            List<OpsManagerNflow> nflows = ((OpsNflowManagerNflowProvider)opsNflowManagerNflowProvider).findNflowsWithFilter(filter);
            if(nflows != null){
                return nflows.stream().map(f->f.getName()).collect(Collectors.joining(","));
            }
            return "NOT FOUND";
        });
    }



    /**
     * Get alerts associated to the nflow
     * Combine both SLA and Nflow alerts
     * @param nflowId
     * @return
     */
    @GET
    @Path("/{nflowName}/alerts")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the name of every nflow.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the nflow names.", response = String.class, responseContainer = "List")
    )
    public Collection<AlertSummaryGrouped> getNflowAlerts(@PathParam("nflowName") String nflowName,@QueryParam("nflowId") String nflowId){
        return getAlerts(nflowName,nflowId);
    }

    private Collection<AlertSummaryGrouped> getAlerts(final String nflowName, final String nflowId){
       return  metadataAccess.read(() -> {

           String derivedNflowId = nflowId;
            //get necessary nflow info
            if (StringUtils.isBlank(nflowId) && StringUtils.isNotBlank(nflowName)) {
                //get the nflowId for this nflow name
                OpsManagerNflow nflow = opsNflowManagerNflowProvider.findByName(nflowName);
                if (nflow != null) {
                    derivedNflowId = nflow.getId().toString();
                }
            }

            if (StringUtils.isBlank(derivedNflowId)) {
                return Collections.emptyList();
            }

            List<? extends ServiceLevelAgreementDescription> slas = serviceLevelAgreementDescriptionProvider.findForNflow(opsNflowManagerNflowProvider.resolveId(derivedNflowId));
            List<String> slaIds = new ArrayList<>();
            if (slas != null && !slas.isEmpty()) {
                slaIds = slas.stream().map(sla -> sla.getSlaId().toString()).collect(Collectors.toList());
            }
            List<String> ids = new ArrayList<>();
            ids.addAll(slaIds);
            ids.add(derivedNflowId);
            String filter = ids.stream().collect(Collectors.joining("||"));

            List<AlertSummary> alerts = new ArrayList<>();
            AlertCriteria criteria = alertProvider.criteria().state(Alert.State.UNHANDLED).orFilter(filter);
            alertProvider.getAlertsSummary(criteria).forEachRemaining(alerts::add);

            return alertsModel.groupAlertSummaries(alerts);
        });
    }
}
