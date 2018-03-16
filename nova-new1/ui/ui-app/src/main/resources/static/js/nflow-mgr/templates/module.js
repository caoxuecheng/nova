define(['angular', 'nflow-mgr/templates/module-name','nova-utils/LazyLoadUtil','constants/AccessConstants','@uirouter/angularjs','nova-common', 'nova-services','nova-nflowmgr','ment-io','jquery','angular-drag-and-drop-lists'], function (angular,moduleName,lazyLoadUtil,AccessConstants) {
    var module = angular.module(moduleName, ['mentio','dndLists']);

    module.config(["$compileProvider",function($compileProvider) {
        $compileProvider.preAssignBindingsEnabled(true);
    }]);

    /**
     * LAZY loaded in from /app.js
     */
    module.config(['$stateProvider',function ($stateProvider) {
        $stateProvider.state('registered-templates',{
            url:'/registered-templates',
            params: {
            },
            views: {
                'content': {
                    templateUrl: 'js/nflow-mgr/templates/registered-templates.html',
                    controller:'RegisteredTemplatesController',
                    controllerAs:'vm'
                }
            },
            resolve: {
                loadMyCtrl: lazyLoadController(['nflow-mgr/templates/RegisteredTemplatesController'])
            },
            data:{
                breadcrumbRoot:true,
                displayName:'Templates',
                module:moduleName,
                permissions:AccessConstants.TEMPLATES_ACCESS

            }
        })


        $stateProvider.state('register-new-template',{
            url:'/register-new-template',
            views: {
                'content': {
                    templateUrl: 'js/nflow-mgr/templates/new-template/register-new-template.html',
                    controller:'RegisterNewTemplateController',
                    controllerAs:'vm'
                }
            },
            resolve: {
                loadMyCtrl: lazyLoadController(['nflow-mgr/templates/new-template/RegisterNewTemplateController'])
            },
            data:{
                breadcrumbRoot:false,
                displayName:'Register Template',
                module:moduleName,
                permissions:AccessConstants.TEMPLATES_EDIT
            }
        });

        $stateProvider.state('register-template',{
            url:'/register-template',
            params:{
                nifiTemplateId:null,
                registeredTemplateId:null
            },
            views: {
                'content': {
                    templateUrl: 'js/nflow-mgr/templates/template-stepper/register-template.html',
                    controller:'RegisterTemplateController',
                    controllerAs:'vm'
                }
            },
            resolve: {
                loadMyCtrl: lazyLoadController(['nflow-mgr/templates/template-stepper/RegisterTemplateController','@uirouter/angularjs'])
            },
            data:{
                breadcrumbRoot:false,
                displayName:'Register Template',
                module:moduleName,
                permissions:AccessConstants.TEMPLATES_EDIT
            }
        }).state('register-template-complete', {
            url: '/register-template-complete',
            params: {
                message: '',
                templateModel: null
            },
            views: {
                'content': {
                    templateUrl: 'js/nflow-mgr/templates/template-stepper/register-template/register-template-complete.html',
                    controller:'RegisterTemplateCompleteController',
                    controllerAs:'vm'
                }
            },
            data: {
                breadcrumbRoot: false,
                displayName: 'Register Template',
                module:moduleName,
                permissions:AccessConstants.TEMPLATES_EDIT
            }
        }).state('import-template',{
            url:'/import-template',
            params: {
            },
            views: {
                'content': {
                    templateUrl:  'js/nflow-mgr/templates/import-template/import-template.html',
                    controller:'ImportTemplateController',
                    controllerAs:'vm'
                }
            },
            resolve: {
                loadMyCtrl: lazyLoadController(['nflow-mgr/templates/import-template/ImportTemplateController'])
            },
            data:{
                breadcrumbRoot:false,
                displayName:'Template Manager',
                module:moduleName,
                permissions:AccessConstants.TEMPLATES_IMPORT
            }
        })




        function lazyLoadController(path){
            return lazyLoadUtil.lazyLoadController(path,'nflow-mgr/templates/module-require');
        }

    }]);


    module.run(['$ocLazyLoad',function($ocLazyLoad){
        $ocLazyLoad.load({name:moduleName,files:['js/vendor/ment.io/styles.css','vendor/ment.io/templates']})
    }])








    return module;
});



