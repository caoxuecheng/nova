define(['angular','nflow-mgr/categories/module-name'], function (angular,moduleName) {
    /**
     * Manages the Related Nflows section of the Category Details page.
     *
     * @constructor
     * @param $scope the application model
     * @param CategoriesService the category service
     * @param StateService the URL service
     */
    function CategoryNflowsController($scope, CategoriesService, StateService) {
        var self = this;

        /**
         * Category data.
         * @type {CategoryModel}
         */
        self.model = CategoriesService.model;

        /**
         * Navigates to the specified nflow.
         *
         * @param {Object} nflow the nflow to navigate to
         */
        self.onNflowClick = function(nflow) {
            StateService.NflowManager().Nflow().navigateToNflowDetails(nflow.id);
        };
    }

    /**
     * Creates a directive for the Related Nflows section.
     *
     * @returns {Object} the directive
     */
    function onescorpinCategoryNflows() {
        return {
            controller: "CategoryNflowsController",
            controllerAs: "vm",
            restrict: "E",
            scope: {},
            templateUrl: "js/nflow-mgr/categories/details/category-nflows.html"
        };
    }

    angular.module(moduleName).controller('CategoryNflowsController', ["$scope","CategoriesService","StateService",CategoryNflowsController]);
    angular.module(moduleName).directive('onescorpinCategoryNflows', onescorpinCategoryNflows);
});
