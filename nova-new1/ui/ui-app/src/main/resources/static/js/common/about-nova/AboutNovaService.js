/*
 * Service to display 'about nova' popup page, and get nova version
 */
define(['angular','common/module-name'], function (angular,moduleName) {
    angular.module(moduleName).service('AboutNovaService', function ($mdDialog) {
        var self = this;

        self.showAboutDialog = function () {
            $mdDialog.show({
                controller: 'AboutNovaDialogController',
                templateUrl: 'js/common/about-nova/about.html',
                parent: angular.element(document.body),
                clickOutsideToClose: false,
                escapeToClose: true,
                fullscreen: false,
                locals: {}
            }).then(function (msg) {
                //callback (success)
            }, function () {
                //callback (failure)
            });
        }
    });

        var controller = function ($scope, $mdDialog, $http) {

            var self = this;

            $http({
                method: "GET",
                url: "/proxy/v1/about/version"
            }).then(function callSuccess(response) {
                $scope.version = response.data;
            }, function callFailure(response) {
                $scope.version = "Not Available"
            });

            $scope.hide = function () {
                $mdDialog.hide();
            };

            $scope.cancel = function () {
                $mdDialog.cancel();
            };

        };
        angular.module(moduleName).controller('AboutNovaDialogController', controller);

});
