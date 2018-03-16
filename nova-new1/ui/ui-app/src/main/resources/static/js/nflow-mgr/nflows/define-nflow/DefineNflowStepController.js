define(["angular", "nflow-mgr/nflows/define-nflow/module-name"], function (angular, moduleName) {
    /**
     * An individual step in the Define Nflow wizard.
     */
    var novaDefineNflowStep = function (StepperService) {
        return {
            restrict: "E",
            scope: {
                step: "=",
                title: "@"
            },
            require: ['^onescorpinStepper'],
            templateUrl: "js/nflow-mgr/nflows/define-nflow/define-nflow-step.html",
            transclude: true,
            link: function link($scope, element, attrs, controller, $transclude) {
                $scope.$transclude = $transclude;
                var stepperController = controller[0];
                if($scope.step != undefined) {
                    stepperController.assignStepName($scope.step, $scope.title);
                }
                else {
                    console.error("UNDEFINED STEP!!!",$scope);
                }

            }
        };
    };

    /**
     * Transcludes the HTML contents of a <nova-define-nflow-step/> into the template of novaDefineNflowStep.
     */
    var novaDefineNflowStepTransclude = function () {
        return {
            restrict: "E",
            link: function ($scope, $element) {
                $scope.$transclude(function (clone) {
                    $element.empty();
                    $element.append(clone);
                });
            }
        };
    };

    angular.module(moduleName).directive("novaDefineNflowStep",['StepperService', novaDefineNflowStep]);
    angular.module(moduleName).directive("novaDefineNflowStepTransclude", novaDefineNflowStepTransclude);
});
