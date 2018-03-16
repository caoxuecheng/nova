define(['angular','nflow-mgr/nflows/module-name'], function (angular,moduleName) {
    var controller = function($scope, $http, AccessControlService, RestUrlService, PaginationDataService, TableOptionsService, AddButtonService, NflowService, StateService,
                              EntityAccessControlService) {

        var self = this;

        /**
         * Indicates if nflows are allowed to be exported.
         * @type {boolean}
         */
        self.allowExport = false;

        self.nflowData = [];
        this.loading = false;

        /**
         * Flag to indicate the page has been loaded the first time
         * @type {boolean}
         */
        var loaded = false;

        this.cardTitle = 'Nflows';

        // Register Add button
        AccessControlService.getUserAllowedActions()
                .then(function(actionSet) {
                    if (AccessControlService.hasAction(AccessControlService.NFLOWS_EDIT, actionSet.actions)) {
                        AddButtonService.registerAddButton("nflows", function() {
                            NflowService.resetNflow();
                            StateService.NflowManager().Nflow().navigateToDefineNflow()
                        });
                    }
                });

        //Pagination DAta
        this.pageName = "nflows";
        this.paginationData = PaginationDataService.paginationData(this.pageName);
        this.paginationId = 'nflows';
        PaginationDataService.setRowsPerPageOptions(this.pageName, ['5', '10', '20', '50']);
        this.currentPage = PaginationDataService.currentPage(self.pageName) || 1;
        this.viewType = PaginationDataService.viewType(this.pageName);
        this.sortOptions = loadSortOptions();

        this.filter = PaginationDataService.filter(self.pageName);

        $scope.$watch(function() {
            return self.viewType;
        }, function(newVal) {
            self.onViewTypeChange(newVal);
        })

        $scope.$watch(function () {
            return self.filter;
        }, function (newVal, oldValue) {
            if (newVal != oldValue || (!loaded && !self.loading)) {
                PaginationDataService.filter(self.pageName, newVal)
                getNflows();
            }
        })

        this.onViewTypeChange = function(viewType) {
            PaginationDataService.viewType(this.pageName, self.viewType);
        }

        this.onOrderChange = function(order) {
            TableOptionsService.setSortOption(self.pageName, order);
            getNflows();
        };

        function onPaginate(page,limit){
            PaginationDataService.currentPage(self.pageName, null, page);
            self.currentPage = page;
            //only trigger the reload if the initial page has been loaded.
            //md-data-table will call this function when the page initially loads and we dont want to have it run the query again.\
            if (loaded) {
                getNflows();
            }
        }

        this.onPaginationChange = function(page, limit) {
            if(self.viewType == 'list') {
                onPaginate(page,limit);
            }

        };

        this.onDataTablePaginationChange = function(page, limit) {
            if(self.viewType == 'table') {
                onPaginate(page,limit);
            }

        };



        /**
         * Called when a user Clicks on a table Option
         * @param option
         */
        this.selectedTableOption = function(option) {
            var sortString = TableOptionsService.toSortString(option);
            var savedSort = PaginationDataService.sort(self.pageName, sortString);
            var updatedOption = TableOptionsService.toggleSort(self.pageName, option);
            TableOptionsService.setSortOption(self.pageName, sortString);
            getNflows();
        }

        /**
         * Build the possible Sorting Options
         * @returns {*[]}
         */
        function loadSortOptions() {
            var options = {'Nflow': 'nflowName', 'State': 'state', 'Category': 'category.name', 'Last Modified': 'updateDate'};
            var sortOptions = TableOptionsService.newSortOptions(self.pageName, options, 'updateDate', 'desc');
            TableOptionsService.initializeSortOption(self.pageName);
            return sortOptions;
        }

        this.nflowDetails = function($event, nflow) {
            if(nflow !== undefined) {
                StateService.NflowManager().Nflow().navigateToNflowDetails(nflow.id);
            }
        }

        function getNflows() {
            self.loading = true;

            var successFn = function(response) {
                self.loading = false;
                if (response.data) {
                	self.nflowData = populateNflows(response.data.data);
                	PaginationDataService.setTotal(self.pageName,response.data.recordsFiltered);
                    loaded = true;
                } else {
                	self.nflowData = [];
                }
            }
            
            var errorFn = function(err) {
                self.loading = false;
                loaded = true;
            }

        	var limit = PaginationDataService.rowsPerPage(self.pageName);
        	var start = limit == 'All' ? 0 : (limit * self.currentPage) - limit;
        	var sort = self.paginationData.sort;
        	var filter = self.paginationData.filter;
            var params = {start: start, limit: limit, sort: sort, filter: filter};
            
            var promise = $http.get(RestUrlService.GET_NFLOWS_URL, {params: params});
            promise.then(successFn, errorFn);
            return promise;

        }

        function populateNflows(nflows) {
            var entityAccessControlled = AccessControlService.isEntityAccessControlled();
        	var simpleNflowData = [];
            
            angular.forEach(nflows, function(nflow) {
                if (nflow.state == 'ENABLED') {
                    nflow.stateIcon = 'check_circle'
                } else {
                    nflow.stateIcon = 'block'
                }
                simpleNflowData.push({
                    templateId: nflow.templateId,
                    templateName: nflow.templateName,
                    exportUrl: RestUrlService.ADMIN_EXPORT_NFLOW_URL + "/" + nflow.id,
                    id: nflow.id,
                    active: nflow.active,
                    state: nflow.state,
                    stateIcon: nflow.stateIcon,
                    nflowName: nflow.nflowName,
                    category: {name: nflow.categoryName, icon: nflow.categoryIcon, iconColor: nflow.categoryIconColor},
                    updateDate: nflow.updateDate,
                    allowEditDetails: !entityAccessControlled || NflowService.hasEntityAccess(EntityAccessControlService.ENTITY_ACCESS.NFLOW.EDIT_NFLOW_DETAILS, nflow),
                    allowExport: !entityAccessControlled || NflowService.hasEntityAccess(EntityAccessControlService.ENTITY_ACCESS.NFLOW.EXPORT, nflow)
                })
            });
            
            return simpleNflowData;
        }

        // Fetch the allowed actions
        AccessControlService.getUserAllowedActions()
                .then(function(actionSet) {
                    self.allowExport = AccessControlService.hasAction(AccessControlService.NFLOWS_EXPORT, actionSet.actions);
                });
    };


    angular.module(moduleName).controller('NflowsTableController',["$scope","$http","AccessControlService","RestUrlService","PaginationDataService","TableOptionsService","AddButtonService",
                                                                  "NflowService","StateService", "EntityAccessControlService", controller]);

});
