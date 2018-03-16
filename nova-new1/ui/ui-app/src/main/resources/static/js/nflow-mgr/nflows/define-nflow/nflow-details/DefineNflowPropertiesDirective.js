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
            require: ['onescorpinDefineNflowProperties', '^onescorpinStepper'],
            scope: {},
            templateUrl: 'js/nflow-mgr/nflows/define-nflow/nflow-details/define-nflow-properties.html',
            controller: "DefineNflowPropertiesController",
            link: function($scope, element, attrs, controllers) {
                var thisController = controllers[0];
                var stepperController = controllers[1];
                thisController.stepperController = stepperController;
                thisController.totalSteps = stepperController.totalSteps;
            }

        };
    };

    var DefineNflowPropertiesDirective = function($scope, $http, $mdToast, RestUrlService, NflowTagService, NflowService) {
        var self = this;

        self.stepNumber = parseInt(this.stepIndex) + 1;
        self.model = NflowService.createNflowModel;
        self.nflowTagService = NflowTagService;
        self.tagChips = {};
        self.tagChips.selectedItem = null;
        self.tagChips.searchText = null;
        self.isValid = true;


        // Update user fields when category changes
        $scope.$watch(
                function() {return self.model.category.id},
                function(categoryId) {
                    if (categoryId !== null) {
                        NflowService.getUserFields(categoryId)
                                .then(self.setUserProperties);
                    }
                }
        );


        /**
         * Sets the user fields for this nflow.
         *
         * @param {Array} userProperties the user fields
         */
        self.setUserProperties = function(userProperties) {
            // Convert old user properties to map
            var oldProperties = {};
            angular.forEach(self.model.userProperties, function(property) {
                if (angular.isString(property.value) && property.value.length > 0) {
                    oldProperties[property.systemName] = property.value;
                }
            });

            // Set new user properties and copy values
            self.model.userProperties = angular.copy(userProperties);

            angular.forEach(self.model.userProperties, function(property) {
                if (angular.isDefined(oldProperties[property.systemName])) {
                    property.value = oldProperties[property.systemName];
                    delete oldProperties[property.systemName];
                }
            });

            // Copy remaining old properties
            angular.forEach(oldProperties, function(value, key) {
                self.model.userProperties.push({locked: false, systemName: key, value: value});
            });
        }

        self.transformChip = function(chip) {
            // If it is an object, it's already a known chip
            if (angular.isObject(chip)) {
                return chip;
            }
            // Otherwise, create a new one
            return {name: chip}
        };

    };

    angular.module(moduleName).controller('DefineNflowPropertiesController', ["$scope","$http","$mdToast","RestUrlService","NflowTagService","NflowService",DefineNflowPropertiesDirective]);
    angular.module(moduleName).directive('onescorpinDefineNflowProperties', directive);
});
