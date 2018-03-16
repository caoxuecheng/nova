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

import com.google.common.collect.Lists;
import com.mifmif.common.regex.Generex;
import com.onescorpin.annotations.AnnotatedFieldProperty;
import com.onescorpin.annotations.AnnotationFieldNameResolver;
import com.onescorpin.discovery.schema.QueryResult;
import com.onescorpin.nflowmgr.nifi.PropertyExpressionResolver;
import com.onescorpin.nflowmgr.rest.model.EditNflowEntity;
import com.onescorpin.nflowmgr.rest.model.NflowMetadata;
import com.onescorpin.nflowmgr.rest.model.NflowSummary;
import com.onescorpin.nflowmgr.rest.model.NflowVersions;
import com.onescorpin.nflowmgr.rest.model.NifiNflow;
import com.onescorpin.nflowmgr.rest.model.UINflow;
import com.onescorpin.nflowmgr.security.NflowServicesAccessControl;
import com.onescorpin.nflowmgr.service.NflowCleanupFailedException;
import com.onescorpin.nflowmgr.service.NflowCleanupTimeoutException;
import com.onescorpin.nflowmgr.service.MetadataService;
import com.onescorpin.nflowmgr.service.datasource.DatasourceService;
import com.onescorpin.nflowmgr.service.nflow.DuplicateNflowNameException;
import com.onescorpin.nflowmgr.service.nflow.NflowManagerPreconditionService;
import com.onescorpin.nflowmgr.service.nflow.NflowModelTransform;
import com.onescorpin.nflowmgr.service.security.SecurityService;
import com.onescorpin.nflowmgr.service.template.RegisteredTemplateService;
import com.onescorpin.nflowmgr.sla.ServiceLevelAgreementService;
import com.onescorpin.hive.service.HiveService;
import com.onescorpin.hive.util.HiveUtils;
import com.onescorpin.metadata.NflowPropertySection;
import com.onescorpin.metadata.NflowPropertyType;
import com.onescorpin.metadata.api.security.MetadataAccessControl;
import com.onescorpin.metadata.modeshape.versioning.VersionNotFoundException;
import com.onescorpin.metadata.rest.model.data.DatasourceDefinition;
import com.onescorpin.metadata.rest.model.data.DatasourceDefinitions;
import com.onescorpin.metadata.rest.model.nflow.NflowLineageStyle;
import com.onescorpin.metadata.rest.model.sla.NflowServiceLevelAgreement;
import com.onescorpin.nifi.rest.client.NifiClientRuntimeException;
import com.onescorpin.nifi.rest.model.NifiProperty;
import com.onescorpin.nifi.rest.support.NifiPropertyUtil;
import com.onescorpin.policy.rest.model.PreconditionRule;
import com.onescorpin.rest.model.RestResponseStatus;
import com.onescorpin.rest.model.search.SearchResult;
import com.onescorpin.security.AccessController;
import com.onescorpin.security.rest.controller.SecurityModelTransform;
import com.onescorpin.security.rest.model.ActionGroup;
import com.onescorpin.security.rest.model.PermissionsChange;
import com.onescorpin.security.rest.model.PermissionsChange.ChangeType;
import com.onescorpin.security.rest.model.RoleMembershipChange;
import com.onescorpin.support.NflowNameUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.directory.api.util.Strings;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.JDBCException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.AccessControlException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;

@Api(tags = "Nflow Manager - Nflows", produces = "application/json")
@Path(NflowRestController.BASE)
@Component
@SwaggerDefinition(tags = @Tag(name = "Nflow Manager - Nflows", description = "manages nflows"))
public class NflowRestController {

    private static final Logger log = LoggerFactory.getLogger(NflowRestController.class);
    public static final String BASE = "/v1/nflowmgr/nflows";

    /**
     * Messages for the default locale
     */
    private static final ResourceBundle STRINGS = ResourceBundle.getBundle("com.onescorpin.nflowmgr.rest.controller.NflowMessages");
    private static final int MAX_LIMIT = 1000;
    private static final String NAMES = "/names";
    private static final String SUMMARY = "/nflow-summary";

    @Inject
    private MetadataService metadataService;

    @Inject
    private HiveService hiveService;

    @Inject
    private NflowManagerPreconditionService nflowManagerPreconditionService;

    @Inject
    private NflowModelTransform nflowModelTransform;

    @Inject
    private DatasourceService datasourceService;

    @Inject
    private SecurityService securityService;

    @Inject
    private SecurityModelTransform securityTransform;

    @Inject
    private ServiceLevelAgreementService serviceLevelAgreementService;

