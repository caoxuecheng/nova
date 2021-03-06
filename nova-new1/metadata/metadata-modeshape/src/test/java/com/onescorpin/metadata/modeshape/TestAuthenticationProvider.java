/**
 *
 */
package com.onescorpin.metadata.modeshape;

/*-
 * #%L
 * onescorpin-metadata-modeshape
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

import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.security.AuthenticationProvider;

import java.util.Map;

import javax.jcr.Credentials;

/**
 *
 */
public class TestAuthenticationProvider implements AuthenticationProvider {

    /* (non-Javadoc)
     * @see org.modeshape.jcr.security.AuthenticationProvider#authenticate(javax.jcr.Credentials, java.lang.String, java.lang.String, org.modeshape.jcr.ExecutionContext, java.util.Map)
     */
    @Override
    public ExecutionContext authenticate(Credentials credentials,
                                         String repositoryName,
                                         String workspaceName,
                                         ExecutionContext repositoryContext,
                                         Map<String, Object> sessionAttributes) {
        if (credentials instanceof TestCredentials) {
            return repositoryContext.with(new TestSecurityContext());
        } else {
            return null;
        }
    }

}
