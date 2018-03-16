define(["angular"], function (angular) {

    /**
     * Directive for viewing and editing data transformation properties.
     */
    var novaDataTransformProperties = function () {
        return {
            controller: "DataTransformPropertiesController",
            controllerAs: "vm",
            restrict: "E",
            templateUrl: "js/plugin/template-table-option/data-transformation/data-transform-properties.html"
        };
    };

    /**
     * Controller for viewing and editing data transformation properties.
     * @constructor
     */
    var DataTransformPropertiesController = function ($scope, $q, AccessControlService, NflowService, StateService, VisualQueryService) {
        var self = this;

        /**
         * Indicates if the nflow NiFi properties may be edited.
         * @type {boolean}
         */
        self.allowEdit = false;

        /**
         * Indicates that the editable section is visible.
         * @type {boolean}
         */
        self.editableSection = false;

        /**
         * Nflow model.
         * @type {Object}
         */
        self.model = NflowService.editNflowModel;

        // Watch for model changes
        $scope.$watch(function () {
            return NflowService.editNflowModel;
        }, function () {
            //only update the model if it is not set yet
            if (self.model == null) {
                self.model = angular.copy(NflowService.editNflowModel);
            }
        });

        /**
         * Navigates to the Edit Nflow page for the current nflow.
         */
        this.navigateToEditNflowInStepper = function () {
            VisualQueryService.resetModel();
            StateService.NflowManager().Nflow().navigateToEditNflowInStepper(self.model.nflowId);
        };

        //Apply the entity access permissions
        $q.when(AccessControlService.hasPermission(AccessControlService.NFLOWS_EDIT, self.model, AccessControlService.ENTITY_ACCESS.NFLOW.EDIT_NFLOW_DETAILS)).then(function (access) {
            self.allowEdit = access;
        });
    };

    angular.module("nova.plugin.template-table-option.data-transformation", [])
        .controller('DataTransformPropertiesController', ["$scope", "$q", "AccessControlService", "NflowService", "StateService", "VisualQueryService", DataTransformPropertiesController])
        .directive('novaDataTransformProperties', novaDataTransformProperties);
});
