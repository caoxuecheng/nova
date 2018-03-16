package com.onescorpin.nifi.provenance.model.util;

/*-
 * #%L
 * onescorpin-nifi-provenance-model
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

import java.util.concurrent.atomic.AtomicLong;


public class LongIdGenerator {

    final static AtomicLong novaEventIdGenerator = new AtomicLong(-1);


    public static Long nextId(){
        Long next = novaEventIdGenerator.decrementAndGet();
        if(next <= Long.MIN_VALUE){
            next = reset();
        }
        return next;

    }

    private static Long reset(){
        novaEventIdGenerator.set(-1);
        return novaEventIdGenerator.get();
    }
}
