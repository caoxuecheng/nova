define(['angular','ops-mgr/overview/module-name'], function (angular,moduleName) {

    var directive = function () {
        return {
            restrict: "E",
            scope: true,
            bindToController: {
                panelTitle: "@",
                nflowName:'@',
                refreshIntervalTime:'=?'
            },
            controllerAs: 'vm',
            templateUrl: 'js/ops-mgr/overview/alerts/alerts-template.html',
            controller: "AlertsOverviewController",
            link: function ($scope, element, attrs) {
                $scope.$on('$destroy', function () {

                });
            } //DOM manipulation\}
        }

    };

    var controller = function ($scope, $element, $interval, AlertsService, StateService,OpsManagerDashboardService,BroadcastService) {
        var self = this;
        this.alertsService = AlertsService;
        this.alerts = [];

        /**
         * Handle on the nflow alerts refresh interval
         * @type {null}
         */
        this.nflowRefresh = null;

        this.refreshIntervalTime = angular.isUndefined(self.refreshIntervalTime) ? 5000 : self.refreshIntervalTime;


        function watchDashboard() {
            BroadcastService.subscribe($scope,OpsManagerDashboardService.DASHBOARD_UPDATED,function(dashboard){
                var alerts = OpsManagerDashboardService.dashboard.alerts;
                AlertsService.transformAlerts(alerts);
                self.alerts = alerts;
            });
        }

        if(this.nflowName == undefined || this.nflowName == ''){
            watchDashboard();
        }
        else {
            self.alerts = [];
            stopNflowRefresh();
            fetchNflowAlerts();
           self.nflowRefresh = $interval(fetchNflowAlerts,5000);
        }

        function fetchNflowAlerts(){
            AlertsService.fetchNflowAlerts(self.nflowName).then(function(alerts) {
                self.alerts =alerts;
            });
        }

        function stopNflowRefresh(){
            if(self.nflowRefresh != null){
                $interval.cancel(self.nflowRefresh);
                self.nflowRefresh = null;
            }
        }


        this.navigateToAlerts = function(alertsSummary) {

            //generate Query
            var query = "UNHANDLED,"+ alertsSummary.type;
            if(alertsSummary.groupDisplayName != null && alertsSummary.groupDisplayName != null) {
                query += ","+alertsSummary.groupDisplayName;
            }
            else if(alertsSummary.subtype != null && alertsSummary.subtype != '') {
                query += ","+alertsSummary.subtype;
            }
            StateService.OpsManager().Alert().navigateToAlerts(query);

        }

        $scope.$on('$destroy', function () {
            stopNflowRefresh();
        });

    };

    angular.module(moduleName).controller('AlertsOverviewController', ["$scope","$element","$interval","AlertsService","StateService","OpsManagerDashboardService","BroadcastService",controller]);


    angular.module(moduleName)
        .directive('tbaAlerts', directive);

});

