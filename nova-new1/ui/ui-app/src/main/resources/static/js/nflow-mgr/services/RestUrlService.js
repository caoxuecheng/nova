/*-
 * #%L
 * onescorpin-ui-nflow-manager
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

define(['angular', 'nflow-mgr/module-name'], function (angular, moduleName) {
    return angular.module(moduleName).service('RestUrlService', function () {

        var self = this;

        this.ROOT = "";
        this.ADMIN_BASE_URL = this.ROOT + "/proxy/v1/nflowmgr/admin";
        this.ADMIN_V2_BASE_URL = this.ROOT + "/proxy/v2/nflowmgr/admin";
        this.SECURITY_BASE_URL = this.ROOT + "/proxy/v1/security";
        this.TEMPLATES_BASE_URL = this.ROOT + "/proxy/v1/nflowmgr/templates";
        this.NFLOWS_BASE_URL = this.ROOT + "/proxy/v1/nflowmgr/nflows";
        this.SLA_BASE_URL = this.ROOT + "/proxy/v1/nflowmgr/sla";
        this.CONTROLLER_SERVICES_BASE_URL = this.ROOT + "/proxy/v1/nflowmgr/nifi/controller-services";
        this.SCHEMA_DISCOVERY_BASE_URL = this.ROOT + "/proxy/v1/schema-discovery";
        this.GET_TEMPLATES_URL = self.TEMPLATES_BASE_URL;
        this.GET_UNREGISTERED_TEMPLATES_URL = self.TEMPLATES_BASE_URL + "/unregistered";
        this.HADOOP_AUTHORIZATATION_BASE_URL = self.ROOT + "/proxy/v1/nflowmgr/hadoop-authorization";
        this.UI_BASE_URL = this.ROOT + "/api/v1/ui";
        this.DOMAIN_TYPES_BASE_URL = this.ROOT + "/proxy/v1/nflowmgr/domain-types";

        this.UPLOAD_SAMPLE_TABLE_FILE = this.SCHEMA_DISCOVERY_BASE_URL + "/hive/sample-file";
        this.LIST_FILE_PARSERS = this.SCHEMA_DISCOVERY_BASE_URL + "/file-parsers";

        this.VALIDATE_CRON_EXPRESSION_URL = this.ROOT + "/proxy/v1/nflowmgr/util/cron-expression/validate";

        this.PREVIEW_CRON_EXPRESSION_URL = this.ROOT + "/proxy/v1/nflowmgr/util/cron-expression/preview";

        this.GET_SYSTEM_NAME = this.ROOT + "/proxy/v1/nflowmgr/util/system-name";

        this.ICONS_URL = this.ROOT + "/proxy/v1/nflowmgr/util/icons";
        this.ICON_COLORS_URL = this.ROOT + "/proxy/v1/nflowmgr/util/icon-colors";

        this.CODE_MIRROR_TYPES_URL = this.ROOT + "/proxy/v1/nflowmgr/util/codemirror-types";

        this.CATEGORIES_URL = this.ROOT + "/proxy/v1/nflowmgr/categories";

        this.SEARCH_URL = this.ROOT + "/proxy/v1/nflowmgr/search";

        this.HIVE_SERVICE_URL = this.ROOT + "/proxy/v1/hive";

        this.SPARK_SHELL_SERVICE_URL = this.ROOT + "/proxy/v1/spark/shell";

        ///TEMPLATE REGISTRATION

        this.REGISTER_TEMPLATE_URL = function () {
            return self.TEMPLATES_BASE_URL + "/register";
        }

        this.SAVE_TEMPLATE_ORDER_URL = self.TEMPLATES_BASE_URL + "/order";

        this.GET_REGISTERED_TEMPLATES_URL = self.TEMPLATES_BASE_URL + "/registered";

        this.GET_REGISTERED_TEMPLATE_PROPERTIES_URL = function (templateId) {
            return self.GET_REGISTERED_TEMPLATES_URL + "/" + templateId + "/properties";
        }

        this.GET_REGISTERED_TEMPLATE_URL = function (templateId) {
            return self.GET_REGISTERED_TEMPLATES_URL + "/" + templateId;
        }

        this.REGISTERED_TEMPLATE_NIFI_INPUT_PORTS = function (nifiTemplateId) {
            return self.TEMPLATES_BASE_URL + "/nifi/" + nifiTemplateId + "/input-ports";
        }

        this.REGISTERED_TEMPLATE_NIFI_OUTPUT_PORTS = function (nifiTemplateId) {
            return self.TEMPLATES_BASE_URL + "/nifi/" + nifiTemplateId + "/output-ports";
        }

        this.REGISTERED_TEMPLATE_NIFI_ALL_PORTS = function (nifiTemplateId) {
            return self.TEMPLATES_BASE_URL + "/nifi/" + nifiTemplateId + "/ports";
        }

        this.TEMPLATE_PROCESSOR_DATASOURCE_DEFINITIONS = function (nifiTemplateId) {
            return self.TEMPLATES_BASE_URL + "/nifi/" + nifiTemplateId + "/datasource-definitions";
        }

        this.TEMPLATE_FLOW_INFORMATION = function (nifiTemplateId) {
            return self.TEMPLATES_BASE_URL + "/nifi/" + nifiTemplateId + "/flow-info";
        }

        this.DISABLE_REGISTERED_TEMPLATE_URL = function (templateId) {
            return self.GET_REGISTERED_TEMPLATES_URL + "/" + templateId + "/disable";
        }
        this.ENABLE_REGISTERED_TEMPLATE_URL = function (templateId) {
            return self.GET_REGISTERED_TEMPLATES_URL + "/" + templateId + "/enable";
        }
        this.DELETE_REGISTERED_TEMPLATE_URL = function (templateId) {
            return self.GET_REGISTERED_TEMPLATES_URL + "/" + templateId + "/delete";
        }

        this.ALL_REUSABLE_NFLOW_INPUT_PORTS = this.ROOT + "/proxy/v1/nflowmgr/nifi/reusable-input-ports";

        this.CONFIGURATION_PROPERTIES_URL = this.ROOT + "/proxy/v1/nflowmgr/nifi/configuration/properties";
        this.METADATA_PROPERTY_NAMES_URL = this.ROOT + "/proxy/v1/nflowmgr/metadata-properties";

        this.GET_DATASOURCE_TYPES = this.ROOT + "/proxy/v1/metadata/datasource/types";

        //NFLOW URLS

        this.CREATE_NFLOW_FROM_TEMPLATE_URL = self.NFLOWS_BASE_URL;

        this.MERGE_NFLOW_WITH_TEMPLATE = function (nflowId) {
            return self.GET_NFLOWS_URL + "/" + nflowId + "/merge-template";
        }

        this.GET_NFLOWS_URL = self.NFLOWS_BASE_URL;

        this.GET_NFLOW_NAMES_URL = self.NFLOWS_BASE_URL + "/names";

        this.GET_POSSIBLE_NFLOW_PRECONDITIONS_URL = self.NFLOWS_BASE_URL + "/possible-preconditions";

        this.GET_POSSIBLE_SLA_METRIC_OPTIONS_URL = self.SLA_BASE_URL + "/available-metrics";

        this.GET_POSSIBLE_SLA_ACTION_OPTIONS_URL = self.SLA_BASE_URL + "/available-responders";

        this.VALIDATE_SLA_ACTION_URL = self.SLA_BASE_URL + "/action/validate";

        this.SAVE_NFLOW_SLA_URL = function (nflowId) {
            return self.SLA_BASE_URL + "/nflow/" + nflowId;
        }
        this.SAVE_SLA_URL = self.SLA_BASE_URL;

        this.DELETE_SLA_URL = function (slaId) {
            return self.SLA_BASE_URL + "/" + slaId;
        }

        this.GET_NFLOW_SLA_URL = function (nflowId) {
            return self.NFLOWS_BASE_URL + "/" + nflowId + "/sla";
        }

        this.GET_SLA_BY_ID_URL = function (slaId) {
            return self.SLA_BASE_URL + "/"+slaId;
        }

        this.GET_SLA_AS_EDIT_FORM = function (slaId) {
            return self.SLA_BASE_URL + "/" + slaId + "/form-object";
        }

        this.GET_SLAS_URL = self.SLA_BASE_URL;

        this.GET_CONTROLLER_SERVICES_TYPES_URL = self.CONTROLLER_SERVICES_BASE_URL + "/types";

        this.GET_CONTROLLER_SERVICES_URL = self.CONTROLLER_SERVICES_BASE_URL;

        this.GET_CONTROLLER_SERVICE_URL = function (serviceId) {
            return self.CONTROLLER_SERVICES_BASE_URL + "/" + serviceId;
        }

        this.NFLOW_PROFILE_STATS_URL = function (nflowId) {
            return self.GET_NFLOWS_URL + "/" + nflowId + "/profile-stats";
        }

        this.NFLOW_PROFILE_SUMMARY_URL = function (nflowId) {
            return self.GET_NFLOWS_URL + "/" + nflowId + "/profile-summary";
        }

        this.NFLOW_PROFILE_VALID_RESULTS_URL = function (nflowId, processingDttm) {
            return self.GET_NFLOWS_URL + "/" + nflowId + "/profile-valid-results";
        }

        this.NFLOW_PROFILE_INVALID_RESULTS_URL = function (nflowId, processingDttm) {
            return self.GET_NFLOWS_URL + "/" + nflowId + "/profile-invalid-results";
        }

        this.ENABLE_NFLOW_URL = function (nflowId) {
            return self.NFLOWS_BASE_URL + "/enable/" + nflowId;
        }
        this.DISABLE_NFLOW_URL = function (nflowId) {
            return self.NFLOWS_BASE_URL + "/disable/" + nflowId;
        }
        this.UPLOAD_FILE_NFLOW_URL = function (nflowId) {
            return self.NFLOWS_BASE_URL + "/" + nflowId + "/upload-file";
        }

        this.NFLOW_DETAILS_BY_NAME_URL = function (nflowName) {
            return self.NFLOWS_BASE_URL + "/by-name/" + nflowName;
        };

        this.CATEGORY_DETAILS_BY_SYSTEM_NAME_URL = function (categoryName) {
            return self.CATEGORIES_URL + "/by-name/" + categoryName;
        };

        this.CATEGORY_DETAILS_BY_ID_URL = function (categoryId) {
            return self.CATEGORIES_URL + "/by-id/" + categoryId;
        };

        /**
         * Gets the URL for retrieving the user fields for a new nflow.
         *
         * @param {string} categoryId the category id
         * @returns {string} the URL
         */
        this.GET_NFLOW_USER_FIELDS_URL = function (categoryId) {
            return self.CATEGORIES_URL + "/" + categoryId + "/user-fields";
        };

        /**
         * URL for retrieving the user fields for a new category.
         * @type {string}
         */
        this.GET_CATEGORY_USER_FIELD_URL = self.CATEGORIES_URL + "/user-fields";

        // Endpoint for administration of user fields
        this.ADMIN_USER_FIELDS = self.ADMIN_BASE_URL + "/user-fields";

        //Field Policy Urls

        this.AVAILABLE_STANDARDIZATION_POLICIES = this.ROOT + "/proxy/v1/field-policies/standardization";
        this.AVAILABLE_VALIDATION_POLICIES = this.ROOT + "/proxy/v1/field-policies/validation";

        this.ADMIN_IMPORT_TEMPLATE_URL = self.ADMIN_V2_BASE_URL + "/import-template";

        this.ADMIN_EXPORT_TEMPLATE_URL = self.ADMIN_BASE_URL + "/export-template";

        this.ADMIN_EXPORT_NFLOW_URL = self.ADMIN_BASE_URL + "/export-nflow";

        this.ADMIN_IMPORT_NFLOW_URL = self.ADMIN_V2_BASE_URL + "/import-nflow";

        this.ADMIN_UPLOAD_STATUS_CHECK = function (key) {
            return self.ADMIN_BASE_URL + "/upload-status/" + key;
        };

        // Hadoop Security Authorization
        this.HADOOP_SECURITY_GROUPS = self.HADOOP_AUTHORIZATATION_BASE_URL + "/groups";

        // Security service URLs

        this.SECURITY_GROUPS_URL = self.SECURITY_BASE_URL + "/groups";

        this.SECURITY_USERS_URL = self.SECURITY_BASE_URL + "/users";

        this.NFLOW_LINEAGE_URL = function (nflowId) {
            return self.ROOT + "/proxy/v1/metadata/nflow/" + nflowId + "/lineage";
        };

        /**
         * Generates a URL for listing the controller services under the specified process group.
         *
         * @param {string} processGroupId the process group id
         * @returns {string} the URL for listing controller services
         */
        this.LIST_SERVICES_URL = function (processGroupId) {
            return self.ROOT + "/proxy/v1/nflowmgr/nifi/controller-services/process-group/" + processGroupId;
        };

        /**
         * The endpoint for retrieving the list of available Hive partition functions.
         *
         * @type {string}
         */
        this.PARTITION_FUNCTIONS_URL = this.ROOT + "/proxy/v1/nflowmgr/util/partition-functions";

        /**
         * The endpoint for retrieving the NiFi status.
         * @type {string}
         */
        this.NIFI_STATUS = this.ROOT + "/proxy/v1/nflowmgr/nifi/status";

        /**
         * the endpoint for determining if NiFi is up or not
         * @type {string}
         */
        this.IS_NIFI_RUNNING_URL = this.ROOT + "/proxy/v1/nflowmgr/nifi/running";

        /**
         * The endpoint for retrieving data sources.
         * @type {string}
         */
        this.GET_DATASOURCES_URL = this.ROOT + "/proxy/v1/metadata/datasource";

        this.GET_NIFI_CONTROLLER_SERVICE_REFERENCES_URL = function(id){
            return self.ROOT + "/proxy/v1/nflowmgr/nifi/controller-services/"+id+"/references";
        }

        /**
         * Get/Post roles changes for a Nflow entity
         * @param nflowId the nflow id
         * @returns {string} the url to get/post nflow role changes
         */
        this.NFLOW_ROLES_URL = function (nflowId) {
            return self.NFLOWS_BASE_URL + "/" + nflowId + "/roles"
        };

        /**
         * Get/Post roles changes for a Category entity
         * @param categoryId the category id
         * @returns {string} the url to get/post category role changes
         */
        this.CATEGORY_ROLES_URL = function (categoryId) {
            return self.CATEGORIES_URL + "/" + categoryId + "/roles"
        };
        
        /**
         * Get/Post roles changes for a Category entity
         * @param categoryId the category id
         * @returns {string} the url to get/post category role changes
         */
        this.CATEGORY_NFLOW_ROLES_URL = function (categoryId) {
        	return self.CATEGORIES_URL + "/" + categoryId + "/nflow-roles"
        };

        /**
         * Get/Post roles changes for a Template entity
         * @param templateId the Template id
         * @returns {string} the url to get/post Template role changes
         */
        this.TEMPLATE_ROLES_URL = function (templateId) {
            return self.TEMPLATES_BASE_URL + "/registered/" + templateId + "/roles"
        };

        /**
         * Endpoint for roles changes to a Datasource entity.
         * @param {string} datasourceId the datasource id
         * @returns {string} the url for datasource role changes
         */
        this.DATASOURCE_ROLES_URL = function (datasourceId) {
            return self.GET_DATASOURCES_URL + "/" + datasourceId + "/roles";
        };

        /**
         * The URL for retrieving the list of template table option plugins.
         * @type {string}
         */
        this.UI_TEMPLATE_TABLE_OPTIONS = this.UI_BASE_URL + "/template-table-options";


        /**
         * The URL for retrieving the list of templates for custom rendering with nifi processors
         * @type {string}
         */
        this.UI_PROCESSOR_TEMPLATES = this.UI_BASE_URL + "/processor-templates";

        /**
         * return a list of the categorySystemName.nflowSystemName
         * @type {string}
         */
        this.OPS_MANAGER_NFLOW_NAMES = "/proxy/v1/nflows/names";

    });
});
