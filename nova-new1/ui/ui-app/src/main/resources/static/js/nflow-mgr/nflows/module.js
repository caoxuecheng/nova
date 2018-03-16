define(['angular','nflow-mgr/nflows/module-name', 'nova-utils/LazyLoadUtil','constants/AccessConstants','@uirouter/angularjs','nova-nflowmgr'], function (angular,moduleName,lazyLoadUtil,AccessConstants) {
    //LAZY LOADED into the application


    var module = angular.module(moduleName, []);


    module.config(['$stateProvider','$compileProvider',function ($stateProvider,$compileProvider) {
        //preassign modules until directives are rewritten to use the $onInit method.
        //https://docs.angularjs.org/guide/migration#migrating-from-1-5-to-1-6
        $compileProvider.preAssignBindingsEnabled(true);
        $stateProvider.state(AccessConstants.UI_STATES.NFLOWS.state, {
            url: '/nflows',
            params: {
                tab: null
            },
            views: {
                'content': {
                    templateUrl: 'js/nflow-mgr/nflows/nflows-table.html',
                    controller:'NflowsTableController',
                    controllerAs:'vm'
                }
            },
            resolve: {
                loadMyCtrl: lazyLoadController('nflow-mgr/nflows/NflowsTableController')
            },
            data: {
                breadcrumbRoot: true,
                displayName: 'Nflows',
                module:moduleName,
                permissions:AccessConstants.UI_STATES.NFLOWS.permissions
            }
        });

        function lazyLoadController(path){
            return lazyLoadUtil.lazyLoadController(path,['nflow-mgr/nflows/module-require']);
        }




    }]);






});
