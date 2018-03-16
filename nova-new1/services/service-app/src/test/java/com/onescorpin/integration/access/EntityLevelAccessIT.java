package com.onescorpin.integration.access;

/*-
 * #%L
 * nova-service-app
 * %%
 * Copyright (C) 2017 Onescorpin
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.jayway.restassured.response.Response;
import com.onescorpin.nflowmgr.rest.controller.NflowCategoryRestController;
import com.onescorpin.nflowmgr.rest.model.NflowCategory;
import com.onescorpin.nflowmgr.rest.model.NflowMetadata;
import com.onescorpin.nflowmgr.rest.model.NflowSummary;
import com.onescorpin.nflowmgr.rest.model.NifiNflow;
import com.onescorpin.nflowmgr.rest.model.RegisteredTemplate;
import com.onescorpin.nflowmgr.security.NflowServicesAccessControl;
import com.onescorpin.nflowmgr.service.template.ExportImportTemplateService;
import com.onescorpin.integration.IntegrationTestBase;
import com.onescorpin.rest.model.RestResponseStatus;
import com.onescorpin.security.rest.model.Action;
import com.onescorpin.security.rest.model.ActionGroup;
import com.onescorpin.security.rest.model.PermissionsChange;
import com.onescorpin.security.rest.model.RoleMembership;
import com.onescorpin.security.rest.model.RoleMembershipChange;
import com.onescorpin.security.rest.model.UserGroup;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.onescorpin.integration.UserContext.User.ADMIN;
import static com.onescorpin.integration.UserContext.User.ANALYST;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;

/**
 * Asserts that Category, Template and Nflows are only accessible when given permission to do so.
 */
