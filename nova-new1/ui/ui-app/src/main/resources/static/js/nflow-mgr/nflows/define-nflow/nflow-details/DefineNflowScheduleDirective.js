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
define(['angular','nflow-mgr/nflows/define-nflow/module-name'], function (angular,moduleName) {

    var directive = function() {
        return {
            restrict: "EA",
            bindToController: {
                stepIndex: '@'
            },
            controllerAs: 'vm',
            require: ['onescorpinDefineNflowSchedule', '^onescorpinStepper'],
            scope: {},
            templateUrl: 'js/nflow-mgr/nflows/define-nflow/nflow-details/define-nflow-schedule.html',
            controller: "DefineNflowScheduleController",
            link: function($scope, element, attrs, controllers) {
                var thisController = controllers[0];
                var stepperController = controllers[1];
                thisController.stepperController = stepperController;
                thisController.totalSteps = stepperController.totalSteps;
            }

        };
    };

    function DefineNflowScheduleController($scope, $http, $mdDialog, $timeout, RestUrlService, NflowService, StateService, StepperService, CategoriesService, BroadcastService,
                                          NflowCreationErrorService) {
        var self = this;

        /**
         * Get notified when a step is changed/becomes active
         */
        BroadcastService.subscribe($scope, StepperService.ACTIVE_STEP_EVENT, onActiveStep);

        /**
         * get notified when any step changes its state (becomes enabled/disabled)
         * This is needed to block out the save button if a step is invalid/disabled
         */
        BroadcastService.subscribe($scope, StepperService.STEP_STATE_CHANGED_EVENT, onStepStateChange);

        /**
         * reference to the parent stepper controller
         * @type {null}
         */
        this.stepperController = null;

        /**
         * The stepperController will be accessible shortly after this controller is created.
         * This indicates the amount of time it should wait in an attempt to wire itself with the controller
         * @type {number}
         */
        this.waitForStepperControllerRetryAmount = 0;

        /**
         * Reference to this step number
         * @type {number}
         */
        this.stepNumber = parseInt(this.stepIndex) + 1;

        /**
         * The model
         */
        this.model = NflowService.createNflowModel;

        /**
         * The Timer amount with default
         * @type {number}
         */
        this.timerAmount = 5;
        /**
         * the timer units with default
         * @type {string}
         */
        this.timerUnits = "min";

        /**
         * flag indicates the data is valid
         * @type {boolean}
         */
        this.isValid = false;

        /**
         * the angular form
         * @type {{}}
         */
        this.defineNflowScheduleForm = {};

        /**
         * The object that is populated after the Nflow is created and returned from the server
         * @type {null}
         */
        this.createdNflow = null;

        /**
         * Indicates if any errors exist from the server  upon saving
         * @type {Array}
         */
        this.nflowErrorsData = [];
        /**
         * reference to error count so the UI can show it
         * @type {number}
         */
        this.nflowErrorsCount = 0;

        /**
         * Indicates that NiFi is clustered.
         *
         * @type {boolean}
         */
        this.isClustered = true;

        this.savingNflow = false;

        /**
         * Indicates that NiFi supports the execution node property.
         * @type {boolean}
         */
        this.supportsExecutionNode = true;

        /**
         * All possible schedule strategies
         * @type {*[]}
         */
        var allScheduleStrategies = [{label: "Cron", value: "CRON_DRIVEN"}, {label: "Timer", value: "TIMER_DRIVEN"}, {label: "Trigger/Event", value: "TRIGGER_DRIVEN"},
            {label: "On primary node", value: "PRIMARY_NODE_ONLY"}];

        /**
         * Different templates have different schedule strategies.
         * Filter out those that are not needed based upon the template
         */
        function updateScheduleStrategies() {
            // Filter schedule strategies
            var allowPreconditions = (self.model.allowPreconditions && self.model.inputProcessorType.indexOf("TriggerNflow") >= 0);

            self.scheduleStrategies = _.filter(allScheduleStrategies, function(strategy) {
                if (allowPreconditions) {
                    return (strategy.value === "TRIGGER_DRIVEN");
                } else if (strategy.value === "PRIMARY_NODE_ONLY") {
                    return self.isClustered && !self.supportsExecutionNode;
                } else {
                    return (strategy.value !== "TRIGGER_DRIVEN");
                }
            });

            // Check if last strategy is valid
            if (self.model.schedule.schedulingStrategy) {
                var validStrategy = _.some(self.scheduleStrategies, function(strategy) {
                    return strategy.value == self.model.schedule.schedulingStrategy;
                });
                if (!validStrategy) {
                    self.model.schedule.schedulingStrategyTouched = false;
                }
            }
        }

        /**
         * Force the model and timer to be set to Timer with the defaults
         */
        function setTimerDriven() {
            self.model.schedule.schedulingStrategy = 'TIMER_DRIVEN';
            self.timerAmount = 5;
            self.timerUnits = "min";
            self.model.schedule.schedulingPeriod = "5 min";
        }

        /**
         * Force the model to be set to Cron
         */
        function setCronDriven() {
            self.model.schedule.schedulingStrategy = 'CRON_DRIVEN';
            self.model.schedule.schedulingPeriod = NflowService.DEFAULT_CRON;
        }

        /**
         * Force the model to be set to Triggger
         */
        function setTriggerDriven() {
            self.model.schedule.schedulingStrategy = 'TRIGGER_DRIVEN'
        }

        /**
         * Set the scheduling strategy to 'On primary node'.
         */
        function setPrimaryNodeOnly() {
            self.model.schedule.schedulingStrategy = "PRIMARY_NODE_ONLY";
            self.timerAmount = 5;
            self.timerUnits = "min";
            self.model.schedule.schedulingPeriod = "5 min";
        }

        function setDefaultScheduleStrategy() {
            if(angular.isUndefined(self.model.cloned) || self.model.cloned == false) {
                if (self.model.inputProcessorType != '' && (self.model.schedule.schedulingStrategyTouched == false || self.model.schedule.schedulingStrategyTouched == undefined)) {
                    if (self.model.inputProcessorType.indexOf("GetFile") >= 0) {
                        setTimerDriven();
                    }
                    else if (self.model.inputProcessorType.indexOf("GetTableData") >= 0) {
                        setCronDriven();
                    }
                    else if (self.model.inputProcessorType.indexOf("TriggerNflow") >= 0) {
                        setTriggerDriven();
                    }
                    self.model.schedule.schedulingStrategyTouched = true;
                }
                else if(self.model.schedule.schedulingPeriod != ''){
                    var split = self.model.schedule.schedulingPeriod.split(' ');
                    self.timerAmount = split[0];
                    self.timerUnits = split[1];
                }
            } else {
                var split = self.model.schedule.schedulingPeriod.split(' ');
                self.timerAmount = split[0];
                self.timerUnits = split[1];

            }
        }

        /**
         * update the default strategies in the list
         */
        updateScheduleStrategies();

        /**
         * Called when any step is active.
         *
         * @param event
         * @param index
         */
        function onActiveStep(event, index) {
            if (index == parseInt(self.stepIndex)) {

                updateScheduleStrategies();
                //make sure the selected strategy is valid

                setDefaultScheduleStrategy();
            }
        }

        /**
         * get notified of the step state (enabled/disabled) changed
         * Validate the form
         * @param event
         * @param index
         */
        function onStepStateChange(event, index) {
            validate();
        }

        /**
         * When the timer changes show warning if its < 3 seconds indicating to the user this is a "Rapid Fire" nflow
         */
        this.timerChanged = function() {
            if (self.timerAmount < 0) {
                self.timerAmount = null;
            }
            if (self.timerAmount != null && (self.timerAmount == 0 || (self.timerAmount < 3 && self.timerUnits == 'sec'))) {
                self.showTimerAlert();
            }
            self.model.schedule.schedulingPeriod = self.timerAmount + " " + self.timerUnits;
            validate();
        };

        self.showTimerAlert = function(ev) {
            $mdDialog.show(
                    $mdDialog.alert()
                            .parent(angular.element(document.body))
                            .clickOutsideToClose(false)
                            .title('Warning. Rapid Timer')
                            .textContent('Warning.  You have this nflow scheduled for a very fast timer.  Please ensure you want this nflow scheduled this fast before you proceed.')
                            .ariaLabel('Warning Fast Timer')
                            .ok('Got it!')
                            .targetEvent(ev)
            );
        };

        /**
         * When the strategy changes ensure the defaults are set
         */
        this.onScheduleStrategyChange = function() {
            self.model.schedule.schedulingStrategyTouched = true;
            if (self.model.schedule.schedulingStrategy == "CRON_DRIVEN") {
                if (self.model.schedule.schedulingPeriod != NflowService.DEFAULT_CRON) {
                    setCronDriven();
                }
            } else if (self.model.schedule.schedulingStrategy == "TIMER_DRIVEN") {
                setTimerDriven();
            } else if (self.model.schedule.schedulingStrategy === "PRIMARY_NODE_ONLY") {
                if (self.supportsExecutionNode) {
                    setTimerDriven();
                    self.model.schedule.schedulingStrategy = "PRIMARY";
                } else {
                    setPrimaryNodeOnly();
                }
            }
            validate();
        };

        /**
         * Show activity
         */
        function showProgress() {
            if (self.stepperController) {
                self.stepperController.showProgress = true;
            }
        }

        /**
         * hide progress activity
         */
        function hideProgress() {
            if (self.stepperController) {
                self.stepperController.showProgress = false;
            }
        }

        /**
         * validate the inputs and model data
         */
        function validate() {
            //cron expression validation is handled via the cron-expression validator
            var valid = (self.model.schedule.schedulingStrategy == "CRON_DRIVEN") ||
                        (self.model.schedule.schedulingStrategy == "TIMER_DRIVEN" && self.timerAmount != undefined && self.timerAmount != null) ||
                        (self.model.schedule.schedulingStrategy == "TRIGGER_DRIVEN" && self.model.schedule.preconditions != null && self.model.schedule.preconditions.length > 0) ||
                        (self.model.schedule.schedulingStrategy == "PRIMARY_NODE_ONLY" && self.timerAmount != undefined && self.timerAmount != null);
            if (valid) {
                waitForStepperController(function() {
                    //since the access control step can be disabled, we care about everything before that step, so we will check the step prior to this step
                    self.isValid = self.stepperController.arePreviousStepsComplete(self.stepIndex-1)
                });

            }
            else {
                self.isValid = valid;
            }
        }

        /**
         * attempt to wire the stepper controller references
         * @param callback
         */
        function waitForStepperController(callback) {
            if (self.stepperController) {
                self.waitForStepperControllerRetryAmount = 0;
                callback();
            }
            else {
                if (self.waitForStepperControllerRetryAmount < 20) {
                    self.waitForStepperControllerRetryAmount++;
                    $timeout(function() {
                        waitForStepperController(callback)
                    }, 10);
                }
            }
        }

        this.deletePrecondition = function($index) {
            if (self.model.schedule.preconditions != null) {
                self.model.schedule.preconditions.splice($index, 1);
            }
        };

        this.showPreconditionDialog = function(index) {
            if (index == undefined) {
                index = null;
            }
            $mdDialog.show({
                controller: 'NflowPreconditionsDialogController',
                templateUrl: 'js/nflow-mgr/nflows/shared/define-nflow-preconditions-dialog.html',
                parent: angular.element(document.body),
                clickOutsideToClose: false,
                fullscreen: true,
                locals: {
                    nflow: self.model,
                    index: index
                }
            }).then(function() {
                validate();
            });
        };

        /**
         * Validate the form
         */
        validate();

        /**
         * Create the nflow, save it to the server, populate the {@code createdNflow} object upon save
         */
        this.createNflow = function() {
            if(self.defineNflowScheduleForm.$valid) {
                self.savingNflow = true;
                showProgress();

                self.createdNflow = null;

                NflowService.saveNflowModel(self.model).then(function (response) {
                    self.createdNflow = response.data;
                    self.savingNflow = false;
                    StateService.NflowManager().Nflow().navigateToDefineNflowComplete(self.createdNflow, null);

                    //  self.showCompleteDialog();
                }, function (response) {
                    self.savingNflow = false;
                    self.createdNflow = response.data;
                    NflowCreationErrorService.buildErrorData(self.model.nflowName, response);
                    hideProgress();
                    NflowCreationErrorService.showErrorDialog();
                });
            }
        };

        // Detect if NiFi is clustered
        $http.get(RestUrlService.NIFI_STATUS).then(function(response) {
            self.isClustered = (angular.isDefined(response.data.clustered) && response.data.clustered);
            self.supportsExecutionNode = (angular.isDefined(response.data.version) && !response.data.version.match(/^0\.|^1\.0/));
            updateScheduleStrategies();
        });
    }

    angular.module(moduleName).controller("DefineNflowScheduleController", DefineNflowScheduleController);
    angular.module(moduleName).directive("onescorpinDefineNflowSchedule", directive);



});


