/**
 * Used to store temporary state of the Edit Nflow Nifi Properties
 * when a user clicks the Edit link for the Nflow Details so the object can be passed to the template factory
 *
 */
define(['angular','nflow-mgr/module-name'], function (angular,moduleName) {
    angular.module(moduleName).service('EditNflowNifiPropertiesService', function () {

        var self = this;
        this.editNflowModel = {};

    });
});
