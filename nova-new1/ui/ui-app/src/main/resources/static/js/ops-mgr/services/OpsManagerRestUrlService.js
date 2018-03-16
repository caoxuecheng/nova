define(['angular','ops-mgr/module-name'], function (angular,moduleName) {
    angular.module(moduleName).service('OpsManagerRestUrlService', function () {

        var self = this;

        this.ROOT = "";

        this.SLA_BASE_URL = "/proxy/v1/nflowmgr/sla";
        this.NFLOWS_BASE = "/proxy/v1/nflows";
        this.JOBS_BASE = "/proxy/v1/jobs";
        this.SECURITY_BASE_URL = this.ROOT + "/proxy/v1/security";
        this.DASHBOARD_URL = this.ROOT + '/proxy/v1/dashboard';

        this.DASHBOARD_PAGEABLE_NFLOWS_URL = this.ROOT + '/proxy/v1/dashboard/pageable-nflows';

        this.NFLOW_HEALTH_URL = this.NFLOWS_BASE + "/health";
        this.NFLOW_NAMES_URL = this.NFLOWS_BASE + "/names";
        this.NFLOW_HEALTH_COUNT_URL = this.NFLOWS_BASE + "/health-count";

        /*this.SPECIFIC_NFLOW_HEALTH_COUNT_URL = function (nflowName) {
            return self.NFLOW_HEALTH_COUNT_URL + '/' + nflowName + '/';
        }
        */

        this.SPECIFIC_NFLOW_HEALTH_URL = function (nflowName) {
            return '/proxy/v1/dashboard/nflows/nflow-name/' + nflowName;
        }
        this.NFLOW_DAILY_STATUS_COUNT_URL = function (nflowName) {
            return self.NFLOWS_BASE + "/" + nflowName + "/daily-status-count";
        }

        this.NFLOW_NAME_FOR_ID = function(nflowId){
            return self.NFLOWS_BASE +"/query/"+nflowId
        }




//JOB urls

        this.JOBS_QUERY_URL = this.JOBS_BASE;
        this.JOBS_CHARTS_QUERY_URL = this.JOBS_BASE + '/list';
        this.JOB_NAMES_URL = this.JOBS_BASE + '/names';

        this.DAILY_STATUS_COUNT_URL = self.JOBS_BASE + "/daily-status-count/";

        //this.RUNNING_OR_FAILED_COUNTS_URL = this.JOBS_BASE + '/running-failed-counts';

        this.RUNNING_JOB_COUNTS_URL = '/proxy/v1/dashboard/running-jobs';

       // this.DATA_CONFIDENCE_URL = "/proxy/v1/data-confidence/summary";

        this.RESTART_JOB_URL = function (executionId) {
            return self.JOBS_BASE + "/" + executionId + "/restart";
        }
        this.STOP_JOB_URL = function (executionId) {
            return self.JOBS_BASE + "/" + executionId + "/stop";
        }

        this.ABANDON_JOB_URL = function (executionId) {
            return self.JOBS_BASE + "/" + executionId + "/abandon";
        }


        this.ABANDON_ALL_JOBS_URL = function (nflowId) {
            return self.JOBS_BASE + "/abandon-all/" + nflowId;
        }

        this.FAIL_JOB_URL = function (executionId) {
            return self.JOBS_BASE + "/" + executionId + "/fail";
        }

        this.LOAD_JOB_URL = function (executionId) {
            return self.JOBS_BASE + "/" + executionId;
        }

        this.RELATED_JOBS_URL = function (executionId) {
            return self.JOBS_BASE + "/" + executionId + "/related";
        }

//Service monitoring

        this.SERVICES_URL = "/proxy/v1/service-monitor/";

        //Provenance Event Stats
        this.STATS_BASE = "/proxy/v1/provenance-stats";

        this.STATS_BASE_V2 = "/proxy/v2/provenance-stats";

        this.PROCESSOR_DURATION_FOR_NFLOW = function (nflowName, from, to) {
            return self.STATS_BASE_V2 + "/" + nflowName + "/processor-duration?from=" + from + "&to=" + to;
        };

        this.NFLOW_STATISTICS_OVER_TIME = function (nflowName, from, to, maxDataPoints) {
            return self.STATS_BASE_V2 + "/" + nflowName + "?from=" + from + "&to=" + to + "&dp=" + maxDataPoints;
        };

        this.NFLOW_PROCESSOR_ERRORS = function (nflowName, from, to) {
            return self.STATS_BASE_V2 + "/" + nflowName + "/processor-errors?from=" + from + "&to=" + to;
        };

        this.PROVENANCE_EVENT_TIME_FRAME_OPTIONS = this.STATS_BASE_V2 + "/time-frame-options";

        /**
         * Gets the alert details endpoint for the specified alert.
         * @param {string} alertId the id of the alert
         * @returns {string} the URL of the endpoint
         */
        this.ALERT_DETAILS_URL = function (alertId) {
            return "/proxy/v1/alerts/" + alertId;
        };

        this.ALERTS_URL = "/proxy/v1/alerts";

        this.ALERTS_SUMMARY_UNHANDLED = "/proxy/v1/dashboard/alerts";

        this.ALERT_TYPES = "/proxy/v1/alerts/alert-types";

        this.NFLOW_ALERTS_URL = function(nflowName) {
            return "/proxy/v1/dashboard/alerts/nflow-name/"+nflowName;
        }

        //assessments
        this.LIST_SLA_ASSESSMENTS_URL = "/proxy/v1/sla/assessments/"

        this.GET_SLA_ASSESSMENT_URL = function(assessmentId){
            return "/proxy/v1/sla/assessments/"+assessmentId;
        };

        this.GET_SLA_BY_ID_URL = function (slaId) {
            return self.SLA_BASE_URL + "/"+slaId;
        }

    });
});
