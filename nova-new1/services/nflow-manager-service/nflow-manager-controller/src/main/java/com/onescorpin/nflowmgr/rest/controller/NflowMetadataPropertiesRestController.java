package com.onescorpin.nflowmgr.rest.controller;

/*-
 * #%L
 * onescorpin-nflow-manager-controller
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

import com.onescorpin.annotations.AnnotatedFieldProperty;
import com.onescorpin.nflowmgr.nifi.PropertyExpressionResolver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Returns a list of all properties that can be assigned durning Nflow Registation process
 * this is the list of @MetadataField annotations on the NflowMetadata object
 */
@Api(tags = "Nflow Manager - Templates", produces = "application/json")
@Path("/v1/nflowmgr/metadata-properties")
@Component
public class NflowMetadataPropertiesRestController {

    @Autowired
    PropertyExpressionResolver propertyExpressionResolver;

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets the available metadata properties.",
                  notes = "Returns a list of all properties that can be assigned during the nflow registration process.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the metadata properties.", response = AnnotatedFieldProperty.class, responseContainer = "List")
    )
    public Response getProperties() {
        List<AnnotatedFieldProperty> properties = propertyExpressionResolver.getMetadataProperties();
        return Response.ok(properties).build();
    }
}
