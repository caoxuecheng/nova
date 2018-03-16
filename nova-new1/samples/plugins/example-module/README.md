## Example Module

This project showcases how you can create custom Java code for nova-services [example-module-services](example-module-services) and custom angular ui code for nova-ui [example-module-ui](example-module-ui)  
 - the nova-service Java code is a Java jar that is placed in the /opt/nova/nova-services/plugin folder
 - the nova-ui code is a jar with angular/html code that is placed in the /opt/nova/nova-ui/plugin folder
 
### Code Structure

   -  [example-module-services](example-module-services) | Custom nova-service code with a example REST controller and adds a new permissions into Nova
   -  [example-module-ui](example-module-ui) | Custom angular module that creates new side navigation using the permission above and calls the custom REST controller
    
    
