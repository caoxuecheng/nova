package com.onescorpin.discovery.parser;

/*-
 * #%L
 * onescorpin-schema-discovery-api
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

import com.onescorpin.discovery.schema.Schema;
import com.onescorpin.discovery.util.TableSchemaType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Parses a file to determine its structure and format.
 */
public interface FileSchemaParser {

    /**
     * Parse the provided file and builds a schema with data types that fit the target
     *
     * @param is      the raw data to parse
     * @param charset the character-set (e.g. UTF-8)
     * @param target  the target platform (e.g. Hive)
     * @return a derived schema for the source
     */
    Schema parse(InputStream is, Charset charset, TableSchemaType target) throws IOException;

}
