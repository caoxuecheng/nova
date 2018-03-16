package com.onescorpin.policy.rest.controller;

/*-
 * #%L
 * onescorpin-field-policy-controller
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

import com.onescorpin.policy.FieldPolicyCache;
import com.onescorpin.policy.rest.model.FieldStandardizationRule;
import com.onescorpin.policy.rest.model.FieldValidationRule;

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
 * Returns a list of all properties that can be assigned during Nflow Registration process
 * this is the list of classes annotated with the {@link com.onescorpin.policy.standardization.Standardizer} and {@link com.onescorpin.policy.validation.Validator}
 */
@Api(tags = "Nflow Manager - Nflows", produces = "application/json")
@Path("/v1/field-policies")
public class FieldPolicyRestController {

    /**
     * Return a list of the possible Standardization rules available for an end user to configure in the nflow user interface
     *
     * @return a list of the possible Standardization rules available for an end user to configure in the nflow user interface
     */
    @GET
    @Path("/standardization")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the available standardization policies.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the standardization policies.", response = FieldStandardizationRule.class, responseContainer = "List")
    )
    public Response getStandardizationPolicies() {
        List<FieldStandardizationRule> standardizationRules = FieldPolicyCache.getStandardizationPolicies();
        return Response.ok(standardizationRules).build();
    }

    /**
     * Return a list of possible Validation rules available for an end user to configure in the nflow user interface
     *
     * @return a list of possible Validation rules available for an end user to configure in the nflow user interface
     */
    @GET
    @Path("/validation")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the available validation policies.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the validation policies.", response = FieldValidationRule.class, responseContainer = "List")
    )
    public Response getValidationPolicies() {
        List<FieldValidationRule> standardizationRules = FieldPolicyCache.getValidationPolicies();
        return Response.ok(standardizationRules).build();
    }
}
