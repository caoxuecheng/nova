define(["angular",  "/example-plugin-1.0/module-name"], function (angular, moduleName) {

    var directive = function () {
        return {
            controller: "ExampleNflowDetailsPreStepController",
            controllerAs: "vm",
            templateUrl: "/example-plugin-1.0/example-nflow-details-pre-step/example-nflow-details-pre-step.html"
        }
    };

    function controller($scope, $q, AccessControlService, NflowService) {
        var self = this;

        // Indicates if the model may be edited
        self.allowEdit = false;

        self.editableSection = false;
        self.editModel = {};

        self.isValid = false;

        self.model = NflowService.editNflowModel;
        $scope.$watch(function() {
            return NflowService.editNflowModel;
        }, function(newVal) {
            //only update the model if it is not set yet
            if (self.model == null) {
                self.model = NflowService.editNflowModel;
            }
        });

        // Copy model for editing
        this.onEdit = function() {
            self.editModel = {
                tableOption: angular.copy(self.model.tableOption)
            };
            setDefaults(self.editModel);

        };

        var setDefaults = function(model){
            var defaults = {"preStepName":"","preStepAge":0,"preStepMagicWord":""}
            _.each(defaults,function(value,key){
             if(angular.isUndefined(model.tableOption[key])){
                 model.tableOption[key] = value;
             }
            });
        }

        // Save changes to the model
        this.onSave = function(ev) {
            NflowService.showNflowSavingDialog(ev, "Saving...", self.model.nflowName);

            var copy = angular.copy(NflowService.editNflowModel);
            copy.tableOption = self.editModel.tableOption;

            NflowService.saveNflowModel(copy).then(function() {
                NflowService.hideNflowSavingDialog();
                self.editableSection = false;
                //save the changes back to the model
                self.model.tableOption = self.editModel.tableOption;
            }, function(response) {
                NflowService.hideNflowSavingDialog();
                NflowService.buildErrorData(self.model.nflowName, response);
                NflowService.showNflowErrorsDialog();
                //make it editable
                self.editableSection = true;
            });
        };

        /**
         * Enforce min age limit
         */
        this.ageChanged = function(){
            var age = self.editModel.tableOption.preStepAge;
            if(angular.isDefined(age) && parseInt(age) >=21) {
                self.form['preStepAge'].$setValidity('ageError', true);
            }
            else {
                self.form['preStepAge'].$setValidity('ageError', false);

            }

        }

        this.magicWordChanged = function(){
            var magicWord = self.editModel.tableOption.preStepMagicWord;
            if(angular.isDefined(magicWord) && magicWord != null) {
                if (magicWord.toLowerCase() == 'nova') {
                    self.form['preStepMagicWord'].$setValidity('magicWordError', true);
                }
                else {
                    self.form['preStepMagicWord'].$setValidity('magicWordError', false);
                }
            }
        }



        //Apply the entity access permissions
        $q.when(AccessControlService.hasPermission(AccessControlService.NFLOWS_EDIT,self.model,AccessControlService.ENTITY_ACCESS.NFLOW.EDIT_NFLOW_DETAILS)).then(function(access) {
            self.allowEdit = access;
        });
    }
    angular.module(moduleName)
        .controller("ExampleNflowDetailsPreStepController", ["$scope", "$q", "AccessControlService", "NflowService", controller])
        .directive("exampleNflowDetailsPreStep", directive);

});
