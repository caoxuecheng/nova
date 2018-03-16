package com.onescorpin.nflowmgr.service.nflow;

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Sets;
import com.onescorpin.nflowmgr.MetadataFieldAnnotationFieldNameResolver;
import com.onescorpin.nflowmgr.rest.ImportComponent;
import com.onescorpin.nflowmgr.rest.ImportSection;
import com.onescorpin.nflowmgr.rest.ImportType;
import com.onescorpin.nflowmgr.rest.model.NflowCategory;
import com.onescorpin.nflowmgr.rest.model.NflowDataTransformation;
import com.onescorpin.nflowmgr.rest.model.NflowMetadata;
import com.onescorpin.nflowmgr.rest.model.ImportComponentOption;
import com.onescorpin.nflowmgr.rest.model.ImportNflowOptions;
import com.onescorpin.nflowmgr.rest.model.ImportOptions;
import com.onescorpin.nflowmgr.rest.model.ImportProperty;
import com.onescorpin.nflowmgr.rest.model.ImportTemplateOptions;
import com.onescorpin.nflowmgr.rest.model.NifiNflow;
import com.onescorpin.nflowmgr.rest.model.RegisteredTemplate;
import com.onescorpin.nflowmgr.rest.model.RegisteredTemplateRequest;
import com.onescorpin.nflowmgr.rest.model.UploadProgress;
import com.onescorpin.nflowmgr.rest.model.UploadProgressMessage;
import com.onescorpin.nflowmgr.security.NflowServicesAccessControl;
import com.onescorpin.nflowmgr.service.MetadataService;
import com.onescorpin.nflowmgr.service.UploadProgressService;
import com.onescorpin.nflowmgr.service.datasource.DatasourceModelTransform;
import com.onescorpin.nflowmgr.service.template.ExportImportTemplateService;
import com.onescorpin.nflowmgr.service.template.RegisteredTemplateService;
import com.onescorpin.nflowmgr.support.ZipFileUtil;
import com.onescorpin.nflowmgr.util.ImportUtil;
import com.onescorpin.json.ObjectMapperSerializer;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.category.Category;
import com.onescorpin.metadata.api.category.CategoryProvider;
import com.onescorpin.metadata.api.category.security.CategoryAccessControl;
import com.onescorpin.metadata.api.datasource.DatasourceProvider;
import com.onescorpin.metadata.api.datasource.UserDatasource;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.security.NflowAccessControl;
import com.onescorpin.metadata.api.template.security.TemplateAccessControl;
import com.onescorpin.metadata.rest.model.data.Datasource;
import com.onescorpin.nifi.rest.model.NifiProperty;
import com.onescorpin.nifi.rest.support.NifiPropertyUtil;
import com.onescorpin.policy.PolicyPropertyTypes;
import com.onescorpin.security.AccessController;
import com.onescorpin.support.NflowNameUtil;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

/**
 * Service used to export and import nflows
 */
public class ExportImportNflowService {

    private static final Logger log = LoggerFactory.getLogger(ExportImportNflowService.class);

    public static final String NFLOW_JSON_FILE = "nflow.json";

    @Inject
    MetadataService metadataService;

    @Inject
    CategoryProvider categoryProvider;

    @Inject
    MetadataAccess metadataAccess;

    @Inject
    ExportImportTemplateService exportImportTemplateService;

    @Inject
    private AccessController accessController;

    @Inject
    private UploadProgressService uploadProgressService;

    /**
     * Provides access to {@code Datasource} objects.
     */
    @Inject
    private DatasourceProvider datasourceProvider;

    /**
     * The {@code Datasource} transformer
     */
    @Inject
    private DatasourceModelTransform datasourceTransform;

    @Inject
    private RegisteredTemplateService registeredTemplateService;

    //Export

    /**
     * Export a nflow as a zip file
     *
     * @param nflowId the id {@link Nflow#getId()} of the nflow to export
     * @return object containing the zip file with data about the nflow.
     */
    public ExportNflow exportNflow(String nflowId) throws IOException {
        this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.EXPORT_NFLOWS);
        this.metadataService.checkNflowPermission(nflowId, NflowAccessControl.EXPORT);

        // Prepare nflow metadata
        final NflowMetadata nflow = metadataService.getNflowById(nflowId);

