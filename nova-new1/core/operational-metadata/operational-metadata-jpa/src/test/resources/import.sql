-- -
-- #%L
-- onescorpin-operational-metadata-jpa
-- %%
-- Copyright (C) 2017 Onescorpin
-- %%
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
-- #L%
-- -
CREATE ALIAS IF NOT EXISTS abandon_nflow_jobs FOR "com.onescorpin.metadata.jpa.nflow.AbandonNflowJobsStoredProcedureMock.call"

SET foreign_key_checks = 0
