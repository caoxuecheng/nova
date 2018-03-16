package com.onescorpin.app;

/*-
 * #%L
 * onescorpin-service-app-api
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

/**
 * Allow classes to subscribe to the startup event of Nova.
 * The startup event is fired when Spring has refreshed its context and started.
 */
public interface ServicesApplicationStartup {

    enum ApplicationType{
        UPGRADE,NOVA
    }

    /**
     * Subscribe to the start of the application
     *
     * @param o a listener that will be called when the application starts and Spring is loaded
     */
    void subscribe(ServicesApplicationStartupListener o);
}
