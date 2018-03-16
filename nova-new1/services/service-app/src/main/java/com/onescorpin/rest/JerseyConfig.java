package com.onescorpin.rest;

/*-
 * #%L
 * onescorpin-service-app
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.wadl.internal.WadlResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.jaxrs.config.BeanConfig;

@ApplicationPath("/api")
@SwaggerDefinition(
    info = @Info(
        title = "Nova",
        description = "Nova is a comprehensive Data Lake platform built-on Apache Hadoop, Spark, and NiFi.",
        version = "v1",
        contact = @Contact(name = "Think Big", url = "https://www.onescorpin.com/nova/"),
        license = @License(name = "Apache License, Version 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0")
    ),
    consumes = MediaType.APPLICATION_JSON,
    produces = MediaType.APPLICATION_JSON,
    schemes = SwaggerDefinition.Scheme.HTTP
)
public class JerseyConfig extends ResourceConfig {

    @Inject
    ApplicationContext applicationContext;


    private static final Logger log = LoggerFactory.getLogger(JerseyConfig.class);

    public JerseyConfig() {

        //Register Swagger
        Set<Class<?>> resources = new HashSet();
        resources.add(io.swagger.jaxrs.listing.ApiListingResource.class);
        resources.add(io.swagger.jaxrs.listing.SwaggerSerializers.class);
        registerClasses(resources);


        packages("com.onescorpin.ui.rest.controller",
                 "com.onescorpin.config.rest.controller",
                 "com.onescorpin.servicemonitor.rest.controller",
                 "com.onescorpin.scheduler.rest.controller",
                 "com.onescorpin.jobrepo.rest.controller",
                 "com.onescorpin.hive.rest.controller",
                 "com.onescorpin.nflowmgr.rest.controller",
                 "com.onescorpin.policy.rest.controller",
                 "com.onescorpin.security.rest.controller",
                 "com.onescorpin.metadata.rest.api",
                 "com.onescorpin.metadata.migration.rest.controller",
                 "com.onescorpin.spark.rest.controller",
                 "com.onescorpin.rest.exception",
                 "com.onescorpin.discovery.rest.controller",
                 "com.onescorpin.audit.rest.controller",
                 "com.onescorpin.alerts.rest.controller",
                 "com.onescorpin.rest.controller"
        );

        register(JacksonFeature.class);
        register(MultiPartFeature.class);
        register(WadlResource.class);

        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JodaModule());
//        om.registerModule(new JavaTimeModule());
        om.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        om.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
        provider.setMapper(om);
        register(provider);


    }

    @PostConstruct
    /**
     * Add ability to scan additional Spring Beans annotated with @Path
     */
    private void init() {
        //register any additional beans that are path annotated
        Map<String, Object> map = applicationContext.getBeansWithAnnotation(Path.class);
        String packageNames = "com.onescorpin";
        if(map != null && !map.isEmpty()) {
            String beanPackageNames = map.values().stream().map(o -> o.getClass().getPackage().getName()).distinct().collect(Collectors.joining(","));
            if(StringUtils.isNotBlank(beanPackageNames)) {
                packageNames += "," + beanPackageNames;
            }
        }
        if (map != null) {
            Set<Class<?>> pathClasses = map.values().stream().map(o -> o.getClass()).collect(Collectors.toSet());
            registerClasses(pathClasses);
        }
        configureSwagger(packageNames);
    }

    private void configureSwagger(String packageNames) {
        final BeanConfig config = new BeanConfig();
        config.setBasePath("/proxy");
        config.setResourcePackage(packageNames);
        config.setPrettyPrint(true);
        config.setScan(true);
    }
}
