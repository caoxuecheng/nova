define(["angular", "nflow-mgr/domain-types/module-name", "nova-utils/LazyLoadUtil", "constants/AccessConstants", "codemirror-require/module", "nova-nflowmgr", "nova-common", "nova-services"],
    function (angular, moduleName, lazyLoadUtil, AccessConstants) {
        //LAZY LOADED into the application
        var module = angular.module(moduleName, []);

        module.config(["$stateProvider", "$compileProvider", function ($stateProvider, $compileProvider) {
            //preassign modules until directives are rewritten to use the $onInit method.
            //https://docs.angularjs.org/guide/migration#migrating-from-1-5-to-1-6
            $compileProvider.preAssignBindingsEnabled(true);

            $stateProvider.state(AccessConstants.UI_STATES.DOMAIN_TYPES.state, {
                url: "/domain-types",
                params: {},
                views: {
                    content: {
                        templateUrl: "js/nflow-mgr/domain-types/domain-types.html",
                        controller: "DomainTypesController",
                        controllerAs: "vm"
                    }
                },
                resolve: {
                    loadMyCtrl: lazyLoadController(["nflow-mgr/domain-types/DomainTypesController"])
                },
                data: {
                    breadcrumbRoot: true,
                    displayName: "Domain Types",
                    module: moduleName,
                    permissions: AccessConstants.UI_STATES.DOMAIN_TYPES.permissions
                }
            }).state(AccessConstants.UI_STATES.DOMAIN_TYPE_DETAILS.state, {
                url: "/domain-type-details/{domainTypeId}",
                params: {
                    domainTypeId: null
                },
                views: {
                    content: {
                        templateUrl: "js/nflow-mgr/domain-types/domain-type-details.html",
                        controller: "DomainTypeDetailsController",
                        controllerAs: "vm"
                    }
                },
                resolve: {
                    loadMyCtrl: lazyLoadController(["nflow-mgr/domain-types/DomainTypeDetailsController"])
                },
                data: {
                    breadcrumbRoot: false,
                    displayName: "Domain Type Details",
                    module: moduleName,
                    permissions: AccessConstants.UI_STATES.DOMAIN_TYPE_DETAILS.permissions
                }
            });

        }]);

        module.run(['$ocLazyLoad', function ($ocLazyLoad) {
            $ocLazyLoad.load({
                name: 'nova',
                files: [
                    "js/nflow-mgr/domain-types/codemirror-regex.css",
                    "js/nflow-mgr/domain-types/domain-types.css"
                ]
            });
        }]);

        function lazyLoadController(path) {
            return lazyLoadUtil.lazyLoadController(path, "nflow-mgr/domain-types/module-require");
        }
    });
