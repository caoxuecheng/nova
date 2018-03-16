package com.onescorpin.rest.exception.nifi;

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

import com.onescorpin.nifi.nflowmgr.NflowRollbackException;
import com.onescorpin.rest.exception.BaseExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 */
@Provider
@Configuration
public class NflowRollbackExceptionMapper extends BaseExceptionMapper implements ExceptionMapper<NflowRollbackException> {

    private static final Logger log = LoggerFactory.getLogger(NflowRollbackExceptionMapper.class);

    @Override
    public Response toResponse(NflowRollbackException e) {
        return defaultResponse(e);
    }

}
