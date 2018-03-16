define(['angular','ops-mgr/nflows/module-name', 'nova-utils/LazyLoadUtil','constants/AccessConstants','nova-common', 'nova-services','nova-opsmgr','ops-mgr/alerts/module','ops-mgr/overview/module','angular-nvd3'], function (angular,moduleName,lazyLoadUtil,AccessConstants) {
   var module = angular.module(moduleName, ['nvd3']);

    module.config(['$stateProvider','$compileProvider',function ($stateProvider,$compileProvider) {
        //preassign modules until directives are rewritten to use the $onInit method.
        //https://docs.angularjs.org/guide/migration#migrating-from-1-5-to-1-6
        $compileProvider.preAssignBindingsEnabled(true);

        $stateProvider.state(AccessConstants.UI_STATES.OPS_NFLOW_DETAILS.state,{
            url:'/ops-nflow-details/{nflowName}',
            params: {
               nflowName:null
            },
            views: {
                'content': {
                    templateUrl: 'js/ops-mgr/nflows/nflow-details.html',
                    controller:"OpsManagerNflowDetailsController",
                    controllerAs:"vm"
                }
            },
            resolve: {
                loadMyCtrl: lazyLoadController(['ops-mgr/nflows/NflowDetailsController'])
            },
            data:{
                breadcrumbRoot:false,
                displayName:'Nflow Details',
                module:moduleName,
                permissions:AccessConstants.UI_STATES.OPS_NFLOW_DETAILS.permissions
            }
        });


        function lazyLoadController(path){
            return lazyLoadUtil.lazyLoadController(path,['ops-mgr/jobs/module','ops-mgr/jobs/module-require','ops-mgr/nflows/module-require','ops-mgr/alerts/module-require','ops-mgr/overview/module-require']);
        }

    }]);
    return module;
});




