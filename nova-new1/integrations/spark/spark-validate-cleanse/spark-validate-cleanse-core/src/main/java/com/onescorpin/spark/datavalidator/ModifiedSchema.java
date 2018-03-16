package com.onescorpin.spark.datavalidator;

/*-
 * #%L
 * nova-spark-validate-cleanse-core
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

import com.onescorpin.policy.FieldPolicy;
import com.onescorpin.spark.datavalidator.functions.CleanseAndValidateRow;
import com.onescorpin.spark.validation.HCatDataType;

import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

class ModifiedSchema {

    private static final Logger log = LoggerFactory.getLogger(ModifiedSchema.class);

    @Nonnull
    public static StructType getValidTableSchema(@Nonnull final StructField[] nflowFields, @Nonnull final StructField[] validFields, @Nonnull final FieldPolicy[] policies) {
        // Map of the lower nflow valid name to the field type
        final Map<String, StructField> validFieldsMap = new HashMap<>();
        for (StructField validField : validFields) {
            String lowerFieldName = validField.name().toLowerCase();
            validFieldsMap.put(lowerFieldName, validField);
        }

        // List of all the nflowFieldNames that are part of the policyMap
        final List<String> policyMapNflowFieldNames = new ArrayList<>();
        // A map of the nflowFieldName to validFieldName
        final Map<String, String> validFieldToNflowFieldMap = new HashMap<>();
        // List of all those validFieldNames that have a standardizer on them
        final List<String> validFieldsWithStandardizers = new ArrayList<>();
        for (FieldPolicy policy : policies) {
            if (policy.getField() != null) {
                String nflowFieldName = policy.getNflowField().toLowerCase();
                String fieldName = policy.getField().toLowerCase();
                policyMapNflowFieldNames.add(nflowFieldName);
                validFieldToNflowFieldMap.put(fieldName, nflowFieldName);
                if (policy.hasStandardizationPolicies()) {
                    validFieldsWithStandardizers.add(fieldName);
                }
            }
        }

        List<StructField> fieldsList = new ArrayList<>(nflowFields.length);
        for (StructField nflowField : nflowFields) {
            String lowerNflowFieldName = nflowField.name().toLowerCase();
            if (policyMapNflowFieldNames.contains(lowerNflowFieldName)) {
                StructField field = nflowField;
                //get the corresponding valid table field name
                String lowerFieldName = validFieldToNflowFieldMap.get(lowerNflowFieldName);
                //if we are standardizing then use the field type matching the _valid table
                if (validFieldsWithStandardizers.contains(lowerFieldName)) {
                    //get the valid table
                    field = validFieldsMap.get(lowerFieldName);
                    HCatDataType dataType = HCatDataType.createFromDataType(field.name(), field.dataType().simpleString());
                    if (dataType != null && dataType.isDateOrTimestamp()) {
                        field = new StructField(field.name(), DataTypes.StringType, field.nullable(), field.metadata());
                    }
                }
                fieldsList.add(field);
            } else {
                log.warn("Valid table field {} is not present in policy map", lowerNflowFieldName);
            }
        }

        // Insert the two custom fields before the processing partition column
        fieldsList.add(new StructField(CleanseAndValidateRow.PROCESSING_DTTM_COL, DataTypes.StringType, true, Metadata.empty()));
        fieldsList.add(fieldsList.size() - 1, new StructField(CleanseAndValidateRow.REJECT_REASON_COL, DataTypes.StringType, true, Metadata.empty()));

        return new StructType(fieldsList.toArray(new StructField[0]));
    }

    private ModifiedSchema() {
        throw new UnsupportedOperationException("Instances of SchemaBuild cannot be constructed");
    }
}
