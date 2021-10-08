/*
 * JSONSerializable
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Jason Lai on 30 Jan 2014
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

public interface JSONSerializable {
    public JSONObject toJSONObject() throws JSONException;
}
