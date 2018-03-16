Nflow Wizard Plugins
===================

Overview
--------

Additional steps can be added to the Create Nflow and Nflow Details pages with a Nflow Wizard plugin. A plugin will have access to the nflow's metadata to add or modify properties.

Two plugins are included with Nova: Data Ingest and Data Transformation. The Data Ingest plugin adds steps to define a destination Hive table. The Data Transformation plugin adds steps to generate a
Spark script that transforms the data. Both of these plugins can be used as examples.

There are a seres of 4 different plugin configurations in this project that setup various nflow steppers.

Plugin Definition
-----------------

A simple stepper in Nova is defined with 5 default steps
1. General Info  - general info about the nflow (name, description)
2. Nflow Details - NiFi processor properties
3. Properties  - Additional business properties (tags, owner)
4. Access Control - if entity access is enabled.
5. Schedule - the schedule of the nflow

The plugin should provide a JSON file describing its purpose and indicates what templates it uses.

**NOTE**:  This json file must end with the suffix `-stepper-definition.json`

 - The metadata properties refer to `model.tableOption` in the templates and will be automatically prefixed with `metadata.tableOption`.
 - You can two types of steps
    1. pre-steps.  These will be rendered prior to the `General Info` step section.  These are useful if you want a form that users fill out that will validate before they create their nflows.  These steps are defined with the `preStepperTemplateUrl` and `totalPreSteps` properties
    2. core-steps.  These will be rendered after the `Nflow Details` step. These are defined with the `stepperTemplateUrl` and `totalCoreSteps` properties

```json

{
  "description": "A human-readable summary of this option. This is displayed as the hint when registering a template (Required) ",
  "displayName": "A human-readable title of this option. This is displayed as the title of this option when registering a template. (Required)  ",
  "resourceContext":"The url prefix/directory where these resources are located.  Example:  /example-plugin-1.0  (Required)",
  "nflowDetailsTemplateUrl": "The location (with the /resoureceContext) of the html when viewing/editing a nflow. (Required if you define the 'stepperTemplateUrl' property)",
  "stepperTemplateUrl": "The location (with the /resourceContext) of the html for creating a new nflow (the nflow steps).  This does not include any pre-steps (Either 'preStepperTemplateUrl' or this property is required)",
  "preStepperTemplateUrl":"The location (with the /resourceContext) of the html for any pre-steps  (Either 'stepperTemplateUrl' or this property is required) ",
  "preNflowDetailsTemplateUrl":"The location (with the /resourceContext) of the html when viewing/editing a nflow for any pre-steps.  (Optional)",
  "metadataProperties": [
    {
      "name": "A property name",
      "description": "A description of the property"
    }   
  ]  ,
  "totalCoreSteps": "A number datatype. The number of steps defined in the 'stepperTemplateUrl'  (does not include pre-steps) (Required only if 'stepperTemplateUrl' property is defined)",
  "totalPreSteps": "A number datatype. The number of steps defined in the 'preStepperTemplateUrl'  (does not include core-steps),  (Required only if 'preStepperTemplateUrl' property is defined)",
  "type": "Unique identifier for this stepper/template type",
  "initializeServiceName":"The name of the angular Initialization Service to call, defined in the 'initializeScript' file.  (Required if you define the 'initializeScript' property) ",
  "initializeScript": "The location (with the /resourceContext) of the initialization angular service.  See Initialization Service section below.  (Optional)"
}
````

Stepper Templates
-----------------

The Stepper template should add additional steps, if any, to the Create Nflow Wizard. Each step is defined by a `<nova-define-nflow-step>` which contains the HTML to be displayed for that step.

```html
<nova-define-nflow-step title="Example" step="getStep(0)">
  <div oc-lazy-load="['/example-ui-nflow-stepper-plugin-1.0/ExampleUiNflowStepperCard.js']">
    <example-ui-nflow-stepper-card step-index="{{getStepIndex(0)}}" ng-if="isStepSelected(0)"></example-ui-nflow-stepper-card>
  </div>
</nova-define-nflow-step>
```

Helper functions are provided for interacting with the stepper. Each function takes the index starting with 0 for the step within the plugin.

Function | Returns | Description
-------- | ------- | -----------
getStep(number) | Object | Gets the object for the table option step at the specified index.
getStepIndex(number) | number | Gets the stepper step index for the specified table option step index.
isStepSelected(number) | boolean | Indicates if the specified step is selected.
isStepVisited(number) | boolean | Indicates if the specified step has been visited.

Nflow Details Template
---------------------

The Nflow Details template should add additional sections, if any, to the edit nflow page. Each section is typically a new directive with its own `<vertical-section-layout>`.


Initialization Service
----------------------

You can define an optional Angular Initialization service that will be called when the nflow creation stepper or edit nflow is rendered.
This allows you to setup your nflow and set default options, show/hide fields, steps etc.
the service must follow the following constructs.  (see the 'initialize.js' for an example)
 - Must be an angular factory or service
 - Must have 2 public methods:
    - initializeCreateNflow(tableOptionsMetadata, nflowStepper).  
     This takes 2 arguments. 
       - tableOptionsMetadata  - This is the metadata defined in this file
       - nflowStepper  - This is the instance of the nflowStepper controller  (see /common/stepper.js).  You can activate/deactivate steps
    - initializeEditNflow(tableOptionsMetadata).  This takes 1 argument
       - tableOptionsMetadata  - This is the metadata defined in this file

Refer to the `initialize.js` for a complete example

**NOTE**  This Angular Service needs to belong to the `nova.nflowmgr.nflows` angular module, not the module for this specific plugin.  This is because that is the module that will be launching and running it.
 
 The example below registers the Service using the `moduleName` coming from the file `nflow-mgr/nflows/module-name` which resolves to: `nova.nflowmgr.nflows` 
 
 ```javascript
 
    define(["angular", "nflow-mgr/nflows/module-name"], function (angular, moduleName) {
        
        var service = function(NflowService, StepperService){
            
             var data = {
                        initializeCreateNflow:function(optionsMetadata,nflowStepper, nflowModel){
                            //init create nflow
                          },
                         initializeEditNflow:function(optionsMetadata,nflowModel) {
                            //init edit nflow
                         }
                    }
                    return data;
        }
        
         angular.module(moduleName)
                .factory("ExampleNflowStepperInitializerService", ["NflowService","StepperService", service])
    }
```

