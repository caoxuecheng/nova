package com.onescorpin.servicemonitor.novadataservice.util;

/*-
 * #%L
 * onescorpin-service-monitor-novadatabase
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
import com.sun.jersey.core.util.Base64;
import sun.misc.BASE64Encoder;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Token {
    public Token() {
    }

    private String token(String str) {
        try {
            MessageDigest e = MessageDigest.getInstance("SHA-256");
            e.update(str.getBytes());
            byte[] xx = e.digest();
            byte[] a = Base64.encode(xx);
            return new String(a, "UTF-8");
        } catch (Exception var5) {
            return null;
        }
    }

    public String token(String user_id, String interid, String time, String token) throws Exception {
        SimpleDateFormat greenwich = new SimpleDateFormat("EEE,d MMM yyyy hh:mm:ss z", Locale.US);
        time = greenwich.parse(time).getTime() + "";
        String up = this.token(user_id + interid + time);
        String down = this.getMD5(token);
        return up + down;
    }

    private String getMD5(String str) {
        try {
            MessageDigest e = MessageDigest.getInstance("MD5");
            new BASE64Encoder();
            e.update(str.getBytes());
            return (new BigInteger(1, e.digest())).toString(16).toUpperCase();
        } catch (Exception var4) {
            return null;
        }
    }
}