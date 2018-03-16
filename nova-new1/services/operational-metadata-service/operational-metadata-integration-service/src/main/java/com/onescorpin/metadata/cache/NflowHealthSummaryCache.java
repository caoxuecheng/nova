package com.onescorpin.metadata.cache;
/*-
 * #%L
 * onescorpin-operational-metadata-integration-service
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

import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.AtomicLongMap;
import com.onescorpin.DateTimeUtil;
import com.onescorpin.jobrepo.query.model.NflowHealth;
import com.onescorpin.jobrepo.query.model.NflowStatus;
import com.onescorpin.jobrepo.query.model.transform.NflowModelTransform;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.event.MetadataEventService;
import com.onescorpin.metadata.api.nflow.NflowSummary;
import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.metadata.api.nflow.OpsManagerNflowProvider;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobExecution;
import com.onescorpin.metadata.cache.util.TimeUtil;
import com.onescorpin.metadata.config.RoleSetExposingSecurityExpressionRoot;
import com.onescorpin.metadata.jpa.nflow.JpaNflowSummary;
import com.onescorpin.metadata.jpa.nflow.security.NflowAclCache;
import com.onescorpin.rest.model.search.SearchResult;
import com.onescorpin.rest.model.search.SearchResultImpl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.comparator.NullSafeComparator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

/**
 * Created by sr186054 on 9/27/17.
 */
public class NflowHealthSummaryCache implements TimeBasedCache<NflowSummary> {


    private static final Logger log = LoggerFactory.getLogger(NflowHealthSummaryCache.class);

    @Inject
    private MetadataEventService metadataEventService;

    @Inject
    private MetadataAccess metadataAccess;

    @Inject
    OpsManagerNflowProvider opsManagerNflowProvider;

    @Inject
    private NflowAclCache nflowAclCache;

    private Comparator<NflowSummary> byRunningStatus = Comparator.comparing(NflowSummary::getRunStatus, Comparator.nullsLast(Comparator.naturalOrder()));

    private Comparator<NflowSummary> byStartTime = Comparator.comparing(NflowSummary::getStartTime, Comparator.nullsLast(Comparator.naturalOrder()));

    private Comparator<NflowSummary> byStream = Comparator.comparing(NflowSummary::isStream, Comparator.nullsLast(Comparator.naturalOrder()));

    private Comparator<NflowSummary> byName = Comparator.comparing(NflowSummary::getNflowName, Comparator.nullsLast(Comparator.naturalOrder()));

    private Comparator<NflowSummary> byHealth = new NullSafeComparator<NflowSummary>(new Comparator<NflowSummary>() {
        @Override
        public int compare(NflowSummary o1, NflowSummary o2) {
            BatchJobExecution.JobStatus s1 = o1.getStatus() != null ? o1.getStatus() : BatchJobExecution.JobStatus.UNKNOWN;
            BatchJobExecution.JobStatus s2 = o2.getStatus() != null ? o2.getStatus() : BatchJobExecution.JobStatus.UNKNOWN;
            int x = s1.ordinal();
            int y = s2.ordinal();
            return (x < y) ? -1 : ((x == y) ? 0 : 1);
        }
    }, true);

    private Comparator<NflowSummary> byStatus = new NullSafeComparator<NflowSummary>(new Comparator<NflowSummary>() {

        @Override
        public int compare(NflowSummary o1, NflowSummary o2) {
            String s1 = o1.getStatus() != null ? o1.getStatus().name() : BatchJobExecution.JobStatus.UNKNOWN.name();
            String s2 = o2.getStatus() != null ? o2.getStatus().name() : BatchJobExecution.JobStatus.UNKNOWN.name();
            if(s1.equalsIgnoreCase(BatchJobExecution.JobStatus.STOPPED.name())){
                s1 = BatchJobExecution.JobStatus.COMPLETED.name();
            }
            if(s2.equalsIgnoreCase(BatchJobExecution.JobStatus.STOPPED.name())){
                s2 = BatchJobExecution.JobStatus.COMPLETED.name();
            }
            if(o1.getRunStatus() != null && (o1.getRunStatus().equals(NflowSummary.RunStatus.RUNNING) || o1.getRunStatus().equals(NflowSummary.RunStatus.INITIAL))){
                s1 = o1.getRunStatus().name();
            }
            if(o2.getRunStatus() != null && (o2.getRunStatus().equals(NflowSummary.RunStatus.RUNNING) || o2.getRunStatus().equals(NflowSummary.RunStatus.INITIAL))){
                s2 = o2.getRunStatus().name();
            }
            return s1.compareTo(s2);
        }
    }, true);

