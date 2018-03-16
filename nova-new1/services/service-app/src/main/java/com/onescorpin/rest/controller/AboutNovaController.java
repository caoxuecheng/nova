package com.onescorpin.rest.controller;

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

import com.onescorpin.NovaVersion;
import com.onescorpin.metadata.api.app.NovaVersionProvider;
import com.onescorpin.security.GroupPrincipal;
import com.onescorpin.security.rest.model.User;

import org.springframework.security.authentication.jaas.JaasGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

import javax.inject.Inject;
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
 * Controller used by 'About Nova' popup
 */
@Api(tags = "Configuration", produces = "application/text")
@Path("/v1/about")
@Component
public class AboutNovaController {

    @Inject
    NovaVersionProvider novaVersionProvider;

    /**
     * Gets information about the current user.
     */
    @GET
    @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets information about the current user.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the user.", response = User.class)
    )
    public Response getCurrentUser() {
        // Create principal from current user
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final User user = new User();
        user.setEnabled(true);

        if (auth.getPrincipal() instanceof UserDetails) {
            final UserDetails details = (UserDetails) auth.getPrincipal();
            user.setGroups(details.getAuthorities().stream()
                               .map(GrantedAuthority::getAuthority)
                               .collect(Collectors.toSet()));
            user.setSystemName(details.getUsername());
        } else {
            user.setGroups(auth.getAuthorities().stream()
                               .filter(JaasGrantedAuthority.class::isInstance)
                               .map(JaasGrantedAuthority.class::cast)
                               .filter(authority -> authority.getPrincipal() instanceof GroupPrincipal)
                               .map(JaasGrantedAuthority::getAuthority)
                               .collect(Collectors.toSet()));
            user.setSystemName(auth.getPrincipal().toString());
        }

        // Return principal
        return Response.ok(user).build();
    }

    /**
     * Get Nova Version for showing in UI About Dialog Box.
     */
    @GET
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation("Gets the version number of Nova.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the version number.", response = String.class)
    )
    public Response getNovaVersion() {

        final String VERSION_NOT_AVAILABLE = "Not Available";
        NovaVersion novaVersion = novaVersionProvider.getCurrentVersion();

        if (novaVersion != null) {
            return Response.ok(novaVersion.getVersion()).build();
        } else {
            return Response.ok(VERSION_NOT_AVAILABLE).build();
        }
    }
}
