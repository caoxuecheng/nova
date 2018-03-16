define(['angular', 'nflow-mgr/nflows/module-name'], function (angular, moduleName) {
    /**
     * Controller for the Edit Nflow page.
     *
     * @constructor
     */
    var EditNflowController = function ($scope, $http, $q, $mdDialog, $transition$, NflowService, RestUrlService, StateService, VisualQueryService, AccessControlService, NflowSecurityGroups,
                                       StepperService, EntityAccessControlService, UiComponentsService) {
        var self = this;

        /**
         * Nflow ID
         *
         * @type {string}
         */
        self.nflowId = $transition$.params().nflowId;

        /**
         * Nflow model
         *
         * @type {Object}
         */
        self.model = NflowService.createNflowModel;
        self.model.loaded = false;

        /**
         * Selected index for the stepper
         *
         * @type {number}
         */
        self.selectedStepIndex = 2;

        /**
         * Fetches and displays the nflow.
         */
        self.init = function () {
            var successFn = function (response) {
                // Set model
                self.model = response.data;
                self.model.loaded = true;
                NflowService.createNflowModel = self.model;

                // Determine table option
                if (self.model.registeredTemplate.templateTableOption === null) {
                    if (self.model.registeredTemplate.defineTable) {
                        self.model.registeredTemplate.templateTableOption = "DEFINE_TABLE";
                    } else if (self.model.registeredTemplate.dataTransformation) {
                        self.model.registeredTemplate.templateTableOption = "DATA_TRANSFORMATION";
                    } else {
                        self.model.registeredTemplate.templateTableOption = "NO_TABLE";
                    }
                }

                // Load table option
                self.model.templateTableOption = self.model.registeredTemplate.templateTableOption;

                if (self.model.templateTableOption !== "NO_TABLE") {
                    UiComponentsService.getTemplateTableOption(self.model.templateTableOption)
                        .then(function (tableOption) {

                            //if we have a pre-stepper configured set the properties
                            if(angular.isDefined(tableOption.preStepperTemplateUrl) && tableOption.preStepperTemplateUrl != null){
                                self.model.totalPreSteps = tableOption.totalPreSteps
                                self.model.renderTemporaryPreStep = true;
                            }
                            //signal the service that we should track rendering the table template
                            //We want to run our initializer when both the Pre Steps and the Nflow Steps have completed.
                            //this flag will be picked up in the TableOptionsStepperDirective.js
                            UiComponentsService.startStepperTemplateRender(tableOption);

                            //add the template steps + 5 (general, nflowDetails, properties, access, schedule)
                            self.model.totalSteps = tableOption.totalSteps +  5;
                        }, function () {
                            $mdDialog.show(
                                $mdDialog.alert()
                                    .clickOutsideToClose(true)
                                    .title("Create Failed")
                                    .textContent("The template table option could not be loaded.")
                                    .ariaLabel("Failed to create nflow")
                                    .ok("Got it!")
                            );
                            StateService.NflowManager().Nflow().navigateToNflows();
                        });
                } else {
                    self.model.totalSteps = 5;
                }

                self.onStepperInitialized();
            };
            var errorFn = function () {
                var alert = $mdDialog.alert()
                    .parent($("body"))
                    .clickOutsideToClose(true)
                    .title("Unable to load nflow details")
                    .textContent("Unable to load nflow details. Please ensure that Apache NiFi is up and running and then refresh this page.")
                    .ariaLabel("Unable to load nflow details")
                    .ok("Got it!");
                $mdDialog.show(alert);
            };

            $http.get(RestUrlService.GET_NFLOWS_URL + "/" + self.nflowId).then(successFn, errorFn);
        };

        /**
         * initialize the stepper and setup step display
         */
        self.onStepperInitialized = function () {
            if (self.model.loaded && self.model.totalSteps > 2 && StepperService.getStep("EditNflowStepper", self.model.totalSteps - 2) !== null) {
                var entityAccess = AccessControlService.checkEntityAccessControlled();
                var accessChecks = {
                    changeNflowPermissions: entityAccess && NflowService.hasEntityAccess(EntityAccessControlService.ENTITY_ACCESS.NFLOW.CHANGE_NFLOW_PERMISSIONS, self.model),
                    securityGroups: NflowSecurityGroups.isEnabled()
                };
                $q.all(accessChecks).then(function (response) {
                    //disable the access control step
                    if (!response.changeNflowPermissions && !response.securityGroups) {
                        //Access Control is second to last step 0 based array index
                        StepperService.deactivateStep("EditNflowStepper", self.model.totalSteps - 2);
                    }
                });
            }
        };

        /**
         * Resets the editor state.
         */
        self.cancelStepper = function () {
            NflowService.resetNflow();
            self.stepperUrl = "";
            StateService.NflowManager().Nflow().navigateToNflows();
        };

        // Initialize this instance
        self.init();
    };

    angular.module(moduleName).controller("EditNflowController", ["$scope", "$http", "$q", "$mdDialog", "$transition$", "NflowService", "RestUrlService", "StateService", "VisualQueryService",
                                                                 "AccessControlService", "NflowSecurityGroups", "StepperService", "EntityAccessControlService", "UiComponentsService", EditNflowController]);
});
