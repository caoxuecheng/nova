package com.onescorpin.metadata.api.nflow.security;

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
 * Actions involving an individual nflow.
 */
public interface NflowAccessControl {
    
    Action ACCESS_NFLOW = Action.create("accessNflow",
                                            "Access Nflow",
                                            "Allows the ability to view the nflow and see basic summary information about it");
    Action EDIT_SUMMARY = ACCESS_NFLOW.subAction("editNflowSummary",
                                                 "Edit Summary",
                                                 "Allows editing of the summary information about the nflow");
    Action ACCESS_DETAILS = ACCESS_NFLOW.subAction("accessNflowDetails",
                                                 "Access Details",
                                                 "Allows viewing the full details about the nflow");
    Action EDIT_DETAILS = ACCESS_DETAILS.subAction("editNflowDetails",
                                                   "Edit Details",
                                                    "Allows editing of the details about the nflow");
    Action DELETE = ACCESS_DETAILS.subAction("deleteNflow",
                                             "Delete",
                                             "Allows deleting the nflow");
    Action ENABLE_DISABLE = ACCESS_DETAILS.subAction("enableNflow",
                                                     "Enable/Disable",
                                                     "Allows enabling and disabling the nflow");
    Action EXPORT = ACCESS_DETAILS.subAction("exportNflow",
                                             "Export",
                                             "Allows exporting the nflow");
//    Action SCHEDULE_NFLOW = ACCESS_DETAILS.subAction("scheduleNflow",
//                                                    "Change Schedule",
//                                                    "Allows the ability to change the execution schedule of the nflow");
    Action ACCESS_OPS = ACCESS_NFLOW.subAction("accessNflowOperations",
                                                   "Access Operations",
                                                   "Allows the ability to see the operational history of the nflow");
    Action CHANGE_PERMS = ACCESS_NFLOW.subAction("changeNflowPermissions",
                                                "Change Permissions",
                                                "Allows editing of the permissions that grant access to the nflow");
}
