/**
 * Include all common services/directives are are used for the nova.nflowmgr module
 */
define(['angular',
        'nflow-mgr/services/VisualQueryService',
        'nflow-mgr/services/RestUrlService',
        'nflow-mgr/services/NflowCreationErrorService',
        'nflow-mgr/services/NflowService',
        'nflow-mgr/services/RegisterTemplateService',
        'nflow-mgr/services/CategoriesService',
        'nflow-mgr/services/NflowSecurityGroupsService',
        'nflow-mgr/services/NflowInputProcessorPropertiesTemplateFactory',
        'nflow-mgr/services/NflowDetailsProcessorRenderingHelper',
        'nflow-mgr/services/ImportService',
        'nflow-mgr/services/SlaService',
        'nflow-mgr/services/NflowPropertyService',
        'nflow-mgr/shared/policy-input-form/policy-input-form',
        'nflow-mgr/shared/policy-input-form/PolicyInputFormService',
        'nflow-mgr/services/HiveService',
        'nflow-mgr/shared/hql-editor/hql-editor',
        'nflow-mgr/services/DBCPTableSchemaService',
        'nflow-mgr/services/EditNflowNifiPropertiesService',
        'nflow-mgr/services/NflowTagService',
        'nflow-mgr/shared/properties-admin/properties-admin',
        'nflow-mgr/shared/property-list/property-list',
        'nflow-mgr/shared/nflow-field-policy-rules/NflowFieldPolicyRuleDialog',
        'nflow-mgr/shared/nflow-field-policy-rules/inline-field-policy-form',
        'nflow-mgr/shared/nifi-property-input/nifi-property-timunit-input',
        'nflow-mgr/shared/nifi-property-input/nifi-property-input',
        'nflow-mgr/shared/cron-expression-validator/cron-expression-validator',
        'nflow-mgr/shared/cron-expression-preview/cron-expression-preview',
        'nflow-mgr/services/DatasourcesService',
        'nflow-mgr/shared/entity-access-control/entity-access',
        'nflow-mgr/shared/entity-access-control/EntityAccessControlService',
        'nflow-mgr/shared/profile-stats/ProfileStats',
        'nflow-mgr/services/UiComponentsService',
        'nflow-mgr/services/DomainTypesService',
        'nflow-mgr/shared/apply-domain-type/ApplyDomainTypeDialog'],function() {

});



