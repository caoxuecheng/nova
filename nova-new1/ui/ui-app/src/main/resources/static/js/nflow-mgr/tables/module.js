define(['angular', 'nflow-mgr/tables/module-name','nova-utils/LazyLoadUtil','constants/AccessConstants','nova-common', 'nova-services','nova-nflowmgr','jquery'], function (angular,moduleName,lazyLoadUtil,AccessConstants) {
    var module = angular.module(moduleName, []);

    /**
     * LAZY loaded in from /app.js
     */
    module.config(['$stateProvider',function ($stateProvider) {
        $stateProvider.state(AccessConstants.UI_STATES.TABLES.state,{
            url:'/tables',
            params: {
            },
            views: {
                'content': {
                    templateUrl: 'js/nflow-mgr/tables/tables.html',
                    controller:"TablesController",
                    controllerAs:"vm"
                }
            },
            resolve: {
                loadMyCtrl: lazyLoadController(['nflow-mgr/tables/TablesController'])
            },
            data:{
                breadcrumbRoot:true,
                displayName:'Tables',
                module:moduleName,
                permissions:AccessConstants.UI_STATES.TABLES.permissions
            }
        });

        $stateProvider.state(AccessConstants.UI_STATES.TABLE.state,{
            url:'/tables/{schema}/{tableName}',
            params: {
                schema:null,
                tableName:null
            },
            views: {
                'content': {
                    templateUrl: 'js/nflow-mgr/tables/table.html',
                    controller:"TableController",
                    controllerAs:"vm"
                }
            },
            resolve: {
                loadMyCtrl: lazyLoadController(['nflow-mgr/tables/TableController'])
            },
            data:{
                breadcrumbRoot:false,
                displayName:'Table Details',
                module:moduleName,
                permissions:AccessConstants.UI_STATES.TABLE.permissions
            }
        })


        function lazyLoadController(path){
            return lazyLoadUtil.lazyLoadController(path,'nflow-mgr/tables/module-require');
        }

    }]);










    return module;
});



