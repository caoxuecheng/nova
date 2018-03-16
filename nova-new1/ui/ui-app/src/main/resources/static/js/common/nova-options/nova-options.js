define(['angular','common/module-name'], function (angular,moduleName) {

        var directive = function ($http, $mdDialog, $window, AboutNovaService) {
            return {
                restrict: "E",
                scope: {
                    selectedOption: "&?",
                    openedMenu: "&?",
                    menuIcon: "@?"
                },
                templateUrl: 'js/common/nova-options/nova-options.html',
                link: function ($scope) {

                    //default the icon to be more_vert
                    if (!angular.isDefined($scope.menuIcon)) {
                        $scope.menuIcon = 'more_vert';
                    }

                    // Get user name
                    $scope.username = "User";
                    $http.get("/proxy/v1/about/me").then(function (response) {
                        $scope.username = response.data.systemName;
                    });

                    $scope.openMenu = function ($mdOpenMenu, ev) {
                        //callback
                        if ($scope.openedMenu) {
                            $scope.openedMenu();
                        }
                        $mdOpenMenu(ev);
                    };

                    $scope.aboutNova = function () {
                        AboutNovaService.showAboutDialog();
                        if ($scope.selectedOption) {
                            $scope.selectedOption()('aboutNova');
                        }
                    };

                    /**
                     * Redirects the user to the logout page.
                     */
                    $scope.logout = function () {
                        $window.location.href = "/logout";
                    }
                }
            }
        };

        angular.module(moduleName).directive('novaOptions', ['$http', '$mdDialog', '$window', 'AboutNovaService', directive]);
});
