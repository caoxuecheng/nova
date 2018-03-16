define(["angular", "nflow-mgr/nflows/edit-nflow/module-name"], function (angular, moduleName) {

    var onescorpinNflowInfo = function () {
        return {
            restrict: "EA",
            bindToController: {
                selectedTabIndex: "="
            },
            controllerAs: "vm",
            scope: {},
            templateUrl: "js/nflow-mgr/nflows/edit-nflow/details/nflow-info.html",
            controller: "NflowInfoController"
        };
    };

    var NflowInfoController = function ($injector,$ocLazyLoad,NflowService, UiComponentsService) {
        var self = this;
        /**
         * Flag if we have fully initialized or not
         * @type {boolean}
         */
        this.initialized=false;
        /**
         * The nflow Model
         * @type {*}
         */
        this.model = NflowService.editNflowModel;

        /**
         * flag to render the custom presteps
         * @type {boolean}
         */
        this.renderPreStepTemplates = false;

        // Determine table option
        if (this.model.registeredTemplate.templateTableOption === null) {
            if (this.model.registeredTemplate.defineTable) {
                this.model.registeredTemplate.templateTableOption = "DEFINE_TABLE";
            } else if (this.model.registeredTemplate.dataTransformation) {
                this.model.registeredTemplate.templateTableOption = "DATA_TRANSFORMATION";
            } else {
                this.model.registeredTemplate.templateTableOption = "NO_TABLE";
            }
        }

        if (this.model.registeredTemplate.templateTableOption !== "NO_TABLE") {
            UiComponentsService.getTemplateTableOption(this.model.registeredTemplate.templateTableOption)
                .then(function (tableOption) {
                    if(tableOption.totalPreSteps >0){
                        self.renderPreStepTemplates = true;
                    }

                   if(angular.isDefined(tableOption.initializeScript) && angular.isDefined(tableOption.initializeServiceName) && tableOption.initializeScript != null &&  tableOption.initializeServiceName != null) {
                       $ocLazyLoad.load([tableOption.initializeScript]).then(function(file){
                           var serviceName = tableOption.initializeServiceName;
                           if(angular.isDefined(serviceName)) {
                               var svc = $injector.get(serviceName);
                               if (angular.isDefined(svc) && angular.isFunction(svc.initializeEditNflow)) {
                                   var nflowModel = NflowService.editNflowModel;
                                   svc.initializeEditNflow(tableOption,nflowModel);
                               }
                           }
                           self.initialized = true
                       });

                   }
                   else {
                       self.initialized = true
                   }
                });
            }
            else {
            self.initialized=true;
        }
    };

    angular.module(moduleName).controller("NflowInfoController", ["$injector","$ocLazyLoad","NflowService","UiComponentsService", NflowInfoController]);
    angular.module(moduleName).directive("onescorpinNflowInfo", [onescorpinNflowInfo]);
});
