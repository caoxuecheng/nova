## Example Service Module

You can code in custom java code using Nova libraries to extend Nova functionality.
Once compiled this jar should be placed in the /nova-services/plugin folder so it is available for nova-services.  You need to restart nova-services if you add/modify these plugins.

### Overview

 - This example creates a new service level permission called 'accessExample' using the Nova's ActionsModuleBuilder class.
 - This permission is wired into Nova using the following Spring configuration code.  It will be available for users to select in the Nova ui to assign to other users/groups.  For this example it is also used to restrict access to a new side navigation link in the  [example-module-ui](../example-module-ui)
 
 ```java 
@Bean
    public PostMetadataConfigAction exampleAccessConfigAction() {
        return () -> metadata.commit(() -> builder
            .module(AllowedActions.SERVICES)
            .action(ExampleAccessControl.ACCESS_EXAMPLE)
            .add()
            .build(), MetadataAccess.SERVICE);
    }
```

**Note**: You need to define a '/plugin/plugin-context.xml' file in your project that defines a spring bean in order for Nova to detect and Inject Spring beans into your classes.

    
    
