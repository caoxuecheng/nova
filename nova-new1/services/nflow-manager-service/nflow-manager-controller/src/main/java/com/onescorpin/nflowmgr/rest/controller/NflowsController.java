package com.onescorpin.nflowmgr.rest.controller;

/*-
 * #%L
 * onescorpin-nflow-manager-controller
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

import com.onescorpin.nflowmgr.rest.NflowLineageBuilder;
import com.onescorpin.nflowmgr.rest.Model;
import com.onescorpin.nflowmgr.security.NflowServicesAccessControl;
import com.onescorpin.nflowmgr.service.MetadataModelTransform;
import com.onescorpin.nflowmgr.service.datasource.DatasourceModelTransform;
import com.onescorpin.nflowmgr.service.datasource.DatasourceService;
import com.onescorpin.nflowmgr.service.security.SecurityService;
import com.onescorpin.nflowmgr.sla.ServiceLevelAgreementModelTransform;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.datasource.Datasource;
import com.onescorpin.metadata.api.datasource.DatasourceProvider;
import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.api.op.NflowDependencyDeltaResults;
import com.onescorpin.metadata.api.op.NflowOperationsProvider;
import com.onescorpin.metadata.core.nflow.NflowPreconditionService;
import com.onescorpin.metadata.rest.model.nflow.Nflow;
import com.onescorpin.metadata.rest.model.nflow.NflowCriteria;
import com.onescorpin.metadata.rest.model.nflow.NflowDependencyGraph;
import com.onescorpin.metadata.rest.model.nflow.NflowDestination;
import com.onescorpin.metadata.rest.model.nflow.NflowLineage;
import com.onescorpin.metadata.rest.model.nflow.NflowPrecondition;
import com.onescorpin.metadata.rest.model.nflow.NflowSource;
import com.onescorpin.metadata.rest.model.nflow.InitializationStatus;
import com.onescorpin.metadata.rest.model.sla.ServiceLevelAssessment;
import com.onescorpin.rest.model.RestResponseStatus;
import com.onescorpin.security.AccessController;
import com.onescorpin.security.action.AllowedEntityActionsProvider;
import com.onescorpin.security.rest.controller.SecurityModelTransform;
import com.onescorpin.security.rest.model.ActionGroup;
import com.onescorpin.security.rest.model.PermissionsChange;
import com.onescorpin.security.rest.model.PermissionsChange.ChangeType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Manages Nflow Metadata and allows nflows to be updated with various Metadata Properties.
 */
@Component
@Api(tags = "Nflow Manager - Nflows", produces = "application/json")
@Path("/v1/metadata/nflow")
public class NflowsController {

    private static final Logger LOG = LoggerFactory.getLogger(NflowsController.class);

    @Inject
    private NflowProvider nflowProvider;

    @Inject
    private NflowOperationsProvider nflowOpsProvider;

    @Inject
    private DatasourceProvider datasetProvider;

    @Inject
    private DatasourceService datasourceService;

    @Inject
    private AllowedEntityActionsProvider actionsProvider;

    @Inject
    private NflowPreconditionService preconditionService;

    @Inject
    private SecurityService securityService;

    @Inject
    private MetadataAccess metadata;

    @Inject
    private MetadataModelTransform metadataTransform;

    @Inject
    private SecurityModelTransform actionsTransform;

    @Inject
    private AccessController accessController;

    @Inject
    private Model model;

    @Inject
    private DatasourceModelTransform datasourceTransform;


