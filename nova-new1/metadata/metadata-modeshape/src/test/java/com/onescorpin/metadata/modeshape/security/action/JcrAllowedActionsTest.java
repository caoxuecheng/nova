package com.onescorpin.metadata.modeshape.security.action;

/*-
 * #%L
 * onescorpin-metadata-modeshape
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

import com.onescorpin.metadata.modeshape.JcrMetadataAccess;
import com.onescorpin.metadata.modeshape.JcrTestConfig;
import com.onescorpin.metadata.modeshape.ModeShapeEngineConfig;
import com.onescorpin.metadata.modeshape.TestCredentials;
import com.onescorpin.metadata.modeshape.TestUserPrincipal;
import com.onescorpin.metadata.modeshape.security.AdminCredentials;
import com.onescorpin.security.GroupPrincipal;
import com.onescorpin.security.action.AllowedActions;
import com.onescorpin.security.action.AllowedEntityActionsProvider;

import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.security.AccessControlException;
import java.security.Principal;
import java.util.Optional;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@SpringApplicationConfiguration(classes = {ModeShapeEngineConfig.class, JcrTestConfig.class, TestSecurityConfig.class})
public class JcrAllowedActionsTest extends AbstractTestNGSpringContextTests {
    
    @Inject
    private JcrMetadataAccess metadata;

    @Inject
    private AllowedEntityActionsProvider provider;

//    @BeforeClass
//    public void print() {
//        this.metadata.read(new AdminCredentials(), () -> {
//            StringWriter sw = new StringWriter();
//            PrintWriter pw = new PrintWriter(sw);
//    
//            JcrTool tool = new JcrTool(true, pw);
//            tool.printSubgraph(JcrMetadataAccess.getActiveSession(), "/metadata/security/prototypes");
//            pw.flush();
//            String result = sw.toString();
//            System.out.println(result);
//        });
//    }

    @Test
    public void testAdminGetAvailable() throws Exception {
        this.metadata.read(() -> {
            Optional<AllowedActions> option = this.provider.getAvailableActions(AllowedActions.SERVICES);

            assertThat(option.isPresent()).isTrue();

            AllowedActions actions = option.get(); // Throws exception on failure

            actions.checkPermission(TestSecurityConfig.EXPORT_NFLOWS);
        }, TestSecurityConfig.ADMIN);
    }

    @Test
    public void testTestGetAvailable() throws Exception {
        this.metadata.read(() -> {
            Optional<AllowedActions> option = this.provider.getAvailableActions(AllowedActions.SERVICES);

            assertThat(option.isPresent()).isTrue();

            option.get().checkPermission(TestSecurityConfig.EXPORT_NFLOWS); // Throws exception on failure
        }, TestSecurityConfig.TEST);
    }

    @Test(dependsOnMethods = "testAdminGetAvailable")
    public void testAdminGetAllowed() throws Exception {
        this.metadata.read(() -> {
            Optional<AllowedActions> option = this.provider.getAllowedActions(AllowedActions.SERVICES);

            assertThat(option.isPresent()).isTrue();

            AllowedActions actions = option.get(); // Throws exception on failure

            actions.checkPermission(TestSecurityConfig.EXPORT_NFLOWS);
        }, TestSecurityConfig.ADMIN);
    }

    @Test(dependsOnMethods = "testTestGetAvailable", expectedExceptions = AccessControlException.class)
    public void testTestGetAllowed() throws Exception {
        this.metadata.read(() -> {
            Optional<AllowedActions> option = this.provider.getAllowedActions(AllowedActions.SERVICES);

            assertThat(option.isPresent()).isTrue();

            option.get().checkPermission(TestSecurityConfig.EXPORT_NFLOWS);
        }, TestSecurityConfig.TEST);
    }

    @Test(dependsOnMethods = {"testAdminGetAllowed", "testTestGetAllowed"})
    public void testEnableExport() {
        boolean changed = this.metadata.commit(() -> {
            Optional<AllowedActions> option = this.provider.getAllowedActions(AllowedActions.SERVICES);

            assertThat(option.isPresent()).isTrue();

            return option.get().enable(new TestUserPrincipal(), TestSecurityConfig.EXPORT_NFLOWS);
        }, TestSecurityConfig.ADMIN);

        assertThat(changed).isTrue();

        boolean passed = this.metadata.read(() -> {
            Optional<AllowedActions> option = this.provider.getAllowedActions(AllowedActions.SERVICES);

            assertThat(option.isPresent()).isTrue();

            option.get().checkPermission(TestSecurityConfig.EXPORT_NFLOWS);
            return true;
        }, TestSecurityConfig.TEST);

        assertThat(passed).isTrue();
    }

    @Test(dependsOnMethods = "testEnableExport", expectedExceptions = AccessControlException.class)
    public void testDisableExport() {
        boolean changed = this.metadata.commit(() -> {
            Optional<AllowedActions> option = this.provider.getAllowedActions(AllowedActions.SERVICES);

            assertThat(option.isPresent()).isTrue();

            return option.get().disable(new TestUserPrincipal(), TestSecurityConfig.EXPORT_NFLOWS);
        }, TestSecurityConfig.ADMIN);

        assertThat(changed).isTrue();

        this.metadata.read(() -> {
            Optional<AllowedActions> option = this.provider.getAllowedActions(AllowedActions.SERVICES);

            assertThat(option.isPresent()).isTrue();

            option.get().checkPermission(TestSecurityConfig.EXPORT_NFLOWS);
        }, TestSecurityConfig.TEST);
    }

    @Test(dependsOnMethods = "testDisableExport", expectedExceptions = AccessControlException.class)
    public void testEnableOnlyCreate() {
        boolean changed = this.metadata.commit(() -> {
            Optional<AllowedActions> option = this.provider.getAllowedActions(AllowedActions.SERVICES);

            assertThat(option.isPresent()).isTrue();

            option.get().enable(new TestUserPrincipal(), TestSecurityConfig.EXPORT_NFLOWS);
            return option.get().enableOnly(new TestUserPrincipal(), TestSecurityConfig.CREATE_NFLOWS);
        }, TestSecurityConfig.ADMIN);

        assertThat(changed).isTrue();

        this.metadata.read(() -> {
            Optional<AllowedActions> option = this.provider.getAllowedActions(AllowedActions.SERVICES);

            assertThat(option.isPresent()).isTrue();

            try {
                option.get().checkPermission(TestSecurityConfig.CREATE_NFLOWS);
            } catch (Exception e) {
                Assert.fail("Permission check should pass", e);
            }

            option.get().checkPermission(TestSecurityConfig.EXPORT_NFLOWS);
        }, TestSecurityConfig.TEST);
    }
}
