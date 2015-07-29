/*
*  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.throttle.core;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/* Runs a scheduled task, which replicates CallerContexts through the cluster.
 * Frequency of the job can be controlled
 */

public class ThrottleWindowReplicator {
    private static final Log log = LogFactory.getLog(ThrottleWindowReplicator.class);
    private static final int MAX_KEYS_TO_REPLICATE = 1000;
    private static int keysToReplicate = MAX_KEYS_TO_REPLICATE;
    private static final int REPLICATOR_THREAD_POOL_SIZE = 1;
    private static int replicatorPoolSize = REPLICATOR_THREAD_POOL_SIZE;

    private ConfigurationContext configContext;

    private int replicatorCount;

    private Set<String> set = new ConcurrentSkipListSet<String>();

    public ThrottleWindowReplicator() {

        String replicatorThreads = System.getProperty("throttlingWindowReplicator.pool.size");
        if (replicatorThreads != null) {
            replicatorPoolSize = Integer.parseInt(replicatorThreads);
        }
        log.debug("Replicator pool size set to " + replicatorPoolSize);
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(replicatorPoolSize,
                new ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        t.setName("Throttle Replicator - " + replicatorCount++);
                        return t;
                    }
                });
        String windowReplicationFrequency = System.getProperty("throttlingWindowReplicator.replication.frequency");
        if (windowReplicationFrequency == null) {
            windowReplicationFrequency = "50";
        }
        log.debug("Throttling Frequency set to " + windowReplicationFrequency);
        String maxKeysToReplicate = System.getProperty("throttlingWindowReplicator.keys.to.replicate");
        if (maxKeysToReplicate != null) {
            keysToReplicate = Integer.parseInt(maxKeysToReplicate);
        }
        log.debug("Max keys to Replicate " + keysToReplicate);
        for (int i = 0; i < replicatorPoolSize; i++) {
            executor.scheduleAtFixedRate(new ReplicatorTask(), Integer.parseInt(windowReplicationFrequency),
                    Integer.parseInt(windowReplicationFrequency), TimeUnit.MILLISECONDS);
        }
    }

    public void setConfigContext(ConfigurationContext configContext) {
        if (this.configContext == null) {
            this.configContext = configContext;
        }
    }

    public void add(String key) {
        if (configContext == null) {
            throw new IllegalStateException("ConfigurationContext has not been set");
        }
        synchronized (key.intern()) {
            set.add(key);
        }
        if (log.isDebugEnabled()) {
            log.trace("Adding key " + key + " to replication list");
        }
    }

    private class ReplicatorTask implements Runnable {
        public void run() {
            try {
                if (!set.isEmpty()) {
                    for (String key : set) {
                        String callerId;
                        long localFirstAcccessTime;
                        synchronized (key.intern()) {
                            ThrottleDataHolder dataHolder = (ThrottleDataHolder)
                                    configContext.getProperty(ThrottleConstants.THROTTLE_INFO_KEY);
                            CallerContext callerContext = dataHolder.getCallerContext(key);
                            //get hazlecast instance and update counters
                            //If both global and local counters are 0 then that means cleanup caller
                            if (callerContext != null) {
                                callerId = callerContext.getId();
                                long sharedTimestamp = SharedParamManager.getSharedTimestamp(callerContext.getId());
                                long sharedNextWindow = sharedTimestamp + callerContext.getUnitTime();
                                localFirstAcccessTime = callerContext.getFirstAccessTime();
                                if(localFirstAcccessTime < sharedTimestamp) {
                                     callerContext.setFirstAccessTime(sharedTimestamp);
                                     callerContext.setNextTimeWindow(sharedNextWindow);
                                } else if(localFirstAcccessTime > sharedTimestamp && localFirstAcccessTime < sharedNextWindow) {
                                    callerContext.setFirstAccessTime(sharedTimestamp);
                                    callerContext.setNextTimeWindow(sharedNextWindow);
                                } else {
                                     SharedParamManager.setSharedTimestamp(callerId, localFirstAcccessTime);
                                     SharedParamManager.setDistributedCounter(callerId, 0);
                                }
                            }
	                        set.remove(key);
                        }

                    }
                }
            } catch (Throwable t) {
                log.error("Could not replicate throttle data", t);
            }
        }
    }

}
