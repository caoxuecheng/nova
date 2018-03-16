package com.onescorpin.metadata.api.jobrepo.nifi;

/*-
 * #%L
 * onescorpin-operational-metadata-api
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


import org.joda.time.DateTime;

import java.util.List;

/**
 * A provider with methods to access statistical information about a nflow and its job executions
 * Statistics are of the type {@link NifiNflowProcessorStats} which are group stats by nflow and then by processor
 */
public interface NifiNflowProcessorStatisticsProvider {

    /**
     * Save a new stats record
     *
     * @return save the stats record
     */
    NifiNflowProcessorStats create(NifiNflowProcessorStats t);

    /**
     * find statistics within a given start and end time
     *
     * @return stats within a start and end time
     */
    List<? extends NifiNflowProcessorStats> findWithinTimeWindow(DateTime start, DateTime end);

    /**
     * Find a list of stats for a given nflow within a time window grouped by nflow and processor
     *
     * @param nflowName a nflow name
     * @param start    a start date
     * @param end      an end date
     * @return a list of nflow processor statistics
     */
    List<? extends NifiNflowProcessorStats> findNflowProcessorStatisticsByProcessorId(String nflowName, DateTime start, DateTime end);

    /**
     * Find a list of stats for a given nflow within a time window grouped by nflow and processor
     *
     * @param nflowName a nflow name
     * @param start    a start date
     * @param end      an end date
     * @return a list of nflow processor statistics
     */
    List<? extends NifiNflowProcessorStats> findNflowProcessorStatisticsByProcessorName(String nflowName, DateTime start, DateTime end);

    /**
     * Find stats for a given nflow within a given timeframe grouped by processor id related to the nflow
     *
     * @param nflowName  the nflow name
     * @param timeFrame a timeframe to look back
     * @return a list of nflow processor statistics
     */
    List<? extends NifiNflowProcessorStats> findNflowProcessorStatisticsByProcessorId(String nflowName, TimeFrame timeFrame);

    /**
     * Find stats for a given nflow within a given timeframe grouped by processor name related to the nflow
     *
     * @param nflowName  the nflow name
     * @param timeFrame a timeframe to look back
     * @return a list of nflow processor statistics
     */
    List<? extends NifiNflowProcessorStats> findNflowProcessorStatisticsByProcessorName(String nflowName, TimeFrame timeFrame);

    /**
     * Find stats for a given nflow and time frame grouped by the stats eventTime
     *
     * @return a list of nflow processor statistics
     */
    List<? extends NifiNflowProcessorStats> findForNflowStatisticsGroupedByTime(String nflowName, DateTime start, DateTime end);

    /**
     * Find stats for a given nflow and time frame grouped by the stats eventTime
     *
     * @return a list of nflow processor statistics
     */
    List<? extends NifiNflowProcessorStats> findForNflowStatisticsGroupedByTime(String nflowName, TimeFrame timeFrame);


    List<? extends NifiNflowProcessorStats> save(List<? extends NifiNflowProcessorStats> stats);



    List<? extends NifiNflowProcessorErrors> findNflowProcessorErrors(String nflowName, DateTime start, DateTime end);


    List<? extends NifiNflowProcessorErrors> findNflowProcessorErrorsAfter(String nflowName, DateTime after);

    /**
     * Finds the latest stats for a nflow.
     * This is bound by Entity Access control rules
     * @param nflowName the name of the nflow
     * @return the stats
     */
    List<NifiNflowProcessorStats> findLatestFinishedStats(String nflowName);

    /**
     * Finds the latest stats for a nflow.
     * This is NOT bound by Entity Access control rules
     * @param nflowName the name of the nflow
     * @return the stats
     */
    List<NifiNflowProcessorStats> findLatestFinishedStatsWithoutAcl(String nflowName);

    /**
     * Compact the NiFi Nflow Processor Stats table.
     *
     * @return a summary of the rows that were compacted
     */
    String compactNflowProcessorStatistics();

    /**
     * allow for specifying a time to look back from when querying for statistical information
     */
    enum TimeFrame {

        ONE_MIN((long) (1000 * 60), "Last Minute"),
        THREE_MIN(ONE_MIN.millis * 3, "Last 3 Minutes"),
        FIVE_MIN(ONE_MIN.millis * 5, "Last 5 Minutes"),
        TEN_MIN(ONE_MIN.millis * 10, "Last 10 Minutes"),
        THIRTY_MIN(ONE_MIN.millis * 30, "Last 30 Minutes"),
        HOUR(ONE_MIN.millis * 60, "Last Hour"),
        THREE_HOUR(HOUR.millis * 3, "Last 3 Hours"),
        FIVE_HOUR(HOUR.millis * 5, "Last 5 Hours"),
        TEN_HOUR(HOUR.millis * 10, "Last 10 Hours"),
        DAY(HOUR.millis * 24, "Last Day"),
        THREE_DAYS(DAY.millis * 3, "Last 3 Days"),
        WEEK(DAY.millis * 7, "Last Week"),
        MONTH(DAY.millis * (365 / 12), "Last Month"),
        THREE_MONTHS(DAY.millis * (365 / 6), "Last 3 Months"),
        SIX_MONTHS(DAY.millis * (365 / 2), "Last 6 Months"),
        YEAR(DAY.millis * 365, "Last Year");

        protected Long millis;
        private String displayName;

        TimeFrame(long millis, String displayName) {
            this.millis = millis;
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public DateTime startTime() {
            return DateTime.now().minus(millis);
        }

        public DateTime startTimeRelativeTo(DateTime dt) {
            return dt.minus(millis);
        }

        public Long getMillis() {
            return millis;
        }
    }


}
