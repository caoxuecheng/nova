define(['angular','nflow-mgr/nflows/define-nflow/module-name'], function (angular,moduleName) {

    var directive = function () {
        return {
            restrict: "EA",
            bindToController: {
                stepIndex: '@'
            },
            scope: {},
            controllerAs: 'vm',
            require:['onescorpinDefineNflowAccessControl','^onescorpinStepper'],
            templateUrl: 'js/nflow-mgr/nflows/define-nflow/nflow-details/define-nflow-access-control.html',
            controller: "DefineNflowAccessControlController",
            link: function ($scope, element, attrs, controllers) {
                var thisController = controllers[0];
                var stepperController = controllers[1];
                thisController.stepperController = stepperController;
                thisController.totalSteps = stepperController.totalSteps;
            }
        };
    };

    function DefineNflowAccessControlController($scope,NflowService, NflowSecurityGroups) {


        this.stepNumber = parseInt(this.stepIndex)+1

        /**
         * ref back to this controller
         * @type {DefineNflowAccessControlController}
         */
        var self = this;

        /**
         * The angular form
         * @type {{}}
         */
        this.nflowAccessControlForm = {};

        /**
         * The nflow model
         * @type {*}
         */
        this.model = NflowService.createNflowModel;

        /**
         * Service to access the Hadoop security groups
         */
        self.nflowSecurityGroups = NflowSecurityGroups;

        /**
         * Hadoop security groups chips model
         * @type {{}}
         */
        self.securityGroupChips = {};
        self.securityGroupChips.selectedItem = null;
        self.securityGroupChips.searchText = null;

        /**
         * Flag to indicate if hadoop groups are enabled or not
         * @type {boolean}
         */
        self.securityGroupsEnabled = false;






        NflowSecurityGroups.isEnabled().then(function(isValid) {
            self.securityGroupsEnabled = isValid;
        });



        self.transformChip = function(chip) {
            // If it is an object, it's already a known chip
            if (angular.isObject(chip)) {
                return chip;
            }
            // Otherwise, create a new one
            return {name: chip}
        };



    }

    angular.module(moduleName).controller("DefineNflowAccessControlController",["$scope","NflowService","NflowSecurityGroups", DefineNflowAccessControlController]);

    angular.module(moduleName).directive("onescorpinDefineNflowAccessControl", directive);
});

