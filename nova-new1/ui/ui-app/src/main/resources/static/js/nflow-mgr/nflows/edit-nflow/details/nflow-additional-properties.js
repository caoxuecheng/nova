define(['angular','nflow-mgr/nflows/edit-nflow/module-name'], function (angular,moduleName) {

    var directive = function() {
        return {
            restrict: "EA",
            bindToController: {},
            controllerAs: 'vm',
            scope: {},
            templateUrl: 'js/nflow-mgr/nflows/edit-nflow/details/nflow-additional-properties.html',
            controller: "NflowAdditionalPropertiesController",
            link: function($scope, element, attrs, controller) {

            }

        };
    };

    var NflowAdditionalPropertiesController = function($scope,$q, AccessControlService, EntityAccessControlService,NflowService, NflowTagService, NflowSecurityGroups) {

        var self = this;

        /**
         * Indicates if the nflow properties may be edited.
         * @type {boolean}
         */
        self.allowEdit = false;

        this.model = NflowService.editNflowModel;
        this.editModel = {};
        this.editableSection = false;

        this.nflowTagService = NflowTagService;
        self.tagChips = {};
        self.tagChips.selectedItem = null;
        self.tagChips.searchText = null;
        this.isValid = true;

        this.nflowSecurityGroups = NflowSecurityGroups;

        self.securityGroupChips = {};
        self.securityGroupChips.selectedItem = null;
        self.securityGroupChips.searchText = null;
        self.securityGroupsEnabled = false;

        NflowSecurityGroups.isEnabled().then(function(isValid) {
                self.securityGroupsEnabled = isValid;
            }

        );

        this.transformChip = function(chip) {
            // If it is an object, it's already a known chip
            if (angular.isObject(chip)) {
                return chip;
            }
            // Otherwise, create a new one
            return {name: chip}
        };

        $scope.$watch(function() {
            return NflowService.editNflowModel;
        }, function(newVal) {
            //only update the model if it is not set yet
            if (self.model == null) {
                self.model = NflowService.editNflowModel;
            }
        });

        this.onEdit = function() {
            // Determine tags value
            var tags = angular.copy(NflowService.editNflowModel.tags);
            if (tags == undefined || tags == null) {
                tags = [];
            }

            // Copy model for editing
            self.editModel = {};
            self.editModel.dataOwner = self.model.dataOwner;
            self.editModel.tags = tags;
            self.editModel.userProperties = angular.copy(self.model.userProperties);

            self.editModel.securityGroups = angular.copy(NflowService.editNflowModel.securityGroups);
            if (self.editModel.securityGroups == undefined) {
                self.editModel.securityGroups = [];
            }
        };

        this.onCancel = function() {
            // do nothing
        };

        this.onSave = function(ev) {
            //save changes to the model
            NflowService.showNflowSavingDialog(ev, "Saving...", self.model.nflowName);
            var copy = angular.copy(NflowService.editNflowModel);

            copy.tags = self.editModel.tags;
            copy.dataOwner = self.editModel.dataOwner;
            copy.userProperties = self.editModel.userProperties;
            copy.securityGroups = self.editModel.securityGroups;

            NflowService.saveNflowModel(copy).then(function(response) {
                NflowService.hideNflowSavingDialog();
                self.editableSection = false;
                //save the changes back to the model
                self.model.tags = self.editModel.tags;
                self.model.dataOwner = self.editModel.dataOwner;
                self.model.userProperties = self.editModel.userProperties;
                self.model.securityGroups = self.editModel.securityGroups;
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
            self.allowEdit = access && !self.model.view.properties.disabled;
        });

    };

    angular.module(moduleName).controller('NflowAdditionalPropertiesController',["$scope","$q","AccessControlService","EntityAccessControlService","NflowService","NflowTagService","NflowSecurityGroups",NflowAdditionalPropertiesController]);
    angular.module(moduleName).directive('onescorpinNflowAdditionalProperties', directive);
});
