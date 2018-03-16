package com.onescorpin.nflowmgr.service.template;

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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.onescorpin.nflowmgr.rest.model.RegisteredTemplate;
import com.onescorpin.json.ObjectMapperSerializer;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.template.NflowManagerTemplate;
import com.onescorpin.metadata.api.template.NflowManagerTemplateProvider;
import com.onescorpin.security.core.encrypt.EncryptionService;
import com.onescorpin.security.rest.controller.SecurityModelTransform;
import com.onescorpin.support.NflowNameUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

/**
 * Transforms data from the domain {@link NflowManagerTemplate} to the REST object {@link RegisteredTemplate}
 */
public class TemplateModelTransform {

    @Inject
    private SecurityModelTransform securityTransform;

    @Inject
    private EncryptionService encryptionService;

    public enum TEMPLATE_TRANSFORMATION_TYPE {
        WITH_NFLOW_NAMES, WITHOUT_NFLOW_NAMES, WITH_SENSITIVE_DATA
    }


    private void prepareForSave(RegisteredTemplate registeredTemplate) {
        //mark password rendertypes as sensitive
        registeredTemplate.getProperties().stream().filter(p -> "password".equalsIgnoreCase(p.getRenderType())).forEach(p -> p.setSensitive(true));
        encryptSensitivePropertyValues(registeredTemplate);
    }

    private void encryptSensitivePropertyValues(RegisteredTemplate registeredTemplate) {
        registeredTemplate.getProperties().stream().filter(property -> property.isSensitive()).forEach(nifiProperty -> nifiProperty.setValue(encryptionService.encrypt(nifiProperty.getValue())));
    }


    public final Function<NflowManagerTemplate, RegisteredTemplate>
        DOMAIN_TO_REGISTERED_TEMPLATE = DOMAIN_TO_REGISTERED_TEMPLATE(true, false);

    public final Function<NflowManagerTemplate, RegisteredTemplate>
        DOMAIN_TO_REGISTERED_TEMPLATE_WITHOUT_NFLOW_NAMES = DOMAIN_TO_REGISTERED_TEMPLATE(false, false);

    public final Function<NflowManagerTemplate, RegisteredTemplate>
        DOMAIN_TO_REGISTERED_TEMPLATE_WITH_SENSITIVE_DATA = DOMAIN_TO_REGISTERED_TEMPLATE(true, true);


    public Function<NflowManagerTemplate, RegisteredTemplate> getTransformationFunction(TEMPLATE_TRANSFORMATION_TYPE transformationType) {
        if(transformationType== null) {
            transformationType = TEMPLATE_TRANSFORMATION_TYPE.WITH_NFLOW_NAMES;
        }
        switch (transformationType) {
            case WITH_NFLOW_NAMES:
                return DOMAIN_TO_REGISTERED_TEMPLATE;
            case WITH_SENSITIVE_DATA:
                return DOMAIN_TO_REGISTERED_TEMPLATE_WITH_SENSITIVE_DATA;
            case WITHOUT_NFLOW_NAMES:
                return DOMAIN_TO_REGISTERED_TEMPLATE_WITHOUT_NFLOW_NAMES;
            default:
                return DOMAIN_TO_REGISTERED_TEMPLATE;
        }
    }

    @Inject
    NflowManagerTemplateProvider templateProvider;

    @Inject
    private SecurityModelTransform actionsTransform;

    public final Function<RegisteredTemplate, NflowManagerTemplate>
        REGISTERED_TEMPLATE_TO_DOMAIN =
        new Function<RegisteredTemplate, NflowManagerTemplate>() {
            @Override
            public NflowManagerTemplate apply(RegisteredTemplate registeredTemplate) {
                //resolve the id
                NflowManagerTemplate.ID domainId = registeredTemplate.getId() != null ? templateProvider.resolveId(registeredTemplate.getId()) : null;
                NflowManagerTemplate domain = null;
                if (domainId != null) {
                    domain = templateProvider.findById(domainId);
                }
                if (domain == null) {
                    domain = templateProvider.ensureTemplate(registeredTemplate.getTemplateName());
                }
                domainId = domain.getId();
                //clean the order from the template
                registeredTemplate.setTemplateOrder(null);

                domain.setNifiTemplateId(registeredTemplate.getNifiTemplateId());
                domain.setAllowPreconditions(registeredTemplate.isAllowPreconditions());
                domain.setName(registeredTemplate.getTemplateName());
                domain.setDataTransformation(registeredTemplate.isDataTransformation());
                domain.setDefineTable(registeredTemplate.isDefineTable());
                domain.setIcon(registeredTemplate.getIcon());
                domain.setIconColor(registeredTemplate.getIconColor());
                domain.setDescription(registeredTemplate.getDescription());
                domain.setOrder(registeredTemplate.getOrder());
                domain.setStream(registeredTemplate.isStream());
                domain.setTemplateTableOption(registeredTemplate.getTemplateTableOption());
                prepareForSave(registeredTemplate);
                String json = ObjectMapperSerializer.serialize(registeredTemplate);
                domain.setJson(json);
                NflowManagerTemplate.State state = NflowManagerTemplate.State.ENABLED;
                try {
                    if (registeredTemplate.getState() != null) {
                        state = NflowManagerTemplate.State.valueOf(registeredTemplate.getState());
                    }
                } catch (IllegalArgumentException e) {
                    // make enabled by default
                }
                domain.setState(state);

                //assign the id back to the ui model
                registeredTemplate.setId(domainId.toString());
                return domain;
            }
        };