    @Inject
    private RegisteredTemplateService registeredTemplateService;

    @Inject
    private AccessController accessController;

    @Inject
    PropertyExpressionResolver propertyExpressionResolver;

    private MetadataService getMetadataService() {
        return metadataService;
    }


    /**
     * Creates a new Nflow using the specified metadata.
     *
     * @param editNflowEntity the nflow metadata
     * @return the nflow
     */
    @POST
    @Path("/edit/{nflowId}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Creates or updates a nflow.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the nflow including any error messages.", response = NifiNflow.class)
    )
    @Nonnull
    public Response editNflow(@Nonnull final EditNflowEntity editNflowEntity) {

        return createNflow(editNflowEntity.getNflowMetadata());
    }

    private void populateNflow(EditNflowEntity editNflowEntity) {
        //fetch the nflow
        NflowMetadata nflow = getMetadataService().getNflowById(editNflowEntity.getNflowMetadata().getNflowId());
        NflowMetadata editNflow = editNflowEntity.getNflowMetadata();
        switch (editNflowEntity.getAction()) {
            case SUMMARY:
                updateNflowMetadata(nflow, editNflow, NflowPropertySection.SUMMARY);
                break;
            case NIFI_PROPERTIES:
                updateNflowMetadata(nflow, editNflow, NflowPropertySection.NIFI_PROPERTIES);
                break;
            case PROPERTIES:
                updateNflowMetadata(nflow, editNflow, NflowPropertySection.PROPERTIES);
                break;
            case TABLE_DATA:
                updateNflowMetadata(nflow, editNflow, NflowPropertySection.TABLE_DATA);
                break;
            case SCHEDULE:
                updateNflowMetadata(nflow, editNflow, NflowPropertySection.SCHEDULE);
                break;
            default:
                break;
        }

        createNflow(nflow);
    }

    private void updateNflowMetadata(NflowMetadata targetNflowMetadata, NflowMetadata modifiedNflowMetadata, NflowPropertySection nflowPropertySection) {

        AnnotationFieldNameResolver annotationFieldNameResolver = new AnnotationFieldNameResolver(NflowPropertyType.class);
        List<AnnotatedFieldProperty> list = annotationFieldNameResolver.getProperties(NflowMetadata.class);
        List<AnnotatedFieldProperty>
            sectionList =
            list.stream().filter(annotatedFieldProperty -> nflowPropertySection.equals(((NflowPropertyType) annotatedFieldProperty.getAnnotation()).section())).collect(Collectors.toList());
        sectionList.forEach(annotatedFieldProperty -> {
            try {
                Object value = FieldUtils.readField(annotatedFieldProperty.getField(), modifiedNflowMetadata);
                FieldUtils.writeField(annotatedFieldProperty.getField(), targetNflowMetadata, value);
            } catch (IllegalAccessException e) {
                log.warn("Unable to update NflowMetadata field: {}.  Exception: {} ", annotatedFieldProperty.getField(), e.getMessage(), e);
            }
        });


    }

    /**
     * Creates a new Nflow using the specified metadata.
     *
     * @param nflowMetadata the nflow metadata
     * @return the nflow
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Creates or updates a nflow.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the nflow including any error messages.", response = NifiNflow.class)
    )
    @Nonnull
    public Response createNflow(@Nonnull final NflowMetadata nflowMetadata) {
        NifiNflow nflow;
        try {
            nflow = getMetadataService().createNflow(nflowMetadata);
        } catch (DuplicateNflowNameException e) {
            log.info("Failed to create a new nflow due to another nflow having the same category/nflow name: " + nflowMetadata.getCategoryAndNflowDisplayName());

            // Create an error message
            String msg = "A nflow already exists in the category \"" + e.getCategoryName() + "\" with name name \"" + e.getNflowName() + "\"";

            // Add error message to nflow
            nflow = new NifiNflow(nflowMetadata, null);
            nflow.addErrorMessage(msg);
            nflow.setSuccess(false);
        } catch (Exception e) {
            log.error("Failed to create a new nflow.", e);

            // Create an error message
            String msg = (e.getMessage() != null) ? "Error saving Nflow " + e.getMessage() : "An unknown error occurred while saving the nflow.";
            if (e.getCause() instanceof JDBCException) {
                msg += ". " + ((JDBCException) e).getSQLException();
            }

            // Add error message to nflow
            nflow = new NifiNflow(nflowMetadata, null);
            nflow.addErrorMessage(msg);
            nflow.setSuccess(false);
        }
        return Response.ok(nflow).build();
    }

    @POST
    @Path("/enable/{nflowId}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Enables a nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "The nflow was enabled.", response = NflowSummary.class),
                      @ApiResponse(code = 500, message = "The nflow could not be enabled.", response = RestResponseStatus.class)
                  })
    public Response enableNflow(@PathParam("nflowId") String nflowId) {
        NflowSummary nflow = getMetadataService().enableNflow(nflowId);
        return Response.ok(nflow).build();
    }

    @POST
    @Path("/disable/{nflowId}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Disables a nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "The nflow was disabled.", response = NflowSummary.class),
                      @ApiResponse(code = 500, message = "The nflow could not be disabled.", response = RestResponseStatus.class)
                  })
    public Response disableNflow(@PathParam("nflowId") String nflowId) {
        NflowSummary nflow = getMetadataService().disableNflow(nflowId);
        return Response.ok(nflow).build();
    }


    @GET
    @Deprecated
    @Path(NAMES)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the list of nflow summaries.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns a list of nflows.", response = NflowSummary.class, responseContainer = "List")
    )
    public Response getNflowNames() {
        Collection<NflowSummary> nflows = getMetadataService().getNflowSummaryData();
        return Response.ok(nflows).build();
    }

    @GET
    @Path(SUMMARY)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the list of nflow summaries.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns a list of nflows.", response = NflowSummary.class, responseContainer = "List")
    )
    public Response getNflowSummaries() {
        Collection<NflowSummary> nflows = getMetadataService().getNflowSummaryData();
        return Response.ok(nflows).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the list of nflows.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns a list of nflows.", response = NflowMetadata.class, responseContainer = "List")
    )
    public SearchResult getNflows(@QueryParam("verbose") @DefaultValue("false") boolean verbose,
                                 @QueryParam("sort") @DefaultValue("nflowName") String sort,
                                 @QueryParam("filter") String filter,
                                 @QueryParam("limit") String limit,
                                 @QueryParam("start") @DefaultValue("0") Integer start) {

        try {
            int size = Strings.isEmpty(limit) || limit.equalsIgnoreCase("all") ? MAX_LIMIT : Integer.parseInt(limit);
            Page<UINflow> page = getMetadataService().getNflowsPage(verbose,
                                                                  pageRequest(start, size, sort),
                                                                  filter != null ? filter.trim() : null);
            return this.nflowModelTransform.toSearchResult(page);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("The value of limit must be an integer or \"all\"");
        }
    }

    @GET
    @Path("/{nflowId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the nflow.", response = NflowMetadata.class),
                      @ApiResponse(code = 500, message = "The nflow is unavailable.", response = RestResponseStatus.class)
                  })
    public Response getNflow(@PathParam("nflowId") String nflowId) {
        NflowMetadata nflow = getMetadataService().getNflowById(nflowId, true);

        return Response.ok(nflow).build();
    }

    @GET
    @Path("/by-name/{nflowName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the nflow.", response = NflowMetadata.class),
                      @ApiResponse(code = 500, message = "The nflow is unavailable.", response = RestResponseStatus.class)
                  })
    public Response getNflowByName(@PathParam("nflowName") String nflowName) {
        String categorySystemName = NflowNameUtil.category(nflowName);
        String nflowSystemName = NflowNameUtil.nflow(nflowName);
        if (StringUtils.isNotBlank(categorySystemName) && StringUtils.isNotBlank(nflowSystemName)) {
            NflowMetadata nflow = getMetadataService().getNflowByName(categorySystemName, nflowSystemName);
            return Response.ok(nflow).build();
        } else {
            throw new NotFoundException("Unable to find the nflow for name: " + nflowName);
        }
    }

    @GET
    @Path("/by-name/{nflowName}/field-policies")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the nflow field policies (List<FieldPolicy>) as json.", response = List.class),
                      @ApiResponse(code = 500, message = "The nflow is unavailable.", response = RestResponseStatus.class)
                  })
    public Response getNflowFieldPoliciesByName(@PathParam("nflowName") String nflowName) {
        String categorySystemName = NflowNameUtil.category(nflowName);
        String nflowSystemName = NflowNameUtil.nflow(nflowName);
        if (StringUtils.isNotBlank(categorySystemName) && StringUtils.isNotBlank(nflowSystemName)) {
            NflowMetadata nflow = getMetadataService().getNflowByName(categorySystemName, nflowSystemName);
            if (nflow != null && nflow.getTable() != null) {
                return Response.ok(nflow.getTable().getFieldPoliciesJson()).build();
            } else {
                throw new NotFoundException("Unable to find the nflow field policies for name: " + nflowName);
            }
        } else {
            throw new NotFoundException("Unable to find the nflow field policies for name: " + nflowName);
        }
    }


    /**
     * Deletes the specified nflow.
     *
     * @param nflowId the nflow id
     * @return the response
     */
    @DELETE
    @Path("/{nflowId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Deletes the specified nflow.")
    @ApiResponses({
                      @ApiResponse(code = 204, message = "The nflow was deleted."),
                      @ApiResponse(code = 404, message = "The nflow does not exist.", response = RestResponseStatus.class),
                      @ApiResponse(code = 409, message = "There are dependent nflows.", response = RestResponseStatus.class),
                      @ApiResponse(code = 500, message = "The nflow could not be deleted.", response = RestResponseStatus.class)
                  })
    @Nonnull
    public Response deleteNflow(@Nonnull @PathParam("nflowId") final String nflowId) {
        try {
            getMetadataService().deleteNflow(nflowId);
            return Response.noContent().build();
        } catch (AccessControlException e) {
            log.debug("Access controll failure attempting to delete a nflow", e);
            throw e;
        } catch (NflowCleanupFailedException e) {
            log.error("Error deleting nflow: Cleanup error", e);
            throw new InternalServerErrorException(STRINGS.getString("deleteNflow.cleanupError"), e);
        } catch (NflowCleanupTimeoutException e) {
            log.error("Error deleting nflow: Cleanup timeout", e);
            throw new InternalServerErrorException(STRINGS.getString("deleteNflow.cleanupTimeout"), e);
        } catch (IllegalArgumentException e) {
            log.error("Error deleting nflow: Illegal Argument", e);
            throw new NotFoundException(STRINGS.getString("deleteNflow.notFound"), e);
        } catch (final IllegalStateException e) {
            log.error("Error deleting nflow: Illegal state", e);
            throw new ClientErrorException(STRINGS.getString("deleteNflow.hasDependents"), Response.Status.CONFLICT, e);
        } catch (NifiClientRuntimeException e) {
            log.error("Error deleting nflow: NiFi error", e);
            throw new InternalServerErrorException(STRINGS.getString("deleteNflow.nifiError"), e);
        } catch (Exception e) {
            log.error("Error deleting nflow: Unknown error", e);
            throw new InternalServerErrorException(STRINGS.getString("deleteNflow.unknownError"), e);
        }
    }

    @GET
    @Path("/{nflowId}/versions")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Updates a nflow with the latest template metadata.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the nflow versions.", response = NflowMetadata.class),
                      @ApiResponse(code = 500, message = "The nflow is unavailable.", response = RestResponseStatus.class)
                  })
    public Response getNflowVersions(@PathParam("nflowId") String nflowId,
                                    @QueryParam("content") @DefaultValue("true") boolean includeContent) {
        NflowVersions nflow = getMetadataService().getNflowVersions(nflowId, includeContent);

        return Response.ok(nflow).build();
    }

    @GET
    @Path("/{nflowId}/versions/{versionId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Updates a nflow with the latest template metadata.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the nflow versions.", response = NflowMetadata.class),
                      @ApiResponse(code = 400, message = "Returns the nflow or version does not exist.", response = NflowMetadata.class),
                      @ApiResponse(code = 500, message = "The nflow is unavailable.", response = RestResponseStatus.class)
                  })
    public Response getNflowVersion(@PathParam("nflowId") String nflowId,
                                   @PathParam("versionId") String versionId,
                                   @QueryParam("content") @DefaultValue("true") boolean includeContent) {
        try {
            return getMetadataService().getNflowVersion(nflowId, versionId, includeContent)
                .map(version -> Response.ok(version).build())
                .orElse(Response.status(Status.NOT_FOUND).build());
        } catch (VersionNotFoundException e) {
            return Response.status(Status.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Unexpected exception retrieving the nflow version", e);
            throw new InternalServerErrorException("Unexpected exception retrieving the nflow version");
        }
    }

    @POST
    @Path("/{nflowId}/merge-template")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Updates a nflow with the latest template metadata.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "The nflow was updated.", response = NflowMetadata.class),
                      @ApiResponse(code = 500, message = "The nflow could not be updated.", response = RestResponseStatus.class)
                  })
    public Response mergeTemplate(@PathParam("nflowId") String nflowId, NflowMetadata nflow) {
        registeredTemplateService.mergeTemplatePropertiesWithNflow(nflow);
        return Response.ok(nflow).build();
    }


    @GET
    @Path("/{nflowId}/profile-summary")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets a summary of the nflow profiles.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the profile summaries.", response = Map.class, responseContainer = "List"),
                      @ApiResponse(code = 500, message = "The profiles are unavailable.", response = RestResponseStatus.class)
                  })
    public Response profileSummary(@PathParam("nflowId") String nflowId) {
        NflowMetadata nflowMetadata = getMetadataService().getNflowById(nflowId);
        final String profileTable = HiveUtils.quoteIdentifier(nflowMetadata.getProfileTableName());
        String query = "SELECT * from " + profileTable + " where columnname = '(ALL)'";

        List<Map<String, Object>> rows = new ArrayList<>();
        try {
            QueryResult results = hiveService.query(query);

            rows.addAll(results.getRows());
            //add in the archive date time fields if applicipable
            String ARCHIVE_PROCESSOR_TYPE = "com.onescorpin.nifi.GetTableData";
            if (nflowMetadata.getInputProcessorType().equalsIgnoreCase(ARCHIVE_PROCESSOR_TYPE)) {
                NifiProperty property = NifiPropertyUtil.findPropertyByProcessorType(nflowMetadata.getProperties(), ARCHIVE_PROCESSOR_TYPE, "Date Field");
                if (property != null && property.getValue() != null) {
                    String field = property.getValue();
                    if (field.contains(".")) {
                        field = StringUtils.substringAfterLast(field, ".");
                    }
                    query = "SELECT * from " + profileTable + " where metrictype IN('MIN_TIMESTAMP','MAX_TIMESTAMP') AND columnname = " + HiveUtils.quoteString(field);

                    QueryResult dateRows = hiveService.query(query);
                    if (dateRows != null && !dateRows.isEmpty()) {
                        rows.addAll(dateRows.getRows());
                    }
                }
            }
        } catch (DataAccessException e) {
            if (e.getCause() instanceof org.apache.hive.service.cli.HiveSQLException && e.getCause().getMessage().contains("Table not found")) {
                //this exception is ok to swallow since it just means no profile data exists yet
            } else if (e.getCause().getMessage().contains("HiveAccessControlException Permission denied")) {
                throw new AccessControlException("You do not have permission to execute this hive query");
            } else {
                throw e;
            }
        }

        return Response.ok(rows).build();
    }

    @GET
    @Path("/{nflowId}/profile-stats")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the profile statistics for the specified job.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the profile statistics.", response = Map.class, responseContainer = "List"),
                      @ApiResponse(code = 500, message = "The profile is unavailable.", response = RestResponseStatus.class)
                  })
    public Response profileStats(@PathParam("nflowId") String nflowId, @QueryParam("processingdttm") String processingdttm) {
        NflowMetadata nflowMetadata = getMetadataService().getNflowById(nflowId);
        String profileTable = nflowMetadata.getProfileTableName();
        String query = "SELECT * from " + HiveUtils.quoteIdentifier(profileTable) + " where processing_dttm = " + HiveUtils.quoteString(processingdttm);
        QueryResult rows = hiveService.query(query);
        return Response.ok(rows.getRows()).build();
    }

    @GET
    @Path("/{nflowId}/profile-invalid-results")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets statistics on the invalid rows for the specified job.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the invalid row statistics.", response = Map.class, responseContainer = "List"),
                      @ApiResponse(code = 500, message = "The profile is unavailable.", response = RestResponseStatus.class)
                  })
    public Response queryProfileInvalidResults(
        @PathParam("nflowId") String nflowId,
        @QueryParam("processingdttm") String processingdttm,
        @QueryParam("limit") int limit,
        @QueryParam("filter") String filter) {
        NflowMetadata nflowMetadata = getMetadataService().getNflowById(nflowId);
        String condition = "";
        if (StringUtils.isNotBlank(filter)) {
            condition = " and dlp_reject_reason like " + HiveUtils.quoteString("%" + filter + "%") + " ";
        }
        return getPage(processingdttm, limit, nflowMetadata.getInvalidTableName(), condition);
    }

    @GET
    @Path("/{nflowId}/profile-valid-results")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets statistics on the valid rows for the specified job.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the valid row statistics.", response = Map.class, responseContainer = "List"),
                      @ApiResponse(code = 500, message = "The profile is unavailable.", response = RestResponseStatus.class)
                  })
    public Response queryProfileValidResults(
        @PathParam("nflowId") String nflowId,
        @QueryParam("processingdttm") String processingdttm,
        @QueryParam("limit") int limit) {
        NflowMetadata nflowMetadata = getMetadataService().getNflowById(nflowId);
        return getPage(processingdttm, limit, nflowMetadata.getValidTableName());
    }

    @GET
    @Path("{nflowId}/roles")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the list of assigned members the nflow's roles")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the role memberships.", response = ActionGroup.class),
                      @ApiResponse(code = 404, message = "A nflow with the given ID does not exist.", response = RestResponseStatus.class)
                  })
    public Response getRoleMemberships(@PathParam("nflowId") String nflowIdStr,
                                       @QueryParam("verbose") @DefaultValue("false") boolean verbose) {
        // TODO: No longer using verbose; all results are verbose now.
        return this.securityService.getNflowRoleMemberships(nflowIdStr)
            .map(m -> Response.ok(m).build())
            .orElseThrow(() -> new WebApplicationException("A nflow with the given ID does not exist: " + nflowIdStr, Status.NOT_FOUND));
    }


    @POST
    @Path("{nflowId}/roles")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Updates the members of one of a nflow's roles.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "The permissions were changed successfully.", response = ActionGroup.class),
                      @ApiResponse(code = 404, message = "No nflow exists with the specified ID.", response = RestResponseStatus.class)
                  })
    public Response postPermissionsChange(@PathParam("nflowId") String nflowIdStr,
                                          RoleMembershipChange changes) {
        return this.securityService.changeNflowRoleMemberships(nflowIdStr, changes)
            .map(m -> Response.ok(m).build())
            .orElseThrow(() -> new WebApplicationException("Either a nflow with the ID \"" + nflowIdStr
                                                           + "\" does not exist or it does not have a role the named \""
                                                           + changes.getRoleName() + "\"", Status.NOT_FOUND));
    }

    @GET
    @Path("{nflowId}/actions/available")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the list of available actions that may be permitted or revoked on a nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the actions.", response = ActionGroup.class),
                      @ApiResponse(code = 404, message = "A nflow with the given ID does not exist.", response = RestResponseStatus.class)
                  })
    public Response getAvailableActions(@PathParam("nflowId") String nflowIdStr) {
        log.debug("Get available actions for nflow: {}", nflowIdStr);

        return this.securityService.getAvailableNflowActions(nflowIdStr)
            .map(g -> Response.ok(g).build())
            .orElseThrow(() -> new WebApplicationException("A nflow with the given ID does not exist: " + nflowIdStr, Status.NOT_FOUND));
    }

    @GET
    @Path("{nflowId}/actions/allowed")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the list of actions permitted for the given username and/or groups.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the actions.", response = ActionGroup.class),
                      @ApiResponse(code = 404, message = "A nflow with the given ID does not exist.", response = RestResponseStatus.class)
                  })
    public Response getAllowedActions(@PathParam("nflowId") String nflowIdStr,
                                      @QueryParam("user") Set<String> userNames,
                                      @QueryParam("group") Set<String> groupNames) {
        log.debug("Get allowed actions for nflow: {}", nflowIdStr);

        Set<? extends Principal> users = Arrays.stream(this.securityTransform.asUserPrincipals(userNames)).collect(Collectors.toSet());
        Set<? extends Principal> groups = Arrays.stream(this.securityTransform.asGroupPrincipals(groupNames)).collect(Collectors.toSet());

        return this.securityService.getAllowedNflowActions(nflowIdStr, Stream.concat(users.stream(), groups.stream()).collect(Collectors.toSet()))
            .map(g -> Response.ok(g).build())
            .orElseThrow(() -> new WebApplicationException("A nflow with the given ID does not exist: " + nflowIdStr, Status.NOT_FOUND));
    }

    @POST
    @Path("{nflowId}/actions/allowed")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Updates the permissions for a nflow using the supplied permission change request.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "The permissions were changed successfully.", response = ActionGroup.class),
                      @ApiResponse(code = 400, message = "The type is not valid.", response = RestResponseStatus.class),
                      @ApiResponse(code = 404, message = "No nflow exists with the specified ID.", response = RestResponseStatus.class)
                  })
    public Response postPermissionsChange(@PathParam("nflowId") String nflowIdStr,
                                          PermissionsChange changes) {

        return this.securityService.changeNflowPermissions(nflowIdStr, changes)
            .map(g -> Response.ok(g).build())
            .orElseThrow(() -> new WebApplicationException("A nflow with the given ID does not exist: " + nflowIdStr, Status.NOT_FOUND));
    }

    @GET
    @Path("{nflowId}/actions/change")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Constructs and returns a permission change request for a set of users/groups containing the actions that the requester may permit or revoke.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the change request that may be modified by the client and re-posted.", response = PermissionsChange.class),
                      @ApiResponse(code = 400, message = "The type is not valid.", response = RestResponseStatus.class),
                      @ApiResponse(code = 404, message = "No nflow exists with the specified ID.", response = RestResponseStatus.class)
                  })
    public PermissionsChange getAllowedPermissionsChange(@PathParam("nflowId") String nflowIdStr,
                                                         @QueryParam("type") String changeType,
                                                         @QueryParam("user") Set<String> userNames,
                                                         @QueryParam("group") Set<String> groupNames) {
        if (StringUtils.isBlank(changeType)) {
            throw new WebApplicationException("The query parameter \"type\" is required", Status.BAD_REQUEST);
        }

        Set<? extends Principal> users = Arrays.stream(this.securityTransform.asUserPrincipals(userNames)).collect(Collectors.toSet());
        Set<? extends Principal> groups = Arrays.stream(this.securityTransform.asGroupPrincipals(groupNames)).collect(Collectors.toSet());

        return this.securityService.createNflowPermissionChange(nflowIdStr,
                                                               ChangeType.valueOf(changeType.toUpperCase()),
                                                               Stream.concat(users.stream(), groups.stream()).collect(Collectors.toSet()))
            .orElseThrow(() -> new WebApplicationException("A nflow with the given ID does not exist: " + nflowIdStr, Status.NOT_FOUND));
    }


    private Response getPage(String processingdttm, int limit, String table) {
        return getPage(processingdttm, limit, table, null);
    }

    private Response getPage(String processingdttm, int limit, String table, String filter) {
        if (limit > MAX_LIMIT) {
            limit = MAX_LIMIT;
        } else if (limit < 1) {
            limit = 1;
        }
        StringBuilder query = new StringBuilder("SELECT * from " + HiveUtils.quoteIdentifier(table) + " where processing_dttm = " + HiveUtils.quoteString(processingdttm) + " ");
        if (StringUtils.isNotBlank(filter)) {
            query.append(filter);
        }
        query.append(" limit ").append(limit);
        QueryResult rows = hiveService.query(query.toString());
        return Response.ok(rows.getRows()).build();
    }

    @GET
    @Path("/possible-preconditions")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the available preconditions for triggering a nflow.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the available precondition rules.", response = PreconditionRule.class, responseContainer = "List")
    )
    public Response getPossiblePreconditions() {
        List<PreconditionRule> conditions = nflowManagerPreconditionService.getPossiblePreconditions();
        return Response.ok(conditions).build();
    }

    @GET
    @Path("/{nflowId}/sla")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the service level agreements referenced by a nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the service level agreements.", response = NflowServiceLevelAgreement.class, responseContainer = "List"),
                      @ApiResponse(code = 500, message = "The nflow is unavailable.", response = RestResponseStatus.class)
                  })
    public Response getSla(@PathParam("nflowId") String nflowId) {
        accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_SERVICE_LEVEL_AGREEMENTS);
        List<NflowServiceLevelAgreement> sla = serviceLevelAgreementService.getNflowServiceLevelAgreements(nflowId);
        if (sla == null) {
            throw new WebApplicationException("No SLAs for the nflow were found", Response.Status.NOT_FOUND);
        }
        return Response.ok(sla).build();
    }

    @POST
    @Path("/update-all-datasources")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        "Updates ALL  sources/destinations used for the nflow lineage for ALL nflows.  WARNING: This will be an expensive call if you have lots of nflows.  This will remove all existing sources/destinations and revaluate the nflow and its template for sources/destinations")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "All the nflow datasources were updated", response = RestResponseStatus.class),
                  })
    public Response updateAllNflowDataSources() {
        this.accessController.checkPermission(AccessController.SERVICES, MetadataAccessControl.ADMIN_METADATA);
        getMetadataService().updateAllNflowsDatasources();
        return Response.ok(new RestResponseStatus.ResponseStatusBuilder().buildSuccess()).build();
    }


    @POST
    @Path("/{nflowId}/update-datasources")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Updates a nflows sources/destinations used for the NflowLineage.  This will remove all existing sources/destinations and revaluate the nflow and its template for sources/destinations")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "the datasources were updated", response = RestResponseStatus.class),
                  })
    public Response updateNflowDatasources(@PathParam("nflowId") String nflowId) {
        this.accessController.checkPermission(AccessController.SERVICES, MetadataAccessControl.ADMIN_METADATA);
        getMetadataService().updateNflowDatasources(nflowId);
        return Response.ok(new RestResponseStatus.ResponseStatusBuilder().buildSuccess()).build();
    }


    @POST
    @Path("/update-nflow-lineage-styles")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Updates the nflow lineage styles.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "The styles were updated.", response = RestResponseStatus.class)
    )
    public Response updateNflowLineageStyles(Map<String, NflowLineageStyle> styles) {
        accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ADMIN_NFLOWS);

        datasourceService.refreshNflowLineageStyles(styles);
        return Response.ok(new RestResponseStatus.ResponseStatusBuilder().buildSuccess()).build();
    }

    @POST
    @Path("/update-datasource-definitions")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Updates the datasource definitions.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the updated definitions..", response = DatasourceDefinitions.class)
    )
    public DatasourceDefinitions updateDatasourceDefinitions(DatasourceDefinitions definitions) {
        accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ADMIN_NFLOWS);

        if (definitions != null) {
            Set<DatasourceDefinition> updatedDefinitions = datasourceService.updateDatasourceDefinitions(definitions.getDefinitions());
            if (updatedDefinitions != null) {
                return new DatasourceDefinitions(Lists.newArrayList(updatedDefinitions));
            }
        }
        return new DatasourceDefinitions();
    }

    @POST
    @Path("/{nflowId}/upload-file")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Uploads a file to be ingested by a nflow.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "The file is ready to be ingested."),
                      @ApiResponse(code = 500, message = "The file could not be saved.", response = RestResponseStatus.class)
                  })
    public Response uploadFile(@PathParam("nflowId") String nflowId,
                               @FormDataParam("file") InputStream fileInputStream,
                               @FormDataParam("file") FormDataContentDisposition fileMetaData) throws Exception {

        NflowMetadata nflow = getMetadataService().getNflowById(nflowId, false);
        // Derive path and file
        nflow = registeredTemplateService.mergeTemplatePropertiesWithNflow(nflow);
        propertyExpressionResolver.resolvePropertyExpressions(nflow);
        List<NifiProperty> properties = nflow.getProperties();
        String dropzone = null;
        String regexFileFilter = null;
        for (NifiProperty property : properties) {

            if (property.getProcessorType().equals("org.apache.nifi.processors.standard.GetFile")) {
                if (property.getKey().equals("File Filter")) {
                    regexFileFilter = property.getValue();
                } else if (property.getKey().equals("Input Directory")) {
                    dropzone = property.getValue();
                }
            }
        }
        if (StringUtils.isEmpty(regexFileFilter) || StringUtils.isEmpty(dropzone)) {
            throw new IOException("Unable to upload file with empty dropzone and file");
        }
        File tempTarget = File.createTempFile("nova-upload", "");
        String fileName = "";
        try {
            Generex fileNameGenerator = new Generex(regexFileFilter);
            fileName = fileNameGenerator.random();

            // Cleanup oddball characters generated by generex
            fileName = fileName.replaceAll("[^A-Za-z0-9\\.\\_\\+\\%\\-\\|]+", "\\.");
            java.nio.file.Path dropZoneTarget = Paths.get(dropzone, fileName);
            File dropZoneFile = dropZoneTarget.toFile();
            if (dropZoneFile.exists()) {
                throw new IOException("File with the name [" + fileName + "] already exists in [" + dropzone + "]");
            }

            Files.copy(fileInputStream, tempTarget.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.move(tempTarget.toPath(), dropZoneTarget);

            // Set read, write
            dropZoneFile.setReadable(true);
            dropZoneFile.setWritable(true);

        } catch (AccessDeniedException e) {
            String errTemplate = "Permission denied attempting to write file [%s] to [%s]. Check with system administrator to ensure this application has write permissions to folder";
            String err = String.format(errTemplate, fileName, dropzone);
            log.error(err);
            throw new InternalServerErrorException(err);

        } catch (Exception e) {
            String errTemplate = "Unexpected exception writing file [%s] to [%s].";
            String err = String.format(errTemplate, fileName, dropzone);
            log.error(err);
            throw new InternalServerErrorException(err, e);
        }
        return Response.ok("").build();
    }

    private PageRequest pageRequest(Integer start, Integer limit, String sort) {
        if (StringUtils.isNotBlank(sort)) {
            Sort.Direction dir = Sort.Direction.ASC;
            if (sort.startsWith("-")) {
                dir = Sort.Direction.DESC;
                sort = sort.substring(1);
            }
            return new PageRequest((start / limit), limit, dir, sort);
        } else {
            return new PageRequest((start / limit), limit);
        }
    }
}

