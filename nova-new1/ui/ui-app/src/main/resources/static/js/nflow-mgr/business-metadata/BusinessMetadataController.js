define(['angular','nflow-mgr/business-metadata/module-name'], function (angular,moduleName) {
    /**
     * Controller for the business metadata page.
     *
     * @constructor
     * @param $scope the application model
     * @param $http the HTTP service
     * @param {AccessControlService} AccessControlService the access control service
     * @param RestUrlService the Rest URL service
     */
    function BusinessMetadataController($scope, $http, AccessControlService, RestUrlService) {
        var self = this;

        /**
         * Indicates if the category fields may be edited.
         * @type {boolean}
         */
        self.allowCategoryEdit = false;

        /**
         * Indicates if the nflow fields may be edited.
         * @type {boolean}
         */
        self.allowNflowEdit = false;

        /**
         * Model for editable sections.
         * @type {{categoryFields: Array, nflowFields: Array}}
         */
        self.editModel = {categoryFields: [], nflowFields: []};

        /**
         * Indicates that the editable section for categories is displayed.
         * @type {boolean}
         */
        self.isCategoryEditable = false;

        /**
         * Indicates that the editable section for categories is valid.
         * @type {boolean}
         */
        self.isCategoryValid = true;

        /**
         * Indicates that the editable section for categories is displayed.
         * @type {boolean}
         */
        self.isNflowEditable = false;

        /**
         * Indicates that the editable section for categories is valid.
         * @type {boolean}
         */
        self.isNflowValid = true;

        /**
         * Indicates that the loading progress bar is displayed.
         * @type {boolean}
         */
        self.loading = true;

        /**
         * Model for read-only sections.
         * @type {{categoryFields: Array, nflowFields: Array}}
         */
        self.model = {categoryFields: [], nflowFields: []};

        /**
         * Creates a copy of the category model for editing.
         */
        self.onCategoryEdit = function() {
            self.editModel.categoryFields = angular.copy(self.model.categoryFields);
        };

        /**
         * Saves the category model.
         */
        self.onCategorySave = function() {
            var model = angular.copy(self.model);
            model.categoryFields = self.editModel.categoryFields;

            $http({
                data: angular.toJson(model),
                headers: {'Content-Type': 'application/json; charset=UTF-8'},
                method: "POST",
                url: RestUrlService.ADMIN_USER_FIELDS
            }).then(function() {
                self.model = model;
            });
        };

        /**
         * Creates a copy of the nflow model for editing.
         */
        self.onNflowEdit = function() {
            self.editModel.nflowFields = angular.copy(self.model.nflowFields);
        };

        /**
         * Saves the nflow model.
         */
        self.onNflowSave = function() {
            var model = angular.copy(self.model);
            model.nflowFields = self.editModel.nflowFields;

            $http({
                data: angular.toJson(model),
                headers: {'Content-Type': 'application/json; charset=UTF-8'},
                method: "POST",
                url: RestUrlService.ADMIN_USER_FIELDS
            }).then(function() {
                self.model = model;
            });
        };

        // Load the field models
        $http.get(RestUrlService.ADMIN_USER_FIELDS).then(function(response) {
            self.model = response.data;
            self.loading = false;
        });

        // Load the permissions
        AccessControlService.getUserAllowedActions()
                .then(function(actionSet) {
                    self.allowCategoryEdit = AccessControlService.hasAction(AccessControlService.CATEGORIES_ADMIN, actionSet.actions);
                    self.allowNflowEdit = AccessControlService.hasAction(AccessControlService.NFLOWS_ADMIN, actionSet.actions);
                });
    }

    // Register the controller
    angular.module(moduleName).controller('BusinessMetadataController', ["$scope","$http","AccessControlService","RestUrlService",BusinessMetadataController]);
});
