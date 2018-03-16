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
/**
 * ui-router service.  Controllers that link/navigate to other controllers/pages use this service.
 * See the corresponding name references in app.js
 */
define(['angular', 'services/module-name'], function (angular, moduleName) {
    return angular.module(moduleName).factory('StateService', ["$state", function ($state) {

        function AuthStates() {
            var data = {}
            /**
             * Navigates to the Groups page.
             */
            data.navigateToGroups = function () {
                $state.go("groups");
            };

            /**
             * Navigates to the Group Details page.
             *
             * @param {string} [opt_groupId] the system name of the group
             */
            data.navigateToGroupDetails = function (opt_groupId) {
                var safeGroupId = angular.isString(opt_groupId) ? encodeURIComponent(opt_groupId) : null;
                $state.go("group-details", {groupId: safeGroupId});
            };

            /**
             * Navigates to the Users page.
             */
            data.navigateToUsers = function () {
                $state.go("users");
            };

            /**
             * Navigates to the User Details page.
             *
             * @param {string} [opt_userId] the system name of the user
             */
            data.navigateToUserDetails = function (opt_userId) {
                var safeUserId = angular.isString(opt_userId) ? encodeURIComponent(opt_userId) : null;
                $state.go("user-details", {userId: safeUserId});
            };
            return data;
        }

        var TemplateStates = function () {
            var data = {};
            data.navigateToRegisterNewTemplate = function () {
                $state.go('register-new-template');
            }

            data.navigateToRegisterTemplateComplete = function (message, templateModel, error) {
                $state.go('register-template-complete', {message: message, templateModel: templateModel, error: error});
            }

            data.navigateToImportTemplate = function () {
                $state.go('import-template');
            }

            data.navigateToRegisterNifiTemplate = function () {
                $state.go('register-template', {registeredTemplateId: null, nifiTemplateId: null});
            }

            data.navigateToRegisteredTemplate = function (templateId, nifiTemplateId) {
                $state.go('register-template', {registeredTemplateId: templateId, nifiTemplateId: nifiTemplateId});
            }
            /**
             * Navigates to the Templates page.
             */
            data.navigateToRegisteredTemplates = function () {
                $state.go("registered-templates");
            };

            return data;
        }

        var NflowStates = function () {
            var data = {};
            data.navigateToNflowDetails = function (nflowId, tabIndex) {
                if (tabIndex == null || tabIndex == undefined) {
                    tabIndex = 0;
                }
                $state.go('nflow-details', {nflowId: nflowId, tabIndex: tabIndex});
            }

            data.navigateToEditNflowInStepper = function (nflowId) {
                $state.go('edit-nflow', {nflowId: nflowId});
            }

            data.navigateToDefineNflow = function (templateId) {
                $state.go('define-nflow', {templateId: templateId});
            }

            data.navigateToCloneNflow = function (nflowName) {
                $state.go('define-nflow', {templateId: null,bcExclude_cloning:true,bcExclude_cloneNflowName:nflowName});
            }

            data.navigateToDefineNflowComplete = function (nflowModel, error) {
                $state.go('define-nflow-complete', {nflowModel: nflowModel, error: error});
            }

            data.navigateToNflows = function () {
                $state.go('nflows');
            }

            data.navigatetoImportNflow = function () {
                $state.go('import-nflow');
            }
            return data;
        }

        var ProfileStates = function () {
            var data = {};
            data.navigateToProfileSummary = function (nflowId) {
                $state.go('nflow-details.profile-summary', {nflowId: nflowId})
            }
            data.navigateToProfileValidResults = function (nflowId, processingdttm) {
                $state.go('nflow-details.profile-valid', {nflowId: nflowId, processingdttm: processingdttm})
            }
            data.navigateToProfileInvalidResults = function (nflowId, processingdttm) {
                $state.go('nflow-details.profile-invalid', {nflowId: nflowId, processingdttm: processingdttm})
            }
            data.navigateToProfileStats = function (nflowId, processingdttm) {
                $state.go('nflow-details.profile-stats', {nflowId: nflowId, processingdttm: processingdttm})
            }
            return data;

        }

        var TableStates = function () {
            var data = {};
            data.navigateToTable = function (schema, table) {
                $state.go('table', {schema: schema, tableName: table});
            }
            data.navigateToTables = function () {
                $state.go('tables');
            }
            return data;
        }

        var SlaStates = function () {
            var data = {};
            data.navigateToServiceLevelAgreements = function () {
                $state.go('service-level-agreements');
            }
            data.navigateToServiceLevelAgreement = function (slaId) {
                $state.go('service-level-agreements',{slaId:slaId});
            }
            data.navigateToNewEmailTemplate = function (templateId) {
                $state.go('sla-email-template',{emailTemplateId:templateId});
            }
            data.navigateToEmailTemplates = function () {
                $state.go('sla-email-templates');
            }
            return data;
        }

        var CategoryStates = function () {
            var data = {};
            data.navigateToCategoryDetails = function (categoryId) {
                $state.go('category-details', {categoryId: categoryId});
            }

            data.navigateToCategories = function () {
                $state.go('categories');
            }
            return data;
        }

        var self = this;

        var SearchStates = function () {
            var data = {};
            data.navigateToSearch = function (resetPaging) {
                if (angular.isUndefined(resetPaging)) {
                    resetPaging = false;
                }
                $state.go('search', {"bcExclude_globalSearchResetPaging":resetPaging});
            }
            return data;
        }

        var DatasourceStates = function() {
            return {
                navigateToDatasourceDetails: function(opt_datasourceId) {
                    var safeDatasourceId = angular.isString(opt_datasourceId) ? encodeURIComponent(opt_datasourceId) : null;
                    $state.go("datasource-details", {datasourceId: safeDatasourceId});
                },

                navigateToDatasources: function() {
                    $state.go("datasources");
                }
            };
        };

        var DomainTypeStates = function () {
            return {
                navigateToDomainTypeDetails: function (opt_domainTypeId) {
                    var safeDomainTypeId = angular.isString(opt_domainTypeId) ? encodeURIComponent(opt_domainTypeId) : null;
                    $state.go("domain-type-details", {domainTypeId: safeDomainTypeId});
                },

                navigateToDomainTypes: function () {
                    $state.go("domain-types");
                }
            }
        };

        var NflowManagerStates = function () {
            var data = {};
            data.Category = CategoryStates;
            data.Nflow = NflowStates;
            data.Sla = SlaStates;
            data.Template = TemplateStates;
            data.Table = TableStates;
            data.Profile = ProfileStates;
            data.Datasource = DatasourceStates;
            data.DomainType = DomainTypeStates;
            return data;
        }

        var OpsManagerJobStates = function () {
            var data = {};
            data.navigateToJobDetails = function (executionId) {
                $state.go('job-details', {executionId: executionId});
            }
            data.navigateToJobs = function (tab,filter) {
                $state.go('jobs', {tab:tab,filter: filter});
            }
            return data;
        }

        var OpsManagerNflowStates = function () {
            var data = {};
            data.navigateToNflowDetails = function (nflowName) {
                $state.go('ops-nflow-details', {nflowName: nflowName});
            }
            data.navigateToNflowStats = function (nflowName) {
                $state.go('nflow-stats', {nflowName: nflowName});
            }
            return data;
        }

        var OpsManagerServiceStates = function () {
            var data = {};
            data.navigateToServiceDetails = function (serviceName) {
                $state.go('service-details', {serviceName: serviceName});
            }

            data.navigateToServiceComponentDetails = function (serviceName, componentName) {
                $state.go('service-component-details', {serviceName: serviceName, componentName: componentName});
            }
            return data;
        }

        var AlertStates = function () {
            var data = {};
            /**
             * Navigates to the details page for the specified alert.
             * @param {string} alertId the id of the alert
             */
            data.navigateToAlertDetails = function (alertId) {
                $state.go("alert-details", {alertId: alertId});
            };
            data.navigateToAlerts = function (query) {
                $state.go("alerts", {query: query});
            };
            return data;
        }

        var SlaAssessmentStates = function () {
            var data = {};
            data.navigateToServiceLevelAssessments = function (filter) {
                filter = angular.isUndefined(filter) ? '' : filter;
                $state.go('service-level-assessments',{filter:filter});
            }
            data.navigateToServiceLevelAssessment = function (assessmentId) {
                $state.go('service-level-assessment',{assessmentId:assessmentId});
            }
            return data;
        }

        var OpsManagerStates = function () {

            var data = {};
            data.dashboard = function () {
                $state.go('dashboard');
            }
            data.Nflow = OpsManagerNflowStates;
            data.Job = OpsManagerJobStates;
            data.ServiceStatus = OpsManagerServiceStates
            data.Alert = AlertStates;
            data.Sla = SlaAssessmentStates;
            return data;
        }



        var States = function () {
            var data = {};
            data.Auth = AuthStates;
            data.Search = SearchStates;
            data.NflowManager = NflowManagerStates;
            data.OpsManager = OpsManagerStates;
            data.Tables = TableStates;
            data.Categories = CategoryStates;
            data.go = function (state, params) {
                $state.go(state, params);
            }
            /**
             * Navigates to the Home page.
             */
            data.navigateToHome = function () {
                $state.go("home");
            };
            return data;

        }

        return new States();

    }]);
});
