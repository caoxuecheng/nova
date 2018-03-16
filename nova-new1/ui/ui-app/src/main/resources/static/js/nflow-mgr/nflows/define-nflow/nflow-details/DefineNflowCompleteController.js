define(['angular','nflow-mgr/nflows/define-nflow/module-name'], function (angular,moduleName) {

    var controller;
    controller = function ($scope, $q, $http, $mdToast,$transition$, RestUrlService, $transition$, NflowService, StateService) {
        var self = this;
       // self.model = $transition$.params().nflowModel;
       // self.error = $transition$.params().error;
        self.model = $transition$.params().nflowModel;
        self.error = $transition$.params().error;

        self.isValid = self.error == null;

        /**
         * Gets the nflow id from the NflowService
         * @returns {*}
         */
        function getNflowId() {
            var nflowId = self.model != null ? self.model.id : null;
            if (nflowId == null && NflowService.createNflowModel != null) {
                nflowId = NflowService.createNflowModel.id;
            }
            if (nflowId == null && NflowService.editNflowModel != null) {
                nflowId = NflowService.editNflowModel.id;
            }
            return nflowId;
        }

        /**
         * Navigate to the Nflow Details SLA tab
         */
        this.onAddServiceLevelAgreement = function () {
            //navigate to Nflow Details and move to the 3 tab (SLA)
            var nflowId = getNflowId();
            StateService.NflowManager().Nflow().navigateToNflowDetails(nflowId, 3);
        }
        this.onViewDetails = function () {
            StateService.NflowManager().Sla().navigateToServiceLevelAgreements();
        }

        /**
         * Navigate to the Nflow Details first tab
         */
        this.onViewDetails = function () {
            var nflowId = getNflowId();
            StateService.NflowManager().Nflow().navigateToNflowDetails(nflowId, 0);
        }

        /**
         * Navigate to the Nflow List page
         */
        this.onViewNflowsList = function () {
            NflowService.resetNflow();
            StateService.NflowManager().Nflow().navigateToNflows();
        }

        this.gotIt = function () {
            self.onViewNflowsList();
        }

    };


    angular.module(moduleName).controller('DefineNflowCompleteController', ["$scope","$q","$http","$mdToast","$transition$","RestUrlService","$transition$","NflowService","StateService",controller]);

});


