define(['angular', 'nflow-mgr/nflows/edit-nflow/module-name'], function (angular, moduleName) {

    var directive = function () {
        return {
            restrict: "EA",
            bindToController: {},
            controllerAs: 'vm',
            scope: {},
            templateUrl: 'js/nflow-mgr/nflows/edit-nflow/details/nflow-data-policies.html',
            controller: "NflowDataPoliciesController",
            link: function ($scope, element, attrs, controller) {

            }

        };
    };

    var controller = function ($scope, $mdDialog, $timeout, $q, $compile, $sce, AccessControlService, EntityAccessControlService, NflowService, StateService, NflowFieldPolicyRuleService,
                               DomainTypesService) {

        var self = this;

        /**
         * Indicates if the nflow data policies may be edited.
         * @type {boolean}
         */
        self.allowEdit = false;

        this.model = NflowService.editNflowModel;
        /**
         * The form for angular errors
         * @type {{}}
         */
        this.editNflowDataPoliciesForm = {};

        this.editableSection = false;

        /**
         * List of available domain types.
         * @type {DomainType[]}
         */
        self.availableDomainTypes = [];
        DomainTypesService.findAll().then(function (domainTypes) {
            self.availableDomainTypes = domainTypes;
            // NOVA-251 Remove data type until schema evolution is supported
            domainTypes.forEach(function (domainType) {
                if (domainType && domainType.field) {
                    domainType.field.derivedDataType = null;
                    domainType.field.precisionScale = null;
                }
            });
        });

        var checkAll = {
            isChecked: true,
            isIndeterminate: false,
            totalChecked: 0,
            clicked: function (checked) {
                if (checked) {
                    this.totalChecked++;
                }
                else {
                    this.totalChecked--;
                }
                this.markChecked();
            },
            markChecked: function () {
                if (this.totalChecked == self.editModel.fieldPolicies.length) {
                    this.isChecked = true;
                    this.isIndeterminate = false;
                }
                else if (this.totalChecked > 0) {
                    this.isChecked = false;
                    this.isIndeterminate = true;
                }
                else if (this.totalChecked == 0) {
                    this.isChecked = false;
                    this.isIndeterminate = false;
                }
            }
        };

        /**
         * Toggle Check All/None on Profile column
         * Default it to true
         * @type {{isChecked: boolean, isIndeterminate: boolean, toggleAll: controller.indexCheckAll.toggleAll}}
         */
        this.profileCheckAll = angular.extend({
            isChecked: true,
            isIndeterminate: false,
            toggleAll: function () {
                var checked = (!this.isChecked || this.isIndeterminate) ? true : false;
                _.each(self.editModel.fieldPolicies, function (field) {
                    field.profile = checked;
                });
                if (checked) {
                    this.totalChecked = self.editModel.fieldPolicies.length;
                }
                else {
                    this.totalChecked = 0;
                }
                this.markChecked();
            },
            setup: function () {
                self.profileCheckAll.totalChecked = 0;
                _.each(self.editModel.fieldPolicies, function (field) {
                    if (field.profile) {
                        self.profileCheckAll.totalChecked++;
                    }
                });
                self.profileCheckAll.markChecked();
            }
        }, checkAll);

        /**
         *
         * Toggle check all/none on the index column
         *
         * @type {{isChecked: boolean, isIndeterminate: boolean, toggleAll: controller.indexCheckAll.toggleAll}}
         */
        this.indexCheckAll = angular.extend({
            isChecked: false,
            isIndeterminate: false,
            toggleAll: function () {
                var checked = (!this.isChecked || this.isIndeterminate) ? true : false;
                _.each(self.editModel.fieldPolicies, function (field) {
                    field.index = checked;
                });
                this.isChecked = checked;

                if (checked) {
                    this.totalChecked = self.editModel.fieldPolicies.length;
                }
                else {
                    this.totalChecked = 0;
                }
                this.markChecked();
            },
            setup: function () {
                self.indexCheckAll.totalChecked = 0;
                _.each(self.editModel.fieldPolicies, function (field) {
                    if (field.index) {
                        self.indexCheckAll.totalChecked++;
                    }
                });
                self.indexCheckAll.markChecked();
            }
        }, checkAll);

        $scope.$watch(function () {
            return NflowService.editNflowModel;
        }, function (newVal) {
            //only update the model if it is not set yet
            if (self.model == null) {
                self.model = NflowService.editNflowModel;
                populateFieldNameMap();
                applyDefaults();

            }
        });

        /**
         * apply default values to the read only model
         */
        function applyDefaults() {
            if (self.model.table.targetFormat === undefined || self.model.table.targetFormat === '' || self.model.table.targetFormat === null) {
                //default to ORC
                self.model.table.targetFormat = 'STORED AS ORC'
            }
            if (self.model.table.options.compressionFormat === undefined || self.model.table.options.compressionFormat === '' || self.model.table.options.compressionFormat === null) {
                self.model.table.options.compressionFormat = 'NONE'
            }
        }

        self.fieldNameMap = {};

        function populateFieldNameMap() {
            self.fieldNameMap = {};

            _.each(self.model.table.tableSchema.fields, function (field) {
                self.fieldNameMap[field.name] = field;
            });
        }

        populateFieldNameMap();
        applyDefaults();

        this.compressionOptions = NflowService.allCompressionOptions();

        this.mergeStrategies = angular.copy(NflowService.mergeStrategies);

        this.targetFormatOptions = NflowService.targetFormatOptions;

        this.transformChip = function (chip) {
            // If it is an object, it's already a known chip
            if (angular.isObject(chip)) {
                return chip;
            }
            // Otherwise, create a new one
            return {name: chip}
        };

        self.editModel = {};

        function findProperty(key) {
            return _.find(self.model.inputProcessor.properties, function (property) {
                //return property.key = 'Source Database Connection';
                return property.key == key;
            });
        }

        /**
         * Returns the readable display name for the mergeStrategy on the edited nflow model
         * @returns {*}
         */
        this.mergeStrategyDisplayName = function () {
            var mergeStrategyObject = _.find(NflowService.mergeStrategies, function (strategy) {
                return strategy.type == self.model.table.targetMergeStrategy;
            });
            return mergeStrategyObject != null ? mergeStrategyObject.name : self.model.table.targetMergeStrategy
        };

        /**
         * Enable/Disable the PK Merge Strategy
         */
        this.onChangePrimaryKey = function () {
            validateMergeStrategies();
        };

        this.onChangeMergeStrategy = function () {
            validateMergeStrategies();
        };

        function validateMergeStrategies() {
            var valid = NflowService.enableDisablePkMergeStrategy(self.editModel, self.mergeStrategies);
            self.editNflowDataPoliciesForm['targetMergeStrategy'].$setValidity('invalidPKOption', valid);

            valid = NflowService.enableDisableRollingSyncMergeStrategy(self.model, self.mergeStrategies);
            self.editNflowDataPoliciesForm['targetMergeStrategy'].$setValidity('invalidRollingSyncOption', valid);
        }

        this.onEdit = function () {
            //copy the model
            var fieldPolicies = angular.copy(NflowService.editNflowModel.table.fieldPolicies);
            var fields = angular.copy(NflowService.editNflowModel.table.tableSchema.fields);
            //assign the field to the policy
            var fieldMap = _.groupBy(fields, function (field) {
                return field.name
            });
            _.each(fieldPolicies, function (policy) {
                var columnDef = fieldMap[policy.name][0];
                policy.columnDef = columnDef;
                if (angular.isString(policy.domainTypeId) && policy.domainTypeId !== "") {
                    policy.$currentDomainType = _.find(self.availableDomainTypes, function (domainType) {
                        return policy.domainTypeId === domainType.id;
                    });
                    if (angular.isUndefined(policy.$currentDomainType)) {
                        policy.domainTypeId = null;
                    }
                }
            });

            self.editModel = {};
            self.editModel.fieldPolicies = fieldPolicies;

            self.editModel.table = {};
            self.editModel.table.tableSchema = {};
            self.editModel.table.tableSchema.fields = fields;
            self.editModel.table.targetFormat = NflowService.editNflowModel.table.targetFormat;
            if (self.editModel.table.targetFormat === undefined) {
                //default to ORC
                self.editModel.table.targetFormat = 'ORC'
            }
            self.editModel.table.targetMergeStrategy = NflowService.editNflowModel.table.targetMergeStrategy;
            self.editModel.table.options = angular.copy(NflowService.editNflowModel.table.options);
            if (self.editModel.table.options.compressionFormat === undefined) {
                self.editModel.options.compressionFormat = 'NONE'
            }
            self.indexCheckAll.setup();
            self.profileCheckAll.setup();

            $timeout(validateMergeStrategies, 400);
        };

        this.onCancel = function () {

        };

        this.getAllFieldPolicies = function (field) {
            return NflowFieldPolicyRuleService.getAllPolicyRules(field);
        };

        this.onSave = function (ev) {
            //save changes to the model
            NflowService.showNflowSavingDialog(ev, "Saving...", self.model.nflowName);
            var copy = angular.copy(NflowService.editNflowModel);

            copy.table.targetFormat = self.editModel.table.targetFormat;
            copy.table.fieldPolicies = self.editModel.fieldPolicies;

            //add back in the changes to the pk, nullable, created, updated tracker columns
            var policyMap = _.groupBy(copy.table.fieldPolicies, function (policy) {
                return policy.name
            });
            _.each(copy.table.tableSchema.fields, function (field) {
                //find the respective changes in the ui object for this field
                var updatedColumnDef = policyMap[field.name] != undefined ? policyMap[field.name][0] : undefined;
                if (updatedColumnDef) {
                    var def = updatedColumnDef.columnDef;
                    angular.extend(field, def);
                }
            });
            //strip off the added 'columnDef' property
            _.each(self.editModel.fieldPolicies, function (policy) {
                policy.columnDef = undefined;
            });

            copy.table.targetMergeStrategy = self.editModel.table.targetMergeStrategy;
            copy.table.options = self.editModel.table.options;
            copy.userProperties = null;

            NflowService.saveNflowModel(copy).then(function (response) {
                NflowService.hideNflowSavingDialog();
                self.editableSection = false;
                //save the changes back to the model
                self.model.table.tableSchema.fields = copy.table.tableSchema.fields;
                self.model.table.targetFormat = self.editModel.table.targetFormat;
                self.model.table.fieldPolicies = self.editModel.fieldPolicies;
                self.model.table.targetMergeStrategy = self.editModel.table.targetMergeStrategy;
                self.model.table.options = self.editModel.table.options;
                populateFieldNameMap();
            }, function (response) {
                NflowService.hideNflowSavingDialog();
                NflowService.buildErrorData(self.model.nflowName, response);
                NflowService.showNflowErrorsDialog();
                //make it editable
                self.editableSection = true;
            });
        };

        this.showFieldRuleDialog = function (field) {
            $mdDialog.show({
                controller: 'NflowFieldPolicyRuleDialogController',
                templateUrl: 'js/nflow-mgr/shared/nflow-field-policy-rules/define-nflow-data-processing-field-policy-dialog.html',
                parent: angular.element(document.body),
                clickOutsideToClose: false,
                fullscreen: true,
                locals: {
                    nflow: self.model,
                    field: field
                }
            })
                .then(function () {
                    if (angular.isObject(field.$currentDomainType)) {
                        var domainStandardization = _.map(field.$currentDomainType.fieldPolicy.standardization, _.property("name"));
                        var domainValidation = _.map(field.$currentDomainType.fieldPolicy.validation, _.property("name"));
                        var fieldStandardization = _.map(field.standardization, _.property("name"));
                        var fieldValidation = _.map(field.validation, _.property("name"));
                        if (!angular.equals(domainStandardization, fieldStandardization) || !angular.equals(domainValidation, fieldValidation)) {
                            delete field.$currentDomainType;
                            field.domainTypeId = null;
                        }
                    }
                });
        };

        /**
         * Gets the domain type with the specified id.
         *
         * @param {string} domainTypeId the domain type id
         * @returns {(DomainType|null)} the domain type
         */
        self.getDomainType = function (domainTypeId) {
            return _.find(self.availableDomainTypes, function (domainType) {
                return (domainType.id === domainTypeId);
            });
        };

        /**
         * Gets the placeholder HTML for the specified domain type option.
         *
         * @param {string} domainTypeId the domain type id
         * @returns {string} the placeholder HTML
         */
        self.getDomainTypePlaceholder = function (domainTypeId) {
            // Find domain type from id
            var domainType = null;
            if (angular.isString(domainTypeId) && domainTypeId !== "") {
                domainType = _.find(self.availableDomainTypes, function (domainType) {
                    return (domainType.id === domainTypeId);
                });
            }

            // Generate the HTML
            if (angular.isObject(domainType)) {
                var element = $("<ng-md-icon/>")
                    .attr("icon", domainType.icon)
                    .attr("title", domainType.title)
                    .css("fill", domainType.iconColor)
                    .css("margin-left", "12px");
                return $sce.trustAsHtml($compile(element)($scope)[0].outerHTML);
            } else {
                return "";
            }
        };

        /**
         * Display a confirmation when the domain type of a field is changed and there are existing standardizers and validators.
         *
         * @param {FieldPolicy} policy the field policy
         */
        self.onDomainTypeChange = function (policy) {
            // Check if removing domain type
            if (!angular.isString(policy.domainTypeId) || policy.domainTypeId === "") {
                delete policy.$currentDomainType;
                return;
            }

            // Find domain type from id
            var domainType = _.find(self.availableDomainTypes, function (domainType) {
                return (domainType.id === policy.domainTypeId);
            });

            // Apply domain type to field
            if ((domainType.field.derivedDataType !== null
                 && (domainType.field.derivedDataType !== policy.columnDef.derivedDataType || domainType.field.precisionScale !== policy.columnDef.precisionScale))
                || (angular.isArray(policy.standardization) && policy.standardization.length > 0)
                || (angular.isArray(policy.columnDef.tags) && policy.columnDef.tags.length > 0)
                || (angular.isArray(policy.validation) && policy.validation.length > 0)) {
                $mdDialog.show({
                    controller: "ApplyDomainTypeDialogController",
                    escapeToClose: false,
                    fullscreen: true,
                    parent: angular.element(document.body),
                    templateUrl: "js/nflow-mgr/shared/apply-domain-type/apply-domain-type-dialog.html",
                    locals: {
                        domainType: domainType,
                        field: policy.columnDef
                    }
                })
                    .then(function () {
                        NflowService.setDomainTypeForField(policy.columnDef, policy, domainType);
                    }, function () {
                        policy.domainTypeId = angular.isDefined(policy.$currentDomainType) ? policy.$currentDomainType.id : null;
                    });
            } else {
                NflowService.setDomainTypeForField(policy.columnDef, policy, domainType);
            }
        };

        /**
         * Shows the Edit Field dialog for the specified field.
         *
         * @param {Object} field the field to edit
         */
        self.showEditFieldDialog = function (field) {
            $mdDialog.show({
                controller: "EditFieldDialogController",
                escapeToClose: false,
                fullscreen: true,
                parent: angular.element(document.body),
                templateUrl: "js/nflow-mgr/nflows/edit-nflow/nflow-details-edit-field-dialog.html",
                locals: {
                    field: field
                }
            }).then(function() {
                field.$edited = true;
            });
        };

        //Apply the entity access permissions
        $q.when(AccessControlService.hasPermission(AccessControlService.NFLOWS_EDIT, self.model, AccessControlService.ENTITY_ACCESS.NFLOW.EDIT_NFLOW_DETAILS)).then(function (access) {
            self.allowEdit = access && !self.model.view.dataPolicies.disabled
        });
    };

    /**
     * Controls the Edit Field dialog.
     * @constructor
     */
    var EditFieldDialogController = function ($scope, $mdDialog, NflowTagService, field) {

        /**
         * Provides a list of available tags.
         * @type {NflowTagService}
         */
        $scope.nflowTagService = NflowTagService;

        /**
         * The field to edit.
         * @type {Object}
         */
        $scope.field = field;
        if (!angular.isArray(field.tags)) {
            field.tags = [];
        }

        /**
         * Metadata for the tag.
         * @type {{searchText: null, selectedItem: null}}
         */
        $scope.tagChips = {searchText: null, selectedItem: null};

        /**
         * Closes and rejects the dialog.
         */
        $scope.cancel = function () {
            $mdDialog.cancel();
        };

        /**
         * Closes and accepts the dialog.
         */
        $scope.hide = function () {
            $mdDialog.hide();
        };

        /**
         * Transforms the specified chip into a tag.
         * @param {string} chip the chip
         * @returns {Object} the tag
         */
        $scope.transformChip = function (chip) {
            return angular.isObject(chip) ? chip : {name: chip};
        };
    };

    angular.module(moduleName)
        .controller('NflowDataPoliciesController', ["$scope", "$mdDialog", "$timeout", "$q", "$compile", "$sce", "AccessControlService", "EntityAccessControlService", "NflowService", "StateService",
                                                   "NflowFieldPolicyRuleService", "DomainTypesService", controller])
        .controller("EditFieldDialogController", ["$scope", "$mdDialog", "NflowTagService", "field", EditFieldDialogController])
        .directive('onescorpinNflowDataPolicies', directive);
});
