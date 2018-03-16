package com.onescorpin.metadata.core.sla.nflow;

/*-
 * #%L
 * onescorpin-metadata-core
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

import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.NflowCriteria;
import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.api.op.NflowOperation;
import com.onescorpin.metadata.api.op.NflowOperationsProvider;
import com.onescorpin.metadata.api.sla.NflowExecutedSinceNflow;
import com.onescorpin.metadata.sla.api.AssessmentResult;
import com.onescorpin.metadata.sla.api.Metric;
import com.onescorpin.metadata.sla.api.MetricAssessment;
import com.onescorpin.metadata.sla.spi.MetricAssessmentBuilder;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NflowExecutedSinceNflowAssessorTest {

    private NflowExecutedSinceNflow metric;
    private TestMetricAssessmentBuilder builder;

    @Before
    public void setUp() {
        metric = new NflowExecutedSinceNflow("mainCategory.mainNflow", "triggeredCategory.triggeredNflow");
        builder = new TestMetricAssessmentBuilder();
    }

    /**
     * Use case: <br>
     * - nflow b depends on a <br>
     * - nflow a1 completes
     */
    @Test
    public void testTriggeredNeverRan() throws Exception {
        int triggeredNflowStartTime = -1;
        boolean isTriggeredNflowRunning = false;
        int mainNflowStopTime = 1;
        boolean isMainNflowRunning = false;

        assertResult(AssessmentResult.SUCCESS, triggeredNflowStartTime, isTriggeredNflowRunning, mainNflowStopTime, isMainNflowRunning);
    }

    /**
     * Use case: <br>
     * - nflow c depends on both a and b <br>
     * - nflow c never ran <br>
     * - nflow b never ran <br>
     * - nflow a completes <br>
     * - assessment for both nflow "a" and "b" will be done on nflow "a" completion <br>
     * - this is the assessment scenario for nflow b <br>
     */
    @Test
    public void testMainNeverRan() throws Exception {
        int triggeredNflowStartTime = 1;
        boolean isTriggeredNflowRunning = false;
        int mainNflowStopTime = -1;
        boolean isMainNflowRunning = false;

        assertResult(AssessmentResult.FAILURE, triggeredNflowStartTime, isTriggeredNflowRunning, mainNflowStopTime, isMainNflowRunning);
    }

    /**
     * Use case:  <br>
     * - nflow a1 triggers nflow b1 and nflow b1 completes  <br>
     * - nflow a2 completes <br>
     */
    @Test
    public void testTriggeredStartedBeforeMainStopped() throws Exception {
        int triggeredNflowStartTime = 1;
        boolean isTriggeredNflowRunning = false;
        int mainNflowStopTime = 2;
        boolean isMainNflowRunning = false;

        assertResult(AssessmentResult.SUCCESS, triggeredNflowStartTime, isTriggeredNflowRunning, mainNflowStopTime, isMainNflowRunning);
    }

    /**
     * Use case:  <br>
     * - nflow c depends on both a and b  <br>
     * - nflow a completes <br>
     * - nflow b completes <br>
     * - nflow c is triggered and completes <br>
     * - nflow a completes <br>
     * - this is the case for nflow b assessment on nflow a's completion <br>
     */
    @Test
    public void testTriggeredStartedAfterMainStopped() throws Exception {
        int triggeredNflowStartTime = 2;
        boolean isTriggeredNflowRunning = false;
        int mainNflowStopTime = 1;
        boolean isMainNflowRunning = false;

        assertResult(AssessmentResult.FAILURE, triggeredNflowStartTime, isTriggeredNflowRunning, mainNflowStopTime, isMainNflowRunning);
    }

    /**
     * Use case: <br>
     * - nflow c depends on a and b <br>
     * - nflow a1 completes <br>
     * - nflow b1 completes <br>
     * - nflow c1 is triggered and completes <br>
     * - nflow a2 is started and is still running <br>
     * - nflow b2 completes <br>
     * - nflow b2 is started and is still running <br>
     * - nflow a2 completes <br>
     * - we are now assessing nflow b - c nflow would have started before nflow b stopped and nflow b is still running <br>
     */
    @Test
    public void testTriggeredStartedBeforeMainStoppedAndMainIsRunning() throws Exception {

        int triggeredNflowStartTime = 1;
        boolean isTriggeredNflowRunning = false;
        int mainNflowStopTime = 2;
        boolean isMainNflowRunning = true;

        assertResult(AssessmentResult.SUCCESS, triggeredNflowStartTime, isTriggeredNflowRunning, mainNflowStopTime, isMainNflowRunning);
    }

    /**
     * Use Case:  <br>
     * - nflow c depends on both a and b <br>
     * - nflow a1 completes <br>
     * - nflow b1 completes <br>
     * - nflow c1 triggered <br>
     * - nflow a2 started and is still running <br>
     * - nflow b2 completes <br>
     * - we are now assessing nflow a <br>
     */
    @Test
    public void testTriggeredStartedAfterMainStoppedAndMainIsRunning() throws Exception {
        int triggeredNflowStartTime = 2;
        boolean isTriggeredNflowRunning = false;
        int mainNflowStopTime = 1;
        boolean isMainNflowRunning = true;

        assertResult(AssessmentResult.FAILURE, triggeredNflowStartTime, isTriggeredNflowRunning, mainNflowStopTime, isMainNflowRunning);
    }

    /**
     * Use Case;  <br>
     * - nflow a1 triggered nflow b1 and b1 completes <br>
     * - nflow a2 triggered nflow b2 and b2 is still running <br>
     * - nflow a3 triggered nflow b3 <br>
     */
    @Test
    public void testTriggeredStartedBeforeMainStoppedAndTriggeredIsRunning() throws Exception {
        int triggeredNflowStartTime = 1;
        boolean isTriggeredNflowRunning = true;
        int mainNflowStopTime = 2;
        boolean isMainNflowRunning = false;

        assertResult(AssessmentResult.SUCCESS, triggeredNflowStartTime, isTriggeredNflowRunning, mainNflowStopTime, isMainNflowRunning);
    }

    /**
     * Use Case:  <br>
     * - nflow c depends on both a and b <br>
     * - nflow a1 completes <br>
     * - nflow b1 completes <br>
     * - nflow c1 is triggered and is still running <br>
     * - nflow b2 completes and c1 is still running <br>
     * - we are now assessing a's preconditions <br>
     */
    @Test
    public void testTriggeredStartedAfterMainStoppedAndTriggeredIsRunning() throws Exception {

        int triggeredNflowStartTime = 2;
        boolean isTriggeredNflowRunning = true;
        int mainNflowStopTime = 1;
        boolean isMainNflowRunning = false;

        assertResult(AssessmentResult.FAILURE, triggeredNflowStartTime, isTriggeredNflowRunning, mainNflowStopTime, isMainNflowRunning);
    }

    /**
     * Use Case:  <br>
     * - nflow a1 triggers nflow b1 and b1 is still running <br>
     * - nflow a2 starts and is still running <br>
     * - nflow a3 completes <br>
     */
    @Test
    public void testTriggeredStartedBeforeMainStoppedAndBothTriggeredAndMainAreRunning() throws Exception {

        int triggeredNflowStartTime = 1;
        boolean isTriggeredNflowRunning = true;
        int mainNflowStopTime = 2;
        boolean isMainNflowRunning = true;

        assertResult(AssessmentResult.SUCCESS, triggeredNflowStartTime, isTriggeredNflowRunning, mainNflowStopTime, isMainNflowRunning);
    }

    /**
     * Use Case:  <br>
     * - its possible!  <br>
     * - nflow c depends on both a and b <br>
     * - nflow a1 completes, nflow b1 completes, triggered nflow c1 completes <br>
     * - nflow a2 starts and is running <br>
     * - nflow a3 completes, nflow b2 completes, triggered nflow c2 started and is still running <br>
     * - nflow b3 completes <br>
     * - we are now assessing nflow a where nflow a2 is still running and c2 is still running and c2 started after last a3 <br>
     */
    @Test
    public void testTriggeredStartedAfterMainStoppedAndBothTriggeredAndMainAreRunning() throws Exception {
        int triggeredNflowStartTime = 2;
        boolean isTriggeredNflowRunning = true;
        int mainNflowStopTime = 1;
        boolean isMainNflowRunning = true;

        assertResult(AssessmentResult.FAILURE, triggeredNflowStartTime, isTriggeredNflowRunning, mainNflowStopTime, isMainNflowRunning);
    }

    private void assertResult(AssessmentResult expected, int triggeredNflowStartTime, boolean isTriggeredNflowRunning, int mainNflowStopTime, boolean isMainNflowRunning) {
        NflowExecutedSinceNflowAssessor assessor = setUpAssessor(triggeredNflowStartTime, isTriggeredNflowRunning, mainNflowStopTime, isMainNflowRunning);
        assessor.assess(metric, builder);
        Assert.assertEquals(expected, builder.getResult());
    }

    /**
     * @param triggeredNflowStartTime pass negative value for empty operations list
     * @param mainNflowStopTime       pass negative for empty operations list
     */
    private NflowExecutedSinceNflowAssessor setUpAssessor(int triggeredNflowStartTime, boolean isTriggeredNflowRunning,
                                                        int mainNflowStopTime, boolean isMainNflowRunning) {
        NflowCriteria dummyCriteria = mock(NflowCriteria.class);
        when(dummyCriteria.name(Mockito.anyString())).thenReturn(dummyCriteria);
        when(dummyCriteria.category(Mockito.anyString())).thenReturn(dummyCriteria);

        NflowProvider nflowProvider = mock(NflowProvider.class);
        when(nflowProvider.nflowCriteria()).thenReturn(dummyCriteria);

        List<Nflow> triggeredNflows = new ArrayList<>();
        Nflow triggeredNflow = mock(Nflow.class);
        Nflow.ID triggeredNflowId = mock(Nflow.ID.class);
        when(triggeredNflow.getId()).thenReturn(triggeredNflowId);
        triggeredNflows.add(triggeredNflow);

        List<Nflow> mainNflows = new ArrayList<>();
        Nflow mainNflow = mock(Nflow.class);
        Nflow.ID mainNflowId = mock(Nflow.ID.class);
        when(mainNflow.getId()).thenReturn(mainNflowId);
        mainNflows.add(mainNflow);
        when(nflowProvider.getNflows(dummyCriteria)).thenReturn(mainNflows, triggeredNflows);

        NflowOperationsProvider opsProvider = mock(NflowOperationsProvider.class);

        List<NflowOperation> triggeredNflowOps = new ArrayList<>();
        if (triggeredNflowStartTime > 0) {
            NflowOperation triggeredOp = mock(NflowOperation.class);
            when(triggeredOp.getStartTime()).thenReturn(new DateTime(triggeredNflowStartTime));
            triggeredNflowOps.add(triggeredOp);
        }

        List<NflowOperation> mainNflowOps = new ArrayList<>();
        if (mainNflowStopTime > 0) {
            NflowOperation mainNflowOp = mock(NflowOperation.class);
            when(mainNflowOp.getStopTime()).thenReturn(new DateTime(mainNflowStopTime));
            mainNflowOps.add(mainNflowOp);
        }

        when(opsProvider.findLatestCompleted(mainNflowId)).thenReturn(mainNflowOps);
        when(opsProvider.findLatest(triggeredNflowId)).thenReturn(triggeredNflowOps);

        when(opsProvider.isNflowRunning(mainNflowId)).thenReturn(isMainNflowRunning);
        when(opsProvider.isNflowRunning(triggeredNflowId)).thenReturn(isTriggeredNflowRunning);

        return new NflowExecutedSinceNflowAssessor() {
            @Override
            protected NflowProvider getNflowProvider() {
                return nflowProvider;
            }

            @Override
            protected NflowOperationsProvider getNflowOperationsProvider() {
                return opsProvider;
            }
        };
    }

    class TestMetricAssessmentBuilder implements MetricAssessmentBuilder<Serializable> {

        AssessmentResult result;
        String message;
        Metric metric;

        public AssessmentResult getResult() {
            return result;
        }

        public String getMessage() {
            return message;
        }

        public Metric getMetric() {
            return metric;
        }

        @Override
        public MetricAssessmentBuilder<Serializable> metric(Metric metric) {
            this.metric = metric;
            return this;
        }

        @Override
        public MetricAssessmentBuilder<Serializable> message(String descr) {
            this.message = descr;
            return this;
        }

        @Override
        public MetricAssessmentBuilder<Serializable> comparitor(Comparator<MetricAssessment<Serializable>> comp) {
            throw new UnsupportedOperationException();
        }

        @Override
        public MetricAssessmentBuilder<Serializable> compareWith(Comparable<? extends Serializable> value, Comparable<? extends Serializable>[] otherValues) {
            throw new UnsupportedOperationException();
        }

        @Override
        public MetricAssessmentBuilder<Serializable> data(Serializable data) {
            throw new UnsupportedOperationException();
        }

        @Override
        public MetricAssessmentBuilder<Serializable> result(AssessmentResult result) {
            this.result = result;
            return this;
        }
    }

}