    /**
     * Deserialize the JSON of the template
     *
     * @param json                       the template json
     * @param includeEncryptedProperties if true the encrypted properties will be returned.  false will set the property values to ""
     * @return the registered template
     */
    private RegisteredTemplate deserialize(String json, boolean includeEncryptedProperties) {
        RegisteredTemplate template = ObjectMapperSerializer.deserialize(json, RegisteredTemplate.class);
        template.getProperties().stream().filter(nifiProperty -> nifiProperty.isSensitive()).forEach(nifiProperty -> {
            if (!includeEncryptedProperties) {
                nifiProperty.setValue("");
            }
            // else {
            //    String val = encryptionService.decrypt(nifiProperty.getValue());
            //   nifiProperty.setValue(val);
            //}
        });
        return template;
    }

    public final Function<NflowManagerTemplate, RegisteredTemplate>
    DOMAIN_TO_REGISTERED_TEMPLATE(boolean includeNflowNames, boolean includeEncryptedProperties) {

        return new Function<NflowManagerTemplate, RegisteredTemplate>() {
            @Override
            public RegisteredTemplate apply(NflowManagerTemplate domain) {
                String json = domain.getJson();
                RegisteredTemplate template = deserialize(json, includeEncryptedProperties);
                template.setId(domain.getId().toString());
                template.setState(domain.getState().name());
                template.setNifiTemplateId(domain.getNifiTemplateId());
                template.setAllowPreconditions(domain.isAllowPreconditions());
                List<Nflow> nflows = domain.getNflows();
                template.setNflowsCount(nflows == null ? 0 : nflows.size());
                template.setStream(domain.isStream());
                if (includeNflowNames && nflows != null) {
                    template.setNflowNames(nflows.stream().map(nflowManagerNflow -> NflowNameUtil.fullName(nflowManagerNflow.getCategory().getSystemName(), nflowManagerNflow.getName())).collect(
                        Collectors.toSet()));
                }
                if (domain.getCreatedTime() != null) {
                    template.setCreateDate(domain.getCreatedTime().toDate());
                }
                if (domain.getModifiedTime() != null) {
                    template.setUpdateDate(domain.getModifiedTime().toDate());
                }
                template.setOrder(domain.getOrder());
                template.setTemplateTableOption(domain.getTemplateTableOption());

                securityTransform.applyAccessControl(domain, template);

                return template;
            }
        };

    }

    public List<RegisteredTemplate> domainToRegisteredTemplateWithNflowNames(Collection<NflowManagerTemplate> domain) {
        return new ArrayList<>(Collections2.transform(domain, DOMAIN_TO_REGISTERED_TEMPLATE));
    }

    public List<RegisteredTemplate> domainToRegisteredTemplate(Collection<NflowManagerTemplate> domain) {
        return new ArrayList<>(Collections2.transform(domain, DOMAIN_TO_REGISTERED_TEMPLATE_WITHOUT_NFLOW_NAMES));
    }

    public RegisteredTemplate domainToRegisteredTemplateWithNflowNames(NflowManagerTemplate domain) {
        return DOMAIN_TO_REGISTERED_TEMPLATE.apply(domain);
    }


    public RegisteredTemplate domainToRegisteredTemplate(NflowManagerTemplate domain) {
        return DOMAIN_TO_REGISTERED_TEMPLATE_WITHOUT_NFLOW_NAMES.apply(domain);
    }

    public List<NflowManagerTemplate> registeredTemplateToDomain(Collection<RegisteredTemplate> registeredTemplates) {
        return new ArrayList<>(Collections2.transform(registeredTemplates, REGISTERED_TEMPLATE_TO_DOMAIN));
    }
}
