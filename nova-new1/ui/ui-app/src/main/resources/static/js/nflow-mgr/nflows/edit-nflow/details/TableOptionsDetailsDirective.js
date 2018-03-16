define(["angular", "nflow-mgr/nflows/edit-nflow/module-name"], function (angular, moduleName) {
    /**
     * Displays a table option nflow details template.
     */
    var novaTableOptionsDetails = function ($compile, $mdDialog, $templateRequest, $injector,$ocLazyLoad, StateService, UiComponentsService) {
        return {
            restrict: "E",
            scope: {
                type: "@",
                stepperTemplateType:'@?'
            },
            link: function ($scope, $element) {

                if(angular.isUndefined($scope.stepperTemplateType)){
                    $scope.stepperTemplateType = 'stepper';
                }

                /**
                 * The table option metadata
                 * @type {null}
                 */
                $scope.tableOption = null;

                // Loads the table option template
                UiComponentsService.getTemplateTableOption($scope.type)
                    .then(function (tableOption) {
                        $scope.tableOption = tableOption;

                        //Determine if we are loading pre-steps or nflow steps
                        var property = 'nflowDetailsTemplateUrl';
                        if($scope.stepperTemplateType == 'pre-step') {
                            property = 'preNflowDetailsTemplateUrl';
                        }
                        return (tableOption[property] !== null) ? $templateRequest(tableOption[property]) : null;
                    })
                    .then(function (html) {
                        if (html !== null) {
                            var template = angular.element(html);
                            $element.append(template);
                            $compile(template)($scope);
                        }

                    }, function () {
                        $mdDialog.show(
                            $mdDialog.alert()
                                .clickOutsideToClose(true)
                                .title("Create Failed")
                                .textContent("The table option template could not be loaded.")
                                .ariaLabel("Failed to create nflow")
                                .ok("Got it!")
                        );
                        StateService.NflowManager().Nflow().navigateToNflows();
                    });
            }
        };
    };

    angular.module(moduleName).directive("novaTableOptionsDetails", ["$compile", "$mdDialog", "$templateRequest", "$injector","$ocLazyLoad","StateService", "UiComponentsService", novaTableOptionsDetails]);
});
