define(["angular", "/example-plugin-1.0/module-name"], function (angular, moduleName) {

    var directive = function () {
        return {
            bindToController: {
                stepIndex: "@"
            },
            controller: "ExampleNflowStepperPreStepController",
            controllerAs: "vm",
            require: ["exampleNflowStepperPreStep", "^onescorpinStepper"],
            templateUrl: "/example-plugin-1.0/example-nflow-stepper-pre-step/example-nflow-stepper-pre-step.html",
            link: function ($scope, element, attrs, controllers) {
                var thisController = controllers[0];
                var stepperController = controllers[1];
                thisController.stepperController = stepperController;
                thisController.totalSteps = stepperController.totalSteps;
            }
        }
    };

    function controller(NflowService) {
        var self= this;
        this.model = NflowService.createNflowModel;
        this.stepNumber = parseInt(this.stepIndex) + 1;
        this.form = {};

        /**
         * Enforce min age limit
         */
        this.ageChanged = function(){
            var age = self.model.tableOption.preStepAge;
            if(parseInt(age) >=21) {
                self.form['preStepAge'].$setValidity('ageError', true);
            }
            else {
                self.form['preStepAge'].$setValidity('ageError', false);
            }

        }

        this.magicWordChanged = function(){
            var magicWord = self.model.tableOption.preStepMagicWord;
            if(angular.isDefined(magicWord) && magicWord != null) {
                if (magicWord.toLowerCase() == 'nova') {
                    self.form['preStepMagicWord'].$setValidity('magicWordError', true);
                }
                else {
                    self.form['preStepMagicWord'].$setValidity('magicWordError', false);
                }
            }
        }
    }

    angular.module(moduleName)
        .controller("ExampleNflowStepperPreStepController", ["NflowService", controller])
        .directive("exampleNflowStepperPreStep", directive);
});