    @GET
    @Path("{id}/actions/available")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the list of available actions that may be permitted or revoked on a nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the actions.", response = ActionGroup.class),
                      @ApiResponse(code = 404, message = "A nflow with the given ID does not exist.", response = RestResponseStatus.class)
                  })
    public ActionGroup getAvailableActions(@PathParam("id") String nflowIdStr) {
        LOG.debug("Get available actions for nflow: {}", nflowIdStr);

        return this.securityService.getAvailableNflowActions(nflowIdStr)
            .orElseThrow(() -> new WebApplicationException("A nflow with the given ID does not exist: " + nflowIdStr, Status.NOT_FOUND));
    }

    @GET
    @Path("{id}/actions/allowed")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the list of actions permitted for the given username and/or groups.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the actions.", response = ActionGroup.class),
                      @ApiResponse(code = 404, message = "A nflow with the given ID does not exist.", response = RestResponseStatus.class)
                  })
    public ActionGroup getAllowedActions(@PathParam("id") String nflowIdStr,
                                         @QueryParam("user") Set<String> userNames,
                                         @QueryParam("group") Set<String> groupNames) {
        LOG.debug("Get allowed actions for nflow: {}", nflowIdStr);

        Set<? extends Principal> users = Arrays.stream(this.actionsTransform.asUserPrincipals(userNames)).collect(Collectors.toSet());
        Set<? extends Principal> groups = Arrays.stream(this.actionsTransform.asGroupPrincipals(groupNames)).collect(Collectors.toSet());

        return this.securityService.getAllowedNflowActions(nflowIdStr, Stream.concat(users.stream(), groups.stream()).collect(Collectors.toSet()))
            .orElseThrow(() -> new WebApplicationException("A nflow with the given ID does not exist: " + nflowIdStr, Status.NOT_FOUND));
    }

    @POST
    @Path("{id}/actions/allowed")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Updates the permissions for a nflow using the supplied permission change request.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "The permissions were changed successfully.", response = ActionGroup.class),
                      @ApiResponse(code = 400, message = "The type is not valid.", response = RestResponseStatus.class),
                      @ApiResponse(code = 404, message = "No nflow exists with the specified ID.", response = RestResponseStatus.class)
                  })
    public ActionGroup postPermissionsChange(@PathParam("id") String nflowIdStr,
                                             PermissionsChange changes) {

        return this.securityService.changeNflowPermissions(nflowIdStr, changes)
            .orElseThrow(() -> new WebApplicationException("A nflow with the given ID does not exist: " + nflowIdStr, Status.NOT_FOUND));
    }

    @GET
    @Path("{id}/actions/change/allowed")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Constructs and returns a permission change request for a set of users/groups containing the actions that the requester may permit or revoke.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the change request that may be modified by the client and re-posted.", response = PermissionsChange.class),
                      @ApiResponse(code = 400, message = "The type is not valid.", response = RestResponseStatus.class),
                      @ApiResponse(code = 404, message = "No nflow exists with the specified ID.", response = RestResponseStatus.class)
                  })
    public PermissionsChange getAllowedPermissionsChange(@PathParam("id") String nflowIdStr,
                                                         @QueryParam("type") String changeType,
                                                         @QueryParam("user") Set<String> userNames,
                                                         @QueryParam("group") Set<String> groupNames) {
        if (StringUtils.isBlank(changeType)) {
            throw new WebApplicationException("The query parameter \"type\" is required", Status.BAD_REQUEST);
        }

        Set<? extends Principal> users = Arrays.stream(this.actionsTransform.asUserPrincipals(userNames)).collect(Collectors.toSet());
        Set<? extends Principal> groups = Arrays.stream(this.actionsTransform.asGroupPrincipals(groupNames)).collect(Collectors.toSet());

        return this.securityService.createNflowPermissionChange(nflowIdStr,
                                                               ChangeType.valueOf(changeType.toUpperCase()),
                                                               Stream.concat(users.stream(), groups.stream()).collect(Collectors.toSet()))
            .orElseThrow(() -> new WebApplicationException("A nflow with the given ID does not exist: " + nflowIdStr, Status.NOT_FOUND));
    }

    @GET
    @Path("{id}/initstatus")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the registration status for the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the registration status.", response = InitializationStatus.class),
                      @ApiResponse(code = 404, message = "The nflow could not be found.", response = RestResponseStatus.class)
                  })
    public InitializationStatus getInitializationStatus(@PathParam("id") String nflowIdStr) {
        LOG.debug("Get nflow initialization status {}", nflowIdStr);

        return this.metadata.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            com.onescorpin.metadata.api.nflow.Nflow.ID nflowId = nflowProvider.resolveNflow(nflowIdStr);
            com.onescorpin.metadata.api.nflow.Nflow nflow = nflowProvider.getNflow(nflowId);

            if (nflow != null) {
                return metadataTransform.domainToInitStatus().apply(nflow.getCurrentInitStatus());
            } else {
                throw new WebApplicationException("A nflow with the given ID does not exist: " + nflowId, Status.NOT_FOUND);
            }
        });
    }

    @GET
    @Path("{id}/initstatus/history")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the registration history for the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the registration history.", response = InitializationStatus.class, responseContainer = "List"),
                      @ApiResponse(code = 404, message = "The nflow could not be found.", response = RestResponseStatus.class)
                  })
    public List<InitializationStatus> getInitializationStatusHistory(@PathParam("id") String nflowIdStr) {
        LOG.debug("Get nflow initialization history {}", nflowIdStr);

        return this.metadata.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            com.onescorpin.metadata.api.nflow.Nflow.ID nflowId = nflowProvider.resolveNflow(nflowIdStr);
            com.onescorpin.metadata.api.nflow.Nflow nflow = nflowProvider.getNflow(nflowId);

            if (nflow != null) {
                return nflow.getInitHistory().stream().map(metadataTransform.domainToInitStatus()).collect(Collectors.toList());
            } else {
                throw new WebApplicationException("A nflow with the given ID does not exist: " + nflowId, Status.NOT_FOUND);
            }
        });
    }

    @PUT
    @Path("{id}/initstatus")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation("Sets the registration status for the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 204, message = "The registration status was updated."),
                      @ApiResponse(code = 404, message = "The nflow could not be found.", response = RestResponseStatus.class),
                      @ApiResponse(code = 500, message = "The registration status could not be updated.", response = RestResponseStatus.class)
                  })
    public void putInitializationStatus(@PathParam("id") String nflowIdStr,
                                        InitializationStatus status) {
        LOG.debug("Get nflow initialization status {}", nflowIdStr);

        this.metadata.commit(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            com.onescorpin.metadata.api.nflow.Nflow.ID nflowId = nflowProvider.resolveNflow(nflowIdStr);
            com.onescorpin.metadata.api.nflow.Nflow nflow = nflowProvider.getNflow(nflowId);

            if (nflow != null) {
                com.onescorpin.metadata.api.nflow.InitializationStatus.State newState
                    = com.onescorpin.metadata.api.nflow.InitializationStatus.State.valueOf(status.getState().name());
                nflow.updateInitStatus(new com.onescorpin.metadata.api.nflow.InitializationStatus(newState));
            } else {
                throw new WebApplicationException("A nflow with the given ID does not exist: " + nflowId, Status.NOT_FOUND);
            }
        });
    }

    @GET
    @Path("{id}/watermark")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the HighWaterMarks used by the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the HighWaterMark names.", response = String.class, responseContainer = "List"),
                      @ApiResponse(code = 404, message = "The nflow could not be found.", response = RestResponseStatus.class)
                  })
    public List<String> getHighWaterMarks(@PathParam("id") String nflowIdStr) {
        LOG.debug("Get nflow watermarks {}", nflowIdStr);

        return this.metadata.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            com.onescorpin.metadata.api.nflow.Nflow.ID nflowId = nflowProvider.resolveNflow(nflowIdStr);
            com.onescorpin.metadata.api.nflow.Nflow nflow = nflowProvider.getNflow(nflowId);

            if (nflow != null) {
                List<String> list = nflow.getWaterMarkNames().stream().collect(Collectors.toList());
                Collections.sort(list);
                return list;
            } else {
                throw new WebApplicationException("A nflow with the given ID does not exist: " + nflowId, Status.NOT_FOUND);
            }
        });
    }

    @GET
    @Path("{id}/watermark/{name}")
    @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
    @ApiOperation("Gets the value for a specific HighWaterMark.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the HighWaterMark value.", response = String.class),
                      @ApiResponse(code = 404, message = "The HighWaterMark could not be found.", response = RestResponseStatus.class)
                  })
    public String getHighWaterMark(@PathParam("id") String nflowIdStr,
                                   @PathParam("name") String waterMarkName) {
        LOG.debug("Get nflow watermark {}: {}", nflowIdStr, waterMarkName);

        return this.metadata.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            com.onescorpin.metadata.api.nflow.Nflow.ID nflowId = nflowProvider.resolveNflow(nflowIdStr);
            com.onescorpin.metadata.api.nflow.Nflow nflow = nflowProvider.getNflow(nflowId);

            if (nflow != null) {
                return nflow.getWaterMarkValue(waterMarkName)
                    .orElseThrow(() -> new WebApplicationException("A nflow high-water mark with the given name does not exist: " + waterMarkName, Status.NOT_FOUND));
            } else {
                throw new WebApplicationException("A nflow with the given ID does not exist: " + nflowId, Status.NOT_FOUND);
            }
        });
    }

    @PUT
    @Path("{id}/watermark/{name}")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation("Sets the value for a specific HighWaterMark.")
    @ApiResponses({
                      @ApiResponse(code = 204, message = "The HighWaterMark value has been changed."),
                      @ApiResponse(code = 404, message = "The nflow could not be found.", response = RestResponseStatus.class),
                      @ApiResponse(code = 500, message = "The HighWaterMark value could not be changed.", response = RestResponseStatus.class)
                  })
    public void putHighWaterMark(@PathParam("id") String nflowIdStr,
                                 @PathParam("name") String waterMarkName,
                                 String value) {
        LOG.debug("Get nflow watermark {}: {}", nflowIdStr, waterMarkName);

        this.metadata.commit(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            com.onescorpin.metadata.api.nflow.Nflow.ID nflowId = nflowProvider.resolveNflow(nflowIdStr);
            com.onescorpin.metadata.api.nflow.Nflow nflow = nflowProvider.getNflow(nflowId);

            if (nflow != null) {
                nflow.setWaterMarkValue(waterMarkName, value);
            } else {
                throw new WebApplicationException("A nflow with the given ID does not exist: " + nflowId, Status.NOT_FOUND);
            }
        });
    }

    @DELETE
    @Path("{id}/watermark/{name}")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation("Deletes the specified HighWaterMark.")
    @ApiResponses({
                      @ApiResponse(code = 204, message = "The HighWaterMark has been deleted."),
                      @ApiResponse(code = 404, message = "The nflow could not be found.", response = RestResponseStatus.class),
                      @ApiResponse(code = 500, message = "The HighWaterMark could not be deleted.", response = RestResponseStatus.class)
                  })
    public void deleteHighWaterMark(@PathParam("id") String nflowIdStr,
                                    @PathParam("name") String waterMarkName) {
        LOG.debug("Get nflow watermark {}: {}", nflowIdStr, waterMarkName);

        this.metadata.commit(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            com.onescorpin.metadata.api.nflow.Nflow.ID nflowId = nflowProvider.resolveNflow(nflowIdStr);
            com.onescorpin.metadata.api.nflow.Nflow nflow = nflowProvider.getNflow(nflowId);

            if (nflow != null) {
                nflow.setWaterMarkValue(waterMarkName, null);
            } else {
                throw new WebApplicationException("A nflow with the given ID does not exist: " + nflowId, Status.NOT_FOUND);
            }
        });
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets a list of nflows.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the matching nflows.", response = Nflow.class, responseContainer = "List")
    )
    public List<Nflow> getNflows(@QueryParam(NflowCriteria.CATEGORY) final String category,
                               @QueryParam(NflowCriteria.NAME) final String name,
                               @QueryParam(NflowCriteria.SRC_ID) final String srcId,
                               @QueryParam(NflowCriteria.DEST_ID) final String destId) {
        LOG.debug("Get nflows {}/{}/{}", name, srcId, destId);

        return this.metadata.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            com.onescorpin.metadata.api.nflow.NflowCriteria criteria = createNflowCriteria(category, name, srcId, destId);
            Collection<com.onescorpin.metadata.api.nflow.Nflow> domainNflows = nflowProvider.getNflows(criteria);
            return domainNflows.stream().map(metadataTransform.domainToNflow()).collect(Collectors.toList());
        });
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the nflow.", response = Nflow.class),
                      @ApiResponse(code = 400, message = "The id is not a valid UUID.", response = RestResponseStatus.class),
                      @ApiResponse(code = 500, message = "The nflow could not be found.", response = RestResponseStatus.class)
                  })
    public Nflow getNflow(@PathParam("id") final String nflowId) {
        LOG.debug("Get nflow {}", nflowId);

        return this.metadata.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            com.onescorpin.metadata.api.nflow.Nflow.ID domainId = nflowProvider.resolveNflow(nflowId);
            com.onescorpin.metadata.api.nflow.Nflow domain = nflowProvider.getNflow(domainId);

            return this.metadataTransform.domainToNflow().apply(domain);
        });
    }

    /*
    @GET
    @Path("{id}/op")
    @Produces(MediaType.APPLICATION_JSON)
    public List<NflowOperation> getNflowOperations(@PathParam("id") final String nflowId,
                                                 @QueryParam("since") @DefaultValue("1970-01-01T00:00:00Z") String sinceStr,
                                                 @QueryParam("limit") @DefaultValue("-1") int limit) {
        final DateTime since = Formatters.parseDateTime(sinceStr);

        return this.metadata.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            com.onescorpin.metadata.api.nflow.Nflow.ID domainId = nflowProvider.resolveNflow(nflowId);
            NflowOperationCriteria criteria = nflowOpsProvider.criteria()
                .nflow(domainId)
                .stoppedSince(since)
                .state(State.SUCCESS);
            List<com.onescorpin.metadata.api.op.NflowOperation> list = nflowOpsProvider.find(criteria);

            return list.stream().map(op -> Model.DOMAIN_TO_NFLOW_OP.apply(op)).collect(Collectors.toList());
        });
    }


    @GET
    @Path("{id}/op/results")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<DateTime, Map<String, Object>> collectNflowOperationsResults(@PathParam("id") final String nflowId,
                                                                           @QueryParam("since") @DefaultValue("1970-01-01T00:00:00Z") String sinceStr) {

        final DateTime since = Formatters.TIME_FORMATTER.parseDateTime(sinceStr);

        return this.metadata.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            com.onescorpin.metadata.api.nflow.Nflow.ID domainId = nflowProvider.resolveNflow(nflowId);
            NflowOperationCriteria criteria = nflowOpsProvider.criteria()
                .nflow(domainId)
                .stoppedSince(since)
                .state(State.SUCCESS);
            Map<DateTime, Map<String, Object>> results = nflowOpsProvider.getAllResults(criteria, null);

            return results.entrySet().stream()
                .collect(Collectors.toMap(te -> te.getKey(),
                                          te -> (Map<String, Object>) te.getValue().entrySet().stream()
                                              .collect(Collectors.toMap(ve -> ve.getKey(),
                                                                        ve -> (Object) ve.getValue().toString()))));
        });
    }
  */
    @GET
    @Path("{id}/depnflows")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the dependencies of the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the dependency graph.", response = NflowDependencyGraph.class),
                      @ApiResponse(code = 404, message = "The nflow could not be found.", response = RestResponseStatus.class)
                  })
    public NflowDependencyGraph getDependencyGraph(@PathParam("id") final String nflowId,
                                                  @QueryParam("preconds") @DefaultValue("false") final boolean assessPrecond) {
        LOG.debug("Get nflow dependencies {}", nflowId);

        return this.metadata.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            com.onescorpin.metadata.api.nflow.Nflow.ID domainId = nflowProvider.resolveNflow(nflowId);
            com.onescorpin.metadata.api.nflow.Nflow startDomain = nflowProvider.getNflow(domainId);

            if (startDomain != null) {
                return collectNflowDependencies(startDomain, assessPrecond);
            } else {
                throw new WebApplicationException("A nflow with the given ID does not exist: " + nflowId, Status.NOT_FOUND);
            }
        });
    }

    @POST
    @Path("{nflowId}/depnflows")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Adds a dependency to the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the new dependency graph.", response = NflowDependencyGraph.class),
                      @ApiResponse(code = 500, message = "The dependency could not be added.", response = RestResponseStatus.class)
                  })
    public NflowDependencyGraph addDependent(@PathParam("nflowId") final String nflowIdStr,
                                            @QueryParam("dependentId") final String depIdStr) {
        com.onescorpin.metadata.api.nflow.Nflow.ID nflowId = this.nflowProvider.resolveNflow(nflowIdStr);
        com.onescorpin.metadata.api.nflow.Nflow.ID depId = this.nflowProvider.resolveNflow(depIdStr);

        this.metadata.commit(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.EDIT_NFLOWS);

            this.nflowProvider.addDependent(nflowId, depId);
            return null;
        });

        return getDependencyGraph(nflowId.toString(), false);
    }

    @DELETE
    @Path("{nflowId}/depnflows")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Removes a dependency from the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the new dependency graph.", response = NflowDependencyGraph.class),
                      @ApiResponse(code = 500, message = "The dependency could not be removed.", response = RestResponseStatus.class)
                  })
    public NflowDependencyGraph removeDependent(@PathParam("nflowId") final String nflowIdStr,
                                               @QueryParam("dependentId") final String depIdStr) {
        com.onescorpin.metadata.api.nflow.Nflow.ID nflowId = this.nflowProvider.resolveNflow(nflowIdStr);
        com.onescorpin.metadata.api.nflow.Nflow.ID depId = this.nflowProvider.resolveNflow(depIdStr);

        this.metadata.commit(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.EDIT_NFLOWS);

            this.nflowProvider.removeDependent(nflowId, depId);
            return null;
        });

        return getDependencyGraph(nflowId.toString(), false);
    }

    @GET
    @Path("{nflowId}/depnflows/delta")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the dependencies delta for the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the dependencies deltas.", response = NflowDependencyDeltaResults.class),
                      @ApiResponse(code = 500, message = "The nflow could not be found.", response = RestResponseStatus.class)
                  })
    public NflowDependencyDeltaResults getDependentResultDeltas(@PathParam("nflowId") final String nflowIdStr) {
        LOG.info("Get nflow dependencies  delta for {}", nflowIdStr);
        com.onescorpin.metadata.api.nflow.Nflow.ID nflowId = this.nflowProvider.resolveNflow(nflowIdStr);

        return this.metadata.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);
            NflowDependencyDeltaResults results = this.nflowOpsProvider.getDependentDeltaResults(nflowId, null);
            return results;
        });
    }

    @GET
    @Path("{id}/source")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the sources of the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the nflow sources.", response = NflowSource.class, responseContainer = "List"),
                      @ApiResponse(code = 400, message = "The id is not a valid UUID.", response = RestResponseStatus.class),
                      @ApiResponse(code = 404, message = "The nflow could not be found.", response = RestResponseStatus.class)
                  })
    public List<NflowSource> getNflowSources(@PathParam("id") final String nflowId) {
        LOG.debug("Get nflow {} sources", nflowId);

        return this.metadata.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            com.onescorpin.metadata.api.nflow.Nflow.ID domainId = nflowProvider.resolveNflow(nflowId);
            com.onescorpin.metadata.api.nflow.Nflow domain = nflowProvider.getNflow(domainId);

            if (domain != null) {
                return domain.getSources().stream().map(this.metadataTransform.domainToNflowSource()).collect(Collectors.toList());
            } else {
                throw new WebApplicationException("A nflow with the given ID does not exist: " + nflowId, Status.NOT_FOUND);
            }
        });
    }