    private Comparator<NflowSummary> bySinceTime = new NullSafeComparator<NflowSummary>(new Comparator<NflowSummary>() {

        private Long getTime(NflowSummary nflowSummary) {
            Long time1 = -1L;
            if (nflowSummary.getRunStatus() == NflowSummary.RunStatus.RUNNING && nflowSummary.getStartTime() != null) {
                time1 = DateTimeUtil.getNowUTCTime().getMillis() - nflowSummary.getStartTime().getMillis();
            } else if (nflowSummary.getEndTime() != null) {
                time1 = DateTimeUtil.getNowUTCTime().getMillis() - nflowSummary.getEndTime().getMillis();
            }
            return time1;
        }

        @Override
        public int compare(NflowSummary o1, NflowSummary o2) {
            Long time1 = getTime(o1);
            Long time2 = getTime(o2);
            return time1.compareTo(time2);
        }
    }, true);

    private Comparator<NflowSummary> byLastRunTime = new NullSafeComparator<NflowSummary>(new Comparator<NflowSummary>() {

        private Long getTime(NflowSummary nflowSummary) {
            Long time1 = -1L;
            if (nflowSummary.getRunStatus() == NflowSummary.RunStatus.RUNNING && nflowSummary.getStartTime() != null) {
                time1 = -1L;
            } else if (nflowSummary.getEndTime() != null && nflowSummary.getStartTime() != null) {
                time1 = nflowSummary.getEndTime().getMillis() - nflowSummary.getStartTime().getMillis();
            }
            return time1;
        }

        @Override
        public int compare(NflowSummary o1, NflowSummary o2) {
            Long time1 = getTime(o1);
            Long time2 = getTime(o2);
            return time1.compareTo(time2);
        }
    }, true);

    private Comparator<NflowSummary> getComparator(String sort) {
        Comparator c = byName;
        if (sort.toLowerCase().contains("nflow")) {
            c = byName;
        } else if (sort.toLowerCase().contains("health")) {
            c = byHealth;
        } else if (sort.toLowerCase().contains("status")) {
            c = byStatus;
        } else if (sort.toLowerCase().contains("since")) {
            c = bySinceTime;
        } else if (sort.toLowerCase().contains("runtime")) {
            c = byLastRunTime;
        } else if (sort.toLowerCase().contains("stream")) {
            c = byStream;
        }

        return sort.startsWith("-") ? c.reversed() : c;
    }


