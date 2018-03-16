define(['angular','ops-mgr/module-name'], function (angular,moduleName) {
    /**
     * Service to call out to Nflow REST.
     *
     */
    angular.module(moduleName).factory('OpsManagerDashboardService', ['$q', '$http', '$interval', '$timeout', 'HttpService', 'IconService', 'AlertsService', 'OpsManagerRestUrlService','BroadcastService','OpsManagerNflowService',
     function ($q, $http, $interval, $timeout, HttpService, IconService, AlertsService, OpsManagerRestUrlService, BroadcastService, OpsManagerNflowService) {
         var data = {
             DASHBOARD_UPDATED:'DASHBOARD_UPDATED',
             NFLOW_SUMMARY_UPDATED:'NFLOW_SUMMARY_UPDATED',
             TAB_SELECTED:'TAB_SELECTED',
             nflowSummaryData:{},
             nflowsArray:[],
             nflowUnhealthyCount:0,
             nflowHealthyCount:0,
             dashboard:{},
             nflowsSearchResult:{},
             totalNflows:0,
             activeNflowRequest:null,
             activeDashboardRequest:null,
             skipDashboardNflowHealth:false,
             selectNflowHealthTab:function(tab) {
                 BroadcastService.notify(data.TAB_SELECTED,tab);
             },
             nflowHealthQueryParams:{fixedFilter:'All',filter:'',start:0,limit:10, sort:''}
         };

         var setupNflowHealth = function(nflowsArray){
             var processedNflows = [];
             if(nflowsArray) {
                     var processed = [];
                     var arr = [];
                     _.each(nflowsArray, function (nflowHealth) {
                         //pointer to the nflow that is used/bound to the ui/service
                         var nflowData = null;
                         if (data.nflowSummaryData[nflowHealth.nflow]) {
                             nflowData = data.nflowSummaryData[nflowHealth.nflow]
                             angular.extend(nflowData, nflowHealth);
                             nflowHealth = nflowData;
                         }
                         else {
                             data.nflowSummaryData[nflowHealth.nflow] = nflowHealth;
                             nflowData = nflowHealth;
                         }
                         arr.push(nflowData);

                         processedNflows.push(nflowData);
                         if (nflowData.lastUnhealthyTime) {
                             nflowData.sinceTimeString = new moment(nflowData.lastUnhealthyTime).fromNow();
                         }

                         OpsManagerNflowService.decorateNflowSummary(nflowData);
                         if(nflowData.stream == true && nflowData.nflowHealth){
                             nflowData.runningCount = nflowData.nflowHealth.runningCount;
                             if(nflowData.runningCount == null){
                                 nflowData.runningCount =0;
                             }
                         }

                         if(nflowData.running){
                             nflowData.timeSinceEndTime = nflowData.runTime;
                             nflowData.runTimeString = '--';
                         }
                          processed.push(nflowData.nflow);
                     });
                     var keysToRemove=_.difference(Object.keys(data.nflowSummaryData),processed);
                     if(keysToRemove != null && keysToRemove.length >0){
                         _.each(keysToRemove,function(key){
                             delete  data.nflowSummaryData[key];
                         })
                     }
                     data.nflowsArray = arr;
                 }
                 return processedNflows;

         };

         data.isFetchingNflowHealth = function(){
             return data.activeNflowRequest != null && angular.isDefined(data.activeNflowRequest);
         }

         data.isFetchingDashboard = function(){
             return data.activeDashboardRequest != null && angular.isDefined(data.activeDashboardRequest);
         }

         data.setSkipDashboardNflowHealth = function(skip){
             data.skipDashboardNflowHealth = skip;
         }

         data.fetchNflows = function(tab,filter,start,limit, sort){
             if(data.activeNflowRequest != null && angular.isDefined(data.activeNflowRequest)){
                 data.activeNflowRequest.reject();
             }
             //Cancel any active dashboard queries as this will supercede them
             if(data.activeDashboardRequest != null && angular.isDefined(data.activeDashboardRequest)){
                 data.skipDashboardNflowHealth = true;
             }

             var canceler = $q.defer();

             data.activeNflowRequest = canceler;

             var params = {start: start, limit: limit, sort: sort, filter:filter, fixedFilter:tab};

             var successFn = function (response) {
                 data.nflowsSearchResult = response.data;
                 if(response.data && response.data.data) {
                     setupNflowHealth(response.data.data);
                     //reset data.dashboard.nflows.data ?
                     data.totalNflows = response.data.recordsFiltered;
                 }
                 data.activeNflowRequest = null;
                 data.skipDashboardNflowHealth = false;
             }
             var errorFn = function (err) {
                 canceler.reject();
                 canceler = null;
                 data.activeNflowRequest = null;
                 data.skipDashboardNflowHealth = false;
             }
             var promise = $http.get(OpsManagerRestUrlService.DASHBOARD_PAGEABLE_NFLOWS_URL,{timeout: canceler.promise,params:params});
             promise.then(successFn, errorFn);
             return promise;
         }

         data.updateNflowHealthQueryParams = function(tab,filter,start,limit, sort){
             var params = {start: start, limit: limit, sort: sort, filter:filter, fixedFilter:tab};
             angular.extend(data.nflowHealthQueryParams,params);
         }

         data.fetchDashboard = function() {
             if(data.activeDashboardRequest != null && angular.isDefined(data.activeDashboardRequest)){
                 data.activeDashboardRequest.reject();
             }
             var canceler = $q.defer();
             data.activeDashboardRequest = canceler;

             var successFn = function (response) {

                 data.dashboard = response.data;
                 //if the pagable nflows query came after this one it will flip the skip flag.
                 // that should supercede this request
                 if(!data.skipDashboardNflowHealth) {
                     data.nflowsSearchResult = response.data.nflows;
                     if (data.dashboard && data.dashboard.nflows && data.dashboard.nflows.data) {
                         var processedNflows = setupNflowHealth(data.dashboard.nflows.data);
                         data.dashboard.nflows.data = processedNflows;
                         data.totalNflows = data.dashboard.nflows.recordsFiltered;
                     }
                 }
                 else {
                //     console.log('Skip processing dashboard results for the nflow since it was superceded');
                 }
                 if(angular.isUndefined(data.dashboard.healthCounts['UNHEALTHY'])) {
                     data.dashboard.healthCounts['UNHEALTHY'] = 0;
                 }
                 if(angular.isUndefined(data.dashboard.healthCounts['HEALTHY'])) {
                     data.dashboard.healthCounts['HEALTHY'] = 0;
                 }

                 data.nflowUnhealthyCount = data.dashboard.healthCounts['UNHEALTHY'] || 0;
                 data.nflowHealthyCount = data.dashboard.healthCounts['HEALTHY'] || 0;
                 data.activeDashboardRequest = null;
                 BroadcastService.notify(data.DASHBOARD_UPDATED,data.dashboard);

             }
             var errorFn = function (err) {
                 canceler.reject();
                 canceler = null;
                 data.activeDashboardRequest = null;
                 data.skipDashboardNflowHealth = false;
                 console.error("Dashboard error!!!")
             }
             var params = data.nflowHealthQueryParams;
             var promise = $http.get(OpsManagerRestUrlService.DASHBOARD_URL,{timeout: canceler.promise,params:params});
             promise.then(successFn, errorFn);
             return promise;
         };

         return data;
     }]);
});
