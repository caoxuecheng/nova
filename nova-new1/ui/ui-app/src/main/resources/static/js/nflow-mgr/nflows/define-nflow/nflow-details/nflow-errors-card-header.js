define(['angular','nflow-mgr/nflows/define-nflow/module-name'], function (angular,moduleName) {

    var directive = function (NflowCreationErrorService) {
        return {
            restrict: "EA",
            scope: {
            },
            templateUrl: 'js/nflow-mgr/nflows/define-nflow/nflow-details/nflow-errors-card-header.html',
            link: function ($scope, element, attrs) {

                $scope.hasNflowCreationErrors = function() {
                    return NflowCreationErrorService.hasErrors();
                };
                $scope.showNflowErrorsDialog = NflowCreationErrorService.showErrorDialog;

            }

        };
    }




    angular.module(moduleName)
        .directive('onescorpinNflowErrorsCardHeader', ["NflowCreationErrorService",directive]);

});