    LoadingCache<Long, List<? extends NflowSummary>> nflowSummaryCache = CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.SECONDS).build(new CacheLoader<Long, List<? extends NflowSummary>>() {
        @Override
        public List<? extends NflowSummary> load(Long millis) throws Exception {
            return fetchNflowSummary();
        }
    });


    public List<? extends NflowSummary> getNflowSummaryList(Long time) {
        return nflowSummaryCache.getUnchecked(time);
    }


    public NflowStatus getUserNflows() {
        Long time = TimeUtil.getTimeNearestFiveSeconds();
        return getUserNflows(time);
    }

    public NflowStatus getUserNflow(String nflowName) {
        Long time = TimeUtil.getTimeNearestFiveSeconds();
        return getUserNflows(time, new NflowSummaryFilter(nflowName));
    }

    public NflowStatus getUserNflows(NflowSummaryFilter nflowSummaryFilter) {
        Long time = TimeUtil.getTimeNearestFiveSeconds();
        return getUserNflows(time, nflowSummaryFilter);
    }

    public Map<String, Long> getUserNflowHealthCounts(Long time) {
        RoleSetExposingSecurityExpressionRoot userContext = nflowAclCache.userContext();
       return getUserNflowHealthCounts(time,userContext);
    }

    public Map<String, Long> getUserNflowHealthCounts(Long time,  RoleSetExposingSecurityExpressionRoot userContext ) {
        AtomicLongMap<String> healthCounts = AtomicLongMap.create();
        List<? extends NflowSummary> list = getNflowSummaryList(time);
        list.stream()
            .filter(filter(new NflowSummaryFilter(), userContext))
            .forEach(f -> {
                String key = f.getFailedCount() == null || f.getFailedCount() == 0 ? "HEALTHY" : "UNHEALTHY";
                healthCounts.incrementAndGet(key);
            });
        return healthCounts.asMap();
    }

    /**
     * Used for Nflow Health KPI and Nflow card
     */
    public NflowStatus getUserNflows(Long time) {
        return getUserNflows(time, new NflowSummaryFilter());
    }

    public SearchResult getUserNflowHealth(NflowSummaryFilter nflowSummaryFilter) {
        Long time = TimeUtil.getTimeNearestFiveSeconds();
        return getUserNflowHealth(time, nflowSummaryFilter);
    }

    /**
     * @return SearchResult filled with NflowSummary objects
     */
    public SearchResult getUserNflowHealth(Long time, NflowSummaryFilter nflowSummaryFilter) {
        RoleSetExposingSecurityExpressionRoot userContext = nflowAclCache.userContext();
       return getUserNflowHealth(time,nflowSummaryFilter,userContext);
    }

    public SearchResult getUserNflowHealth(Long time, NflowSummaryFilter nflowSummaryFilter,  RoleSetExposingSecurityExpressionRoot userContext) {
        SearchResult<com.onescorpin.jobrepo.query.model.NflowSummary> searchResult = new SearchResultImpl();
        List<NflowHealth> nflowSummaryHealth = null;
        //get the entire list back and filter it for user access
        List<? extends NflowSummary> list = getNflowSummaryList(time).stream().filter(filter(nflowSummaryFilter, userContext)).collect(Collectors.toList());
        nflowSummaryHealth = list.stream()
            .sorted(nflowSummaryFilter.getSort() != null ? getComparator(nflowSummaryFilter.getSort()) : byName)
            .skip(nflowSummaryFilter.getStart())
            .limit(nflowSummaryFilter.getLimit() > 0 ? nflowSummaryFilter.getLimit() : Integer.MAX_VALUE).map(f -> NflowModelTransform.nflowHealth(f)).
                collect(Collectors.toList());

        //Transform it to NflowSummary objects
        NflowStatus nflowStatus = NflowModelTransform.nflowStatus(nflowSummaryHealth);
        Long total = new Long(list.size());
        searchResult.setData(nflowStatus.getNflowSummary());
        searchResult.setRecordsTotal(total);
        searchResult.setRecordsFiltered(total);

        return searchResult;
    }

    public NflowStatus getUserNflows(Long time, NflowSummaryFilter nflowSummaryFilter) {
        SearchResult<com.onescorpin.jobrepo.query.model.NflowSummary> searchResult = getUserNflowHealth(time, nflowSummaryFilter);
        List<com.onescorpin.jobrepo.query.model.NflowSummary> nflowSummaryHealth = searchResult.getData();
        return NflowModelTransform.nflowStatusFromNflowSummary(nflowSummaryHealth);
    }


    @Override
    public List<NflowSummary> getCache(Long time) {
        return (List<NflowSummary>) getNflowSummaryList(time);
    }

    @Override
    public List<NflowSummary> getUserCache(Long time) {
        return (List<NflowSummary>) getUserNflows(time);
    }

    private List<? extends NflowSummary> fetchNflowSummary() {
        return metadataAccess.read(() -> {
            Stopwatch stopwatch = Stopwatch.createStarted();
            List<? extends NflowSummary> list = opsManagerNflowProvider.findNflowSummary();

            Map<String, NflowSummary> latestNflows = new HashMap<>();
            //NOTE it could also populate the last job execution time since the above query gets a union of the running jobs along with the latest finished jobs by nflow
            list.stream()
                .sorted(byRunningStatus.thenComparing(byStartTime)).forEach(f -> {
                String nflowId = f.getNflowId().toString();
                if (!latestNflows.containsKey(nflowId)) {
                    latestNflows.put(nflowId, f);
                }
            });
            //add in initial nflows
            List<? extends OpsManagerNflow> allNflows = opsManagerNflowProvider.findAllWithoutAcl();
            allNflows.stream().filter(f -> !latestNflows.containsKey(f.getId().toString())).forEach(f -> {
                                                                                        JpaNflowSummary s = new JpaNflowSummary();
                                                                                        s.setStream(f.isStream());
                                                                                        s.setNflowId(UUID.fromString(f.getId().toString()));
                                                                                        s.setNflowName(f.getName());
                                                                                        s.setNflowType(f.getNflowType());
                                                                                        s.setRunningCount(0L);
                                                                                        s.setAbandonedCount(0L);
                                                                                        s.setFailedCount(0L);
                                                                                        s.setAllCount(0L);
                                                                                        s.setCompletedCount(0L);
                                                                                        s.setRunStatus(NflowSummary.RunStatus.INITIAL);
                                                                                        s.setStatus(BatchJobExecution.JobStatus.UNKNOWN);
                                                                                       latestNflows.put(s.getNflowId().toString(),s);
                                                                                    }
            );

            stopwatch.stop();
            log.debug("Time to fetchAndDedupe NflowSummary: {} ", stopwatch.elapsed(TimeUnit.MILLISECONDS));
            return new ArrayList<>(latestNflows.values());
        }, MetadataAccess.SERVICE);
    }

    @Override
    public boolean isAvailable() {
        return nflowAclCache.isUserCacheAvailable();
    }

    private Predicate<NflowSummary> filter(NflowSummaryFilter filter, RoleSetExposingSecurityExpressionRoot userContext) {
        return s -> {
            try {
                return nflowAclCache.hasAccess(userContext, s.getNflowId().toString()) && fixedFilter(s, filter) && (filter.containsNflow(s.getNflowName()) && filter
                    .containsState(s.getRunStatus().name().toLowerCase()));
            } catch (Exception e) {
                return false;
            }
        };

    }

    /**
     * Streaming Nflows only show up in ALL or Streaming tab  (Running tab ??)
     * @param nflowSummary
     * @param nflowSummaryFilter
     * @return
     */
    private boolean fixedFilter(NflowSummary nflowSummary, NflowSummaryFilter nflowSummaryFilter) {
        switch (nflowSummaryFilter.getFilter()) {
            case ALL:
                return true;
            case HEALTHY:
                return !nflowSummary.isStream() && (nflowSummary.getFailedCount() == null || nflowSummary.getFailedCount() == 0L);
            case UNHEALTHY:
                return !nflowSummary.isStream() && (nflowSummary.getFailedCount() != null && nflowSummary.getFailedCount() > 0L);
            case RUNNING:
                return nflowSummary.getRunStatus() == NflowSummary.RunStatus.RUNNING;
            case STREAMING:
                return nflowSummary.isStream();
            default:
                return true;
        }
    }


    public static class NflowSummaryFilter {

        public enum FIXED_FILTER {
            ALL, HEALTHY, UNHEALTHY, RUNNING, STREAMING
        }

        String nflowName;
        String state;

        Integer limit = 0;
        Integer start = 0;
        String sort;
        boolean applyPaging = false;

        FIXED_FILTER filter = FIXED_FILTER.ALL;

        public NflowSummaryFilter() {
        }

        public NflowSummaryFilter(String fixedFilter, String nflowName, String state) {
            this.nflowName = nflowName;
            this.state = state;
            if (StringUtils.isNotBlank(fixedFilter)) {
                try {
                    this.filter = FIXED_FILTER.valueOf(fixedFilter.toUpperCase());
                } catch (Exception e) {
                    this.filter = FIXED_FILTER.ALL;
                }
            } else {
                this.filter = FIXED_FILTER.ALL;
            }
        }

        public NflowSummaryFilter(String nflowName, String state) {
            this.nflowName = nflowName;
            this.state = state;
            this.filter = FIXED_FILTER.ALL;
        }

        public NflowSummaryFilter(String nflowName) {
            this.nflowName = nflowName;
            this.filter = FIXED_FILTER.ALL;
        }

        public NflowSummaryFilter(String fixedFilter, String nflowName, String state, Integer limit, Integer start, String sort) {
            this(fixedFilter, nflowName, state);
            this.limit = limit;
            this.start = start;
            this.sort = sort;
        }

        public String getNflowName() {
            return nflowName;
        }

        public void setNflowName(String nflowName) {
            this.nflowName = nflowName;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public Integer getLimit() {
            return limit;
        }

        public void setLimit(Integer limit) {
            this.limit = limit;
        }

        public Integer getStart() {
            return start;
        }

        public void setStart(Integer start) {
            this.start = start;
        }

        public String getSort() {
            return sort;
        }

        public void setSort(String sort) {
            this.sort = sort;
        }

        public FIXED_FILTER getFilter() {
            return filter;
        }

        public void setFilter(FIXED_FILTER filter) {
            this.filter = filter;
        }

        public boolean containsNflow(String nflow) {
            return StringUtils.isBlank(this.nflowName) || (StringUtils.isNotBlank(this.nflowName) && nflow.contains(this.nflowName));
        }

        public boolean containsState(String state) {
            return StringUtils.isBlank(this.state) || (StringUtils.isNotBlank(this.state) && state.contains(this.state));
        }

        public boolean isApplyPaging() {
            return applyPaging;
        }

        public void setApplyPaging(boolean applyPaging) {
            this.applyPaging = applyPaging;
        }
    }

}
