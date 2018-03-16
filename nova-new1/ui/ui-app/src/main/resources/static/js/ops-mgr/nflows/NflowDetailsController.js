define(['angular','ops-mgr/nflows/module-name'], function (angular,moduleName) {

    var controller = function($scope, $timeout,$q, $interval,$transition$, $http, OpsManagerNflowService, OpsManagerRestUrlService,StateService, OpsManagerJobService, BroadcastService ){
        var self = this;
        self.loading = true;
        var deferred = $q.defer();
        self.nflowName = null;
        self.nflowData = {}
        self.nflow = OpsManagerNflowService.emptyNflow();
        self.refreshIntervalTime = 5000;

        //Track active requests and be able to cancel them if needed
        this.activeRequests = []

        BroadcastService.subscribe($scope, 'ABANDONED_ALL_JOBS', abandonedAllJobs);



        var nflowName =$transition$.params().nflowName;
        if(nflowName != undefined && isGuid(nflowName)){
            //fetch the nflow name from the server using the guid
            $http.get(OpsManagerRestUrlService.NFLOW_NAME_FOR_ID(nflowName)).then( function(response){
                deferred.resolve(response.data);
            }, function(err) {
                deferred.reject(err);
            });
        }
        else {
            deferred.resolve(nflowName);
        }

        $q.when(deferred.promise).then(function(nflowNameResponse){
            self.nflowName = nflowNameResponse;
            self.loading = false;
            getNflowHealth();
            // getNflowNames();
            setRefreshInterval();
        });


        function isGuid(str) {
            if (str[0] === "{")
            {
                str = str.substring(1, str.length - 1);
            }
            //var regexGuid = /^(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}$/gi;
            var regexGuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
            return regexGuid.test(str);
        }


        function getNflowHealth(){
            var canceler = $q.defer();
            self.activeRequests.push(canceler);
            var successFn = function (response) {
                if (response.data) {
                    //transform the data for UI
                    self.nflowData = response.data;
                    if(self.nflowData.nflowSummary){
                        angular.extend(self.nflow,self.nflowData.nflowSummary[0]);
                        self.nflow.isEmpty = false;
                        if(self.nflow.nflowHealth && self.nflow.nflowHealth.nflowId ){
                            self.nflow.nflowId = self.nflow.nflowHealth.nflowId
                        }
                        OpsManagerNflowService.decorateNflowSummary(self.nflow);

                    }
                    if (self.loading) {
                        self.loading = false;
                    }
                    finishedRequest(canceler);
                }
            }
            var errorFn = function (err) {
            }
            var finallyFn = function () {

            }


            $http.get(OpsManagerNflowService.SPECIFIC_NFLOW_HEALTH_URL(self.nflowName),{timeout: canceler.promise}).then( successFn, errorFn);
        }

        function abortActiveRequests(){
            angular.forEach(self.activeRequests,function(canceler,i){
                canceler.resolve();
            });
            self.activeRequests = [];
        }

        function finishedRequest(canceler) {
            var index = _.indexOf(self.activeRequests,canceler);
            if(index >=0){
                self.activeRequests.splice(index,1);
            }
            canceler.resolve();
            canceler = null;
        }


        function getNflowNames(){

            var successFn = function (response) {
                if (response.data) {
                   self.nflowNames = response.data;
                }
            }
            var errorFn = function (err) {
            }
            var finallyFn = function () {

            }
            $http.get(OpsManagerNflowService.NFLOW_NAMES_URL).then( successFn, errorFn);
        }




        function clearRefreshInterval() {
            if (self.refreshInterval != null) {
                $interval.cancel(self.refreshInterval);
                self.refreshInterval = null;
            }
        }

        function setRefreshInterval() {
            clearRefreshInterval();
            if (self.refreshIntervalTime) {
                self.refreshInterval = $interval(getNflowHealth, self.refreshIntervalTime);

            }
            OpsManagerNflowService.fetchNflowHealth();
        }

        this.gotoNflowDetails = function(ev){
            if(self.nflow.nflowId != undefined) {
                StateService.NflowManager().Nflow().navigateToNflowDetails(self.nflow.nflowId);
            }
        }

        this.onJobAction = function(eventName,job) {

            var forceUpdate = false;
            //update status info if nflow job matches
            if(self.nflowData && self.nflowData.nflows && self.nflowData.nflows.length >0 && self.nflowData.nflows[0].lastOpNflow){
                var thisExecutionId = self.nflowData.nflows[0].lastOpNflow.nflowExecutionId;
                var thisInstanceId = self.nflowData.nflows[0].lastOpNflow.nflowInstanceId;
                if(thisExecutionId <= job.executionId && self.nflow){
                    abortActiveRequests();
                    clearRefreshInterval();
                    self.nflow.displayStatus = job.displayStatus =='STARTED' || job.displayStatus == 'STARTING' ? 'RUNNING' : job.displayStatus;
                    self.nflow.timeSinceEndTime = job.timeSinceEndTime;
                    if(self.nflow.displayStatus == 'RUNNING'){
                        self.nflow.timeSinceEndTime = job.runTime;
                    }
                    if(eventName == 'restartJob'){
                        self.nflow.timeSinceEndTime =0;
                    }
                    self.nflowData.nflows[0].lastOpNflow.nflowExecutionId = job.executionId;
                    self.nflowData.nflows[0].lastOpNflow.nflowInstanceId = job.instanceId;
                    if(eventName == 'updateEnd'){
                        setRefreshInterval();
                    }

                }
            }
        }

        this.changedNflow = function(nflowName){
            StateService.OpsManager().Nflow().navigateToNflowDetails(nflowName);
        }

        $scope.$on('$destroy', function(){
            clearRefreshInterval();
            abortActiveRequests();
            OpsManagerNflowService.stopFetchNflowHealthTimeout();
        });

        function abandonedAllJobs() {
            getNflowHealth();
        }



    };

    angular.module(moduleName).controller('OpsManagerNflowDetailsController',['$scope', '$timeout','$q', '$interval','$transition$','$http','OpsManagerNflowService','OpsManagerRestUrlService','StateService', 'OpsManagerJobService', 'BroadcastService', controller]);



});


