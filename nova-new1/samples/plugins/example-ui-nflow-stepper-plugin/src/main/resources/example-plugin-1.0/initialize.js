define(["angular", "nflow-mgr/nflows/module-name"], function (angular, moduleName) {

    var service = function (NflowService, StepperService) {


        var initializeCreateNflow = function(optionsMetadata,nflowStepper, nflowModel){

            //setup schedule strategy to be every 5 seconds
            nflowModel.schedule.schedulingStrategy="TIMER_DRIVEN";
            nflowModel.schedule.schedulingPeriod="25 sec";
            nflowModel.schedule.schedulingStrategyTouched =true;

            // disable the schedule form to prevent users from editing
            nflowModel.view.schedule.disabled=true

            //disable  the access control step.  users will bypass this step
            var accessControlStep = nflowStepper.getStepByName('Access Control');
            if(accessControlStep != null){
                nflowStepper.deactivateStep(accessControlStep.index)
            }

        }

        var initializeEditNflow = function(optionsMetadata,nflowModel){

            //Prevent users from editing the Nflow Definition / General Info section even if they own/can edit the nflow
            //nflowModel.view.generalInfo.disabled=true;

            //Prevent users from editing the Nflow Details / NiFi properties section even if they own/can edit the nflow
            //nflowModel.view.nflowDetails.disabled=true;

            //Prevent users from editing the data policies (standardizers/validators) section even if they own/can edit the nflow
            //nflowModel.view.dataPolicies.disabled=true;

            //Prevent users from editing the properties section even if they own/can edit the nflow
            //nflowModel.view.properties.disabled=true;

            //Prevent users from editing the schedule even if they own the nflow.
            nflowModel.view.schedule.disabled=true;

        }

        var data = {
            initializeCreateNflow:function(optionsMetadata,nflowStepper, nflowModel){
                  initializeCreateNflow(optionsMetadata,nflowStepper,nflowModel);
              },
             initializeEditNflow:function(optionsMetadata,nflowModel) {
                 initializeEditNflow(optionsMetadata,nflowModel);
             }
        }
        return data;
    };


    angular.module(moduleName)
        .factory("ExampleNflowStepperInitializerService", ["NflowService","StepperService", service])
});
