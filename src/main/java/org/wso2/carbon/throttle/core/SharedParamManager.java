package org.wso2.carbon.throttle.core;

import com.hazelcast.core.HazelcastInstance;
import org.wso2.carbon.throttle.core.internal.ThrottleServiceDataHolder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SharedParamManager {

	private static Map<String, Long> counters= new ConcurrentHashMap<String, Long>();//Locally managed counters map for non clustered environment
	private static Map<String, Long> timestamps = new ConcurrentHashMap<String, Long>();//Locally managed time stamps map for non clustered environment

	/**
	 * Return hazelcast shared counter for this caller context
	 *
	 * @return shared hazelcast current shared counter
	 */
	public static long getDistributedCounter(String id) {
		HazelcastInstance hazelcastInstance = getHazelcastInstance();
		if(hazelcastInstance != null) {
			return hazelcastInstance.getAtomicLong(id).get();
		} else {
			Long counter = counters.get(id);
			if(counter != null) {
				return counter;
			} else {
				counters.put(id, 0L);
				return 0;
			}
		}
	}

	public static void setDistributedCounter(String id, long value) {
		HazelcastInstance hazelcastInstance = getHazelcastInstance();
		if(hazelcastInstance != null) {
			hazelcastInstance.getAtomicLong(id).set(value);
		} else {
			counters.put(id, value);
		}
	}

	public static long addAndGetDistributedCounter(String id, long value) {
		HazelcastInstance hazelcastInstance = getHazelcastInstance();
		if(hazelcastInstance != null) {
			return hazelcastInstance.getAtomicLong(id).addAndGet(value);
		} else {
			long currentCount = counters.get(id);
			long updatedCount = currentCount + value;
			counters.put(id, updatedCount);
			return updatedCount;
		}
	}

	public static void removeCounter(String id) {
		HazelcastInstance hazelcastInstance = getHazelcastInstance();
		if(hazelcastInstance != null) {
			hazelcastInstance.getAtomicLong(id).destroy();
		} else {
			counters.remove(id);
		}
	}

	public static long getSharedTimestamp(String id) {
		String key = ThrottleConstants.THROTTLE_TIMESTAMP_KEY + id;
		HazelcastInstance hazelcastInstance = getHazelcastInstance();
		if(hazelcastInstance != null) {
			return hazelcastInstance.getAtomicLong(key).get();
		} else {
			Long timestamp = timestamps.get(id);
			if(timestamp != null) {
				return timestamp;
			} else {
				timestamps.put(key, 0L);
				return 0;
			}
		}
	}

	public static void setSharedTimestamp(String id, long timestamp) {
		String key = ThrottleConstants.THROTTLE_TIMESTAMP_KEY + id;
		HazelcastInstance hazelcastInstance = getHazelcastInstance();
		if(hazelcastInstance != null) {
			hazelcastInstance.getAtomicLong(key).set(timestamp);
		} else {
			timestamps.put(id, timestamp);
		}
	}

	public static void removeTimestamp(String id) {
		String key = ThrottleConstants.THROTTLE_TIMESTAMP_KEY + id;
		HazelcastInstance hazelcastInstance = getHazelcastInstance();
		if(hazelcastInstance != null) {
			hazelcastInstance.getAtomicLong(key).destroy();
		} else {
			timestamps.remove(key);
		}
	}

	private static HazelcastInstance getHazelcastInstance() {
		return ThrottleServiceDataHolder.getInstance().getHazelCastInstance();
	}
}
