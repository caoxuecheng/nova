package com.onescorpin.integration.sla;

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

import com.onescorpin.alerts.rest.model.Alert;
import com.onescorpin.alerts.rest.model.AlertRange;
import com.onescorpin.nflowmgr.rest.model.NflowMetadata;
import com.onescorpin.nflowmgr.sla.ServiceLevelAgreementGroup;
import com.onescorpin.integration.IntegrationTestBase;
import com.onescorpin.metadata.rest.model.sla.ObligationAssessment;
import com.onescorpin.metadata.rest.model.sla.ServiceLevelAssessment;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static com.onescorpin.metadata.rest.model.sla.ServiceLevelAssessment.Result.FAILURE;


/**
 * Creates a nflow, creates two SLAs for the nflow, which are expected to succeed and to fail,
 * triggers SLA assessments, asserts SLA assessment results, asserts SLA failures appear in Alerts
 */
public class SlaIT extends IntegrationTestBase {

    private static final String TEST_FILE = "sla-assessment.txt";
    private static final String HTTP_NOVA_IO_ALERT_ALERT_SLA_VIOLATION = "http://nova.io/alert/alert/sla/violation";

    @Test
    public void testSla() throws IOException {
        copyDataToDropzone(TEST_FILE);

        LocalDateTime now = LocalDateTime.now();
        String systemName = now.format(DateTimeFormatter.ofPattern("HH_mm_ss_SSS"));
        NflowMetadata response = createSimpleNflow("sla_" + systemName, TEST_FILE);

        waitForNflowToComplete();

        ServiceLevelAgreementGroup oneHourAgoSla = createOneHourAgoNflowProcessingDeadlineSla(response.getCategoryAndNflowName(), response.getNflowId());
        triggerSla(oneHourAgoSla.getName());
        assertSLA(oneHourAgoSla.getId(), FAILURE);
        assertFilterByFailuresContains(oneHourAgoSla.getId());
        assertFilterBySuccessContainsNot(oneHourAgoSla.getId());
        assertFailedSlaAppearsInAlerts(oneHourAgoSla.getId());

        ServiceLevelAgreementGroup oneHourAheadSla = createOneHourAheadNflowProcessingDeadlineSla(response.getCategoryAndNflowName(), response.getNflowId());
        triggerSla(oneHourAheadSla.getName());
        assertFilterByFailuresContainsNot(oneHourAheadSla.getId());
        assertFilterBySuccessContains(oneHourAheadSla.getId());
        assertSuccessfulSlaAppearsNotInAlerts(oneHourAheadSla.getId());

        deleteExistingSla();
    }

    @Override
    public void startClean() {
        super.startClean();
    }

//    @Test
    public void temp() {
        deleteExistingSla();
    }

    private void assertFailedSlaAppearsInAlerts(String slaId) {
        AlertRange range = getAlerts();
        List<Alert> alerts = range.getAlerts();
        Assert.assertTrue(anyAlertMatch(slaId, alerts));
    }

    private void assertSuccessfulSlaAppearsNotInAlerts(String slaId) {
        AlertRange range = getAlerts();
        List<Alert> alerts = range.getAlerts();
        Assert.assertFalse(anyAlertMatch(slaId, alerts));
    }

    private boolean anyAlertMatch(String slaId, List<Alert> alerts) {
        return alerts.stream().anyMatch(alert -> slaId.equals(alert.getEntityId()) && HTTP_NOVA_IO_ALERT_ALERT_SLA_VIOLATION.equals(alert.getType().toString()));
    }

    private void assertFilterBySuccessContainsNot(String slaId) {
        ServiceLevelAssessment[] array = getServiceLevelAssessments(FILTER_BY_SUCCESS);
        Assert.assertFalse(anySlaMatch(slaId, array));
    }

    private void assertFilterBySuccessContains(String slaId) {
        ServiceLevelAssessment[] array = getServiceLevelAssessments(FILTER_BY_SUCCESS);
        Assert.assertTrue(anySlaMatch(slaId, array));
    }

    private void assertFilterByFailuresContains(String slaId) {
        ServiceLevelAssessment[] array = getServiceLevelAssessments(FILTER_BY_FAILURE);
        Assert.assertTrue(anySlaMatch(slaId, array));
    }

    private void assertFilterByFailuresContainsNot(String slaId) {
        ServiceLevelAssessment[] array = getServiceLevelAssessments(FILTER_BY_FAILURE);
        Assert.assertFalse(anySlaMatch(slaId, array));
    }

    private boolean anySlaMatch(String slaId, ServiceLevelAssessment[] array) {
        return Arrays.stream(array).anyMatch(assessment -> slaId.equals(assessment.getAgreement().getId()));
    }

    private void assertSLA(String slaId, ServiceLevelAssessment.Result status) {
        ServiceLevelAssessment[] array = getServiceLevelAssessments(FILTER_BY_SLA_ID + slaId);
        for (ServiceLevelAssessment sla : array) {
            List<ObligationAssessment> list = sla.getObligationAssessments();
            for (ObligationAssessment assessment : list) {
                Assert.assertEquals(status, assessment.getResult());
            }
        }
    }
}
