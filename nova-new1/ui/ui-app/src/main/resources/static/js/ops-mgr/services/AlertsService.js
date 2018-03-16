define(['angular','ops-mgr/module-name','ops-mgr/module'], function (angular,moduleName) {
    angular.module(moduleName).service('AlertsService', function () {

        var self = this;
        this.nflowFailureAlerts = {};
        this.serviceAlerts = {};
        this.alerts = [];
        this.alertsById = {};
        this.findNflowFailureAlert = function (nflowName) {
            return _.find(self.alerts, function (alert) {
                return alert.type == 'Nflow' && alert.name == nflowName;
            });
        }

        this.findNflowFailureAlertIndex = function (nflowName) {
            return _.findIndex(self.alerts, function (alert) {
                return alert.type == 'Nflow' && alert.name == nflowName;
            });
        }

        this.replaceNflowAlert = function (nflowName, nflowHealth) {

        }

        this.addNflowHealthFailureAlert = function (nflowHealth) {
            //first remove it and add a new entry
            if (self.nflowFailureAlerts[nflowHealth.nflow] != undefined) {
                self.removeNflowFailureAlertByName(nflowHealth.nflow);
            }
            var alertId = IDGenerator.generateId('alert');
            var alert = {
                id: alertId,
                type: 'Nflow',
                name: nflowHealth.nflow,
                'summary': nflowHealth.nflow,
                message: 'UNHEALTHY',
                severity: 'FATAL',
                sinceTime: nflowHealth.lastUnhealthyTime,
                count: nflowHealth.unhealthyCount,
                since: nflowHealth.sinceTimeString
            };
            self.nflowFailureAlerts[nflowHealth.nflow] = alert;
            self.alertsById[alertId] = alert;
            self.alerts.push(alert);

        }

        this.addServiceAlert = function (service) {
            if (self.serviceAlerts[service.serviceName] == undefined) {
                var alertId = IDGenerator.generateId('service');

                var alert = {
                    id: alertId,
                    type: 'Service',
                    name: service.serviceName,
                    'summary': service.serviceName,
                    message: service.alertsCount + " alerts",
                    severity: 'FATAL',
                    count: 1,
                    sinceTime: service.latestAlertTimestamp,
                    since: new moment(service.latestAlertTimestamp).fromNow()
                };
                self.serviceAlerts[service.serviceName] = alert;
                self.alertsById[alertId] = alert;
                self.alerts.push(alert);
            }
            else {
                self.serviceAlerts[service.serviceName].sinceTime = service.latestAlertTimestamp;
                self.serviceAlerts[service.serviceName].since = new moment(service.latestAlertTimestamp).fromNow();
            }
        }

        this.removeNflowFailureAlertByName = function (nflow) {
            var alert = self.nflowFailureAlerts[nflow];
            if (alert) {
                delete self.nflowFailureAlerts[nflow];
                delete self.alertsById[alert.id];
                var matchingIndex = self.findNflowFailureAlertIndex(nflow);
                if (matchingIndex != -1) {
                    self.alerts.splice(matchingIndex, 1);
                }
            }
        }

        this.removeServiceAlert = function (service) {
            var alert = self.serviceAlerts[service.serviceName];
            if (alert) {
                delete self.serviceAlerts[service.serviceName];
                delete self.alertsById[alert.id];
                var index = self.alerts.indexOf(alert);
                self.alerts.splice(index, 1);
            }
        }

    });
});
