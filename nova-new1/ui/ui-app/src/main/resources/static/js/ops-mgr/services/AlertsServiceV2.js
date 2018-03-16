define(['angular','ops-mgr/module-name','ops-mgr/module'], function (angular,moduleName) {
    angular.module(moduleName).factory('AlertsService', ["$q","$http","$interval","OpsManagerRestUrlService", function ($q,$http,$interval, OpsManagerRestUrlService) {

        /**
         * Flag to indicate the alerts have been loaded at least once
         * @type {boolean}
         */
        var loadedGlobalAlertsSummary = false;

        var alertSummaryRefreshTimeMillis = 5000;

        /**
         * ref to the refresh interval so we can cancel it
         * @type {null}
         */
        var alertsSummaryIntervalObject = null;

        var transformAlertSummaryResponse = function(alertSummaries){
            _.each(alertSummaries,function(summary){
                summary.since =  new moment(summary.lastAlertTimestamp).fromNow();

            });
        }

        var data =
        {
            alertsSummary:{
                lastRefreshTime:'',
                data:[]
            },
            transformAlerts:transformAlertSummaryResponse,
            fetchNflowAlerts:function(nflowName, nflowId) {
                var deferred = $q.defer();
                $http.get(OpsManagerRestUrlService.NFLOW_ALERTS_URL(nflowName),{params:{"nflowId":nflowId}}).then(function (response) {
                    transformAlertSummaryResponse(response.data)
                    deferred.resolve(response.data);
                }, function(err){
                    deferred.reject(err)
                });
                return deferred.promise;
            },

        };
        return data;




    }]);
});



