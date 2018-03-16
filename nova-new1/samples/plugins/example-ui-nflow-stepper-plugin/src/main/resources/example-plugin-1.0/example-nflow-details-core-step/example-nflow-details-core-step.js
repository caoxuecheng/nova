define(["angular", "/example-plugin-1.0/module-name"], function (angular, moduleName) {

    var directive = function () {
        return {
            controller: "ExampleNflowDetailsCoreStepController",
            controllerAs: "vm",
            templateUrl: "/example-plugin-1.0/example-nflow-details-core-step/example-nflow-details-core-step.html"
        }
    };

    function controller($scope, $q, AccessControlService, NflowService) {
        var self = this;

        // Indicates if the model may be edited
        self.allowEdit = false;

        self.editableSection = false;
        self.editModel = {};

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
        };

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

        //Apply the entity access permissions
        $q.when(AccessControlService.hasPermission(AccessControlService.NFLOWS_EDIT,self.model,AccessControlService.ENTITY_ACCESS.NFLOW.EDIT_NFLOW_DETAILS)).then(function(access) {
            self.allowEdit = access;
        });
    }

    angular.module(moduleName)
        .controller("ExampleNflowDetailsCoreStepController", ["$scope", "$q", "AccessControlService", "NflowService", controller])
        .directive("exampleNflowDetailsCoreStep", directive);
});
