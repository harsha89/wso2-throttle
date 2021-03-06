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

package org.wso2.carbon.throttle.core.impl.ipbase;

import org.wso2.carbon.throttle.core.ThrottleConfiguration;
import org.wso2.carbon.throttle.core.ThrottleConstants;
import org.wso2.carbon.throttle.core.ThrottleContext;
import org.wso2.carbon.throttle.core.ThrottleReplicator;

/**
 * Holds all the run time data for all IP based  remote callers
 */

public class IPBaseThrottleContext extends ThrottleContext {

    public IPBaseThrottleContext(ThrottleConfiguration throttleConfiguration, ThrottleReplicator replicator) {
        super(throttleConfiguration,replicator);
    }

    public int getType() {
        return ThrottleConstants.IP_BASE;
    }

}
