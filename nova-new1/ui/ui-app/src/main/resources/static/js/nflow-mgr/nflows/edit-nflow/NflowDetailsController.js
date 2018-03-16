define(['angular','nflow-mgr/nflows/edit-nflow/module-name'], function (angular,moduleName) {

    /**
     * Displays the details for a nflow.
     *
     * @param $scope
     * @param $q
     * @param $transition$.params()
     * @param $mdDialog
     * @param $mdToast
     * @param $http
     * @param $state
     * @param {AccessControlService} AccessControlService the access control service
     * @param RestUrlService
     * @param NflowService
     * @param RegisterTemplateService
     * @param StateService
     */
    var controller = function ($scope, $q, $transition$, $mdDialog, $mdToast, $http, $state, AccessControlService, RestUrlService, NflowService, RegisterTemplateService, StateService, SideNavService,
                               FileUpload, ConfigurationService,EntityAccessControlDialogService, EntityAccessControlService, UiComponentsService) {

        var SLA_INDEX = 3;
        var self = this;

        /**
         * Flag to indicate style of page
         * if true it will fit the card to the 980px width
         * if false it will make it go 100% width
         * @type {boolean}
         */
        self.cardWidth = true;

        /**
         * Indicates if admin operations are allowed.
         * @type {boolean}
         */
        self.allowAdmin = false;

        /**
         * Allow the Changing of this nflows permissions
         * @type {boolean}
         */
        self.allowChangePermissions = false;

        /**
         * Indicates if edit operations are allowed.
         * @type {boolean}
         */
        self.allowEdit = false;

        /**
         * Indicates if export operations are allowed.
         * @type {boolean}
         */
        self.allowExport = false;

        /**
         * Alow user to access the sla tab
         * @type {boolean}
         */
        self.allowSlaAccess = false;

        this.nflowId = null;
        this.selectedTabIndex = 0;

        this.loadingNflowData = false;
        this.model = NflowService.editNflowModel;
        this.model.loaded = false;
        this.loadMessage = ''
        this.uploadFile = null;
        this.uploading = false;
        this.uploadAllowed = false;

        /**
         * flag to indicate the nflow could not be loaded
         * @type {boolean}
         */
        this.errorLoadingNflow = false;



        /** flag to indicate if we get a valid connection back from NiFi.  Initially to true. it will be rechecked on load **/
        this.isNiFiRunning = true;

        var requestedTabIndex = $transition$.params().tabIndex;



        $scope.$watch(function() {
            return self.selectedTabIndex;
        }, function(newVal) {
            //Make the Lineage tab fit without side nav
            //open side nav if we are not navigating between lineage links
            if (newVal == 2 || (requestedTabIndex != undefined && requestedTabIndex == 2)) {
                SideNavService.hideSideNav();
                self.cardWidth = false;
                requestedTabIndex = 0;
            }
            else {
                SideNavService.showSideNav();
                self.cardWidth = true;
            }

        })

        /**
         * flag to indicate if the SLA page should be set to empty new form rather than the list
         * Used for when the "Add SLA" button is clicked
         * @type {boolean}
         */
        this.newSla = false;

        var init = function() {
            self.nflowId = $transition$.params().nflowId;

            self.exportNflowUrl = RestUrlService.ADMIN_EXPORT_NFLOW_URL+"/"+self.nflowId

            loadNflow(requestedTabIndex);
            nifiRunningCheck();
        };

        this.cloneNflow = function(){
            StateService.NflowManager().Nflow().navigateToCloneNflow(this.model.nflowName);
        }

        /**
         * Displays a confirmation dialog for deleting the nflow.
         */
        this.confirmDeleteNflow = function() {
            if(self.allowAdmin) {
                // Verify there are no dependent nflows
                if (angular.isArray(self.model.usedByNflows) && self.model.usedByNflows.length > 0) {
                    var list = "<ul>";
                    list += _.map(self.model.usedByNflows, function (nflow) {
                        return "<li>" + _.escape(nflow.nflowName) + "</li>";
                    });
                    list += "</ul>";

                    var alert = $mdDialog.alert()
                        .parent($("body"))
                        .clickOutsideToClose(true)
                        .title("Nflow is referenced")
                        .htmlContent("This nflow is referenced by other nflows and cannot be deleted. The following nflows should be deleted first: " + list)
                        .ariaLabel("nflow is referenced")
                        .ok("Got it!");
                    $mdDialog.show(alert);

                    return;
                }

                // Display delete dialog
                var $dialogScope = $scope.$new();
                $dialogScope.dialog = $mdDialog;
                $dialogScope.vm = self;

                $mdDialog.show({
                    escapeToClose: false,
                    fullscreen: true,
                    parent: angular.element(document.body),
                    scope: $dialogScope,
                    templateUrl: "js/nflow-mgr/nflows/edit-nflow/nflow-details-delete-dialog.html"
                });
            }
        };

        /**
         * Permanently deletes this nflow.
         */
        this.deleteNflow = function() {
            // Update model state
            self.model.state = "DELETED";

            // Delete the nflow
            var successFn = function() {
                $state.go("nflows");
            };
            var errorFn = function(response) {
                // Update model state
                self.model.state = "DISABLED";

                // Display error message
                var msg = "<p>The nflow cannot be deleted at this time.</p><p>";
                msg += angular.isString(response.data.message) ? _.escape(response.data.message) : "Please try again later.";
                msg += "</p>";

                $mdDialog.hide();
                $mdDialog.show(
                        $mdDialog.alert()
                                .ariaLabel("Error deleting nflow")
                                .clickOutsideToClose(true)
                                .htmlContent(msg)
                                .ok("Got it!")
                                .parent(document.body)
                                .title("Error deleting nflow")
                );
            };

            $http.delete(RestUrlService.GET_NFLOWS_URL + "/" + self.nflowId).then(successFn, errorFn);
        };

        this.showNflowUploadDialog = function() {
            $mdDialog.show({
                controller: 'NflowUploadFileDialogController',
                escapeToClose: false,
                fullscreen: true,
                parent: angular.element(document.body),
                templateUrl: "js/nflow-mgr/nflows/edit-nflow/nflow-details-upload-dialog.html",
                locals: {nflowId: self.nflowId}
            }).then(function(msg) {
                $mdToast.show(
                    $mdToast.simple()
                        .textContent('File uploaded.')
                        .hideDelay(3000)
                );
            });
        }

        this.showAccessControlDialog = function(){

            function onCancel(){

            }

            function onSave(){
            }

            EntityAccessControlDialogService.showAccessControlDialog(self.model,"nflow",self.model.nflowName,onSave,onCancel);

        }


        this.openNflowMenu = function($mdOpenMenu, ev) {
            $mdOpenMenu(ev);
        };


        /**
         * Enables this nflow.
         */
        this.enableNflow = function() {
            if(!self.enabling && self.allowEdit) {
                self.enabling = true;
                $http.post(RestUrlService.ENABLE_NFLOW_URL(self.nflowId)).then(function (response) {
                    self.model.state = response.data.state;
                    NflowService.updateEditModelStateIcon();
                    self.enabling = false;
                }, function () {
                    $mdDialog.show(
                        $mdDialog.alert()
                            .clickOutsideToClose(true)
                            .title("NiFi Error")
                            .textContent("The nflow could not be enabled.")
                            .ariaLabel("Cannot enable nflow.")
                            .ok("OK")
                    );
                    self.enabling = false;
                });
            }
        };

        /**
         * Disables this nflow.
         */
        this.disableNflow = function() {
            if(!self.disabling && self.allowEdit) {
                self.disabling = true;
                $http.post(RestUrlService.DISABLE_NFLOW_URL(self.nflowId)).then(function (response) {
                    self.model.state = response.data.state;
                    NflowService.updateEditModelStateIcon();
                    self.disabling = false;
                }, function () {
                    $mdDialog.show(
                        $mdDialog.alert()
                            .clickOutsideToClose(true)
                            .title("NiFi Error")
                            .textContent("The nflow could not be disabled.")
                            .ariaLabel("Cannot disable nflow.")
                            .ok("OK")
                    );
                    self.disabling = false;
                });
            }
        };


        function mergeTemplateProperties(nflow) {
            var successFn = function(response) {
                return response;
            }
            var errorFn = function(err) {

            }

            var promise = $http({
                url: RestUrlService.MERGE_NFLOW_WITH_TEMPLATE(nflow.id),
                method: "POST",
                data: angular.toJson(nflow),
                headers: {
                    'Content-Type': 'application/json; charset=UTF-8'
                }
            }).then(successFn, errorFn);

            return promise;
        }

        /**
         * Navigates to the category details page for this nflow's category.
         *
         * An error is displayed if the user does not have permissions to access categories.
         */
        this.onCategoryClick = function() {
            AccessControlService.getUserAllowedActions()
                    .then(function(actionSet) {
                        if (AccessControlService.hasAction(AccessControlService.CATEGORIES_ACCESS, actionSet.actions)) {
                            StateService.NflowManager().Category().navigateToCategoryDetails(self.model.category.id);
                        } else {
                            $mdDialog.show(
                                    $mdDialog.alert()
                                            .clickOutsideToClose(true)
                                            .title("Access Denied")
                                            .textContent("You do not have permissions to access categories.")
                                            .ariaLabel("Access denied for categories")
                                            .ok("OK")
                            );
                        }
                    });
        };

        this.onTableClick = function() {
            StateService.NflowManager().Table().navigateToTable(self.model.category.systemName, self.model.table.tableSchema.name);
        }

        this.addSla = function() {
            self.selectedTabIndex = SLA_INDEX;
            self.newSla = true;
        }

        this.updateMenuOptions = function() {
            self.uploadAllowed=false;
            var model = self.model;
            if (model && model.inputProcessor && model.inputProcessor.allProperties.length > 0) {
                angular.forEach(model.inputProcessor.allProperties, function (property) {
                   if (property.processorType == 'org.apache.nifi.processors.standard.GetFile') {
                       self.uploadAllowed = true;
                       return;
                   }
                });
            }
        }

        function loadNflow(tabIndex) {
            self.errorLoadingNflow = false;
            self.loadingNflowData = true;
            self.model.loaded = false;
            self.loadMessage = '';
            var successFn = function(response) {
                if (response.data) {
                    var promises = {
                        nflowPromise: mergeTemplateProperties(response.data),
                        processorTemplatesPromise:  UiComponentsService.getProcessorTemplates()
                    };

                    $q.all(promises).then(function(result) {


                        //deal with the nflow data
                        var updatedNflowResponse = result.nflowPromise;
                            //merge in the template properties
                            //this will update teh self.model as they point to the same object
                            if (updatedNflowResponse == undefined || updatedNflowResponse.data == undefined) {
                                self.loadingNflowData = false;
                                var loadMessage = 'Unable to load Nflow Details.  Please ensure that Apache Nifi is up and running and then refresh this page.';
                                self.loadMessage = loadMessage;
                                $mdDialog.show(
                                    $mdDialog.alert()
                                    //   .parent(angular.element(document.querySelector('#popupContainer')))
                                        .clickOutsideToClose(true)
                                        .title('Unable to load Nflow Details')
                                        .textContent(loadMessage)
                                        .ariaLabel('Unable to load Nflow Details')
                                        .ok('Got it!')
                                );
                            } else {
                                self.model.loaded = true;
                                NflowService.updateNflow(updatedNflowResponse.data);
                                if (tabIndex != null && tabIndex != undefined && tabIndex != self.selectedTabIndex) {
                                    self.selectedTabIndex = tabIndex;
                                }

                                RegisterTemplateService.initializeProperties(updatedNflowResponse.data.registeredTemplate,'edit');
                                self.model.inputProcessors = RegisterTemplateService.removeNonUserEditableProperties(updatedNflowResponse.data.registeredTemplate.inputProcessors,true);
                                //sort them by name
                                self.model.inputProcessors = _.sortBy(self.model.inputProcessors,'name')

                                self.model.inputProcessor = _.find(self.model.inputProcessors,function(processor){
                                    return angular.isDefined(self.model.inputProcessorName) && self.model.inputProcessorName != null ? self.model.inputProcessorType == processor.type && self.model.inputProcessorName.toLowerCase() == processor.name.toLowerCase() : self.model.inputProcessorType == processor.type;
                                });

                                if(angular.isUndefined(self.model.inputProcessor)){
                                    self.model.inputProcessor = _.find(self.model.inputProcessors,function(processor){
                                        return self.model.inputProcessorType == processor.type;
                                    });
                                }
                                self.model.nonInputProcessors = RegisterTemplateService.removeNonUserEditableProperties(updatedNflowResponse.data.registeredTemplate.nonInputProcessors,false);
                                self.updateMenuOptions();
                                self.loadingNflowData = false;
                                self.model.isStream = updatedNflowResponse.data.registeredTemplate.stream;
                                NflowService.updateEditModelStateIcon();

                                var entityAccessControlled = AccessControlService.isEntityAccessControlled();
                                //Apply the entity access permissions
                                var requests = {
                                    entityEditAccess: entityAccessControlled === true
                                        ? NflowService.hasEntityAccess(EntityAccessControlService.ENTITY_ACCESS.NFLOW.EDIT_NFLOW_DETAILS, self.model)
                                        : true,
                                    entityExportAccess: !entityAccessControlled || NflowService.hasEntityAccess(EntityAccessControlService.ENTITY_ACCESS.NFLOW.EXPORT, self.model),
                                    entityPermissionAccess: entityAccessControlled === true
                                        ? NflowService.hasEntityAccess(EntityAccessControlService.ENTITY_ACCESS.NFLOW.CHANGE_NFLOW_PERMISSIONS, self.model)
                                        : true,
                                    functionalAccess: AccessControlService.getUserAllowedActions()
                                };
                                $q.all(requests).then(function (response) {
                                    var allowEditAccess =  AccessControlService.hasAction(AccessControlService.NFLOWS_EDIT, response.functionalAccess.actions);
                                    var allowAdminAccess =  AccessControlService.hasAction(AccessControlService.NFLOWS_ADMIN, response.functionalAccess.actions);
                                    var slaAccess =  AccessControlService.hasAction(AccessControlService.SLA_ACCESS, response.functionalAccess.actions);
                                    var allowExport = AccessControlService.hasAction(AccessControlService.NFLOWS_EXPORT, response.functionalAccess.actions);

                                    self.allowEdit = response.entityEditAccess && allowEditAccess;
                                    self.allowChangePermissions = entityAccessControlled && response.entityPermissionAccess && allowEditAccess;
                                    self.allowAdmin = allowAdminAccess;
                                    self.allowSlaAccess = slaAccess;
                                    self.allowExport = response.entityExportAccess && allowExport;
                                });
                            }








                    },function(err){
                        //handle err
                        self.loadingNflowData = false;
                    });
                }
                else {
                    errorFn(" The nflow was not found.")
                }
            }
            var errorFn = function(err) {
                self.loadingNflowData = false;
                self.errorLoadingNflow = true;
                var message = angular.isDefined(err) && angular.isString(err) ? err : '';
                $mdDialog.show(
                        $mdDialog.alert()
                                .parent(angular.element(document.querySelector('body')))
                                .clickOutsideToClose(true)
                                .title('Error loading nflow')
                                .textContent('Error loading nflow. '+message)
                                .ariaLabel('Error loading nflow')
                                .ok('Got it!')
                        //.targetEvent(ev)
                );

            }
            var promise = $http.get(RestUrlService.GET_NFLOWS_URL + "/" + self.nflowId);
            promise.then(successFn, errorFn);
            return promise;
        }

         function nifiRunningCheck(){
            var promise = $http.get(RestUrlService.IS_NIFI_RUNNING_URL);
            promise.then(function(response) {
                self.isNiFiRunning =response.data;
            }, function(err) {
                self.isNiFiRunning = false;
            });
        }

        this.gotoNflowStats = function (ev) {
            ev.preventDefault();
            ev.stopPropagation();
            var nflowName = self.model.systemCategoryName + "." + self.model.systemNflowName;
            StateService.OpsManager().Nflow().navigateToNflowStats(nflowName);
        };

        this.gotoNflowDetails = function (ev) {
            ev.preventDefault();
            ev.stopPropagation();
            var nflowName = self.model.systemCategoryName + "." + self.model.systemNflowName;
            StateService.OpsManager().Nflow().navigateToNflowDetails(nflowName);
        };

        init();
    };

    var NflowUploadFileDialogController = function ($scope, $mdDialog, $http, RestUrlService, FileUpload, nflowId){
        var self = this;
        $scope.uploading = false;
        $scope.uploadFile = null;

        /**
         * Upload file
         */
        $scope.doUpload = function() {

            $scope.uploading = true;
            $scope.errorMessage = '';

            var uploadUrl = RestUrlService.UPLOAD_FILE_NFLOW_URL(nflowId);
            var params = {};
            var successFn = function (response) {
                $scope.uploading = false;
                $mdDialog.hide('Upload successfully submitted.');
            }
            var errorFn = function (data) {
                $scope.uploading = false;
                $scope.errorMessage = 'Failed to submit file.';
            }
            FileUpload.uploadFileToUrl($scope.uploadFile, uploadUrl, successFn, errorFn, params);
        };


        $scope.hide = function() {
            $mdDialog.hide();
        };

        $scope.cancel = function() {
            $mdDialog.cancel();
        };


    };

    angular.module(moduleName).controller('NflowDetailsController', ["$scope","$q","$transition$","$mdDialog","$mdToast","$http","$state","AccessControlService","RestUrlService","NflowService","RegisterTemplateService","StateService","SideNavService","FileUpload","ConfigurationService","EntityAccessControlDialogService","EntityAccessControlService","UiComponentsService",controller]);

    angular.module(moduleName).controller('NflowUploadFileDialogController',["$scope","$mdDialog","$http","RestUrlService","FileUpload","nflowId",NflowUploadFileDialogController]);
});
