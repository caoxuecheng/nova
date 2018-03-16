define(['angular','nflow-mgr/nflows/define-nflow/module-name','nova-utils/LazyLoadUtil','constants/AccessConstants','nflow-mgr/nflows/module','@uirouter/angularjs','nova-nflowmgr','nflow-mgr/visual-query/module'], function (angular,moduleName,lazyLoadUtil,AccessConstants) {
    //LAZY LOADED into the application
    var module = angular.module(moduleName, []);
    module.config(["$compileProvider",function($compileProvider) {
        $compileProvider.preAssignBindingsEnabled(true);
    }]);

    module.config(['$stateProvider','$compileProvider',function ($stateProvider,$compileProvider) {
        //preassign modules until directives are rewritten to use the $onInit method.
        //https://docs.angularjs.org/guide/migration#migrating-from-1-5-to-1-6
        $compileProvider.preAssignBindingsEnabled(true);

        $stateProvider.state(AccessConstants.UI_STATES.DEFINE_NFLOW.state, {
            url: '/define-nflow?templateId&templateName&nflowDescriptor',
            params: {
                templateId: null,
                templateName:null,
                nflowDescriptor:null,
                bcExclude_cloning:null,
                bcExclude_cloneNflowName:null
            },
            views: {
                'content': {
                    templateUrl: 'js/nflow-mgr/nflows/define-nflow/define-nflow.html',
                    controller: 'DefineNflowController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                loadMyCtrl: lazyLoadController(['nflow-mgr/nflows/define-nflow/DefineNflowController'])
            },
            data: {
                breadcrumbRoot: false,
                displayName: 'Define Nflow',
                module:moduleName,
                permissions:AccessConstants.UI_STATES.DEFINE_NFLOW.permissions
            }
        });

        $stateProvider.state(AccessConstants.UI_STATES.DEFINE_NFLOW_COMPLETE.state, {
            url: '/define-nflow-complete',
            params: {
                templateId: null
            },
            views: {
                'content': {
                    templateUrl: 'js/nflow-mgr/nflows/define-nflow/nflow-details/define-nflow-complete.html',
                    controller: 'DefineNflowCompleteController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                loadMyCtrl: lazyLoadController(['nflow-mgr/nflows/define-nflow/nflow-details/DefineNflowCompleteController'])
            },
            data: {
                breadcrumbRoot: false,
                displayName: 'Define Nflow',
                module:moduleName,
                permissions:AccessConstants.UI_STATES.DEFINE_NFLOW_COMPLETE.permissions
            }
        });



        $stateProvider.state(AccessConstants.UI_STATES.IMPORT_NFLOW.state, {
            url: '/import-nflow',
            params: {},
            views: {
                'content': {
                    templateUrl: 'js/nflow-mgr/nflows/define-nflow/import-nflow.html',
                    controller: 'ImportNflowController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                loadMyCtrl: lazyLoadController(['nflow-mgr/nflows/define-nflow/ImportNflowController'])
            },
            data: {
                breadcrumbRoot: false,
                displayName: 'Import Nflow',
                module:moduleName,
                permissions:AccessConstants.UI_STATES.IMPORT_NFLOW.permissions
            }
        });

        function lazyLoadController(path){
            return lazyLoadUtil.lazyLoadController(path,['nflow-mgr/nflows/module-require','nflow-mgr/nflows/define-nflow/module-require','nflow-mgr/visual-query/module-require']);
        }
    }]);




return module;



});