//
//    @GET
//    @Path("{fid}/source/{sid}")
//    @Produces(MediaType.APPLICATION_JSON)
//    public NflowSource getNflowSource(@PathParam("fid") final String nflowId, @PathParam("sid") final String srcId) {
//        LOG.debug("Get nflow {} source {}", nflowId, srcId);
//
//        return this.metadata.read(() -> {
//            com.onescorpin.metadata.api.nflow.Nflow.ID domainId = nflowProvider.resolveNflow(nflowId);
//            com.onescorpin.metadata.api.nflow.NflowSource.ID domainSrcId = nflowProvider.resolveSource(srcId);
//            com.onescorpin.metadata.api.nflow.Nflow domain = nflowProvider.getNflow(domainId);
//
//            if (domain != null) {
//                com.onescorpin.metadata.api.nflow.NflowSource domainSrc = domain.getSource(domainSrcId);
//
//                if (domainSrc != null) {
//                    return Model.domainToNflowSource.apply(domainSrc);
//                } else {
//                    throw new WebApplicationException("A nflow source with the given ID does not exist: " + srcId, Status.NOT_FOUND);
//                }
//            } else {
//                throw new WebApplicationException("A nflow with the given ID does not exist: " + nflowId, Status.NOT_FOUND);
//            }
//        });
//    }

    @GET
    @Path("{id}/destination")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the destinations of the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the nflow destinations.", response = NflowDestination.class, responseContainer = "List"),
                      @ApiResponse(code = 400, message = "The id is not a valid UUID.", response = RestResponseStatus.class),
                      @ApiResponse(code = 404, message = "The nflow could not be found.", response = RestResponseStatus.class)
                  })
    public List<NflowDestination> getNflowDestinations(@PathParam("id") final String nflowId) {
        LOG.debug("Get nflow {} destinations", nflowId);

        return this.metadata.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            com.onescorpin.metadata.api.nflow.Nflow.ID domainId = nflowProvider.resolveNflow(nflowId);
            com.onescorpin.metadata.api.nflow.Nflow domain = nflowProvider.getNflow(domainId);

            if (domain != null) {
                return domain.getDestinations().stream().map(this.metadataTransform.domainToNflowDestination()).collect(Collectors.toList());
            } else {
                throw new WebApplicationException("A nflow with the given ID does not exist: " + nflowId, Status.NOT_FOUND);
            }
        });
    }
