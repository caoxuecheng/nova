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

    var directive = function () {
        return {
            restrict: "EA",
            bindToController: {
                stepIndex: '@'
            },
            require:['onescorpinDefineNflowGeneralInfo','^onescorpinStepper'],
            scope: {},
            controllerAs: 'vm',
            templateUrl: 'js/nflow-mgr/nflows/define-nflow/nflow-details/define-nflow-general-info.html',
            controller: "DefineNflowGeneralInfoController",
            link: function ($scope, element, attrs, controllers) {
                var thisController = controllers[0];
                var stepperController = controllers[1];
                thisController.stepperController = stepperController;
                thisController.totalSteps = stepperController.totalSteps;
            }

        };
    }

    var controller =  function($scope,$log, $http,$mdToast,RestUrlService, NflowService, CategoriesService) {

        var self = this;

        /**
         * The angular form
         * @type {{}}
         */
        this.defineNflowGeneralForm = {};
        this.templates = [];
        this.model = NflowService.createNflowModel;
        this.isValid = false;
        this.stepNumber = parseInt(this.stepIndex)+1
        this.stepperController = null;

        // Contains existing system nflow names for the current category
        this.existingNflowNames = {};

        this.categorySearchText = '';
        this.category;
        self.categorySelectedItemChange = selectedItemChange;
        self.categorySearchTextChanged = searchTextChange;
        self.categoriesService = CategoriesService;

        /**
         * are we populating the nflow name list for validation
         * @type {boolean}
         */
        self.populatingExsitngNflowNames = false;

        function searchTextChange(text) {
         //   $log.info('Text changed to ' + text);
        }
        function selectedItemChange(item) {
            //only allow it if the category is there and the 'createNflow' flag is true
            if(item != null && item != undefined && item.createNflow) {
                self.model.category.name = item.name;
                self.model.category.id = item.id;
                self.model.category.systemName = item.systemName;
                setSecurityGroups(item.name);
                validateUniqueNflowName();

                if (self.defineNflowGeneralForm && self.defineNflowGeneralForm['category']) {
                    self.defineNflowGeneralForm['category'].$setValidity('accessDenied', true);
                }
            }
            else {
                self.model.category.name = null;
                self.model.category.id = null;
                self.model.category.systemName = null;
                if (self.defineNflowGeneralForm && self.defineNflowGeneralForm['nflowName']) {
                    self.defineNflowGeneralForm['nflowName'].$setValidity('notUnique', true);
                }

                if(item && item.createNflow == false){
                    if (self.defineNflowGeneralForm && self.defineNflowGeneralForm['category']) {
                        self.defineNflowGeneralForm['category'].$setValidity('accessDenied', false);
                    }
                }
                else {
                    if (self.defineNflowGeneralForm && self.defineNflowGeneralForm['category']) {
                        self.defineNflowGeneralForm['category'].$setValidity('accessDenied', true);
                    }
                }
            }
        }

        function existingNflowNameKey(categoryName, nflowName) {
            return categoryName + "." + nflowName;
        }

        populateExistingNflowNames();

        /**
         * updates the {@code existingNflowNames} object with the latest nflow names from the server
         * @returns {promise}
         */
        function populateExistingNflowNames() {
            if(!self.populatingExsitngNflowNames) {
                self.populatingExsitngNflowNames = true;
                NflowService.getNflowNames().then()
                return $http.get(RestUrlService.OPS_MANAGER_NFLOW_NAMES).then(function (response) {
                    self.existingNflowNames = {};
                    if (response.data != null && response.data != null) {
                        angular.forEach(response.data, function (categoryAndNflow) {
                            var categoryName = categoryAndNflow.substr(0, categoryAndNflow.indexOf('.'));
                            var nflowName = categoryAndNflow.substr(categoryAndNflow.indexOf('.')+1)
                            self.existingNflowNames[categoryAndNflow] = nflowName;
                        });
                        self.populatingExsitngNflowNames = false;
                    }
                }, function () {
                    self.populatingExsitngNflowNames = false;
                });
            }
        }

        function validateUniqueNflowName() {

            function _validate() {
                //validate to ensure the name is unique in this category
                if (self.model && self.model.category && self.existingNflowNames[existingNflowNameKey(self.model.category.systemName, self.model.systemNflowName)]) {
                    if (self.defineNflowGeneralForm && self.defineNflowGeneralForm['nflowName']) {
                        self.defineNflowGeneralForm['nflowName'].$setValidity('notUnique', false);
                    }
                }
                else {
                    if (self.defineNflowGeneralForm && self.defineNflowGeneralForm['nflowName']) {
                        self.defineNflowGeneralForm['nflowName'].$setValidity('notUnique', true);
                    }
                }
            }

            if (self.model && self.model.id && self.model.id.length > 0) {
                self.defineNflowGeneralForm['nflowName'].$setValidity('notUnique', true);
            } else if (_.isEmpty(self.existingNflowNames)) {
                if(!self.populatingExsitngNflowNames) {
                    populateExistingNflowNames().then(function () {
                        _validate();
                    });
                }
            } else {
                _validate();
            }

        }



      //  getRegisteredTemplates();

        function validate(){
           var valid = isNotEmpty(self.model.category.name) && isNotEmpty(self.model.nflowName) && isNotEmpty(self.model.templateId);
            self.isValid = valid;
        }

        function setSecurityGroups(newVal) {
            if(newVal) {
                var category = self.categoriesService.findCategoryByName(newVal)
                if(category != null) {
                    var securityGroups = category.securityGroups;
                    self.model.securityGroups = securityGroups;
                }
            }
        }

        function isNotEmpty(item){
            return item != null && item != undefined && item != '';
        }

        this.onTemplateChange = function() {

        }

        $scope.$watch(function(){
            return self.model.id;
        },function(newVal){
            if(newVal == null && (angular.isUndefined(self.model.cloned) || self.model.cloned == false)) {
                self.category = null;
            }
            else {
                self.category = self.model.category;
            }
        })

       var nflowNameWatch = $scope.$watch(function(){
            return self.model.nflowName;
        },function(newVal) {
           NflowService.getSystemName(newVal).then(function (response) {
               self.model.systemNflowName = response.data;
               validateUniqueNflowName();
               validate();
           });

        });

        $scope.$watch(function(){
            return self.model.category.name;
        },function(newVal) {

            validate();
        })

      var templateIdWatch =  $scope.$watch(function(){
            return self.model.templateId;
        },function(newVal) {
            validate();
        });

        /**
         * Return a list of the Registered Templates in the system
         * @returns {HttpPromise}
         */
        function getRegisteredTemplates() {
            var successFn = function (response) {
                self.templates = response.data;
            }
            var errorFn = function (err) {

            }
            var promise = $http.get(RestUrlService.GET_REGISTERED_TEMPLATES_URL);
            promise.then(successFn, errorFn);
            return promise;
        };

            $scope.$on('$destroy',function(){
                nflowNameWatch();
                templateIdWatch();
                self.model = null;
            });

    };


    angular.module(moduleName).controller('DefineNflowGeneralInfoController', ["$scope","$log","$http","$mdToast","RestUrlService","NflowService","CategoriesService",controller]);

    angular.module(moduleName)
        .directive('onescorpinDefineNflowGeneralInfo', directive);

});
