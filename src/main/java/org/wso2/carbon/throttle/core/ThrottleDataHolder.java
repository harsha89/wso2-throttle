/*
* Copyright 2005,2006 WSO2, Inc. http://wso2.com
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
*
*/

package org.wso2.carbon.throttle.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used to keep frequently changing Throttle data. Earlier ConfigurationContext was used to keep this data
 * , but when properties in the CC are modified concurrently, it makes the HashMap to corrupt, leading certain threads
 * to read entries indefinitely.
 */
public class ThrottleDataHolder {

    private static final Log log = LogFactory.getLog(ThrottleDataHolder.class);

    private ConcurrentHashMap<String,CallerContext> callerContextMap = new ConcurrentHashMap<String, CallerContext>();
    private ConcurrentHashMap<String,ThrottleContext> applicationThrottleContexts =
            new ConcurrentHashMap<String, ThrottleContext>();

    public ThrottleDataHolder(){
        log.debug("Created new ThrottleDataHolder");
    }

    public ThrottleContext getThrottleContext(String applicationId) {
        return applicationThrottleContexts.get(applicationId);
    }

    public void addThrottleContext(String applicationId, ThrottleContext applicationThrottleContext) {
        this.applicationThrottleContexts.put(applicationId,applicationThrottleContext);
    }

    public void addCallerContext(String id, CallerContext callerContext){
        callerContextMap.put(id,callerContext);
    }

    public CallerContext getCallerContext(String id){
        return callerContextMap.get(id);
    }

    public void removeCaller(String id) {
        log.debug("Removing caller for "+ id);
        callerContextMap.remove(id);
    }
}