public class EntityLevelAccessIT extends IntegrationTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(EntityLevelAccessIT.class);

    private static final String GROUP_ANALYSTS = "analysts";
    private static final String SERVICES = "services";
    private static final String PERMISSION_READ_ONLY = "readOnly";
    private static final String PERMISSION_EDITOR = "editor";
    private static final String PERMISSION_ADMIN = "admin";
    private static final String PERMISSION_NFLOW_CREATOR = "nflowCreator";

    private static final String NFLOW_EDIT_FORBIDDEN = "Error saving Nflow Not authorized to perform the action: Edit Nflows";
    private static final String NFLOW_NOT_FOUND = "Error saving Nflow Nflow not found for id";
    private static final String TEST_FILE = "access.txt";

    private NflowCategory category;
    private ExportImportTemplateService.ImportTemplate template;
    private NflowMetadata nflow;

    @Test
    public void test() {
        createCategoryWithAdmin();
        assertAnalystCantAccessCategories();
//        assertCategoryNotEditableForAnalyst();

        grantAccessCategoriesToAnalysts();
        assertAnalystCanAccessCategoriesButCantSeeCategory();

//        grantCategoryEntityPermissionsToAnalysts();
//        assertCategoryIsVisibleToAnalyst();

        createTemplateWithAdmin();
        assertAnalystCantAccessTemplates();

        grantAccessTemplatesToAnalysts();
        assertAnalystCanAccessTemplatesButCantSeeTemplate();

//        grantTemplateEntityPermissionsToAnalysts();
//        assertTemplateIsVisibleToAnalyst();

        createNflowWithAdmin();
        assertAnalystCantAccessNflows();
        assertAnalystCantEditNflow(NFLOW_EDIT_FORBIDDEN);
        assertAnalystCantExportNflow(HTTP_FORBIDDEN);
        assertAnalystCantDisableEnableNflow(HTTP_FORBIDDEN);
        assertAnalystCantEditNflowPermissions(HTTP_FORBIDDEN);
        assertAnalystCantDeleteNflow(HTTP_FORBIDDEN);
//        assertAnalystCantAccessNflowOperations(HTTP_FORBIDDEN); //todo here and everywhere below

        grantAccessNflowsToAnalysts();
        assertAnalystCanAccessNflowsButCantSeeNflow();
        assertAnalystCantEditNflow(NFLOW_EDIT_FORBIDDEN);
        assertAnalystCantExportNflow(HTTP_FORBIDDEN);
        assertAnalystCantDisableEnableNflow(HTTP_FORBIDDEN);
        assertAnalystCantEditNflowPermissions(HTTP_NOT_FOUND);
        assertAnalystCantDeleteNflow(HTTP_FORBIDDEN);

        grantNflowEntityPermissionToAnalysts(PERMISSION_READ_ONLY);
        assertAnalystCanSeeNflow();
        assertAnalystCantEditNflow(NFLOW_EDIT_FORBIDDEN);
        assertAnalystCantExportNflow(HTTP_FORBIDDEN);
        assertAnalystCantDisableEnableNflow(HTTP_FORBIDDEN);
        assertAnalystCantEditNflowPermissions(HTTP_FORBIDDEN);
        assertAnalystCantDeleteNflow(HTTP_FORBIDDEN);

        grantNflowEntityPermissionToAnalysts(PERMISSION_EDITOR);
        assertAnalystCanSeeNflow();
        assertAnalystCantEditNflow(NFLOW_EDIT_FORBIDDEN); //cant edit nflow until required service permissions are added for nflow, category, template and entity access to category
        grantEditNflowsToAnalysts();
        grantCategoryEntityPermissionToAnalysts(PERMISSION_NFLOW_CREATOR);
        assertAnalystCanEditNflow();
        assertAnalystCanDisableEnableNflow();
        assertAnalystCantExportNflow(HTTP_FORBIDDEN);
        grantTemplateAndNflowExportToAnalysts();
        assertAnalystCanExportNflow();
        assertAnalystCantEditNflowPermissions(HTTP_FORBIDDEN);
        assertAnalystCantDeleteNflow(HTTP_FORBIDDEN);

        grantNflowEntityPermissionToAnalysts(PERMISSION_ADMIN);
        assertAnalystCanSeeNflow();
        assertAnalystCanEditNflow();
        assertAnalystCanExportNflow();
        assertAnalystCanDisableEnableNflow();

        grantAdminNflowsToAnalysts();
        assertAnalystCanEditNflowPermissions();

        revokeNflowEntityPermissionsFromAnalysts();
        assertAnalystCanAccessNflowsButCantSeeNflow();
        assertAnalystCantEditNflow(NFLOW_NOT_FOUND);
        assertAnalystCantExportNflow(HTTP_NOT_FOUND);
        assertAnalystCantDisableEnableNflow(HTTP_NOT_FOUND);
        assertAnalystCantEditNflowPermissions(HTTP_NOT_FOUND);
        assertAnalystCantDeleteNflow(HTTP_NOT_FOUND);

        grantNflowEntityPermissionToAnalysts(PERMISSION_ADMIN);
        grantCategoryEntityPermissionToAnalysts(PERMISSION_EDITOR); //to delete a nflow one has to have an Editor permission to the category too
        grantAdminNflowsToAnalysts();
        assertAnalystCanDeleteNflow();

        resetServicePermissionsForAnalysts();
        assertAnalystCantAccessCategories();
        assertAnalystCantAccessTemplates();
        assertAnalystCantAccessNflows();
    }

    @Override
    protected void cleanup() {
        runAs(ADMIN);

        super.cleanup();
        resetServicePermissionsForAnalysts();
    }

