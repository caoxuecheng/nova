define(['angular', 'nflow-mgr/module-name', 'codemirror-require/module', 'nova-common', 'nova-services', 'jquery','angular-drag-and-drop-lists'], function (angular, moduleName) {
    var module = angular.module(moduleName, ['ui.codemirror','dndLists']);

    module.run(['$ocLazyLoad', function ($ocLazyLoad) {
        $ocLazyLoad.load({
            name: 'nova', files: ['bower_components/angular-ui-grid/ui-grid.css', 'assets/ui-grid-material.css', 'js/nflow-mgr/shared/cron-expression-preview/cron-expression-preview.css'
            ], serie: true
        })
    }]);
    return module;

});
