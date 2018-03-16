define(['angular','ops-mgr/overview/module-name'], function (angular,moduleName) {

    var directive = function () {
        return {
            restrict: "EA",
            bindToController: {
                cardTitle: "@"
            },
            controllerAs: 'vm',
            scope: true,
            templateUrl: 'js/ops-mgr/overview/nflow-health/nflow-health-table-card-template.html',
            controller: "NflowHealthTableCardController",
            link: function ($scope, element, attrs,ctrl,transclude) {

            }
        };
    };

    var controller = function ($scope,$rootScope,$http,$interval, OpsManagerNflowService, OpsManagerDashboardService,TableOptionsService,PaginationDataService, TabService,AlertsService, StateService,BroadcastService) {
        var self = this;
        this.pageName="nflow-health";

        this.fetchNflowHealthPromise = null;


        //Pagination and view Type (list or table)
        this.paginationData = PaginationDataService.paginationData(this.pageName);
        PaginationDataService.setRowsPerPageOptions(this.pageName,['5','10','20','50']);

        /**
         * the view either list, or table
         */
        this.viewType = PaginationDataService.viewType(this.pageName);


        //Setup the Tabs
        var tabNames =  ['All','Running','Healthy','Unhealthy','Streaming'];

        /**
         * Create the Tab objects
         */
        this.tabs = TabService.registerTabs(this.pageName,tabNames, this.paginationData.activeTab);

        /**
         * Setup the metadata about the tabs
         */
        this.tabMetadata = TabService.metadata(this.pageName);

        this.sortOptions = loadSortOptions();

        /**
         * The object[nflowName] = nflow
         * @type {{}}
         */
        this.dataMap = {};

        /**
         * object {data:{total:##,content:[]}}
         */
        this.data = TabService.tabPageData(this.pageName);

        /**
         * filter used for this card
         */
        this.filter = PaginationDataService.filter(self.pageName)

        /**
         * Flag to indicate the page successfully loaded for the first time and returned data in the card
         * @type {boolean}
         */
        var loaded = false;

        /**
         * Flag to indicate loading/fetching data
         * @type {boolean}
         */
        self.showProgress = false;

        /**
         * The pagination Id
         * @param tab optional tab to designate the pagination across tabs.
         */
        this.paginationId = function(tab) {
            return PaginationDataService.paginationId(self.pageName, tab.title);
        }


        /**
         * Refresh interval object for the nflow health data
         * @type {null}
         */
        var nflowHealthInterval = null;


        this.onTabSelected = function(tab) {
            TabService.selectedTab(self.pageName, tab);
            if(loaded || (!loaded && !OpsManagerDashboardService.isFetchingDashboard())) {
                return loadNflows(true, true);
            }
        };


        $scope.$watch(function(){
            return self.viewType;
        },function(newVal) {
            self.onViewTypeChange(newVal);
        })

        this.onViewTypeChange = function(viewType) {
            PaginationDataService.viewType(this.pageName, self.viewType);
        }

        this.onOrderChange = function (order) {
            PaginationDataService.sort(self.pageName, order);
            TableOptionsService.setSortOption(self.pageName,order);
            return loadNflows(true,true);
        };

        this.onPaginationChange = function (page, limit) {
            if( self.viewType == 'list') {
                if (loaded) {
                    var activeTab= TabService.getActiveTab(self.pageName);
                    activeTab.currentPage = page;
                    return loadNflows(true, true);
                }
            }
        };

        this.onTablePaginationChange = function(page, limit){
            if( self.viewType == 'table') {
                var activeTab= TabService.getActiveTab(self.pageName);
                if (loaded) {
                    activeTab.currentPage = page;
                    return loadNflows(true, true);
                }
            }
        }

        $scope.$watch(function() {
            return self.paginationData.rowsPerPage;
        }, function (newVal, oldVal) {
            if (newVal != oldVal) {
                if (loaded) {
                    return loadNflows(false,true);
                }
            }
        });

        this.nflowDetails = function(event, nflow){
            if(nflow.stream) {
                StateService.OpsManager().Nflow().navigateToNflowStats(nflow.nflow);
            }
            else {
                StateService.OpsManager().Nflow().navigateToNflowDetails(nflow.nflow);
            }
        }

        $scope.$watch(function() {
            return self.filter;
        }, function (newVal, oldVal) {
            if (newVal != oldVal) {
                return loadNflows(true, true);
            }
        });


        /**
         * Called when a user Clicks on a table Option
         * @param option
         */
        this.selectedTableOption = function(option) {
            var sortString = TableOptionsService.toSortString(option);
            PaginationDataService.sort(self.pageName,sortString);
            var updatedOption = TableOptionsService.toggleSort(self.pageName,option);
            TableOptionsService.setSortOption(self.pageName,sortString);
            return loadNflows(true,true);
        }

        /**
         * Build the possible Sorting Options
         * @returns {*[]}
         */
        function loadSortOptions() {
            var options = {'Nflow':'nflow','Health':'healthText','Status':'displayStatus','Since':'timeSinceEndTime','Last Run Time':'runTime'};

            var sortOptions = TableOptionsService.newSortOptions(self.pageName,options,'nflow','desc');
            var currentOption = TableOptionsService.getCurrentSort(self.pageName);
            if(currentOption) {
                TableOptionsService.saveSortOption(self.pageName,currentOption)
            }
            return sortOptions;
        }

        /**
         * Add additional data back to the data object.
         * @param nflows
         */
        function mergeUpdatedNflows(nflows) {
             var activeTab = TabService.getActiveTab(self.pageName);
            var tab = activeTab.title.toLowerCase();

            if (tab != 'All') {
                angular.forEach(nflows, function (nflow, nflowName) {

                    var tabState = nflow.state.toLowerCase()
                    if(tab == 'running'){
                        if(tabState == 'running'){
                            self.dataMap[nflow.nflow] = nflow;
                        }
                        else {
                            delete self.dataMap[nflow.nflow];
                        }

                    }
                    else if(tab == 'healthy') {
                        if (nflow.healthText.toLowerCase() == 'healthy') {
                            self.dataMap[nflow.nflow] = nflow;
                        } else {
                            delete self.dataMap[nflow.nflow];
                        }

                    }
                    else if(tab == 'unhealthy') {
                        if ((nflow.healthText.toLowerCase() == 'unhealthy' || nflow.healthText.toLowerCase() == 'unknown')) {
                            self.dataMap[nflow.nflow] = nflow;
                        }
                        else {
                            delete self.dataMap[nflow.nflow];
                        }
                    }
                    else if(tab == 'stream') {
                        if (nflow.stream) {
                            self.dataMap[nflow.nflow] = nflow;
                        }
                        else {
                            delete self.dataMap[nflow.nflow];
                        }
                    }
                });

            }
        }
        function getNflowHealthQueryParams(){
            var limit = self.paginationData.rowsPerPage;
            var activeTab = TabService.getActiveTab(self.pageName);
            var tab = activeTab.title;
            var sort = PaginationDataService.sort(self.pageName);
            var start = (limit * activeTab.currentPage) - limit;
            return {limit:limit,fixedFilter:tab,sort:sort,start:start,filter:self.filter};
        }

            /**
             * Fetch and load the nflows
             * @param force (true to alwasy refresh, false or undefined to only refresh if not refreshing
             * @return {*|null}
             */
        function loadNflows(force, showProgress){
          if((angular.isDefined(force) && force == true) || !OpsManagerDashboardService.isFetchingNflowHealth()) {
              OpsManagerDashboardService.setSkipDashboardNflowHealth(true);
              self.refreshing = true;
              if(showProgress){
                  self.showProgress = true;
              }
              var queryParams = getNflowHealthQueryParams();
              var limit = queryParams.limit;
              var tab =queryParams.fixedFilter;
              var sort = queryParams.sort;
              var start = queryParams.start;
              var filter = queryParams.filter;
              OpsManagerDashboardService.updateNflowHealthQueryParams(tab,filter,start , limit, sort);
              self.fetchNflowHealthPromise =  OpsManagerDashboardService.fetchNflows(tab,filter,start , limit, sort).then(function (response) {
                    populateNflowData(tab);
              },
              function(err){
                  loaded = true;
                  var activeTab = TabService.getActiveTab(self.pageName);
                  activeTab.clearContent();
                  self.showProgress = false;
              });
          }
            return self.fetchNflowHealthPromise;

        }


        function populateNflowData(tab){
            var activeTab = TabService.getActiveTab(self.pageName);
            activeTab.clearContent();
            self.dataArray = OpsManagerDashboardService.nflowsArray;
            _.each(self.dataArray,function(nflow,i) {
                activeTab.addContent(nflow);
            });
            TabService.setTotal(self.pageName, activeTab.title, OpsManagerDashboardService.totalNflows)
            loaded = true;
            self.showProgress = false;
        }


        function watchDashboard() {
            BroadcastService.subscribe($scope,OpsManagerDashboardService.DASHBOARD_UPDATED,function(event,dashboard){
                populateNflowData();
            });
            /**
             * If the Job Running KPI starts/finishes a job, update the Nflow Health Card and add/remove the running state nflows
             * so the cards are immediately in sync with each other
             */
            BroadcastService.subscribe($scope,OpsManagerDashboardService.NFLOW_SUMMARY_UPDATED,function(event,updatedNflows) {
                if (angular.isDefined(updatedNflows) && angular.isArray(updatedNflows) && updatedNflows.length > 0) {
                    mergeUpdatedNflows(updatedNflows);
                }
                else {
                    var activeTab = TabService.getActiveTab(self.pageName);
                    var tab = activeTab.title;
                    if(tab.toLowerCase() == 'running') {
                        loadNflows(false);
                    }
                }
            });

            /**
             * if a user clicks on the KPI for Healthy,Unhealty select the tab in the nflow health card
             */
            BroadcastService.subscribe($scope,OpsManagerDashboardService.TAB_SELECTED,function(e,selectedTab){
               var tabData = _.find(self.tabs,function(tab){ return tab.title == selectedTab});
               if(tabData != undefined) {
                   var idx = _.indexOf(self.tabs,tabData);
                   //Setting the selected index will trigger the onTabSelected()
                   self.tabMetadata.selectedIndex = idx;
               }
            });
        }



        $scope.$on('$destroy', function(){
            //cleanup

        });

        function init() {
            watchDashboard();
        }

        init();



    };


    angular.module(moduleName).controller('NflowHealthTableCardController', ["$scope","$rootScope","$http","$interval","OpsManagerNflowService","OpsManagerDashboardService","TableOptionsService","PaginationDataService","TabService","AlertsService","StateService","BroadcastService",controller]);

    angular.module(moduleName)
        .directive('tbaNflowHealthTableCard', directive);

});