//
//    @GET
//    @Path("{fid}/destination/{sid}")
//    @Produces(MediaType.APPLICATION_JSON)
//    public NflowDestination getNflowDestination(@PathParam("fid") final String nflowId, @PathParam("sid") final String destId) {
//        LOG.debug("Get nflow {} destination {}", nflowId, destId);
//
//        return this.metadata.read(() -> {
//            com.onescorpin.metadata.api.nflow.Nflow.ID domainId = nflowProvider.resolveNflow(nflowId);
//            com.onescorpin.metadata.api.nflow.NflowDestination.ID domainDestId = nflowProvider.resolveDestination(destId);
//            com.onescorpin.metadata.api.nflow.Nflow domain = nflowProvider.getNflow(domainId);
//
//            if (domain != null) {
//                com.onescorpin.metadata.api.nflow.NflowDestination domainDest = domain.getDestination(domainDestId);
//
//                if (domainDest != null) {
//                    return Model.domainToNflowDestination.apply(domainDest);
//                } else {
//                    throw new WebApplicationException("A nflow destination with the given ID does not exist: " + destId, Status.NOT_FOUND);
//                }
//            } else {
//                throw new WebApplicationException("A nflow with the given ID does not exist: " + nflowId, Status.NOT_FOUND);
//            }
//        });
//    }

    @GET
    @Path("{id}/precondition")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the precondition for the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the nflow precondition.", response = NflowPrecondition.class),
                      @ApiResponse(code = 400, message = "The id is not a valid UUID.", response = RestResponseStatus.class),
                      @ApiResponse(code = 404, message = "The nflow could not be found.", response = RestResponseStatus.class)
                  })
    public NflowPrecondition getNflowPrecondition(@PathParam("id") final String nflowId) {
        LOG.debug("Get nflow {} precondition", nflowId);

        return this.metadata.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            com.onescorpin.metadata.api.nflow.Nflow.ID domainId = nflowProvider.resolveNflow(nflowId);
            com.onescorpin.metadata.api.nflow.Nflow domain = nflowProvider.getNflow(domainId);

            if (domain != null) {
                return this.metadataTransform.domainToNflowPrecond().apply(domain.getPrecondition());
            } else {
                throw new WebApplicationException("A nflow with the given ID does not exist: " + nflowId, Status.NOT_FOUND);
            }
        });
    }

    @GET
    @Path("{id}/precondition/assessment")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Assess the precondition of the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the assessment.", response = ServiceLevelAssessment.class),
                      @ApiResponse(code = 400, message = "The id is not a valid UUID or the nflow does not have a precondition.", response = RestResponseStatus.class),
                      @ApiResponse(code = 404, message = "The nflow could not be found.", response = RestResponseStatus.class)
                  })
    public ServiceLevelAssessment assessPrecondition(@PathParam("id") final String nflowId) {
        LOG.debug("Assess nflow {} precondition", nflowId);

        return this.metadata.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            com.onescorpin.metadata.api.nflow.Nflow.ID domainId = nflowProvider.resolveNflow(nflowId);
            com.onescorpin.metadata.api.nflow.Nflow domain = nflowProvider.getNflow(domainId);

            if (domain != null) {
                com.onescorpin.metadata.api.nflow.NflowPrecondition precond = domain.getPrecondition();

                if (precond != null) {
                    return generateModelAssessment(precond);
                } else {
                    throw new WebApplicationException("The nflow with the given ID does not have a precondition: " + nflowId, Status.BAD_REQUEST);
                }
            } else {
                throw new WebApplicationException("A nflow with the given ID does not exist: " + nflowId, Status.NOT_FOUND);
            }
        });
    }

    @GET
    @Path("{id}/precondition/assessment/result")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation("Assess the precondition of the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the assessment result.", response = ServiceLevelAssessment.class),
                      @ApiResponse(code = 400, message = "The id is not a valid UUID or the nflow does not have a precondition.", response = RestResponseStatus.class),
                      @ApiResponse(code = 404, message = "The nflow could not be found.", response = RestResponseStatus.class)
                  })
    public String assessPreconditionResult(@PathParam("id") final String nflowId) {
        return assessPrecondition(nflowId).getResult().toString();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Creates a new nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "The nflow was created.", response = Nflow.class),
                      @ApiResponse(code = 400, message = "The name is already in use.", response = RestResponseStatus.class),
                      @ApiResponse(code = 500, message = "The nflow could not be created.", response = RestResponseStatus.class)
                  })
    public Nflow createNflow(final Nflow nflow, @QueryParam("ensure") @DefaultValue("true") final boolean ensure) {
        LOG.debug("Create nflow (ensure={}) {}", ensure, nflow);

        this.metadataTransform.validateCreate(nflow);

        return this.metadata.commit(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.EDIT_NFLOWS);

            com.onescorpin.metadata.api.nflow.NflowCriteria crit = nflowProvider.nflowCriteria().name(nflow.getSystemName()).category(nflow.getCategory().getSystemName());
            Collection<com.onescorpin.metadata.api.nflow.Nflow> existing = nflowProvider.getNflows(crit);

            if (existing.isEmpty()) {
                com.onescorpin.metadata.api.nflow.Nflow domainNflow = nflowProvider.ensureNflow(nflow.getCategory().getSystemName(), nflow.getSystemName(), nflow.getDescription());

                ensureDependentDatasources(nflow, domainNflow);
                ensurePrecondition(nflow, domainNflow);
                ensureProperties(nflow, domainNflow);

                return this.metadataTransform.domainToNflow().apply(nflowProvider.getNflow(domainNflow.getId()));
            } else if (ensure) {
                return this.metadataTransform.domainToNflow().apply(existing.iterator().next());
            } else {
                throw new WebApplicationException("A nflow with the given name already exists: " + nflow.getSystemName(), Status.BAD_REQUEST);
            }
        });
    }

    /**
     * Updates an existing nflow.  Note that POST is used here rather than PUT since it behaves more like a PATCH; which isn't supported in Jersey.
     */
    @POST
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Updates the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "The nflow was updated.", response = Nflow.class),
                      @ApiResponse(code = 404, message = "The nflow was not found.", response = RestResponseStatus.class),
                      @ApiResponse(code = 500, message = "The nflow could not be updated.", response = RestResponseStatus.class)
                  })
    public Nflow updateNflow(@PathParam("id") final String nflowId, final Nflow nflow) {
        LOG.debug("Update nflow: {}", nflow);

        this.metadataTransform.validateCreate(nflow);

        return this.metadata.commit(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.EDIT_NFLOWS);

            com.onescorpin.metadata.api.nflow.Nflow.ID domainId = nflowProvider.resolveNflow(nflowId);
            com.onescorpin.metadata.api.nflow.Nflow domain = nflowProvider.getNflow(domainId);

            if (domain != null) {
                domain = this.metadataTransform.updateDomain(nflow, domain);
                return this.metadataTransform.domainToNflow().apply(domain);
            } else {
                throw new WebApplicationException("No nflow exist with the ID: " + nflow.getId(), Status.NOT_FOUND);
            }
        });
    }

    /**
     * Gets the properties for the specified nflow.
     *
     * @param nflowId the nflow id or the nflow category and name
     * @return the metadata properties
     */
    @GET
    @Path("{id}/props")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the properties of the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the nflow properties.", response = Map.class),
                      @ApiResponse(code = 400, message = "The id is not a valid UUID.", response = RestResponseStatus.class),
                      @ApiResponse(code = 404, message = "The nflow was not found.", response = RestResponseStatus.class)
                  })
    public Map<String, Object> getNflowProperties(@PathParam("id") final String nflowId) {
        LOG.debug("Get nflow properties ID: {}", nflowId);

        return this.metadata.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            String[] parts = nflowId.split("\\.", 2);
            com.onescorpin.metadata.api.nflow.Nflow domain = (parts.length == 2) ? nflowProvider.findBySystemName(parts[0], parts[1]) : nflowProvider.getNflow(nflowProvider.resolveNflow(nflowId));

            if (domain != null) {
                return domain.getProperties();
            } else {
                throw new WebApplicationException("No nflow exist with the ID: " + nflowId, Status.NOT_FOUND);
            }
        });
    }

    /**
     * Merges the properties for the specified nflows. New properties will be added and existing properties will be overwritten.
     *
     * @param nflowId the nflow id or the nflow category and name
     * @param props  the properties to be merged
     * @return the merged metadata properties
     */
    @POST
    @Path("{id}/props")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Merges the properties for the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the updated properties.", response = Map.class),
                      @ApiResponse(code = 400, message = "The id is not a valid UUID.", response = RestResponseStatus.class),
                      @ApiResponse(code = 404, message = "The nflow could not be found.", response = RestResponseStatus.class),
                      @ApiResponse(code = 500, message = "The properties could not be updated.", response = RestResponseStatus.class)
                  })
    public Map<String, Object> mergeNflowProperties(@PathParam("id") final String nflowId, final Properties props) {
        LOG.debug("Merge nflow properties ID: {}, properties: {}", nflowId, props);

        return this.metadata.commit(() -> {
            String[] parts = nflowId.split("\\.", 2);
            com.onescorpin.metadata.api.nflow.Nflow domain = (parts.length == 2) ? nflowProvider.findBySystemName(parts[0], parts[1]) : nflowProvider.getNflow(nflowProvider.resolveNflow(nflowId));

            if (domain != null) {
                return updateProperties(props, domain, false);
            } else {
                throw new WebApplicationException("No nflow exist with the ID: " + nflowId, Status.NOT_FOUND);
            }
        });
    }

    @PUT
    @Path("{id}/props")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Sets the properties for the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the updated properties.", response = Properties.class),
                      @ApiResponse(code = 400, message = "The id is not a valid UUID.", response = RestResponseStatus.class),
                      @ApiResponse(code = 404, message = "The nflow could not be found.", response = RestResponseStatus.class),
                      @ApiResponse(code = 500, message = "The properties could not be updated.", response = RestResponseStatus.class)
                  })
    public Properties replaceNflowProperties(@PathParam("id") final String nflowId,
                                            final Properties props) {
        LOG.debug("Replace nflow properties ID: {}, properties: {}", nflowId, props);

        return this.metadata.commit(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.EDIT_NFLOWS);

            com.onescorpin.metadata.api.nflow.Nflow.ID domainId = nflowProvider.resolveNflow(nflowId);
            com.onescorpin.metadata.api.nflow.Nflow domain = nflowProvider.getNflow(domainId);

            if (domain != null) {
                Map<String, Object> domainProps = updateProperties(props, domain, true);
                Properties newProps = new Properties();

                newProps.putAll(domainProps);
                return newProps;
            } else {
                throw new WebApplicationException("No nflow exist with the ID: " + nflowId, Status.NOT_FOUND);
            }
        });
    }


    @GET
    @Path("{nflowId}/lineage")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the lineage of the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the nflow lineage.", response = NflowLineage.class),
                      @ApiResponse(code = 400, message = "The id is not a valid UUID.", response = RestResponseStatus.class)
                  })
    public NflowLineage getNflowLineage(@PathParam("nflowId") final String nflowId) {

        return this.metadata.read(() -> {

            com.onescorpin.metadata.api.nflow.Nflow domainNflow = nflowProvider.getNflow(nflowProvider.resolveNflow(nflowId));

            if (domainNflow != null) {
                NflowLineageBuilder builder = new NflowLineageBuilder(domainNflow, model, datasourceTransform);
                Nflow nflow = builder.build();//Model.DOMAIN_TO_NFLOW_WITH_DEPENDENCIES.apply(domainNflow);
                return new NflowLineage(nflow, datasourceService.getNflowLineageStyleMap());
            }
            return null;
        });

    }

    /*
    @GET
    @Path("remove-sources")
    @Produces(MediaType.APPLICATION_JSON)
    public RestResponseStatus removeSources(){

        try {
            this.metadata.commit(() -> {
                List<? extends com.onescorpin.metadata.api.nflow.Nflow> nflows = nflowProvider.getNflows();
                nflows.stream().forEach(nflow -> {
                    nflowProvider.removeNflowSources(nflow.getId());
                });
            }, MetadataAccess.SERVICE);
        }catch (Exception e){
            e.printStackTrace();
        }


        this.metadata.commit(() -> {
                                 List<? extends com.onescorpin.metadata.api.nflow.Nflow> nflows = nflowProvider.getNflows();
                                 nflows.stream().forEach(nflow -> {
                                     nflowProvider.removeNflowDestinations(nflow.getId());
                                 });
                             }, MetadataAccess.SERVICE);
        this.metadata.commit(() -> {
            List<Datasource> datasources = datasetProvider.getDatasources();
            datasources.stream().forEach(datasource -> {
                datasetProvider.removeDatasource(datasource.getId());
            });

        }, MetadataAccess.SERVICE);
        return new RestResponseStatus.ResponseStatusBuilder().buildSuccess();

    }

*/


    @POST
    @Path("{nflowId}/source")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Adds a source to the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the updated nflow.", response = Nflow.class),
                      @ApiResponse(code = 400, message = "The nflow could not be found.", response = RestResponseStatus.class),
                      @ApiResponse(code = 500, message = "The nflow could not be updated.", response = RestResponseStatus.class)
                  })
    public Nflow addNflowSource(@PathParam("nflowId") final String nflowId,
                              @FormParam("datasourceId") final String datasourceId) {
        LOG.debug("Add nflow source, nflow ID: {}, datasource ID: {}", nflowId, datasourceId);

        return this.metadata.commit(() -> {
            com.onescorpin.metadata.api.nflow.Nflow.ID domainNflowId = nflowProvider.resolveNflow(nflowId);
            Datasource.ID domainDsId = datasetProvider.resolve(datasourceId);
            com.onescorpin.metadata.api.nflow.NflowSource domainDest = nflowProvider.ensureNflowSource(domainNflowId, domainDsId);

            return this.metadataTransform.domainToNflow().apply(domainDest.getNflow());
        });
    }

    @POST
    @Path("{nflowId}/destination")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Adds a destination to the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the updated nflow.", response = Nflow.class),
                      @ApiResponse(code = 400, message = "The nflow could not be found.", response = RestResponseStatus.class),
                      @ApiResponse(code = 500, message = "The nflow could not be updated.", response = RestResponseStatus.class)
                  })
    public Nflow addNflowDestination(@PathParam("nflowId") final String nflowId,
                                   @FormParam("datasourceId") final String datasourceId) {
        LOG.debug("Add nflow destination, nflow ID: {}, datasource ID: {}", nflowId, datasourceId);

        return this.metadata.commit(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.EDIT_NFLOWS);

            com.onescorpin.metadata.api.nflow.Nflow.ID domainNflowId = nflowProvider.resolveNflow(nflowId);
            Datasource.ID domainDsId = datasetProvider.resolve(datasourceId);
            com.onescorpin.metadata.api.nflow.NflowDestination domainDest = nflowProvider.ensureNflowDestination(domainNflowId, domainDsId);

            return this.metadataTransform.domainToNflow().apply(domainDest.getNflow());
        });
    }

    @POST
    @Path("{nflowId}/precondition")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Adds a precondition to the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the updated nflow.", response = Nflow.class),
                      @ApiResponse(code = 400, message = "The nflow could not be found.", response = RestResponseStatus.class),
                      @ApiResponse(code = 500, message = "The nflow could not be updated.", response = RestResponseStatus.class)
                  })
    public Nflow setPrecondition(@PathParam("nflowId") final String nflowId, final NflowPrecondition precond) {
        LOG.debug("Add nflow precondition, nflow ID: {}, precondition: {}", nflowId, precond);

        return this.metadata.commit(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.EDIT_NFLOWS);

            com.onescorpin.metadata.api.nflow.Nflow.ID domainNflowId = nflowProvider.resolveNflow(nflowId);
            List<com.onescorpin.metadata.sla.api.Metric> domainMetrics
                = precond.getSla().getObligations().stream()
                .flatMap((grp) -> grp.getMetrics().stream())
                .map((metric) -> metric)
                .collect(Collectors.toList());

            com.onescorpin.metadata.api.nflow.Nflow domainNflow
                = nflowProvider.createPrecondition(domainNflowId, "", domainMetrics);

            return this.metadataTransform.domainToNflow().apply(domainNflow);
        });
    }


    private Map<String, Object> updateProperties(final Properties props,
                                                 com.onescorpin.metadata.api.nflow.Nflow domain,
                                                 boolean replace) {
        return metadata.commit(() -> {
            Map<String, Object> newProperties = new HashMap<String, Object>();
            for (String name : props.stringPropertyNames()) {
                newProperties.put(name, props.getProperty(name));
            }
            if (replace) {
                nflowProvider.replaceProperties(domain.getId(), newProperties);
            } else {
                nflowProvider.mergeNflowProperties(domain.getId(), newProperties);
            }
            return domain.getProperties();
        });
    }

    private ServiceLevelAssessment generateModelAssessment(com.onescorpin.metadata.api.nflow.NflowPrecondition precond) {
        com.onescorpin.metadata.sla.api.ServiceLevelAssessment assmt = this.preconditionService.assess(precond);
        return ServiceLevelAgreementModelTransform.DOMAIN_TO_SLA_ASSMT.apply(assmt);
    }

    private void ensurePrecondition(Nflow nflow, com.onescorpin.metadata.api.nflow.Nflow domainNflow) {
        NflowPrecondition precond = nflow.getPrecondition();

        if (precond != null) {
            List<com.onescorpin.metadata.sla.api.Metric> domainMetrics
                = precond.getSla().getObligations().stream()
                .flatMap((grp) -> grp.getMetrics().stream())
                .map((metric) -> metric)
                .collect(Collectors.toList());

            nflowProvider.createPrecondition(domainNflow.getId(), "", domainMetrics);
        }

    }

    private void ensureDependentDatasources(Nflow nflow, com.onescorpin.metadata.api.nflow.Nflow domainNflow) {
        for (NflowSource src : nflow.getSources()) {
            Datasource.ID dsId = this.datasetProvider.resolve(src.getId());
            nflowProvider.ensureNflowSource(domainNflow.getId(), dsId);
        }

        for (NflowDestination src : nflow.getDestinations()) {
            Datasource.ID dsId = this.datasetProvider.resolve(src.getId());
            nflowProvider.ensureNflowDestination(domainNflow.getId(), dsId);
        }
    }

    private void ensureProperties(Nflow nflow, com.onescorpin.metadata.api.nflow.Nflow domainNflow) {
        Map<String, Object> domainProps = domainNflow.getProperties();
        Properties props = nflow.getProperties();

        for (String key : nflow.getProperties().stringPropertyNames()) {
            domainProps.put(key, props.getProperty(key));
        }
    }

    private com.onescorpin.metadata.api.nflow.NflowCriteria createNflowCriteria(String category,
                                                                                    String name,
                                                                                    String srcId,
                                                                                    String destId) {
        com.onescorpin.metadata.api.nflow.NflowCriteria criteria = nflowProvider.nflowCriteria();

        if (StringUtils.isNotEmpty(category)) {
            criteria.category(category);
        }

        if (StringUtils.isNotEmpty(name)) {
            criteria.name(name);
        }
        if (StringUtils.isNotEmpty(srcId)) {
            Datasource.ID dsId = this.datasetProvider.resolve(srcId);
            criteria.sourceDatasource(dsId);
        }
        if (StringUtils.isNotEmpty(destId)) {
            Datasource.ID dsId = this.datasetProvider.resolve(destId);
            criteria.destinationDatasource(dsId);
        }

        return criteria;
    }

    private NflowDependencyGraph collectNflowDependencies(com.onescorpin.metadata.api.nflow.Nflow currentNflow, boolean assessPrecond) {
        List<com.onescorpin.metadata.api.nflow.Nflow> domainDeps = currentNflow.getDependentNflows();
        NflowDependencyGraph nflowDep = new NflowDependencyGraph(this.metadataTransform.domainToNflow().apply(currentNflow), null);

        if (!domainDeps.isEmpty()) {
            for (com.onescorpin.metadata.api.nflow.Nflow depNflow : domainDeps) {
                NflowDependencyGraph childDep = collectNflowDependencies(depNflow, assessPrecond);
                nflowDep.addDependecy(childDep);
            }
        }

        if (assessPrecond && currentNflow.getPrecondition() != null) {
            nflowDep.setPreconditonResult(generateModelAssessment(currentNflow.getPrecondition()));
        }

        return nflowDep;
    }

}
