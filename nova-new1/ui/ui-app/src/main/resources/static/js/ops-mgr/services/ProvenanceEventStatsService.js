define(['angular','ops-mgr/module-name'], function (angular,moduleName) {
    angular.module(moduleName).factory('ProvenanceEventStatsService', ["$http", "$q", "OpsManagerRestUrlService", function ($http, $q, OpsManagerRestUrlService) {

        var data = {

            getTimeFrameOptions: function () {

                var promise = $http.get(OpsManagerRestUrlService.PROVENANCE_EVENT_TIME_FRAME_OPTIONS);
                return promise;
            },

            getNflowProcessorDuration: function (nflowName, from, to) {
                var self = this;

                var successFn = function (response) {

                }
                var errorFn = function (err) {
                    self.loading = false;

                }
                var promise = $http.get(OpsManagerRestUrlService.PROCESSOR_DURATION_FOR_NFLOW(nflowName, from, to));
                promise.then(successFn, errorFn);
                return promise;
            },
            getNflowStatisticsOverTime: function (nflowName, from, to) {
                var self = this;

                var successFn = function (response) {

                };
                var errorFn = function (err) {
                    self.loading = false;
                };
                var promise = $http.get(OpsManagerRestUrlService.NFLOW_STATISTICS_OVER_TIME(nflowName, from, to));
                promise.then(successFn, errorFn);
                return promise;
            },

            getNflowProcessorErrors: function (nflowName, from, to, after) {
                var self = this;

                var successFn = function (response) {

                }
                var errorFn = function (err) {
                    self.loading = false;

                }
                var promise = $http.get(OpsManagerRestUrlService.NFLOW_PROCESSOR_ERRORS(nflowName, from, to),{params:{after:after}});
                promise.then(successFn, errorFn);
                return promise;
            }

        }
        return data;

    }]);
});
