package com.onescorpin.servicemonitor.check;

/*-
 * #%L
 * onescorpin-service-monitor-pipeline-db
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


import com.onescorpin.servicemonitor.db.dao.DatabaseServiceCheckDao;
import com.onescorpin.servicemonitor.model.ServiceStatusResponse;

import javax.inject.Inject;

/**
 */

public class PipelineDatabaseServiceStatusCheck implements ServiceStatusCheck {

    @Inject
    DatabaseServiceCheckDao databaseServiceCheckDao;


    @Override
    public ServiceStatusResponse healthCheck() {

        return databaseServiceCheckDao.healthCheck();


    }

    protected void setDatabaseServiceCheckDao(DatabaseServiceCheckDao databaseServiceCheckDao) {
        this.databaseServiceCheckDao = databaseServiceCheckDao;
    }
}
