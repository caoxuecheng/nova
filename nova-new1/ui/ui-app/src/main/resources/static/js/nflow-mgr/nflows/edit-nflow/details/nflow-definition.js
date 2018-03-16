define(['angular','nflow-mgr/nflows/edit-nflow/module-name'], function (angular,moduleName) {

    var directive = function () {
        return {
            restrict: "EA",
            bindToController: {
            },
            controllerAs: 'vm',
            scope: {},
            templateUrl: 'js/nflow-mgr/nflows/edit-nflow/details/nflow-definition.html',
            controller: "NflowDefinitionController",
            link: function ($scope, element, attrs, controller) {

            }

        };
    }

    var controller =  function($scope, $q, AccessControlService,EntityAccessControlService, NflowService) {

        var self = this;

        /**
         * Indicates if the nflow definitions may be edited.
         * @type {boolean}
         */
        self.allowEdit = false;

        this.model = NflowService.editNflowModel;
        this.editableSection = false;

        $scope.$watch(function(){
            return NflowService.editNflowModel;
        },function(newVal) {
            //only update the model if it is not set yet
            if(self.model == null) {
                self.model = angular.copy(NflowService.editNflowModel);
            }
        })


        self.editModel = {};


        this.onEdit = function(){
            //copy the model
            var copy = NflowService.editNflowModel;
            self.editModel= {};
            self.editModel.nflowName = copy.nflowName;
            self.editModel.systemNflowName = copy.systemNflowName;
            self.editModel.description = copy.description;
            self.editModel.templateId = copy.templateId;
        }

        this.onCancel = function() {

        }

        this.onSave = function (ev) {
            //save changes to the model
            NflowService.showNflowSavingDialog(ev, "Saving...", self.model.nflowName);
            var copy = angular.copy(NflowService.editNflowModel);

            copy.nflowName = self.editModel.nflowName;
            copy.systemNflowName = self.editModel.systemNflowName;
            copy.description = self.editModel.description;
            copy.templateId = self.editModel.templateId;
            copy.userProperties = null;

            NflowService.saveNflowModel(copy).then(function (response) {
                NflowService.hideNflowSavingDialog();
                self.editableSection = false;
                //save the changes back to the model
                self.model.nflowName = self.editModel.nflowName;
                self.model.systemNflowName = self.editModel.systemNflowName;
                self.model.description = self.editModel.description;
                self.model.templateId = self.editModel.templateId;
            }, function (response) {
                NflowService.hideNflowSavingDialog();
                NflowService.buildErrorData(self.model.nflowName, response);
                NflowService.showNflowErrorsDialog();
                //make it editable
                self.editableSection = true;
            });
        };

        //Apply the entity access permissions
        $q.when(AccessControlService.hasPermission(AccessControlService.NFLOWS_EDIT,self.model,AccessControlService.ENTITY_ACCESS.NFLOW.EDIT_NFLOW_DETAILS)).then(function(access) {
            self.allowEdit = access && !self.model.view.generalInfo.disabled;
        });
    };


    angular.module(moduleName).controller('NflowDefinitionController', ["$scope","$q","AccessControlService","EntityAccessControlService","NflowService",controller]);

    angular.module(moduleName)
        .directive('onescorpinNflowDefinition', directive);

});
