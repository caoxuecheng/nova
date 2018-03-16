define(['angular','nflow-mgr/nflows/edit-nflow/module-name','nova-utils/LazyLoadUtil','constants/AccessConstants','vis','nova-nflowmgr','nflow-mgr/nflows/module','nflow-mgr/sla/module','nflow-mgr/visual-query/module',"nflow-mgr/nflows/define-nflow/module",'angular-nvd3'], function (angular,moduleName,lazyLoadUtil,AccessConstants, vis) {
    //LAZY LOADED into the application
    var module = angular.module(moduleName, []);
      // load vis in the global state
        if(window.vis === undefined) {
            window.vis = vis;
        }
    module.config(['$stateProvider','$compileProvider',function ($stateProvider,$compileProvider) {
        //preassign modules until directives are rewritten to use the $onInit method.
        //https://docs.angularjs.org/guide/migration#migrating-from-1-5-to-1-6
        $compileProvider.preAssignBindingsEnabled(true);

        $stateProvider.state(AccessConstants.UI_STATES.NFLOW_DETAILS.state,{
            url:'/nflow-details/{nflowId}',
            params: {
                nflowId: null,
                tabIndex: 0
            },
            views: {
                'content': {
                    templateUrl: 'js/nflow-mgr/nflows/edit-nflow/nflow-details.html',
                    controller: 'NflowDetailsController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                loadMyCtrl: lazyLoadController(['nflow-mgr/nflows/edit-nflow/NflowDetailsController'])
            },
            data:{
                breadcrumbRoot:false,
                displayName:'Edit Nflow',
                module:moduleName,
                permissions:AccessConstants.UI_STATES.NFLOW_DETAILS.permissions
            }
        }).state(AccessConstants.UI_STATES.EDIT_NFLOW.state,{
                url:'/edit-nflow/{nflowId}',
                params: {
                    nflowId: null
                },
                views: {
                    'content': {
                        templateUrl: 'js/nflow-mgr/nflows/edit-nflow/edit-nflow.html',
                        controller: 'EditNflowController',
                        controllerAs: 'vm'
                    }
                },
               resolve: {
                   loadMyCtrl: lazyLoadController(['nflow-mgr/nflows/edit-nflow/EditNflowController', "nflow-mgr/nflows/define-nflow/module-require"])
               },
                data:{
                    breadcrumbRoot: false,
                    displayName: 'Edit Nflow',
                    module:moduleName,
                    permissions:AccessConstants.UI_STATES.EDIT_NFLOW.permissions
                }
            })

        function lazyLoadController(path){
            return lazyLoadUtil.lazyLoadController(path,['nflow-mgr/nflows/module-require','nflow-mgr/nflows/edit-nflow/module-require','nflow-mgr/sla/module-require','nflow-mgr/visual-query/module-require','angular-visjs']);
        }
    }]);

    module.run(['$ocLazyLoad',function ($ocLazyLoad) {
        $ocLazyLoad.load({
            name: 'nova',
            files: [
                'js/vendor/font-awesome/css/font-awesome.min.css',
                'js/nflow-mgr/nflows/edit-nflow/nflow-details.css'
            ],
            serie: true
        });
    }]);

return module;



});
