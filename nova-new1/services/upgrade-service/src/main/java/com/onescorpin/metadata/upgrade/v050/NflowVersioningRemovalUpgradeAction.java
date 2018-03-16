/**
 * 
 */
package com.onescorpin.metadata.upgrade.v050;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/*-
 * #%L
 * nova-upgrade-service
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

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.onescorpin.NovaVersion;
import com.onescorpin.metadata.modeshape.JcrMetadataAccess;
import com.onescorpin.metadata.modeshape.support.JcrUtil;
import com.onescorpin.metadata.modeshape.support.JcrVersionUtil;
import com.onescorpin.server.upgrade.NovaUpgrader;
import com.onescorpin.server.upgrade.UpgradeException;
import com.onescorpin.server.upgrade.UpgradeState;

/**
 * Adds the services-level permissions for the nflow manager.
 */
@Component("nflowVersioningRemovalUpgradeAction050")
@Profile(NovaUpgrader.NOVA_UPGRADE)
public class NflowVersioningRemovalUpgradeAction implements UpgradeState {

    private static final Logger log = LoggerFactory.getLogger(NflowVersioningRemovalUpgradeAction.class);

    @Override
    public boolean isTargetVersion(NovaVersion version) {
        return version.matches("0.5", "0", "");
    }

    @Override
    public void upgradeTo(NovaVersion startingVersion) {
        log.info("Removing nflow versioning: {}", startingVersion);
        
        Session session = JcrMetadataAccess.getActiveSession();
        try {
            Node nflowsNode = session.getRootNode().getNode("metadata/nflows");

            NodeTypeManager typeMgr = (NodeTypeManager) session.getWorkspace().getNodeTypeManager();
            NodeType currentNflowType = typeMgr.getNodeType("tba:nflow");
            List<String> currentSupertypes = Arrays.asList(currentNflowType.getDeclaredSupertypeNames());

            if (currentSupertypes.contains("mix:versionable")) {
                log.info("Removing versionable nflow type {} ", currentNflowType);
                // Remove nflow version history
                for (Node catNode : JcrUtil.getNodesOfType(nflowsNode, "tba:category")) {
                    for (Node nflowNode : JcrUtil.getNodesOfType(catNode, "tba:nflow")) {
                        log.debug("Removing prior versions of nflow: {}.{}", catNode.getName(), nflowNode.getName());
                        if (JcrVersionUtil.isVersionable(nflowNode)) {
                            VersionManager versionManager = session.getWorkspace().getVersionManager();
                            VersionHistory versionHistory = versionManager.getVersionHistory(nflowNode.getPath());
                            VersionIterator vIt = versionHistory.getAllVersions();
                            int count = 0;
                            String last = "";

                            while (vIt.hasNext()) {
                                Version version = vIt.nextVersion();
                                String versionName = version.getName();
                                String baseVersion = "";
                                if (!"jcr:rootVersion".equals(versionName)) {
                                    //baseVersion requires actual versionable node to get the base version name
                                    baseVersion = JcrVersionUtil.getBaseVersion(nflowNode).getName();
                                }
                                if (!"jcr:rootVersion".equals(versionName) && !versionName.equalsIgnoreCase(baseVersion)) {
                                    last = version.getName();
                                    // removeVersion writes directly to workspace, no session.save is necessary
                                    versionHistory.removeVersion(version.getName());
                                    count++;
                                }
                            }

                            if (count > 0) {
                                log.info("Removed {} versions through {} of nflow {}", count, last, nflowNode.getName());
                            } else {
                                log.debug("Nflow {} had no versions", nflowNode.getName());
                            }
                        }
                    }
                }

                // Redefine the NodeType of tba:nflow to remove versionable but retain the versionable properties with weaker constraints
                // Retaining the properties seems to override some residual properties on nflow nodes that causes a failure later.
                // In particular, jcr:predecessors was accessed later but redefining all mix:versionable properties to be safe.
                NodeTypeTemplate template = typeMgr.createNodeTypeTemplate(currentNflowType);
                List<String> newSupertypes = currentSupertypes.stream().filter(type -> !type.equals("mix:versionable")).collect(Collectors.toList());

                template.setDeclaredSuperTypeNames(newSupertypes.toArray(new String[newSupertypes.size()]));

                @SuppressWarnings("unchecked")
                List<PropertyDefinitionTemplate> propTemplates = template.getPropertyDefinitionTemplates();
                PropertyDefinitionTemplate prop = typeMgr.createPropertyDefinitionTemplate();
                prop.setName("jcr:versionHistory");
                prop.setRequiredType(PropertyType.WEAKREFERENCE);
                propTemplates.add(prop);
                prop = typeMgr.createPropertyDefinitionTemplate();
                prop.setName("jcr:baseVersion");
                prop.setRequiredType(PropertyType.WEAKREFERENCE);
                propTemplates.add(prop);
                prop = typeMgr.createPropertyDefinitionTemplate();
                prop.setName("jcr:predecessors");
                prop.setRequiredType(PropertyType.WEAKREFERENCE);
                prop.setMultiple(true);
                propTemplates.add(prop);
                prop = typeMgr.createPropertyDefinitionTemplate();
                prop.setName("jcr:mergeFailed");
                prop.setRequiredType(PropertyType.WEAKREFERENCE);
                propTemplates.add(prop);
                prop = typeMgr.createPropertyDefinitionTemplate();
                prop.setName("jcr:activity");
                prop.setRequiredType(PropertyType.WEAKREFERENCE);
                propTemplates.add(prop);
                prop = typeMgr.createPropertyDefinitionTemplate();
                prop.setName("jcr:configuration");
                prop.setRequiredType(PropertyType.WEAKREFERENCE);
                propTemplates.add(prop);

                log.info("Replacing the versionable nflow type '{}' with a non-versionable type", currentNflowType);
                NodeType newType = typeMgr.registerNodeType(template, true);
                log.info("Replaced with new nflow type '{}' with a non-versionable type", newType);

                // This step may not be necessary.
                for (Node catNode : JcrUtil.getNodesOfType(nflowsNode, "tba:category")) {
                    for (Node nflowNode : JcrUtil.getNodesOfType(catNode, "tba:nflow")) {
                        nflowNode.setPrimaryType(newType.getName());
                        // log.info("Replaced type of node {}", nflowNode);

                        if (nflowNode.hasProperty("jcr:predecessors")) {
                            nflowNode.getProperty("jcr:predecessors").setValue(new Value[0]);
                            ;
                            nflowNode.getProperty("jcr:predecessors").remove();
                        }
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error("Failure while attempting to remove versioning from nflows", e);
            throw new UpgradeException("Failure while attempting to remove versioning from nflows", e);
        }

    }

}
