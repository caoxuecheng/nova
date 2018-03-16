package com.onescorpin.nflowmgr.util;

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

import com.onescorpin.nflowmgr.rest.ImportComponent;
import com.onescorpin.nflowmgr.rest.ImportType;
import com.onescorpin.nflowmgr.rest.model.NflowDataTransformation;
import com.onescorpin.nflowmgr.rest.model.NflowMetadata;
import com.onescorpin.nflowmgr.rest.model.ImportComponentOption;
import com.onescorpin.nflowmgr.rest.model.ImportOptions;
import com.onescorpin.nflowmgr.rest.model.ImportProperty;
import com.onescorpin.nflowmgr.rest.model.RegisteredTemplate;
import com.onescorpin.nflowmgr.service.nflow.ExportImportNflowService;
import com.onescorpin.nflowmgr.service.template.ExportImportTemplateService;
import com.onescorpin.nifi.rest.model.NifiError;
import com.onescorpin.nifi.rest.model.NifiProcessGroup;
import com.onescorpin.nifi.rest.model.NifiProperty;
import com.onescorpin.support.NflowNameUtil;

import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nonnull;

public class ImportUtil {


    public static Set<ImportComponentOption> inspectZipComponents(byte[] content, ImportType importType) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(content);
        return inspectZipComponents(inputStream, importType);
    }


    public static Set<ImportComponentOption> inspectZipComponents(InputStream inputStream, ImportType importType) throws IOException {
        Set<ImportComponentOption> options = new HashSet<>();
        ZipInputStream zis = new ZipInputStream(inputStream);
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            if (entry.getName().startsWith(ExportImportTemplateService.NIFI_TEMPLATE_XML_FILE)) {
                options.add(new ImportComponentOption(ImportComponent.NIFI_TEMPLATE, importType.equals(ImportType.TEMPLATE) ? true : false));
            } else if (entry.getName().startsWith(ExportImportTemplateService.TEMPLATE_JSON_FILE)) {
                options.add(new ImportComponentOption(ImportComponent.TEMPLATE_DATA, importType.equals(ImportType.TEMPLATE) ? true : false));
            } else if (entry.getName().startsWith(ExportImportTemplateService.NIFI_CONNECTING_REUSABLE_TEMPLATE_XML_FILE)) {
                options.add(new ImportComponentOption(ImportComponent.REUSABLE_TEMPLATE, false));
            } else if (importType.equals(ImportType.NFLOW) && entry.getName().startsWith(ExportImportNflowService.NFLOW_JSON_FILE)) {
                options.add(new ImportComponentOption(ImportComponent.NFLOW_DATA, true));
                options.add(new ImportComponentOption(ImportComponent.USER_DATASOURCES, true));
            }
        }
        zis.closeEntry();
        zis.close();

        return options;
    }


    public static void addToImportOptionsSensitiveProperties(ImportOptions importOptions, List<NifiProperty> sensitiveProperties, ImportComponent component) {
        ImportComponentOption option = importOptions.findImportComponentOption(component);
        if (option.getProperties().isEmpty()) {
            option.setProperties(sensitiveProperties.stream().map(p -> new ImportProperty(p.getProcessorName(), p.getProcessorId(), p.getKey(), "", p.getProcessorType())).collect(
                Collectors.toList()));
        } else {
            //only add in those that are unique
            Map<String, ImportProperty> propertyMap = option.getProperties().stream().collect(Collectors.toMap(p -> p.getProcessorNameTypeKey(), p -> p));
            sensitiveProperties.stream().filter(nifiProperty -> !propertyMap.containsKey(nifiProperty.getProcessorNameTypeKey())).forEach(p -> {
                option.getProperties().add(new ImportProperty(p.getProcessorName(), p.getProcessorId(), p.getKey(), "", p.getProcessorType()));
            });
        }
    }


    public static boolean applyImportPropertiesToTemplate(RegisteredTemplate template, ExportImportTemplateService.ImportTemplate importTemplate, ImportComponent component) {
        ImportComponentOption option = importTemplate.getImportOptions().findImportComponentOption(component);

        if (!option.getProperties().isEmpty() && option.getProperties().stream().anyMatch(importProperty -> StringUtils.isBlank(importProperty.getPropertyValue()))) {
            importTemplate.setSuccess(false);
            importTemplate.setTemplateResults(new NifiProcessGroup());
            String msg = "Unable to import Template. Additional properties to be supplied before importing.";
            importTemplate.getTemplateResults().addError(NifiError.SEVERITY.WARN, msg, "");
            option.getErrorMessages().add(msg);
            return false;
        } else {
            template.getSensitiveProperties().forEach(nifiProperty -> {
                ImportProperty
                    userSuppliedValue =
                    option.getProperties().stream().filter(importNflowProperty -> nifiProperty.getProcessorId().equalsIgnoreCase(importNflowProperty.getProcessorId()) && nifiProperty.getKey()
                        .equalsIgnoreCase(importNflowProperty.getPropertyKey())).findFirst().orElse(null);
                //deal with nulls?
                if(userSuppliedValue == null) {
                    //attempt to find it via the name
                    userSuppliedValue =
                        option.getProperties().stream().filter(importNflowProperty -> nifiProperty.getProcessorName().equalsIgnoreCase(importNflowProperty.getProcessorName()) && nifiProperty.getKey()
                            .equalsIgnoreCase(importNflowProperty.getPropertyKey())).findFirst().orElse(null);
                }
                if(userSuppliedValue != null) {
                    nifiProperty.setValue(userSuppliedValue.getPropertyValue());
                }
            });
            return true;
        }
    }

    public static boolean applyImportPropertiesToNflow(NflowMetadata metadata, ExportImportNflowService.ImportNflow importNflow, ImportComponent component) {
        ImportComponentOption option = importNflow.getImportOptions().findImportComponentOption(component);

        if (!option.getProperties().isEmpty() && option.getProperties().stream().anyMatch(importProperty -> StringUtils.isBlank(importProperty.getPropertyValue()))) {
            importNflow.setSuccess(false);
            if (importNflow.getTemplate() == null) {
                ExportImportTemplateService.ImportTemplate importTemplate = new ExportImportTemplateService.ImportTemplate(importNflow.getFileName());
                importNflow.setTemplate(importTemplate);
            }
            String nflowCategory = importNflow.getImportOptions().getCategorySystemName() != null ? importNflow.getImportOptions().getCategorySystemName() : metadata.getSystemCategoryName();
            String msg = "The nflow " + NflowNameUtil.fullName(nflowCategory, metadata.getSystemNflowName())
                         + " needs additional properties to be supplied before importing.";
            importNflow.addErrorMessage(metadata, msg);
            option.getErrorMessages().add(msg);
            return false;
        } else {
            metadata.getSensitiveProperties().forEach(nifiProperty -> {
                ImportProperty userSuppliedValue = importNflow.getImportOptions().getProperties(ImportComponent.NFLOW_DATA).stream().filter(importNflowProperty -> {
                    return nifiProperty.getProcessorId().equalsIgnoreCase(importNflowProperty.getProcessorId()) && nifiProperty.getKey().equalsIgnoreCase(importNflowProperty.getPropertyKey());
                }).findFirst().orElse(null);
                //deal with nulls?
                if (userSuppliedValue != null) {
                    nifiProperty.setValue(userSuppliedValue.getPropertyValue());
                }
            });
            return true;
        }
    }

    /**
     * Replaces the specified data source id with a new data source id.
     *
     * @param metadata the nflow metadata
     * @param oldDatasourceId the id of the data source to be replaced
     * @param newDatasourceId the id of the new data source
     */
    @SuppressWarnings("unchecked")
    public static void replaceDatasource(@Nonnull final NflowMetadata metadata, @Nonnull final String oldDatasourceId, @Nonnull final String newDatasourceId) {
        // Update data transformation
        final NflowDataTransformation transformation = metadata.getDataTransformation();
        if (transformation != null) {
            // Update chart view model
            Optional.of(transformation.getChartViewModel())
                .map(model -> (List<Map<String, Object>>) model.get("nodes"))
                .ifPresent(
                    nodes -> nodes.forEach(
                        node -> {
                            final String nodeDatasourceId = (String) node.get("datasourceId");
                            if (nodeDatasourceId != null && oldDatasourceId.equals(nodeDatasourceId)) {
                                node.put("datasourceId", newDatasourceId);
                            }
                        }
                    )
                );

            // Update data source id list
            transformation.getDatasourceIds().replaceAll(id -> oldDatasourceId.equals(id) ? newDatasourceId : id);

            // Update transform script
            final String updatedDataTransformScript = transformation.getDataTransformScript().replace(oldDatasourceId, newDatasourceId);
            transformation.setDataTransformScript(updatedDataTransformScript);
        }

        // Update processor properties
        metadata.getProperties().forEach(property -> {
            final String value = property.getValue();
            if (value != null && !value.isEmpty()) {
                property.setValue(value.replace(oldDatasourceId, newDatasourceId));
            }
        });
    }

    public static byte[] streamToByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = inputStream.read(buf)) >= 0) {
            baos.write(buf, 0, n);
        }
        byte[] content = baos.toByteArray();
        return content;
    }

}
