define(['angular'], function (angular) {
    /**
     * Displays the home page.
     *
     * @constructor
     * @param {Object} $scope the application model
     * @param $mdDialog the dialog service
     * @param {AccessControlService} AccessControlService the access control service
     * @param StateService the state service
     */
    function HomeController($scope, $mdDialog, AccessControlService, StateService) {
        var self = this;

        /**
         * Indicates that the page is currently being loaded.
         * @type {boolean}
         */
        self.loading = true;

        /**
         * Determines the home page based on the specified allowed actions.
         *
         * @param actions the allowed actions
         */
        self.onLoad = function(actions) {


            // Determine the home page
            if (AccessControlService.hasAction(AccessControlService.NFLOWS_ACCESS, actions)) {
                return StateService.NflowManager().Nflow().navigateToNflows();
            }
            if (AccessControlService.hasAction(AccessControlService.OPERATIONS_MANAGER_ACCESS, actions)) {
                return StateService.OpsManager().dashboard();
            }
            if (AccessControlService.hasAction(AccessControlService.CATEGORIES_ACCESS, actions)) {
                return StateService.NflowManager().Category().navigateToCategories();
            }
            if (AccessControlService.hasAction(AccessControlService.TEMPLATES_ACCESS, actions)) {
                return StateService.NflowManager().Template().navigateToRegisteredTemplates();
            }
            if (AccessControlService.hasAction(AccessControlService.USERS_ACCESS, actions)) {
                return StateService.Auth().navigateToUsers();
            }
            if (AccessControlService.hasAction(AccessControlService.GROUP_ACCESS, actions)) {
                return StateService.Auth().navigateToGroups();
            }

/*
            // Determine if Nflow Manager is allowed at all
            if (!AccessControlService.hasAction(AccessControlService.NFLOW_MANAGER_ACCESS, actions) && !AccessControlService.hasAction(AccessControlService.USERS_GROUPS_ACCESS, actions)) {
                self.loading = false;
                $mdDialog.show(
                    $mdDialog.alert()
                        .clickOutsideToClose(true)
                        .title("Access Denied")
                        .textContent("You do not have access to the Nflow Manager.")
                        .ariaLabel("Access denied to nflow manager")
                        .ok("OK")
                );
                return;
            }
            */

            // Otherwise, let the user pick
            self.loading = false;
        };

        // Fetch the list of allowed actions
        AccessControlService.getUserAllowedActions()
                .then(function(actionSet) {
                    self.onLoad(actionSet.actions);
                });
    }

        angular.module('nova').controller('HomeController', HomeController);

});
