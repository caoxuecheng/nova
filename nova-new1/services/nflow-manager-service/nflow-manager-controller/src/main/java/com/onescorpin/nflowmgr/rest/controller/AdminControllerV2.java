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

import com.fasterxml.jackson.core.type.TypeReference;
import com.onescorpin.cluster.ClusterService;
import com.onescorpin.nflowmgr.rest.model.ImportComponentOption;
import com.onescorpin.nflowmgr.rest.model.ImportNflowOptions;
import com.onescorpin.nflowmgr.rest.model.ImportTemplateOptions;
import com.onescorpin.nflowmgr.rest.model.UploadProgress;
import com.onescorpin.nflowmgr.security.NflowServicesAccessControl;
import com.onescorpin.nflowmgr.service.MetadataService;
import com.onescorpin.nflowmgr.service.UploadProgressService;
import com.onescorpin.nflowmgr.service.nflow.ExportImportNflowService;
import com.onescorpin.nflowmgr.service.template.ExportImportTemplateService;
import com.onescorpin.nflowmgr.util.ImportUtil;
import com.onescorpin.json.ObjectMapperSerializer;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.nflow.OpsManagerNflowProvider;
import com.onescorpin.metadata.api.nflow.security.NflowOpsAccessControlProvider;
import com.onescorpin.metadata.jpa.cache.AbstractCacheBackedProvider;
import com.onescorpin.metadata.jpa.nflow.OpsManagerNflowCacheById;
import com.onescorpin.metadata.jpa.nflow.OpsManagerNflowCacheByName;
import com.onescorpin.metadata.jpa.nflow.security.NflowAclCache;
import com.onescorpin.metadata.jpa.sla.ServiceLevelAgreementDescriptionCache;
import com.onescorpin.rest.model.RestResponseStatus;
import com.onescorpin.security.AccessController;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;

/**
 * REST API for administrative functions.
 */
@Api(tags = "Nflow Manager - Administration", produces = "application/json")
@Path(AdminControllerV2.BASE)
@SwaggerDefinition(tags = @Tag(name = "Nflow Manager - Administration", description = "administrator operations"))
public class AdminControllerV2 {

    private static final Logger log = LoggerFactory.getLogger(AdminControllerV2.class);


    public static final String BASE = "/v2/nflowmgr/admin";
    public static final String IMPORT_TEMPLATE = "/import-template";
    public static final String IMPORT_NFLOW = "/import-nflow";

    @Inject
    ExportImportTemplateService exportImportTemplateService;

    @Inject
    ExportImportNflowService exportImportNflowService;

    @Inject
    UploadProgressService uploadProgressService;

    @Inject
    private ClusterService clusterService;

    /**
     * Nflow manager metadata service
     */
    @Inject
    MetadataService metadataService;

    @Inject
    NflowAclCache nflowAclCache;

    @Inject
    OpsManagerNflowCacheById opsManagerNflowCacheById;

    @Inject
    OpsManagerNflowCacheByName opsManagerNflowCacheByName;

    @Inject
    NflowOpsAccessControlProvider nflowOpsAccessControlProvider;

    @Inject
    OpsManagerNflowProvider opsManagerNflowProvider;

    @Inject
    ServiceLevelAgreementDescriptionCache serviceLevelAgreementDescriptionCache;

    @Inject
    private AccessController accessController;

    @Inject
    private MetadataAccess metadataAccess;

