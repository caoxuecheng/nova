define(['angular', 'nflow-mgr/nflows/edit-nflow/module-name'], function (angular, moduleName) {

    var directive = function () {
        return {
            restrict: "EA",
            bindToController: {
                processingdttm: '=',
                rowsPerPage: '='
            },
            controllerAs: 'vm',
            scope: {},
            templateUrl: 'js/nflow-mgr/nflows/edit-nflow/profile-history/profile-stats.html',
            controller: "NflowProfileStatsController"
        };
    };

    var controller = function ($scope, $http, $sce, PaginationDataService, NflowService, RestUrlService, HiveService, Utils, BroadcastService) {
        var self = this;

        self.data = [];
        self.loading = true;
        self.processingDate = new Date(HiveService.getUTCTime(self.processingdttm));
        self.model = NflowService.editNflowModel;
        self.hideColumns = ["processing_dttm"];

        self.getProfileStats = function () {
            self.loading = true;
            var successFn = function (response) {
                self.data = response.data;
                self.loading = false;
                BroadcastService.notify('PROFILE_TAB_DATA_LOADED', 'profile-stats');
            };
            var errorFn = function (err) {
                self.loading = false;
            };
            var promise = $http.get(RestUrlService.NFLOW_PROFILE_STATS_URL(self.model.id), {params: {'processingdttm': self.processingdttm}});
            promise.then(successFn, errorFn);
            return promise;
        };

        self.getProfileStats();
    };

    angular.module(moduleName).controller('NflowProfileStatsController', ["$scope", "$http", "$sce", "PaginationDataService", "NflowService", "RestUrlService", "HiveService", "Utils",
                                                                         "BroadcastService", controller]);
    angular.module(moduleName).directive('onescorpinNflowProfileStats', directive);
});
