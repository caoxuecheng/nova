define(['angular','nflow-mgr/nflows/edit-nflow/module-name'], function (angular,moduleName) {

    var directive = function () {
        return {
            restrict: "EA",
            bindToController: {},
            controllerAs: 'vm',
            scope: {},
            templateUrl: 'js/nflow-mgr/nflows/edit-nflow/details/nflow-nifi-properties.html',
            controller: "NflowNifiPropertiesController",
            link: function ($scope, element, attrs, controller) {

            }

        };
    }

    var controller = function ($scope, $http,$q,RestUrlService,AccessControlService,EntityAccessControlService, NflowService, EditNflowNifiPropertiesService, NflowInputProcessorOptionsFactory, NflowDetailsProcessorRenderingHelper, BroadcastService,NflowPropertyService) {

        var self = this;

        /**
         * The ng-form object
         * @type {{}}
         */
        this.nflowDetailsForm = {}



        /**
         * Indicates if the nflow NiFi properties may be edited.
         * @type {boolean}
         */
        self.allowEdit = false;

        this.model = NflowService.editNflowModel;
        this.editModel = {};
        this.editableSection = false;
        this.INCREMENTAL_DATE_PROPERTY_KEY = 'Date Field';

        $scope.$watch(function () {
            return NflowService.editNflowModel;
        }, function (newVal) {
            //only update the model if it is not set yet
            if (self.model == null) {
                self.model = angular.copy(NflowService.editNflowModel);

            }
            //tell the ui what properties to show/hide
            var renderGetTableData = NflowDetailsProcessorRenderingHelper.updateGetTableDataRendering(self.model.inputProcessor, self.model.nonInputProcessors);
            //   var renderSqoop = NflowDetailsProcessorRenderingHelper.updateSqoopProcessorRendering(self.model.inputProcessor, self.model.nonInputProcessors);
            updateControllerServiceProperties();
        })

        var inputProcessorIdWatch = $scope.$watch(function () {
            return self.editModel.inputProcessorId;
        }, function (newVal) {
            updateInputProcessor(newVal);
            //validate();
        });

        function updateInputProcessor(newVal) {
            angular.forEach(self.editModel.inputProcessors, function (processor) {
                if (processor.processorId == newVal) {
                    //check the type and return the custom form if there is one via a factory
                    var renderGetTableData = NflowDetailsProcessorRenderingHelper.updateGetTableDataRendering(processor, self.editModel.nonInputProcessors);
                    //   var renderSqoop = NflowDetailsProcessorRenderingHelper.updateSqoopProcessorRendering(processor, self.editModel.nonInputProcessors);
                    if (renderGetTableData) {
                        self.model.table.method = 'EXISTING_TABLE';
                    }
                    self.editModel.inputProcessor = processor;
                    self.editModel.inputProcessorType = processor.type;
                    return false;
                }
            })
        }

        function findIncrementalDateFieldProperty() {
            return findProperty(self.INCREMENTAL_DATE_PROPERTY_KEY);
        }

        /**
         * add the select options to controller services
         */
        function updateControllerServiceProperties(){

               _.filter(self.model.nonInputProcessors, function (processor) {
                if(processor && processor.properties){
                    var props = _.filter(processor.properties,function(property){
                        if(isControllerServiceProperty(property)){
                            setControllerServicePropertyDisplayName(property);
                        }
                    });
                }
            });

            _.filter(self.model.inputProcessor, function (processor) {
                if(processor && processor.properties){
                    var props = _.filter(processor.properties,function(property){
                        if(isControllerServiceProperty(property)){
                            setControllerServicePropertyDisplayName(property);
                        }
                    });
                }
            });


        }

        /**
         * determine if a property is a controller service
         * @param property
         * @returns {boolean}
         */
        function isControllerServiceProperty(property){
            var controllerService = property.propertyDescriptor.identifiesControllerService;
            if(controllerService != null && controllerService != undefined && controllerService != ''){
                return true;
            }
            return false;
        }

        /**
         * add the proper select values to controller services
         * @param property
         */
        function setControllerServicePropertyDisplayName(property){
         var controllerService = property.propertyDescriptor.identifiesControllerService;
         if(controllerService != null && controllerService != undefined && controllerService != ''){
             //fetch the name
             var promise = $http.get(RestUrlService.GET_CONTROLLER_SERVICE_URL(property.value));
             promise.then(function(response) {
                 if(response && response.data ){
                     property.displayValue = response.data.name;
                     //set the allowable values on the property
                     if(property.propertyDescriptor.allowableValues == null){
                         property.propertyDescriptor.allowableValues = [];
                         property.propertyDescriptor.allowableValues.push({value:property.value,displayName:property.displayValue});
                     }
                 }
             }, function(err){
                 //unable to fetch controller service... the id will display
             });


         }
        }

        function findProperty(key) {
            return _.find(self.model.allProperties, function (property) {
                //return property.key = 'Source Database Connection';
                return property.key == key;
            });
        }

        this.onEdit = function () {
            //copy the model
            var inputProcessors = angular.copy(NflowService.editNflowModel.inputProcessors);
            var nonInputProcessors = angular.copy(NflowService.editNflowModel.nonInputProcessors);
            self.editModel = {};

            var allInputProperties = _.filter(self.model.properties, function (property) {
                return property.inputProperty == true;
            });

            var allInputProcessorProperties = _.groupBy(allInputProperties, function (property) {
                return property.processorId;
            })

            var allInputProcessorProperties = angular.copy(allInputProcessorProperties);
            self.editModel.allInputProcessorProperties = allInputProcessorProperties;
            self.editModel.inputProcessors = inputProcessors;
            self.editModel.nonInputProcessors = nonInputProcessors;

            // Find controller services
            _.chain(self.editModel.inputProcessors.concat(self.editModel.nonInputProcessors ))
                .pluck("properties")
                .flatten(true)
                .filter(function(property) {
                    return angular.isObject(property.propertyDescriptor) && angular.isString(property.propertyDescriptor.identifiesControllerService);
                })
                .each(NflowService.findControllerServicesForProperty);



            //NEED TO COPY IN TABLE PROPS HERE
            self.editModel.table = angular.copy(NflowService.editNflowModel.table);
            EditNflowNifiPropertiesService.editNflowModel = self.editModel;
            if (angular.isDefined(self.model.inputProcessor)) {
                updateInputProcessor(self.model.inputProcessor.processorId);
                self.editModel.inputProcessorId = self.model.inputProcessor.processorId;
            }
        };

        this.onCancel = function () {

        }
        this.onSave = function (ev) {
            NflowService.showNflowSavingDialog(ev, "Saving...", self.model.nflowName);

            var copy = angular.copy(NflowService.editNflowModel);

            copy.inputProcessors = self.editModel.inputProcessors;
            copy.nonInputProcessors = self.editModel.nonInputProcessors;
            copy.inputProcessorId = self.editModel.inputProcessorId;
            copy.inputProcessor = self.editModel.inputProcessor;
            copy.inputProcessorType = self.editModel.inputProcessorType;
            copy.userProperties = null;

            //table type is edited here so need to update that prop as well
            copy.table.tableType = self.editModel.table.tableType

            if (copy.table.incrementalDateField) {
                var dateProperty = findIncrementalDateFieldProperty();
                if(dateProperty) {
                    dateProperty.value = self.editModel.table.incrementalDateField;
                }
                copy.table.incrementalDateField = self.editModel.table.incrementalDateField;
            }

            //update the db properties

            NflowService.saveNflowModel(copy).then(function (response) {
                NflowService.hideNflowSavingDialog();

                self.editableSection = false;


                self.model.inputProcessors = self.editModel.inputProcessors;
                self.model.nonInputProcessors = self.editModel.nonInputProcessors;
                self.model.inputProcessorId = self.editModel.inputProcessorId;
                self.model.inputProcessor = self.editModel.inputProcessor;
                self.model.table.tableType = self.editModel.table.tableType;
                self.model.table.incrementalDateField = self.editModel.table.incrementalDateField;
                self.model.inputProcessorType = self.editModel.inputProcessorType;

                NflowPropertyService.updateDisplayValueForProcessors(self.model.inputProcessors);
                NflowPropertyService.updateDisplayValueForProcessors(self.model.nonInputProcessors)

                updateControllerServiceProperties();
                //update the displayValue

            }, function (response) {
                NflowService.hideNflowSavingDialog();
                console.log('ERRORS were found ', response)
                NflowService.buildErrorData(self.model.nflowName, response);
                NflowService.showNflowErrorsDialog();
                //make it editable
                self.editableSection = true;
            });
        };

        //Apply the entity access permissions
        $q.when(AccessControlService.hasPermission(AccessControlService.NFLOWS_EDIT,self.model,AccessControlService.ENTITY_ACCESS.NFLOW.EDIT_NFLOW_DETAILS)).then(function(access) {
            self.allowEdit = access && !self.model.view.nflowDetails.disabled
        });
    };

    angular.module(moduleName).controller('NflowNifiPropertiesController', ["$scope","$http","$q","RestUrlService","AccessControlService","EntityAccessControlService","NflowService","EditNflowNifiPropertiesService","NflowInputProcessorOptionsFactory","NflowDetailsProcessorRenderingHelper","BroadcastService","NflowPropertyService",controller]);

    angular.module(moduleName)
        .directive('onescorpinNflowNifiProperties', directive);

});
