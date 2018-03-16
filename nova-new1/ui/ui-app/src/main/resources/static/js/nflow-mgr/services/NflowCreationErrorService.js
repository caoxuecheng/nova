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
define(['angular','nflow-mgr/module-name'], function (angular,moduleName) {
    angular.module(moduleName).factory('NflowCreationErrorService',["$mdDialog", function ($mdDialog) {

        function parseNifiNflowForErrors(nifiNflow, errorMap) {
            var count = 0;

            if (nifiNflow != null) {

                if (nifiNflow.errorMessages != null && nifiNflow.errorMessages.length > 0) {
                    angular.forEach(nifiNflow.errorMessages, function (msg) {
                        errorMap['FATAL'].push({category: 'General', message: msg});
                        count++;
                    })
                }

                if (nifiNflow.nflowProcessGroup != null) {
                    angular.forEach(nifiNflow.nflowProcessGroup.errors, function (processor) {
                        if (processor.validationErrors) {
                            angular.forEach(processor.validationErrors, function (error) {
                                var copy = {};
                                angular.extend(copy, error);
                                angular.extend(copy, processor);
                                copy.validationErrors = null;
                                errorMap[error.severity].push(copy);
                                count++;
                            });
                        }
                    });
                }
                if (errorMap['FATAL'].length == 0) {
                    delete errorMap['FATAL'];
                }
                if (errorMap['WARN'].length == 0) {
                    delete errorMap['WARN'];
                }
            }
            return count;

        }

        function buildErrorMapAndSummaryMessage() {
            var count = 0;
            var errorMap = {"FATAL": [], "WARN": []};
            if (data.nflowError.nifiNflow != null && data.nflowError.response.status < 500) {

                count = parseNifiNflowForErrors(data.nflowError.nifiNflow, errorMap);
                data.nflowError.nflowErrorsData = errorMap;
                data.nflowError.nflowErrorsCount = count;

                if (data.nflowError.nflowErrorsCount > 0) {

                    data.nflowError.message = data.nflowError.nflowErrorsCount + " invalid items were found.  Please review and fix these items.";
                    data.nflowError.isValid = false;
                }
                else {
                    data.nflowError.isValid = true;
                }
            }
            else if (data.nflowError.response.status === 502) {
                data.nflowError.message = 'Error creating nflow, bad gateway'
            } else if (data.nflowError.response.status === 503) {
                data.nflowError.message = 'Error creating nflow, service unavailable'
            } else if (data.nflowError.response.status === 504) {
                data.nflowError.message = 'Error creating nflow, gateway timeout'
            } else if (data.nflowError.response.status === 504) {
                data.nflowError.message = 'Error creating nflow, HTTP version not supported'
            } else {
                data.nflowError.message = 'Error creating nflow.'
            }

        }

        function newErrorData() {
            return {
                isValid: false,
                hasErrors: false,
                nflowName: '',
                nifiNflow: {},
                message: '',
                nflowErrorsData: {},
                nflowErrorsCount: 0
            };
        }

        var data = {
            nflowError: {
                isValid: false,
                hasErrors: false,
                nflowName: '',
                nifiNflow: {},
                message: '',
                nflowErrorsData: {},
                nflowErrorsCount: 0
            },
            buildErrorData: function (nflowName, response) {
                this.nflowError.nflowName = nflowName;
                this.nflowError.nifiNflow = response.data;
                this.nflowError.response = response;
                buildErrorMapAndSummaryMessage();
                this.nflowError.hasErrors = this.nflowError.nflowErrorsCount > 0;
            },
            parseNifiNflowErrors: function (nifiNflow, errorMap) {
                return parseNifiNflowForErrors(nifiNflow, errorMap);
            },
            reset: function () {
                angular.extend(this.nflowError, newErrorData());
            },
            hasErrors: function () {
                return this.nflowError.hasErrors;
            },
            showErrorDialog: function () {

                $mdDialog.show({
                    controller: 'NflowErrorDialogController',
                    templateUrl: 'js/nflow-mgr/nflows/define-nflow/nflow-error-dialog.html',
                    parent: angular.element(document.body),
                    clickOutsideToClose: false,
                    fullscreen: true,
                    locals: {}
                }).then(function (msg) {
                    //respond to action in dialog if necessary... currently dont need to do anything
                }, function () {

                });
            }

        };
        return data;

    }]);


        var controller = function ($scope, $mdDialog, NflowCreationErrorService) {
            var self = this;

            var errorData = NflowCreationErrorService.nflowError;
            $scope.nflowName = errorData.nflowName;
            $scope.createdNflow = errorData.nifiNflow;
            $scope.isValid = errorData.isValid;
            $scope.message = errorData.message;
            $scope.nflowErrorsData = errorData.nflowErrorsData;
            $scope.nflowErrorsCount = errorData.nflowErrorsCount;

            $scope.fixErrors = function () {
                $mdDialog.hide('fixErrors');
            }

            $scope.hide = function () {
                $mdDialog.hide();
            };

            $scope.cancel = function () {
                $mdDialog.cancel();
            };

        };

        angular.module(moduleName).controller('NflowErrorDialogController',["$scope","$mdDialog","NflowCreationErrorService",controller]);

});
