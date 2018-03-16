define(['angular','nflow-mgr/nflows/edit-nflow/module-name'], function (angular,moduleName) {


    var directive = function () {
        return {
            restrict: "EA",
            bindToController: {
                processingdttm:'=',
                rowsPerPage:'='
            },
            controllerAs: 'vm',
            scope: {},
            templateUrl: 'js/nflow-mgr/nflows/edit-nflow/profile-history/profile-valid-results.html',
            controller: "NflowProfileValidResultsController",
            link: function ($scope, element, attrs, controller) {

            }

        };
    }

    var controller =  function($scope,$http, NflowService, RestUrlService, HiveService, Utils,BroadcastService) {

        var self = this;

        this.model = NflowService.editNflowModel;
        this.loading = false;
        this.limitOptions = [10, 50, 100, 500, 1000];
        this.limit = this.limitOptions[2];

        //noinspection JSUnusedGlobalSymbols
        this.onLimitChange = function() {
            getProfileValidation();
        };

        $scope.gridOptions = {
            columnDefs: [],
            data: null,
            enableColumnResizing: true,
            enableGridMenu: true
        };

        function getProfileValidation(){
            self.loading = true;

            var successFn = function (response) {
                var result = self.queryResults = HiveService.transformResultsToUiGridModel(response);

                $scope.gridOptions.columnDefs = result.columns;
                $scope.gridOptions.data = result.rows;

                self.loading = false;

                BroadcastService.notify('PROFILE_TAB_DATA_LOADED','valid');
            };
            var errorFn = function (err) {
                self.loading = false;
            };
            var promise = $http.get(RestUrlService.NFLOW_PROFILE_VALID_RESULTS_URL(self.model.id),
                { params:
                    {
                        'processingdttm': self.processingdttm,
                        'limit': self.limit
                    }
                });
            promise.then(successFn, errorFn);
            return promise;
        }

        getProfileValidation();
    };


    angular.module(moduleName).controller('NflowProfileValidResultsController', ["$scope","$http","NflowService","RestUrlService","HiveService","Utils","BroadcastService",controller]);
    angular.module(moduleName)
        .directive('onescorpinNflowProfileValid', directive);

});
