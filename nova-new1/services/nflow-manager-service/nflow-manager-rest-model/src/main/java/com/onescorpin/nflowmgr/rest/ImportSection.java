package com.onescorpin.nflowmgr.rest;

/*-
 * #%L
 * onescorpin-nflow-manager-rest-model
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

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImportSection {

    public static enum Section {
        VALIDATE_NFLOW_CATEGORY(ImportType.NFLOW), VALIDATE_NFLOW(ImportType.NFLOW), VALIDATE_PROPERTIES(ImportType.NFLOW), IMPORT_NFLOW_DATA(ImportType.NFLOW),
        VALIDATE_REUSABLE_TEMPLATE(ImportType.NFLOW, ImportType.TEMPLATE, ImportType.TEMPLATE_XML), VALIDATE_REGISTERED_TEMPLATE(ImportType.NFLOW, ImportType.TEMPLATE),
        VALIDATE_NIFI_TEMPLATE(ImportType.NFLOW, ImportType.TEMPLATE, ImportType.TEMPLATE_XML), IMPORT_REUSABLE_TEMPLATE(ImportType.NFLOW, ImportType.TEMPLATE, ImportType.TEMPLATE_XML),
        IMPORT_NIFI_TEMPLATE(ImportType.NFLOW, ImportType.TEMPLATE, ImportType.TEMPLATE_XML), CREATE_NIFI_INSTANCE(ImportType.NFLOW, ImportType.TEMPLATE),
        IMPORT_REGISTERED_TEMPLATE(ImportType.NFLOW, ImportType.TEMPLATE), VALIDATE_USER_DATASOURCES(ImportType.NFLOW);


        private ImportType[] importTypes;

        private Section(ImportType... importTypes) {
            this.importTypes = importTypes;
        }

        public boolean hasImportType(ImportType importType) {
            return Stream.of(importTypes).anyMatch(type -> type.equals(importType));
        }

    }

    public static Set<Section> sectionsForImport(ImportType importType) {
        return Stream.of(Section.values()).filter(section -> section.hasImportType(importType)).collect(Collectors.toSet());
    }

    public static Set<String> sectionsForImportAsString(ImportType importType) {
        return ImportSection.sectionsForImport(importType).stream().map(Enum::name).collect(Collectors.toSet());
    }
}
