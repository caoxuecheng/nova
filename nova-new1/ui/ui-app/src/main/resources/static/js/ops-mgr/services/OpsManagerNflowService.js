define(['angular','ops-mgr/module-name'], function (angular,moduleName) {
    /**
     * Service to call out to Nflow REST.
     *
     */
    angular.module(moduleName).factory('OpsManagerNflowService', ['$q', '$http', '$interval', '$timeout', 'HttpService', 'IconService', 'AlertsService', 'OpsManagerRestUrlService',
     function ($q, $http, $interval, $timeout, HttpService, IconService, AlertsService, OpsManagerRestUrlService) {
         var data = {};
         data.NFLOW_HEALTH_URL = OpsManagerRestUrlService.NFLOW_HEALTH_URL;
         data.NFLOW_NAMES_URL = OpsManagerRestUrlService.NFLOW_NAMES_URL;
         data.NFLOW_HEALTH_COUNT_URL = OpsManagerRestUrlService.NFLOW_HEALTH_COUNT_URL;
         data.FETCH_NFLOW_HEALTH_INTERVAL = 5000;
         data.fetchNflowHealthInterval = null;
         data.nflowHealth = {};

        // data.SPECIFIC_NFLOW_HEALTH_COUNT_URL = OpsManagerRestUrlService.SPECIFIC_NFLOW_HEALTH_COUNT_URL;

         data.SPECIFIC_NFLOW_HEALTH_URL = OpsManagerRestUrlService.SPECIFIC_NFLOW_HEALTH_URL;

         data.DAILY_STATUS_COUNT_URL = OpsManagerRestUrlService.NFLOW_DAILY_STATUS_COUNT_URL;

         data.nflowSummaryData = {};
         data.nflowUnhealthyCount = 0;
         data.nflowHealthyCount = 0;

         data.nflowHealth = 0;

         data.emptyNflow = function () {
             var nflow = {};
             nflow.displayStatus = 'LOADING';
             nflow.lastStatus = 'LOADING',
                 nflow.timeSinceEndTime = 0;
             nflow.isEmpty = true;
             return nflow;
         }

         data.decorateNflowSummary = function (nflow) {
             //GROUP FOR FAILED

             if (nflow.isEmpty == undefined) {
                 nflow.isEmpty = false;
             }

             var health = "---";
             if (!nflow.isEmpty) {
                 health = nflow.healthy ? 'HEALTHY' : 'UNHEALTHY';
                 var iconData = IconService.iconForNflowHealth(health);
                 nflow.icon = iconData.icon
                 nflow.iconstyle = iconData.style
             }
             nflow.healthText = health;
             if (nflow.running) {
                 nflow.displayStatus = 'RUNNING';
             }
             else if ("FAILED" == nflow.lastStatus || ( "FAILED" == nflow.lastExitCode && "ABANDONED" != nflow.lastStatus)) {
                 nflow.displayStatus = 'FAILED';
             }
             else if ("COMPLETED" == nflow.lastExitCode) {
                 nflow.displayStatus = 'COMPLETED';
             }
             else if ("STOPPED" == nflow.lastStatus) {
                 nflow.displayStatus = 'STOPPED';
             }
             else if ("UNKNOWN" == nflow.lastStatus) {
                 nflow.displayStatus = 'INITIAL';
                 nflow.sinceTimeString = '--';
                 nflow.runTimeString = "--"
             }
             else {
                 nflow.displayStatus = nflow.lastStatus;
             }

             nflow.statusStyle = IconService.iconStyleForJobStatus(nflow.displayStatus);
         }

         data.fetchNflowSummaryData = function () {
             var successFn = function (response) {
                 data.nflowSummaryData = response.data;
                 if (response.data) {
                     data.nflowUnhealthyCount = response.data.failedCount;
                     data.nflowHealthyCount = response.data.healthyCount;
                 }
             }
             var errorFn = function (err) {

             }
             var finallyFn = function () {

             }

             var promise = $http.get(data.NFLOW_HEALTH_URL);
             promise.then(successFn, errorFn);
             return promise;
         };

         data.fetchNflowHealth = function () {
             var successFn = function (response) {

                 var unhealthyNflowNames = [];
                 if (response.data) {
                     angular.forEach(response.data, function (nflowHealth) {
                         if (data.nflowHealth[nflowHealth.nflow]) {
                             angular.extend(data.nflowHealth[nflowHealth.nflow], nflowHealth);
                         }
                         else {
                             data.nflowHealth[nflowHealth.nflow] = nflowHealth;
                         }
                         if (nflowHealth.lastUnhealthyTime) {
                             nflowHealth.sinceTimeString = new moment(nflowHealth.lastUnhealthyTime).fromNow();
                         }
                         if (nflowHealth.healthy) {
                        //     AlertsService.removeNflowFailureAlertByName(nflowHealth.nflow);
                         }
                         else {
                             unhealthyNflowNames.push(nflowHealth.nflow);
                      //       AlertsService.addNflowHealthFailureAlert(nflowHealth);
                         }
                     });
                     //only unhealthy will come back
                     //if nflowName is not in the response list, but currently failed.. remove it
               //      var failedNflows = AlertsService.nflowFailureAlerts;
            //         angular.forEach(failedNflows, function (alert, nflowName) {
            //             if (_.indexOf(unhealthyNflowNames, nflowName) == -1) {
            //                 AlertsService.removeNflowFailureAlertByName(nflowName);
             //            }
              //       });

                 }
                 data.fetchNflowHealthTimeout();
             }
             var errorFn = function (err) {
                 data.fetchNflowHealthTimeout();
             }
             var finallyFn = function () {

             }

             var promise = $http.get(data.NFLOW_HEALTH_COUNT_URL);
             promise.then(successFn, errorFn);
             return promise;
         };

         data.startFetchNflowHealth = function () {
             if (data.fetchNflowHealthInterval == null) {
                 data.fetchNflowHealth();

                 data.fetchNflowHealthInterval = $interval(function () {
                     data.fetchNflowHealth();
                 }, data.FETCH_NFLOW_HEALTH_INTERVAL)
             }
         }

         data.fetchNflowHealthTimeout = function () {
             data.stopFetchNflowHealthTimeout();

             data.fetchNflowHealthInterval = $timeout(function () {
                 data.fetchNflowHealth();
             }, data.FETCH_NFLOW_HEALTH_INTERVAL);
         }

         data.stopFetchNflowHealthTimeout = function () {
             if (data.fetchNflowHealthInterval != null) {
                 $timeout.cancel(data.fetchNflowHealthInterval);
             }
         }

         return data;
     }]);
});
