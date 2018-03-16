package com.onescorpin.nflowmgr.security;

/*-
 * #%L
 * onescorpin-nflow-manager-api
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

import com.onescorpin.security.action.Action;

/**
 * Actions involving nflows.
 */
public interface NflowServicesAccessControl {

    Action NFLOWS_SUPPORT = Action.create("accessNflowsSupport",
                                         "Access Nflow Support",
                                         "Allows access to nflows and nflow-related functions");

    Action ACCESS_CATEGORIES = NFLOWS_SUPPORT.subAction("accessCategories",
                                                       "Access Categories",
                                                       "Allows access to categories and their metadata");

    Action EDIT_CATEGORIES = ACCESS_CATEGORIES.subAction("editCategories",
                                                         "Edit Categories",
                                                         "Allows creating, updating and deleting categories");

    Action ADMIN_CATEGORIES = ACCESS_CATEGORIES.subAction("adminCategories",
                                                          "Administer Categories",
                                                          "Allows updating category metadata");

    Action ACCESS_NFLOWS = NFLOWS_SUPPORT.subAction("accessNflows",
                                                  "Access Nflows",
                                                  "Allows access to nflows and their metadata");

    Action EDIT_NFLOWS = ACCESS_NFLOWS.subAction("editNflows",
                                               "Edit Nflows",
                                               "Allows creating, updating, enabling and disabling nflows");

    Action IMPORT_NFLOWS = ACCESS_NFLOWS.subAction("importNflows",
                                                 "Import Nflows",
                                                 "Allows importing of previously exported nflows along with their associated templates (.zip files)");

    Action EXPORT_NFLOWS = ACCESS_NFLOWS.subAction("exportNflows",
                                                 "Export Nflows",
                                                 "Allows exporting nflows definitions with their associated templates (.zip files)");

    Action ADMIN_NFLOWS = ACCESS_NFLOWS.subAction("adminNflows",
                                                "Administer Nflows",
                                                "Allows deleting nflows and editing nflow metadata");

    Action ACCESS_TABLES = NFLOWS_SUPPORT.subAction("accessTables",
                                                   "Access Tables",
                                                   "Allows listing and querying Hive tables");

    Action ACCESS_VISUAL_QUERY = NFLOWS_SUPPORT.subAction("accessVisualQuery",
                                                         "Access Visual Query",
                                                         "Allows access to visual query data wrangler");

    Action ACCESS_TEMPLATES = NFLOWS_SUPPORT.subAction("accessTemplates",
                                                      "Access Templates",
                                                      "Allows access to nflow templates");

    Action EDIT_TEMPLATES = ACCESS_TEMPLATES.subAction("editTemplates",
                                                       "Edit Templates",
                                                       "Allows creating, updating, deleting and sequencing nflow templates");

    Action IMPORT_TEMPLATES = ACCESS_TEMPLATES.subAction("importTemplates",
                                                         "Import Templates",
                                                         "Allows importing of previously exported templates (.xml and .zip files)");

    Action EXPORT_TEMPLATES = ACCESS_TEMPLATES.subAction("exportTemplates",
                                                         "Export Templates",
                                                         "Allows exporting template definitions (.zip files)");

    Action ADMIN_TEMPLATES = ACCESS_TEMPLATES.subAction("adminTemplates",
                                                        "Administer Templates",
                                                        "Allows enabling and disabling nflow templates");


    Action ACCESS_DATASOURCES = NFLOWS_SUPPORT.subAction("accessDatasources",
                                                        "Access Data Sources",
                                                        "Allows (a) access to data sources (b) viewing tables and schemas from a data source (c) using a data source in transformation nflow");

    Action EDIT_DATASOURCES = ACCESS_DATASOURCES.subAction("editDatasources",
                                                           "Edit Data Sources",
                                                           "Allows creating and editing data sources");

    Action ADMIN_DATASOURCES = ACCESS_DATASOURCES.subAction("adminDatasources",
                                                            "Administer Data Sources",
                                                            "Allows getting data source details with sensitive info");

    Action ACCESS_SERVICE_LEVEL_AGREEMENTS = NFLOWS_SUPPORT.subAction("accessServiceLevelAgreements",
                                                                     "Access Service Level Agreements",
                                                                     "Allows access to service level agreements");

    Action EDIT_SERVICE_LEVEL_AGREEMENTS = ACCESS_SERVICE_LEVEL_AGREEMENTS.subAction("editServiceLevelAgreements",
                                                                                     "Edit Service Level Agreements",
                                                                                     "Allows creating and editing service level agreements");

    Action EDIT_SERVICE_LEVEL_AGREEMENT_EMAIL_TEMPLATE = ACCESS_SERVICE_LEVEL_AGREEMENTS.subAction("editServiceLevelAgreementEmailTemplate",
                                                                                     "Edit Service Level Agreement Email Templates",
                                                                                     "Allows creating and editing service level agreement email templates");

    Action ACCESS_GLOBAL_SEARCH = NFLOWS_SUPPORT.subAction("accessSearch",
                                                          "Access Global Search",
                                                          "Allows access to search all indexed columns");
}
