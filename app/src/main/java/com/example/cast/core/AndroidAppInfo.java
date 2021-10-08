/*
 * AppInfo
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on 19 Jan 2014
 * 
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
 */

package com.example.cast.core;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Normalized reference object for information about a DeviceService's app. This
 * object will, in most cases, be used to launch apps.
 * 
 * In some cases, all that is needed to launch an app is the app id.
 */
public class AndroidAppInfo implements JSONSerializable {

    /* renamed from: id */
    String id;
    String name;
    JSONObject raw;

    public AndroidAppInfo() {
    }

    public AndroidAppInfo(String str) {
        this.id = str;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String str) {
        this.id = str;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String str) {
        this.name = str.trim();
    }

    public JSONObject getRawData() {
        return this.raw;
    }

    public void setRawData(JSONObject jSONObject) {
        this.raw = jSONObject;
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("name", this.name);
        jSONObject.put("id", this.id);
        return jSONObject;
    }

    public boolean equals(Object obj) {
        if (obj instanceof AndroidAppInfo) {
            return this.id.equals(((AndroidAppInfo) obj).id);
        }
        return super.equals(obj);
    }
}