package com.onescorpin.metadata.upgrade.v080;

/*-
 * #%L
 * nova-operational-metadata-upgrade-service
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

import com.onescorpin.NovaVersion;
import com.onescorpin.metadata.modeshape.JcrMetadataAccess;
import com.onescorpin.metadata.modeshape.nflow.JcrNflow;
import com.onescorpin.metadata.modeshape.support.JcrUtil;
import com.onescorpin.server.upgrade.NovaUpgrader;
import com.onescorpin.server.upgrade.UpgradeException;
import com.onescorpin.server.upgrade.UpgradeState;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

import javax.annotation.Nonnull;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

@Component("upgradeAction080")
@Profile(NovaUpgrader.NOVA_UPGRADE)
public class UpgradeAction implements UpgradeState {

    private static final Logger log = LoggerFactory.getLogger(UpgradeAction.class);

    private static final String CATEGORY_TYPE = "tba:category";
    private static final String NFLOW_TYPE = "tba:nflow";
    private static final String UPGRADABLE_TYPE = "tba:upgradable";

    private static final String CAT_DETAILS_TYPE = "tba:categoryDetails";
    private static final String NFLOW_SUMMARY_TYPE = "tba:nflowSummary";
    private static final String NFLOW_DETAILS_TYPE = "tba:nflowDetails";
    private static final String NFLOW_DATA_TYPE = "tba:nflowData";

    @Override
    public boolean isTargetVersion(NovaVersion version) {
        return version.matches("0.8", "0", "");
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.upgrade.UpgradeState#upgradeFrom(com.onescorpin.metadata.api.app.NovaVersion)
     */
    @Override
    public void upgradeTo(NovaVersion startingVersion) {
        log.info("Upgrading from version: " + startingVersion);

        Session session = JcrMetadataAccess.getActiveSession();
        Node nflowsNode = JcrUtil.getNode(session, "metadata/nflows");

        int categoryCount = 0;
        int categoryNflowCount = 0;
        int totalNflowCount = 0;

        for (Node catNode : JcrUtil.getNodesOfType(nflowsNode, CATEGORY_TYPE)) {
            String catName= "";
            try {
                catName = catNode != null ? catNode.getName() : "";
            }catch (RepositoryException e){
                // its fine to swallow this exception
            }

            log.info("Starting upgrading category: [{}] {}", ++categoryCount, catName);

            categoryNflowCount = 0;
            Node detailsNode = JcrUtil.getOrCreateNode(catNode, "tba:details", CAT_DETAILS_TYPE);
            moveProperty("tba:initialized", catNode, detailsNode);
            moveProperty("tba:securityGroups", catNode, detailsNode);

            for (Node nflowNode : JcrUtil.getNodesOfType(catNode, NFLOW_TYPE)) {
                moveNode(session, nflowNode, detailsNode);
                String nflowName= "";
                try {
                    nflowName = nflowNode != null ? nflowNode.getName() : "";
                }catch (RepositoryException e){
                    // its fine to swallow this exception
                }

                log.info("\tStarting upgrading nflow: [{}] {}", ++categoryNflowCount, nflowName);
                ++totalNflowCount;
                Node nflowSummaryNode = JcrUtil.getOrCreateNode(nflowNode, "tba:summary", NFLOW_SUMMARY_TYPE);
                Node nflowDataNode = JcrUtil.getOrCreateNode(nflowNode, "tba:data", NFLOW_DATA_TYPE);
                addMixin(nflowNode, UPGRADABLE_TYPE);

                moveProperty(JcrNflow.SYSTEM_NAME, nflowNode, nflowSummaryNode);
                moveProperty("tba:category", nflowNode, nflowSummaryNode);
                moveProperty("tba:tags", nflowNode, nflowSummaryNode);
                moveProperty(JcrNflow.TITLE, nflowNode, nflowSummaryNode);
                moveProperty(JcrNflow.DESCRIPTION, nflowNode, nflowSummaryNode);

                if (JcrUtil.hasNode(nflowNode, "tba:properties")) {
                    final Node nflowPropertiesNode = JcrUtil.getNode(nflowNode, "tba:properties");
                    moveNode(session, nflowPropertiesNode, nflowSummaryNode);
                }

                Node nflowDetailsNode = JcrUtil.getOrCreateNode(nflowSummaryNode, "tba:details", NFLOW_DETAILS_TYPE);

                moveProperty("tba:nflowTemplate", nflowNode, nflowDetailsNode);
                moveProperty("tba:slas", nflowNode, nflowDetailsNode);
                moveProperty("tba:dependentNflows", nflowNode, nflowDetailsNode, PropertyType.WEAKREFERENCE);
                moveProperty("tba:usedByNflows", nflowNode, nflowDetailsNode, PropertyType.WEAKREFERENCE);
                moveProperty("tba:json", nflowNode, nflowDetailsNode);

                // Loop is needed because sns is specified for node type
                List<Node> nflowSourceNodes = JcrUtil.getNodeList(nflowNode, "tba:sources");
                for (Node nflowSourceNode : nflowSourceNodes) {
                    moveNode(session, nflowSourceNode, nflowDetailsNode);
                }

                // Loop is needed because sns is specified for node type
                List<Node> nflowDestinationNodes = JcrUtil.getNodeList(nflowNode, "tba:destinations");
                for (Node nflowDestinationNode : nflowDestinationNodes) {
                    moveNode(session, nflowDestinationNode, nflowDetailsNode);
                }

                if (JcrUtil.hasNode(nflowNode, "tba:precondition")) {
                    Node nflowPreconditionNode = JcrUtil.getNode(nflowNode, "tba:precondition");
                    moveNode(session, nflowPreconditionNode, nflowDetailsNode);
                }

                moveProperty("tba:state", nflowNode, nflowDataNode);
                moveProperty("tba:schedulingPeriod", nflowNode, nflowDataNode);
                moveProperty("tba:schedulingStrategy", nflowNode, nflowDataNode);
                moveProperty("tba:securityGroups", nflowNode, nflowDataNode);

                if (JcrUtil.hasNode(nflowNode, "tba:highWaterMarks")) {
                    Node nflowWaterMarksNode = JcrUtil.getNode(nflowNode, "tba:highWaterMarks");
                    moveNode(session, nflowWaterMarksNode, nflowDataNode);
                }

                if (JcrUtil.hasNode(nflowNode, "tba:initialization")) {
                    Node nflowInitializationNode = JcrUtil.getNode(nflowNode, "tba:initialization");
                    moveNode(session, nflowInitializationNode, nflowDataNode);
                }

                removeMixin(nflowNode, UPGRADABLE_TYPE);
                log.info("\tCompleted upgrading nflow: " + nflowName);
            }
            log.info("Completed upgrading category: " + catName);
        }

        // Update templates
        int templateCount = 0;
        final Node templatesNode = JcrUtil.getNode(session, "metadata/templates");

        for (Node templateNode : JcrUtil.getNodesOfType(templatesNode, "tba:nflowTemplate")) {
            String templateName = "";
            try {
                templateName = templateNode != null ? templateNode.getName() : "";
            }catch (RepositoryException e){
                // its fine to swallow this exception
            }

            log.info("Starting upgrading template: [{}] {}", ++templateCount, templateName);
            JcrUtil.getOrCreateNode(templateNode, "tba:allowedActions", "tba:allowedActions");
            log.info("Completed upgrading template: " + templateName);
        }

        log.info("Upgrade complete for {} categories and {} nflows and {} templates", categoryCount, totalNflowCount, templateCount);
    }

    /**
     * Adds the specified mixin to the specified node.
     *
     * @param node      the target node
     * @param mixinName the name of the mixin
     */
    private void addMixin(@Nonnull final Node node, @Nonnull final String mixinName) {
        try {
            node.addMixin(mixinName);
        } catch (final RepositoryException e) {
            throw new UpgradeException("Failed to add mixin " + mixinName + " to node " + node, e);
        }
    }

    private void moveNode(Session session, Node node, Node parentNode) {
        try {
            if ((node != null) && (parentNode != null)) {
                final String srcPath = node.getParent().getPath() + "/" + StringUtils.substringAfterLast(node.getPath(), "/");  // Path may not be accurate if parent node moved recently
                session.move(srcPath, parentNode.getPath() + "/" + node.getName());
            }
        } catch (RepositoryException e) {
            throw new UpgradeException("Failed to moved node " + node + " under parent " + parentNode, e);
        }
    }

    /**
     * move a property from one node to another
     *
     * @param propName     the name of the property to move
     * @param fromNode     the node to move from
     * @param toNode       the node to move to
     * @param propertyType Optional.  This is the new property type, or null if unchanged
     */
    private void moveProperty(String propName, Node fromNode, Node toNode, Integer propertyType) {
        try {
            if ((fromNode != null) && (toNode != null)) {
                if (fromNode.hasProperty(propName)) {
                    Property prop = fromNode.getProperty(propName);

                    if (propertyType == null) {
                        propertyType = prop.getType();
                    }

                    if (propertyType != prop.getType()) {
                        log.info("Property type for {} on Node {} is changing from {} to {} ", propName, fromNode.getName(), prop.getType(), propertyType);
                    }

                    if (prop.isMultiple()) {
                        toNode.setProperty(propName, prop.getValues(), propertyType);
                    } else {
                        toNode.setProperty(propName, prop.getValue(), propertyType);
                    }

                    prop.remove();
                }
            }
        } catch (RepositoryException e) {
            throw new UpgradeException("Failed to moved property " + propName + " from " + fromNode + " to " + toNode, e);
        }
    }


    /**
     * Move a property from one node to another keeping the same property type
     *
     * @param propName the name of the property to move
     * @param fromNode the node to move the property from
     * @param toNode   the new node to move it to
     */
    private void moveProperty(String propName, Node fromNode, Node toNode) {
        moveProperty(propName, fromNode, toNode, null);
    }


    /**
     * Removes the specified mixin from the specified node.
     *
     * @param node      the target node
     * @param mixinName the name of the mixin
     */
    private void removeMixin(@Nonnull final Node node, @Nonnull final String mixinName) {
        try {
            node.removeMixin(mixinName);
        } catch (final RepositoryException e) {
            throw new UpgradeException("Failed to remove mixin " + mixinName + " from node " + node, e);
        }
    }
}