//    @Test
    public void temp() {
//        category = new NflowCategory();
//        category.setId("67d5fd01-096d-41bf-a0b0-e0a0fe8d4587");
//        category.setSystemName("entity_access_tests");
//
//        ingestTemplate = new ExportImportTemplateService.ImportTemplate();
//        ingestTemplate.setTemplateId("57ca6102-39bc-42d7-9eff-754663fc4f4b");
//        ingestTemplate.setTemplateName("Data Ingest");
//
//        nflow = new NflowMetadata();
//        nflow.setId("c39209c8-cc50-421e-a5de-25e93cf22c5d");
//        nflow.setNflowId("c39209c8-cc50-421e-a5de-25e93cf22c5d");
//        nflow.setNflowName("Nflow A");

    }


    private void assertAnalystCantDeleteNflow(int failureStatusCode) {
        LOG.debug("EntityLevelAccessIT.assertAnalystCantDeleteNflow");

        runAs(ANALYST);
        deleteNflowExpecting(nflow.getNflowId(), failureStatusCode);
    }

    private void assertAnalystCanDeleteNflow() {
        LOG.debug("EntityLevelAccessIT.assertAnalystCanDeleteNflow");

        runAs(ANALYST);
        disableNflow(nflow.getNflowId());
        deleteNflow(nflow.getNflowId());

        NflowSummary[] nflows = getNflows();
        Assert.assertEquals(0, nflows.length);
    }

    private void assertAnalystCantEditNflowPermissions(int status) {
        LOG.debug("EntityLevelAccessIT.assertAnalystCantEditNflowPermissions");

        runAs(ANALYST);
        RoleMembershipChange roleChange = new RoleMembershipChange(RoleMembershipChange.ChangeType.REPLACE, PERMISSION_ADMIN);
        roleChange.addGroup(GROUP_ANALYSTS);
        setNflowEntityPermissionsExpectingStatus(roleChange, nflow.getNflowId(), status);
    }

    private void assertAnalystCanEditNflowPermissions() {
        LOG.debug("EntityLevelAccessIT.assertAnalystCanEditNflowPermissions");

        runAs(ANALYST);
        RoleMembershipChange roleChange = new RoleMembershipChange(RoleMembershipChange.ChangeType.REPLACE, PERMISSION_ADMIN);
        roleChange.addGroup(GROUP_ANALYSTS);
        setNflowEntityPermissions(roleChange, nflow.getNflowId());
    }

    private void assertAnalystCantDisableEnableNflow(int code) {
        LOG.debug("EntityLevelAccessIT.assertAnalystCantDisableEnableNflow");

        runAs(ANALYST);
        disableNflowExpecting(nflow.getNflowId(), code);
        enableNflowExpecting(nflow.getNflowId(), code);
    }

    private void assertAnalystCanDisableEnableNflow() {
        LOG.debug("EntityLevelAccessIT.assertAnalystCanDisableEnableNflow");

        runAs(ANALYST);
        disableNflow(nflow.getNflowId());
        enableNflow(nflow.getNflowId());
    }

    private void assertAnalystCantExportNflow(int failureStatusCode) {
        LOG.debug("EntityLevelAccessIT.assertAnalystCantExportNflow");

        runAs(ANALYST);
        exportNflowExpecting(nflow.getNflowId(), failureStatusCode);
    }

    private void assertAnalystCanExportNflow() {
        LOG.debug("EntityLevelAccessIT.assertAnalystCanExportNflow");

        runAs(ANALYST);
        exportNflow(nflow.getNflowId());
    }

    private void assertAnalystCantEditNflow(String errorMessage) {
        LOG.debug("EntityLevelAccessIT.assertAnalystCantEditNflow");

        runAs(ANALYST);

        NflowMetadata editNflowRequest = getEditNflowRequest();
        NifiNflow nflow = createNflow(editNflowRequest);
        Assert.assertEquals(1, nflow.getErrorMessages().size());
        Assert.assertTrue(nflow.getErrorMessages().get(0).startsWith(errorMessage));

    }

    private void assertAnalystCanEditNflow() {
        LOG.debug("EntityLevelAccessIT.assertAnalystCanEditNflow");

        runAs(ANALYST);

        NflowMetadata editNflowRequest = getEditNflowRequest();
        NifiNflow nflow = createNflow(editNflowRequest);
        Assert.assertTrue(nflow.getErrorMessages() == null);
    }

    private NflowMetadata getEditNflowRequest() {
        NflowMetadata editNflowRequest = makeCreateNflowRequest(category, template, nflow.getNflowName(), TEST_FILE);
        editNflowRequest.setId(nflow.getId());
        editNflowRequest.setNflowId(nflow.getNflowId());
        editNflowRequest.setDescription("New Description");
        editNflowRequest.setIsNew(false);
        return editNflowRequest;
    }

    private void assertAnalystCanAccessNflowsButCantSeeNflow() {
        LOG.debug("EntityLevelAccessIT.assertAnalystCanAccessNflowsButCantSeeNflow");
        runAs(ANALYST);
        NflowSummary[] nflows = getNflows();
        Assert.assertEquals(0, nflows.length);
    }

    private void assertAnalystCanSeeNflow() {
        LOG.debug("EntityLevelAccessIT.assertAnalystCanSeeNflow");
        runAs(ANALYST);
        NflowSummary[] nflows = getNflows();
        Assert.assertEquals(1, nflows.length);
    }

    private void createNflowWithAdmin() {
        LOG.debug("EntityLevelAccessIT.createNflowWithAdmin");
        runAs(ADMIN);
        NflowMetadata nflowRequest = makeCreateNflowRequest(category, template, "Nflow A", TEST_FILE);
        nflow = createNflow(nflowRequest).getNflowMetadata();
    }

    private void assertAnalystCanAccessTemplatesButCantSeeTemplate() {
        LOG.debug("EntityLevelAccessIT.assertAnalystCanAccessTemplatesButCantSeeTemplate");
        runAs(ANALYST);
        RegisteredTemplate[] templates = getTemplates();
        Assert.assertEquals(0, templates.length);
    }

    private void createTemplateWithAdmin() {
        LOG.debug("EntityLevelAccessIT.createTemplateWithAdmin");
        runAs(ADMIN);
        template = importSimpleTemplate();
    }

    private void assertAnalystCanAccessCategoriesButCantSeeCategory() {
        LOG.debug("EntityLevelAccessIT.assertAnalystCanAccessCategoriesButCantSeeCategory");
        runAs(ANALYST);
        NflowCategory[] categories = getCategories();
        Assert.assertEquals(0, categories.length);
    }

    private void grantEditNflowsToAnalysts() {
        LOG.debug("EntityLevelAccessIT.grantEditNflowsToAnalysts");

        runAs(ADMIN);
        Action nflowsSupport = createAction(NflowServicesAccessControl.NFLOWS_SUPPORT);
        Action accessNflows = createAction(NflowServicesAccessControl.ACCESS_NFLOWS);
        accessNflows.addAction(createAction(NflowServicesAccessControl.EDIT_NFLOWS));
        nflowsSupport.addAction(accessNflows);

        grantServiceActionToAnalysts(nflowsSupport);
    }

    private void grantTemplateAndNflowExportToAnalysts() {
        LOG.debug("EntityLevelAccessIT.grantTemplateAndNflowExportToAnalysts");

        runAs(ADMIN);
        Action nflowsSupport = createAction(NflowServicesAccessControl.NFLOWS_SUPPORT);
        Action accessNflows = createAction(NflowServicesAccessControl.ACCESS_NFLOWS);
        accessNflows.addAction(createAction(NflowServicesAccessControl.EXPORT_NFLOWS));

        Action accessTemplates = createAction(NflowServicesAccessControl.ACCESS_TEMPLATES);
        accessTemplates.addAction(createAction(NflowServicesAccessControl.EXPORT_TEMPLATES));
        nflowsSupport.addAction(accessTemplates);
        nflowsSupport.addAction(accessNflows);

        grantServiceActionToAnalysts(nflowsSupport);
    }

    private void grantAdminNflowsToAnalysts() {
        LOG.debug("EntityLevelAccessIT.grantAdminNflowsToAnalysts");
        runAs(ADMIN);
        Action nflowsSupport = createAction(NflowServicesAccessControl.NFLOWS_SUPPORT);
        Action accessNflows = createAction(NflowServicesAccessControl.ACCESS_NFLOWS);
        accessNflows.addAction(createAction(NflowServicesAccessControl.ADMIN_NFLOWS));

        nflowsSupport.addAction(accessNflows);

        grantServiceActionToAnalysts(nflowsSupport);
    }

    private void grantAccessTemplatesToAnalysts() {
        LOG.debug("EntityLevelAccessIT.grantAccessTemplatesToAnalysts");
        runAs(ADMIN);
        Action nflowsSupport = createAction(NflowServicesAccessControl.NFLOWS_SUPPORT);
        nflowsSupport.addAction(createAction(NflowServicesAccessControl.ACCESS_TEMPLATES));
        grantServiceActionToAnalysts(nflowsSupport);
    }

    private void grantAccessCategoriesToAnalysts() {
        LOG.debug("EntityLevelAccessIT.grantAccessCategoriesToAnalysts");
        runAs(ADMIN);
        Action nflowsSupport = createAction(NflowServicesAccessControl.NFLOWS_SUPPORT);
        nflowsSupport.addAction(createAction(NflowServicesAccessControl.ACCESS_CATEGORIES));
        grantServiceActionToAnalysts(nflowsSupport);
    }

    private void grantAccessNflowsToAnalysts() {
        LOG.debug("EntityLevelAccessIT.grantAccessNflowsToAnalysts");
        Action nflowsSupport = createAction(NflowServicesAccessControl.NFLOWS_SUPPORT);
        nflowsSupport.addAction(createAction(NflowServicesAccessControl.ACCESS_NFLOWS));
        grantServiceActionToAnalysts(nflowsSupport);
    }

    private void createCategoryWithAdmin() {
        LOG.debug("EntityLevelAccessIT.createCategoryWithAdmin");
        runAs(ADMIN);
        category = createCategory("Entity Access Tests");
    }

    private void assertAnalystCantAccessCategories() {
        LOG.debug("EntityLevelAccessIT.assertAnalystCantAccessCategories");
        runAs(ANALYST);
        Response response = getCategoriesExpectingStatus(HTTP_FORBIDDEN);
        RestResponseStatus status  = response.as(RestResponseStatus.class);
        Assert.assertEquals("Not authorized to perform the action: Access Categories", status.getMessage());
    }

    private void assertAnalystCantAccessTemplates() {
        LOG.debug("EntityLevelAccessIT.assertAnalystCantAccessTemplates");
        runAs(ANALYST);
        Response response = getTemplatesExpectingStatus(HTTP_FORBIDDEN);
        RestResponseStatus status  = response.as(RestResponseStatus.class);
        Assert.assertEquals("Not authorized to perform the action: Access Templates", status.getMessage());
    }

    private void assertAnalystCantAccessNflows() {
        LOG.debug("EntityLevelAccessIT.assertAnalystCantAccessNflows");
        runAs(ANALYST);
        Response response = getNflowsExpectingStatus(HTTP_FORBIDDEN);
        RestResponseStatus status  = response.as(RestResponseStatus.class);
        Assert.assertEquals("Not authorized to perform the action: Access Nflows", status.getMessage());
    }

    private void grantServiceActionToAnalysts(Action action) {
        LOG.debug("EntityLevelAccessIT.grantServiceActionToAnalysts");
        runAs(ADMIN);
        ActionGroup actions = new ActionGroup(SERVICES);
        actions.addAction(action);
        PermissionsChange permissionsChange = new PermissionsChange(PermissionsChange.ChangeType.REPLACE, actions);
        permissionsChange.addGroup(GROUP_ANALYSTS);

        permissionsChange.union(getServicePermissions(GROUP_ANALYSTS));

        setServicePermissions(permissionsChange);
    }

    private void resetServicePermissionsForAnalysts() {
        LOG.debug("EntityLevelAccessIT.resetServicePermissionsForAnalysts");
        runAs(ADMIN);
        ActionGroup actions = new ActionGroup(SERVICES);
        PermissionsChange permissionsChange = new PermissionsChange(PermissionsChange.ChangeType.REPLACE, actions);
        permissionsChange.addGroup(GROUP_ANALYSTS);
        setServicePermissions(permissionsChange);
    }

    private void grantNflowEntityPermissionToAnalysts(String roleName) {
        LOG.debug("EntityLevelAccessIT.grantNflowEntityPermissionToAnalysts " + roleName);
        runAs(ADMIN);
        RoleMembershipChange roleChange = new RoleMembershipChange(RoleMembershipChange.ChangeType.REPLACE, roleName);
        roleChange.addGroup(GROUP_ANALYSTS);
        setNflowEntityPermissions(roleChange, nflow.getNflowId());
    }

    private void grantCategoryEntityPermissionToAnalysts(String roleName) {
        LOG.debug("EntityLevelAccessIT.grantCategoryEntityPermissionToAnalysts " + roleName);
        runAs(ADMIN);
        RoleMembership roleMembership = category.getRoleMemberships().stream().filter(r -> r.getRole().getSystemName().equalsIgnoreCase(roleName)).findFirst().orElse(null);
        if(roleMembership == null) {
            roleMembership =new RoleMembership(roleName,roleName,roleName);
            category.getRoleMemberships().add(roleMembership);
        }
        roleMembership.addGroup(new UserGroup(GROUP_ANALYSTS));
        Response response = given(NflowCategoryRestController.BASE)
            .body(category)
            .when()
            .post();

        response.then().statusCode(HTTP_OK);
    }

    private void revokeNflowEntityPermissionsFromAnalysts() {
        LOG.debug("EntityLevelAccessIT.revokeNflowEntityPermissionsFromAnalysts");
        runAs(ADMIN);

        RoleMembershipChange roleChange = new RoleMembershipChange(RoleMembershipChange.ChangeType.REPLACE, PERMISSION_READ_ONLY);
        setNflowEntityPermissions(roleChange, nflow.getNflowId());

        roleChange = new RoleMembershipChange(RoleMembershipChange.ChangeType.REPLACE, PERMISSION_EDITOR);
        setNflowEntityPermissions(roleChange, nflow.getNflowId());

        roleChange = new RoleMembershipChange(RoleMembershipChange.ChangeType.REPLACE, PERMISSION_ADMIN);
        setNflowEntityPermissions(roleChange, nflow.getNflowId());
    }

    private static Action createAction(com.onescorpin.security.action.Action nflowsSupport) {
        return new Action(nflowsSupport.getSystemName(), nflowsSupport.getTitle(), nflowsSupport.getDescription());
    }


}
