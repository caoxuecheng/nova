define(['angular','ops-mgr/nflows/nflow-stats/module-name',  'nova-utils/LazyLoadUtil','constants/AccessConstants','nova-common', 'nova-services','nova-opsmgr','angular-nvd3'], function (angular,moduleName,lazyLoadUtil,AccessConstants) {
   var module = angular.module(moduleName, []);


    module.config(['$stateProvider','$compileProvider',function ($stateProvider,$compileProvider) {
        //preassign modules until directives are rewritten to use the $onInit method.
        //https://docs.angularjs.org/guide/migration#migrating-from-1-5-to-1-6
        $compileProvider.preAssignBindingsEnabled(true);

        $stateProvider.state(AccessConstants.UI_STATES.NFLOW_STATS.state,{
            url:'/nflow-stats/{nflowName}',
            params:{
               nflowName:null
            },
            views: {
                'content': {
                    templateUrl: 'js/ops-mgr/nflows/nflow-stats/nflow-stats.html',
                    controller:"NflowStatsController",
                    controllerAs:"vm"
                }
            },
            resolve: {
                loadMyCtrl: lazyLoadController(['ops-mgr/nflows/nflow-stats/nflow-stats'])
            },
            data:{
                breadcrumbRoot:false,
                displayName:'Nflow Stats',
                module:moduleName,
                permissions:AccessConstants.UI_STATES.NFLOW_STATS.permissions
            }
        });


        function lazyLoadController(path){
            return lazyLoadUtil.lazyLoadController(path,['ops-mgr/nflows/nflow-stats/module-require']);
        }

        function lazyLoad(){
            return lazyLoadUtil.lazyLoad(['ops-mgr/nflows/nflow-stats/module-require']);
        }

    }]);
    return module;






});