        if (nflow == null) {
            //nflow will not be found when user is allowed to export nflows but has no entity access to nflow with nflow id
            throw new NotFoundException("Nflow not found for id " + nflowId);
        }

        final List<Datasource> userDatasources = Optional.ofNullable(nflow.getDataTransformation())
            .map(NflowDataTransformation::getDatasourceIds)
            .map(datasourceIds -> metadataAccess.read(
                () ->
                    datasourceIds.stream()
                        .map(datasourceProvider::resolve)
                        .map(datasourceProvider::getDatasource)
                        .map(domain -> datasourceTransform.toDatasource(domain, DatasourceModelTransform.Level.FULL))
                        .map(datasource -> {
                            // Clear sensitive fields
                            datasource.getDestinationForNflows().clear();
                            datasource.getSourceForNflows().clear();
                            return datasource;
                        })
                        .collect(Collectors.toList())
                 )
            )
            .orElse(null);
        if (userDatasources != null && !userDatasources.isEmpty()) {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_DATASOURCES);
            nflow.setUserDatasources(userDatasources);
        }

        // Add nflow json to template zip file
        final ExportImportTemplateService.ExportTemplate exportTemplate = exportImportTemplateService.exportTemplateForNflowExport(nflow.getTemplateId());
        final String nflowJson = ObjectMapperSerializer.serialize(nflow);

        final byte[] zipFile = ZipFileUtil.addToZip(exportTemplate.getFile(), nflowJson, NFLOW_JSON_FILE);
        return new ExportNflow(nflow.getSystemNflowName() + ".nflow.zip", zipFile);
    }

    //Validate

    /**
     * Validate a nflow for importing
     *
     * @param fileName the name of the file to import
     * @param content  the contents of the nflow zip file
     * @param options  user options about what/how it should be imported
     * @return the nflow data to import
     */
    public ImportNflow validateNflowForImport(final String fileName, byte[] content, ImportNflowOptions options) throws IOException {
        this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.IMPORT_NFLOWS);
        ImportNflow importNflow = null;
        UploadProgressMessage nflowImportStatusMessage = uploadProgressService.addUploadStatus(options.getUploadKey(), "Validating Nflow import.");
        boolean isValid = ZipFileUtil.validateZipEntriesWithRequiredEntries(content, getValidZipFileEntries(), Sets.newHashSet(NFLOW_JSON_FILE));
        if (!isValid) {
            nflowImportStatusMessage.update("Validation error. Nflow import error. The zip file you uploaded is not valid nflow export.", false);
            throw new ImportNflowException("The zip file you uploaded is not valid nflow export.");
        }

        try {
            //get the Nflow Data
            importNflow = readNflowJson(fileName, content);
            //initially mark as valid.
            importNflow.setValid(true);
            //merge in the file components to the user options
            Set<ImportComponentOption> componentOptions = ImportUtil.inspectZipComponents(content, ImportType.NFLOW);
            options.addOptionsIfNotExists(componentOptions);
            importNflow.setImportOptions(options);

            //validate the import

            //read the JSON into the Nflow object
            NflowMetadata metadata = importNflow.getNflowToImport();

            //validate the incoming category exists
            validateNflowCategory(importNflow, options, metadata);

            //verify if we should overwrite the nflow if it already exists
            String nflowCategory = StringUtils.isNotBlank(options.getCategorySystemName()) ? options.getCategorySystemName() : metadata.getSystemCategoryName();
            //query for this nflow.
            //first read in the nflow as a service account
            NflowMetadata existingNflow = metadataAccess.read(() -> {
                return metadataService.getNflowByName(nflowCategory, metadata.getSystemNflowName());
            }, MetadataAccess.SERVICE);
            if (!validateOverwriteExistingNflow(existingNflow, metadata, importNflow)) {
                //exit
                return importNflow;
            }

            if (accessController.isEntityAccessControlled()) {
                if (!validateEntityAccess(existingNflow, nflowCategory, metadata, importNflow)) {
                    return importNflow;
                }
            }

            //sensitive properties
            if (!validateSensitiveProperties(metadata, importNflow, options)) {
                return importNflow;
            }

            // Valid data sources
            if (!validateUserDatasources(metadata, importNflow, options)) {
                return importNflow;
            }

            //UploadProgressMessage statusMessage = uploadProgressService.addUploadStatus(options.getUploadKey(),"Validating the template data");
            ExportImportTemplateService.ImportTemplate importTemplate = exportImportTemplateService.validateTemplateForImport(importNflow.getFileName(), content, options);
            // need to set the importOptions back to the nflow options
            //find importOptions for the Template and add them back to the set of options
            //importNflow.getImportOptions().updateOptions(importTemplate.getImportOptions().getImportComponentOptions());
            importNflow.setTemplate(importTemplate);
            // statusMessage.update("Validated the template data",importTemplate.isValid());
            if (!importTemplate.isValid()) {
                importNflow.setValid(false);
                List<String> errorMessages = importTemplate.getTemplateResults().getAllErrors().stream().map(nifiError -> nifiError.getMessage()).collect(Collectors.toList());
                if (!errorMessages.isEmpty()) {
                    for (String msg : errorMessages) {
                        importNflow.addErrorMessage(metadata, msg);
                    }
                }
            }
            //  statusMessage = uploadProgressService.addUploadStatus(options.getUploadKey(),"Validation complete: the nflow is "+(importNflow.isValid() ? "valid" : "invalid"),true,importNflow.isValid());

        } catch (Exception e) {
            nflowImportStatusMessage.update("Validation error. Nflow import error: " + e.getMessage(), false);
            throw new UnsupportedOperationException("Error importing template  " + fileName + ".  " + e.getMessage());
        }
        nflowImportStatusMessage.update("Validated Nflow import.", importNflow.isValid());
        return importNflow;
    }

    private Set<String> getValidZipFileEntries() {
        // do not include nifiConnectingReusableTemplate.xml - it may or may not be there or there can be many of them if flow connects to multiple reusable templates
        String[] entries = {
            NFLOW_JSON_FILE,
            ExportImportTemplateService.NIFI_TEMPLATE_XML_FILE,
            ExportImportTemplateService.TEMPLATE_JSON_FILE
        };
        return Sets.newHashSet(entries);
    }

    private boolean validateSensitiveProperties(NflowMetadata metadata, ImportNflow importNflow, ImportNflowOptions importOptions) {
        //detect any sensitive properties and prompt for input before proceeding
        UploadProgressMessage statusMessage = uploadProgressService.addUploadStatus(importNflow.getImportOptions().getUploadKey(), "Validating nflow properties.");
        List<NifiProperty> sensitiveProperties = metadata.getSensitiveProperties();
        ImportUtil.addToImportOptionsSensitiveProperties(importOptions, sensitiveProperties, ImportComponent.NFLOW_DATA);
        boolean valid = ImportUtil.applyImportPropertiesToNflow(metadata, importNflow, ImportComponent.NFLOW_DATA);
        if (!valid) {
            statusMessage.update("Validation Error. Additional properties are needed before uploading the nflow.", false);
            importNflow.setValid(false);
        } else {
            statusMessage.update("Validated nflow properties.", valid);
        }
        completeSection(importNflow.getImportOptions(), ImportSection.Section.VALIDATE_PROPERTIES);
        return valid;

    }

    /**
     * Validates that user data sources can be imported with provided properties.
     *
     * @param metadata      the nflow data
     * @param importNflow    the import request
     * @param importOptions the import options
     * @return {@code true} if the nflow can be imported, or {@code false} otherwise
     */
    private boolean validateUserDatasources(@Nonnull final NflowMetadata metadata, @Nonnull final ImportNflow importNflow, @Nonnull final ImportNflowOptions importOptions) {
        final UploadProgressMessage statusMessage = uploadProgressService.addUploadStatus(importNflow.getImportOptions().getUploadKey(), "Validating data sources.");

        // Get data sources needing to be created
        final Set<String> availableDatasources = metadataAccess.read(
            () -> datasourceProvider.getDatasources(datasourceProvider.datasetCriteria().type(UserDatasource.class)).stream()
                .map(com.onescorpin.metadata.api.datasource.Datasource::getId)
                .map(Object::toString)
                .collect(Collectors.toSet())
        );
        final ImportComponentOption componentOption = importOptions.findImportComponentOption(ImportComponent.USER_DATASOURCES);
        final List<Datasource> providedDatasources = Optional.ofNullable(metadata.getUserDatasources()).orElse(Collections.emptyList());

        if (componentOption.getProperties().isEmpty()) {
            componentOption.setProperties(
                providedDatasources.stream()
                    .filter(datasource -> !availableDatasources.contains(datasource.getId()))
                    .map(datasource -> new ImportProperty(datasource.getName(), datasource.getId(), null, null, null))
                    .collect(Collectors.toList())
            );
        }

        // Update nflow with re-mapped data sources
        final boolean valid = componentOption.getProperties().stream()
            .allMatch(property -> {
                if (property.getPropertyValue() != null) {
                    ImportUtil.replaceDatasource(metadata, property.getProcessorId(), property.getPropertyValue());
                    return true;
                } else {
                    return false;
                }
            });

        if (valid) {
            statusMessage.update("Validated data sources.", true);
        } else {
            statusMessage.update("Validation Error. Additional properties are needed before uploading the nflow.", false);
            importNflow.setValid(false);
        }

        completeSection(importNflow.getImportOptions(), ImportSection.Section.VALIDATE_USER_DATASOURCES);
        return valid;
    }

    private boolean validateEntityAccess(NflowMetadata existingNflow, String nflowCategory, NflowMetadata importingNflow, ImportNflow nflow) {
        if (existingNflow != null) {
            NflowMetadata userAccessNflow = metadataAccess.read(() -> {
                return metadataService.getNflowByName(nflowCategory, importingNflow.getSystemNflowName());
            });
            if (userAccessNflow == null || !userAccessNflow.hasAction(NflowAccessControl.EDIT_DETAILS.getSystemName())) {
                //error
                nflow.setValid(false);
                if (nflow.getTemplate() == null) {
                    ExportImportTemplateService.ImportTemplate importTemplate = new ExportImportTemplateService.ImportTemplate(nflow.getFileName());
                    nflow.setTemplate(importTemplate);
                }
                String msg = "Access Denied.  You do not have access to edit this nflow.";
                nflow.getImportOptions().addErrorMessage(ImportComponent.NFLOW_DATA, msg);
                nflow.addErrorMessage(existingNflow, msg);
                nflow.setValid(false);
                return false;
            } else {
                return true;
            }

        } else {
            //ensure the user can create under the category
            Category category = metadataAccess.read(() -> {
                return categoryProvider.findBySystemName(nflowCategory);
            }, MetadataAccess.SERVICE);

            if (category == null) {
                //ensure the user has functional access to create categories
                boolean hasPermission = accessController.hasPermission(AccessController.SERVICES, NflowServicesAccessControl.EDIT_CATEGORIES);
                if (!hasPermission) {
                    String msg = "Access Denied. The category for this nflow," + nflowCategory + ", doesn't exist and you do not have access to create a new category.";
                    nflow.getImportOptions().addErrorMessage(ImportComponent.NFLOW_DATA, msg);
                    nflow.addErrorMessage(existingNflow, msg);
                    nflow.setValid(false);
                    return false;
                }
                return true;
            } else {
                //if the nflow is new ensure the user has write access to create nflows
                return metadataAccess.read(() -> {
                    //Query for Category and ensure the user has access to create nflows on that category
                    Category domainCategory = categoryProvider.findBySystemName(nflowCategory);
                    if (domainCategory == null || (!domainCategory.getAllowedActions().hasPermission(CategoryAccessControl.CREATE_NFLOW))) {
                        String msg = "Access Denied. You do not have access to create nflows under the category " + nflowCategory
                                     + ". Attempt made to create nflow " + NflowNameUtil.fullName(nflowCategory, importingNflow.getSystemNflowName()) + ".";
                        nflow.getImportOptions().addErrorMessage(ImportComponent.NFLOW_DATA, msg);
                        nflow.addErrorMessage(existingNflow, msg);
                        nflow.setValid(false);
                        return false;
                    }

                    /*
                       TemplateAccessControl.CREATE_NFLOW permission is not being used right now.
                       Uncomment this code once/if we should be checking it

                    // Query for Template and ensure the user has access to create nflows
                    final RegisteredTemplate domainTemplate = registeredTemplateService.findRegisteredTemplate(
                        new RegisteredTemplateRequest.Builder().templateName(importingNflow.getTemplateName()).isNflowEdit(true).build());
                    if (domainTemplate != null && !registeredTemplateService.hasTemplatePermission(domainTemplate.getId(), TemplateAccessControl.CREATE_NFLOW)) {
                        final String msg = "Access Denied. You do not have access to create nflows using the template " + importingNflow.getTemplateName()
                                           + ". Attempt made to create nflow " + NflowNameUtil.fullName(nflowCategory, importingNflow.getSystemNflowName()) + ".";
                        nflow.getImportOptions().addErrorMessage(ImportComponent.NFLOW_DATA, msg);
                        nflow.addErrorMessage(existingNflow, msg);
                        nflow.setValid(false);
                        return false;
                    }
                    */
                    return true;
                });
            }

        }
    }

    private boolean validateOverwriteExistingNflow(NflowMetadata existingNflow, NflowMetadata importingNflow, ImportNflow nflow) {
        if (existingNflow != null && !nflow.getImportOptions().isImportAndOverwrite(ImportComponent.NFLOW_DATA)) {
            UploadProgressMessage
                statusMessage =
                uploadProgressService.addUploadStatus(nflow.getImportOptions().getUploadKey(), "Validation error. " + importingNflow.getCategoryAndNflowName() + " already exists.", true, false);
            //if we dont have permission to overwrite then return with error that nflow already exists
            nflow.setValid(false);
            ExportImportTemplateService.ImportTemplate importTemplate = new ExportImportTemplateService.ImportTemplate(nflow.getFileName());
            nflow.setTemplate(importTemplate);
            String msg = "The nflow " + existingNflow.getCategoryAndNflowName()
                         + " already exists.";
            nflow.getImportOptions().addErrorMessage(ImportComponent.NFLOW_DATA, msg);
            nflow.addErrorMessage(existingNflow, msg);
            nflow.setValid(false);
            return false;
        } else {
            String message = "Validated Nflow data.  This import will " + (existingNflow != null ? "overwrite" : "create") + " the nflow " + importingNflow.getCategoryAndNflowName();
            uploadProgressService.addUploadStatus(nflow.getImportOptions().getUploadKey(), message, true, true);
        }

        completeSection(nflow.getImportOptions(), ImportSection.Section.VALIDATE_NFLOW);
        return true;
    }

    private boolean validateNflowCategory(ImportNflow importNflow, ImportNflowOptions importOptions, NflowMetadata metadata) {
        boolean valid = true;
        if (StringUtils.isNotBlank(importOptions.getCategorySystemName())) {
            UploadProgressMessage
                statusMessage =
                uploadProgressService.addUploadStatus(importOptions.getUploadKey(), "Validating the newly specified category. Ensure " + importOptions.getCategorySystemName() + " exists.");
            NflowCategory optionsCategory = metadataService.getCategoryBySystemName(importOptions.getCategorySystemName());
            if (optionsCategory == null) {
                importNflow.setValid(false);
                statusMessage.update("Validation Error. The category " + importOptions.getCategorySystemName() + " does not exist, or you dont have access to it.", false);
                valid = false;
            } else {
                if (valid) {
                    metadata.getCategory().setSystemName(importOptions.getCategorySystemName());
                    statusMessage.update("Validated. The category " + importOptions.getCategorySystemName() + " exists.", true);
                }
            }
        }
        completeSection(importOptions, ImportSection.Section.VALIDATE_NFLOW_CATEGORY);
        return valid;
    }

    //Import

    /**
     * Import a nflow zip file
     *
     * @param fileName      the name of the file
     * @param content       the file content
     * @param importOptions user options about what/how it should be imported
     * @return the nflow data to import
     */
    public ImportNflow importNflow(String fileName, byte[] content, ImportNflowOptions importOptions) throws Exception {
        this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.IMPORT_NFLOWS);
        UploadProgress progress = uploadProgressService.getUploadStatus(importOptions.getUploadKey());
        progress.setSections(ImportSection.sectionsForImportAsString(ImportType.NFLOW));

        ImportNflow nflow = validateNflowForImport(fileName, content, importOptions);

        if (nflow.isValid()) {
            //read the JSON into the Nflow object
            NflowMetadata metadata = nflow.getNflowToImport();
            //query for this nflow.
            String nflowCategory = StringUtils.isNotBlank(importOptions.getCategorySystemName()) ? importOptions.getCategorySystemName() : metadata.getSystemCategoryName();
            NflowMetadata existingNflow = metadataAccess.read(() -> metadataService.getNflowByName(nflowCategory, metadata.getSystemNflowName()));

            metadata.getCategory().setSystemName(nflowCategory);

            ImportTemplateOptions importTemplateOptions = new ImportTemplateOptions();
            importTemplateOptions.setImportComponentOptions(importOptions.getImportComponentOptions());
            importTemplateOptions.findImportComponentOption(ImportComponent.TEMPLATE_DATA).setContinueIfExists(true);
            ExportImportTemplateService.ImportTemplate importTemplate = nflow.getTemplate();
            importTemplate.setImportOptions(importTemplateOptions);
            importTemplateOptions.setUploadKey(importOptions.getUploadKey());
            importTemplate.setValid(true);
            ExportImportTemplateService.ImportTemplate template = exportImportTemplateService.importZipForNflowImport(importTemplate);
            if (template.isSuccess()) {
                //import the nflow
                nflow.setTemplate(template);
                //now that we have the Nflow object we need to create the instance of the nflow
                UploadProgressMessage uploadProgressMessage = uploadProgressService.addUploadStatus(importOptions.getUploadKey(), "Saving  and creating nflow instance in NiFi");

                metadata.setIsNew(existingNflow == null ? true : false);
                metadata.setNflowId(existingNflow != null ? existingNflow.getNflowId() : null);
                metadata.setId(existingNflow != null ? existingNflow.getId() : null);
                //reassign the templateId to the newly registered template id
                metadata.setTemplateId(template.getTemplateId());
                if (metadata.getRegisteredTemplate() != null) {
                    metadata.getRegisteredTemplate().setNifiTemplateId(template.getNifiTemplateId());
                    metadata.getRegisteredTemplate().setId(template.getTemplateId());
                }
                //get/create category
                NflowCategory category = metadataService.getCategoryBySystemName(metadata.getCategory().getSystemName());
                if (category == null) {
                    metadata.getCategory().setId(null);
                    metadataService.saveCategory(metadata.getCategory());
                } else {
                    metadata.setCategory(category);
                }
                if (importOptions.isDisableUponImport()) {
                    metadata.setActive(false);
                    metadata.setState(NflowMetadata.STATE.DISABLED.name());
                }

                //remap any preconditions to this new nflow/category name.
                if (metadata.getSchedule().hasPreconditions()) {
                    metadata.getSchedule().getPreconditions().stream()
                        .flatMap(preconditionRule -> preconditionRule.getProperties().stream())
                        .filter(fieldRuleProperty -> PolicyPropertyTypes.PROPERTY_TYPE.currentNflow.name().equals(fieldRuleProperty.getType()))
                        .forEach(fieldRuleProperty -> fieldRuleProperty.setValue(metadata.getCategoryAndNflowName()));
                }

                ////for all those properties where the template value is != userEditable and the template value has a metadata. property, remove that property from the nflow properties so it can be imported and assigned correctly
                RegisteredTemplate template1 = registeredTemplateService.findRegisteredTemplateById(template.getTemplateId());
                if (template1 != null) {

                    //Find all the properties in the template that have ${metadata. and are not userEditable.
                    //These are the properties we need to replace on the nflow metadata
                    List<NifiProperty> metadataProperties = template1.getProperties().stream().filter(nifiProperty -> {

                        return nifiProperty != null && StringUtils.isNotBlank(nifiProperty.getValue()) && !nifiProperty.isUserEditable() && nifiProperty.getValue().contains("${" +
                                                                                                                                                                             MetadataFieldAnnotationFieldNameResolver.metadataPropertyPrefix);
                    }).collect(Collectors.toList());

                    //Replace the Nflow Metadata properties with those that match the template ones from above.
                    List<NifiProperty> updatedProperties = metadata.getProperties().stream().map(nifiProperty -> {
                        NifiProperty p = NifiPropertyUtil.findPropertyByProcessorName(metadataProperties, nifiProperty);
                        return p != null ? p : nifiProperty;
                    }).collect(Collectors.toList());
                    metadata.setProperties(updatedProperties);

                }

                NifiNflow nifiNflow = metadataService.createNflow(metadata);

                if (nifiNflow != null) {
                    nflow.setNflowName(nifiNflow.getNflowMetadata().getCategoryAndNflowName());
                    uploadProgressMessage.update("Successfully saved the nflow " + nflow.getNflowName(), true);
                }
                nflow.setNifiNflow(nifiNflow);
                nflow.setSuccess(nifiNflow != null && nifiNflow.isSuccess());
            } else {
                nflow.setSuccess(false);
                nflow.setTemplate(template);
                nflow.addErrorMessage(existingNflow, "The nflow " + NflowNameUtil.fullName(nflowCategory, metadata.getSystemNflowName())
                                                   + " needs additional properties to be supplied before importing.");

            }

            completeSection(importOptions, ImportSection.Section.IMPORT_NFLOW_DATA);
        }
        return nflow;
    }

    //Utility

    private void completeSection(ImportOptions options, ImportSection.Section section) {
        UploadProgress progress = uploadProgressService.getUploadStatus(options.getUploadKey());
        progress.completeSection(section.name());
    }

    private ImportNflow readNflowJson(String fileName, byte[] content) throws IOException {

        byte[] buffer = new byte[1024];
        InputStream inputStream = new ByteArrayInputStream(content);
        ZipInputStream zis = new ZipInputStream(inputStream);
        ZipEntry zipEntry;
        // while there are entries I process them
        ImportNflow importNflow = new ImportNflow(fileName);

        while ((zipEntry = zis.getNextEntry()) != null) {

            if (zipEntry.getName().startsWith(NFLOW_JSON_FILE)) {
                String zipEntryContents = ZipFileUtil.zipEntryToString(buffer, zis, zipEntry);
                importNflow.setNflowJson(zipEntryContents);
            }
        }
        return importNflow;
    }

    //Internal classes

    public class ExportNflow {

        private String fileName;
        private byte[] file;

        public ExportNflow(String fileName, byte[] file) {
            this.fileName = fileName;
            this.file = file;
        }

        public String getFileName() {
            return fileName;
        }

        public byte[] getFile() {
            return file;
        }
    }

    public static class ImportNflow {

        private boolean valid;

        private boolean success;
        private String fileName;
        private String nflowName;
        private ExportImportTemplateService.ImportTemplate template;
        private NifiNflow nifiNflow;
        private String nflowJson;
        private ImportNflowOptions importOptions;

        @JsonIgnore
        private NflowMetadata nflowToImport;

        public ImportNflow() {
        }

        public ImportNflow(String fileName) {
            this.fileName = fileName;
            this.template = new ExportImportTemplateService.ImportTemplate(fileName);
        }

        public String getNflowJson() {
            return nflowJson;
        }

        public void setNflowJson(String nflowJson) {
            this.nflowJson = nflowJson;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public ExportImportTemplateService.ImportTemplate getTemplate() {
            return template;
        }

        public void setTemplate(ExportImportTemplateService.ImportTemplate template) {
            this.template = template;
        }

        public String getNflowName() {
            return nflowName;
        }

        public void setNflowName(String nflowName) {
            this.nflowName = nflowName;
        }

        public NifiNflow getNifiNflow() {
            return nifiNflow;
        }

        public void setNifiNflow(NifiNflow nifiNflow) {
            this.nifiNflow = nifiNflow;
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public void addErrorMessage(NflowMetadata nflowMetadata, String errorMessage) {
            if (nifiNflow == null) {
                nifiNflow = new NifiNflow(nflowMetadata, null);
            }
            nifiNflow.addErrorMessage(errorMessage);
        }

        public ImportNflowOptions getImportOptions() {
            return importOptions;
        }

        public void setImportOptions(ImportNflowOptions importOptions) {
            this.importOptions = importOptions;
        }

        @JsonIgnore
        public NflowMetadata getNflowToImport() {
            if (nflowToImport == null && StringUtils.isNotBlank(nflowJson)) {
                nflowToImport = ObjectMapperSerializer.deserialize(getNflowJson(), NflowMetadata.class);
            }
            return nflowToImport;
        }

        @JsonIgnore
        public void setNflowToImport(NflowMetadata nflowToImport) {
            this.nflowToImport = nflowToImport;
        }
    }


}