    @GET
    @Path("/upload-status/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets thet status of a given upload/import.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the upload status")
                  })
    public Response uploadStatus(@NotNull @PathParam("key") String key) {
        UploadProgress uploadProgress = uploadProgressService.getUploadStatus(key);
        if (uploadProgress != null) {
            uploadProgress.checkAndIncrementPercentage();
            return Response.ok(uploadProgress).build();
        } else {
            return Response.ok(uploadProgress).build();
        }
    }


    @POST
    @Path(IMPORT_NFLOW)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Imports a nflow zip file.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the nflow metadata.", response = ExportImportNflowService.ImportNflow.class),
                      @ApiResponse(code = 500, message = "There was a problem importing the nflow.", response = RestResponseStatus.class)
                  })
    public Response uploadNflow(@NotNull @FormDataParam("file") InputStream fileInputStream,
                               @NotNull @FormDataParam("file") FormDataContentDisposition fileMetaData,
                               @NotNull @FormDataParam("uploadKey") String uploadKey,
                               @FormDataParam("categorySystemName") String categorySystemName,
                               @FormDataParam("disableNflowUponImport") @DefaultValue("false") boolean disableNflowUponImport,
                               @FormDataParam("importComponents") String importComponents)
        throws Exception {
        ImportNflowOptions options = new ImportNflowOptions();
        options.setUploadKey(uploadKey);
        options.setDisableUponImport(disableNflowUponImport);
        ExportImportNflowService.ImportNflow importNflow = null;

        options.setCategorySystemName(categorySystemName);

        boolean overwriteNflow = true;
        boolean overwriteTemplate = true;
        uploadProgressService.newUpload(uploadKey);

        if (importComponents == null) {
            byte[] content = ImportUtil.streamToByteArray(fileInputStream);
            importNflow = exportImportNflowService.validateNflowForImport(fileMetaData.getFileName(), content, options);
            importNflow.setSuccess(false);
        } else {
            options.setImportComponentOptions(ObjectMapperSerializer.deserialize(importComponents, new TypeReference<Set<ImportComponentOption>>() {
            }));
            byte[] content = ImportUtil.streamToByteArray(fileInputStream);
            importNflow = exportImportNflowService.importNflow(fileMetaData.getFileName(), content, options);
        }
        uploadProgressService.removeUpload(uploadKey);
        return Response.ok(importNflow).build();
    }

    @POST
    @Path(IMPORT_TEMPLATE)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Imports a template xml or zip file.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the template metadata.", response = ExportImportTemplateService.ImportTemplate.class),
                      @ApiResponse(code = 500, message = "There was a problem importing the template.", response = RestResponseStatus.class)
                  })
    public Response uploadTemplate(@NotNull @FormDataParam("file") InputStream fileInputStream,
                                   @NotNull @FormDataParam("file") FormDataContentDisposition fileMetaData,
                                   @NotNull @FormDataParam("uploadKey") String uploadKey,
                                   @FormDataParam("importComponents") String importComponents)
        throws Exception {
        ImportTemplateOptions options = new ImportTemplateOptions();
        options.setUploadKey(uploadKey);
        ExportImportTemplateService.ImportTemplate importTemplate = null;
        byte[] content = ImportUtil.streamToByteArray(fileInputStream);

        uploadProgressService.newUpload(uploadKey);

        if (importComponents == null) {
            importTemplate = exportImportTemplateService.validateTemplateForImport(fileMetaData.getFileName(), content, options);
            importTemplate.setSuccess(false);
        } else {
            options.setImportComponentOptions(ObjectMapperSerializer.deserialize(importComponents, new TypeReference<Set<ImportComponentOption>>() {
            }));
            importTemplate = exportImportTemplateService.importTemplate(fileMetaData.getFileName(), content, options);
        }
        return Response.ok(importTemplate).build();
    }

    @GET
    @Path("/cache-summary")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the size of the Nflow and NflowACL cache")
    public Response getCacheSizes() {
        Long nflowAclSize = nflowAclCache.size();
        Long nflowCacheNameSize = opsManagerNflowCacheByName.size();
        Long nflowCacheIdSize = opsManagerNflowCacheById.size();
        Long slaDescriptionSize = serviceLevelAgreementDescriptionCache.size();
        CacheSummary cacheSummary = new CacheSummary(nflowAclSize, nflowCacheNameSize, nflowCacheIdSize, slaDescriptionSize);
        return Response.ok(cacheSummary).build();

    }

    @POST
    @Path("/reset-cache")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Resets the Nflow and NflowACL cache.")
    public Response refreshCache() {
        accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ADMIN_NFLOWS);
        log.info("RESET Nflow, NflowAcl, and SLA description caches");
        metadataAccess.read(() ->((AbstractCacheBackedProvider) nflowOpsAccessControlProvider).refreshCache(), MetadataAccess.SERVICE);
        metadataAccess.read(() ->((AbstractCacheBackedProvider) opsManagerNflowProvider).refreshCache(), MetadataAccess.SERVICE);
        metadataAccess.read(() ->  serviceLevelAgreementDescriptionCache.refreshCache(), MetadataAccess.SERVICE);
        return getCacheSizes();
    }


    private class CacheSummary {

        Long nflowAclSize;
        Long nflowByNameSize;
        Long nflowByIdSize;
        Long slaDescriptionSize;

        public CacheSummary() {

        }

        public CacheSummary(Long nflowAclSize, Long nflowByNameSize, Long nflowByIdSize,Long slaDescriptionSize) {
            this.nflowAclSize = nflowAclSize;
            this.nflowByNameSize = nflowByNameSize;
            this.nflowByIdSize = nflowByIdSize;
            this.slaDescriptionSize = slaDescriptionSize;
        }

        public Long getNflowAclSize() {
            return nflowAclSize;
        }

        public void setNflowAclSize(Long nflowAclSize) {
            this.nflowAclSize = nflowAclSize;
        }

        public Long getNflowByNameSize() {
            return nflowByNameSize;
        }

        public void setNflowByNameSize(Long nflowByNameSize) {
            this.nflowByNameSize = nflowByNameSize;
        }

        public Long getNflowByIdSize() {
            return nflowByIdSize;
        }

        public void setNflowByIdSize(Long nflowByIdSize) {
            this.nflowByIdSize = nflowByIdSize;
        }

        public Long getSlaDescriptionSize() {
            return slaDescriptionSize;
        }

        public void setSlaDescriptionSize(Long slaDescriptionSize) {
            this.slaDescriptionSize = slaDescriptionSize;
        }
    }

}
