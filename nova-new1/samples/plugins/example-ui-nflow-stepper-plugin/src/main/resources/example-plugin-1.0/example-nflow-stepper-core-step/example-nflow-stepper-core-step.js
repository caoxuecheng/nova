define(["angular", "/example-plugin-1.0/module-name"], function (angular, moduleName) {

    var directive = function () {
        return {
            bindToController: {
                stepIndex: "@"
            },
            controller: "ExampleNflowStepperCoreStepController",
            controllerAs: "vm",
            require: ["exampleNflowStepperCoreStep", "^onescorpinStepper"],
            templateUrl: "/example-plugin-1.0/example-nflow-stepper-core-step/example-nflow-stepper-core-step.html",
            link: function ($scope, element, attrs, controllers) {
                var thisController = controllers[0];
                var stepperController = controllers[1];
                thisController.stepperController = stepperController;
                thisController.totalSteps = stepperController.totalSteps;
            }
        }
    };

    function controller(NflowService) {
        this.model = NflowService.createNflowModel;
        this.stepNumber = parseInt(this.stepIndex) + 1;
    }

    angular.module(moduleName)
        .controller("ExampleNflowStepperCoreStepController", ["NflowService", controller])
        .directive("exampleNflowStepperCoreStep", directive);
});
