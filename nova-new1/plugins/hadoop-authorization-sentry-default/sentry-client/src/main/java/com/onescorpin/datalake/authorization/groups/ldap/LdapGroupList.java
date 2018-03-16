package com.onescorpin.datalake.authorization.groups.ldap;

import com.onescorpin.datalake.authorization.client.SentryClient;

/*-
 * #%L
 * onescorpin-sentry-client
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

import com.onescorpin.datalake.authorization.client.SentryClientConfig;
import com.onescorpin.datalake.authorization.model.HadoopAuthorizationGroup;
import com.onescorpin.datalake.authorization.model.SentryGroup;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.NamingException;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;
import static org.springframework.ldap.query.LdapQueryBuilder.query;
/*-
 * #%L
 * onescorpin-sentry-client
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

public class LdapGroupList {

    private static final Logger log = LoggerFactory.getLogger(LdapGroupList.class);

    private String OWNER = "nova";
    private String DESCRIPTION = "Nova Authorization Group";
    private String DEFAULT_ID="1";
    List<String> groupInfo;

    public void  getAllGroups(LdapTemplate ldapTemplate , String groupBaseDnPattern) {

        try
        {
            groupInfo =new  ArrayList<>();
            LdapQuery query = query().base(groupBaseDnPattern);
            groupInfo = ldapTemplate.list(query.base());
        }
        catch(NamingException e)
        {
            log.error("Unable to Groups from LDAP " + e.getMessage());
            throw new RuntimeException(e);
        }

    }

    public List<HadoopAuthorizationGroup>  getHadoopAuthorizationList(SentryClientConfig clientConfig , LdapTemplate ldapTemplate)
    {
        List<HadoopAuthorizationGroup> sentryHadoopAuthorizationGroups = new ArrayList<>();
        SentryGroup hadoopAuthorizationGroup = new SentryGroup();
        getAllGroups(ldapTemplate, clientConfig.getLdapGroupDnPattern() );

        for(String group:groupInfo){   

            if(group.contains("cn"))
            {
                /**
                 * Skip Processing - Do not include CN in group list
                 */
            }
            else
            {
                if(group.contains("ou"))
                {
                    group = group.split("=")[1];
                    hadoopAuthorizationGroup.setId(DEFAULT_ID);
                    hadoopAuthorizationGroup.setDescription(DESCRIPTION);
                    hadoopAuthorizationGroup.setName(group);
                    hadoopAuthorizationGroup.setOwner(OWNER);
                    sentryHadoopAuthorizationGroups.add(hadoopAuthorizationGroup);
                    hadoopAuthorizationGroup = new SentryGroup();
                }
            }
        }

        return sentryHadoopAuthorizationGroups;
    }
}
