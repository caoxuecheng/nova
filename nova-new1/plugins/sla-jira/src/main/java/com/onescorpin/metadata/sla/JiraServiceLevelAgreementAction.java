package com.onescorpin.metadata.sla;

/*-
 * #%L
 * onescorpin-sla-jira
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

import com.onescorpin.alerts.api.Alert;
import com.onescorpin.classnameregistry.ClassNameChange;
import com.onescorpin.jira.JiraClient;
import com.onescorpin.jira.JiraException;
import com.onescorpin.jira.domain.Issue;
import com.onescorpin.jira.domain.IssueBuilder;
import com.onescorpin.metadata.sla.alerts.ServiceLevelAssessmentAlertUtil;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreementAction;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreementActionValidation;
import com.onescorpin.metadata.sla.api.ServiceLevelAssessment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * create the JIRA issue when  SLA is violated
 */
@ClassNameChange(classNames = {"com.onescorpin.metadata.sla.alerts.JiraServiceLevelAgreementAction"})
public class JiraServiceLevelAgreementAction implements ServiceLevelAgreementAction<JiraServiceLevelAgreementActionConfiguration> {

    private static final Logger log = LoggerFactory.getLogger(JiraServiceLevelAgreementAction.class);
    @Inject
    private JiraClient jiraClient;

    public void setJiraClient(JiraClient jiraClient) {
        this.jiraClient = jiraClient;
    }

    @Override
    public boolean respond(JiraServiceLevelAgreementActionConfiguration actionConfiguration, ServiceLevelAssessment assessment, Alert a) {
        String desc = ServiceLevelAssessmentAlertUtil.getDescription(assessment);
        String projectKey = actionConfiguration.getProjectKey();
        String issueType = actionConfiguration.getIssueType();
        String assignee = actionConfiguration.getAssignee();

        if (jiraClient.isHostConfigured()) {
            Issue issue = new IssueBuilder(projectKey, issueType)
                .setAssignee(assignee)
                .setDescription(desc)
                .setSummary("JIRA for " + assessment.getAgreement().getName())
                .build();
            log.info("Generating Jira issue: \"{}\"", issue.getSummary());
            log.debug("Jira description: {}", issue.getDescription());
            try {
                jiraClient.createIssue(issue);
            } catch (JiraException e) {
                log.error("Unable to create Jira Issue ", e);
            }
        } else {
            log.debug("Jira is not configured.  Issue will not be generated: \"{}\"", desc);
        }
        return true;
    }

    public ServiceLevelAgreementActionValidation validateConfiguration() {
        boolean configured = jiraClient.isHostConfigured();
        if (configured) {
            return ServiceLevelAgreementActionValidation.VALID;
        } else {
            return new ServiceLevelAgreementActionValidation(false, "JIRA connection information is not setup.  Please contact an administrator to set this up.");
        }
    }

}
