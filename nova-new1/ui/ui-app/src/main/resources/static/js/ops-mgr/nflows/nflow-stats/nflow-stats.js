define(['angular','ops-mgr/nflows/nflow-stats/module-name'], function (angular,moduleName) {

    var controller = function ($scope, $transition$) {
        var self = this;

        self.nflowName = $transition$.params().nflowName;

    };

    angular.module(moduleName).controller('NflowStatsController',["$scope", "$transition$", controller]);

});
