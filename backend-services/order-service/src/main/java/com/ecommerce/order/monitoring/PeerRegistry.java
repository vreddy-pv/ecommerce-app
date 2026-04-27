package com.ecommerce.order.monitoring;

import com.ecommerce.common.monitoring.HeartbeatResponse.PeerStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PeerRegistry {

    private static final int SUSPECT_THRESHOLD = 3;

    private final Map<String, String> peers = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> missCounts = new ConcurrentHashMap<>();
    private final Map<String, PeerStatus> statuses = new ConcurrentHashMap<>();

    public void registerPeer(String serviceId, String url) {
        peers.put(serviceId, url);
        missCounts.put(serviceId, new AtomicInteger(0));
        statuses.put(serviceId, PeerStatus.HEALTHY);
    }

    public void recordMiss(String serviceId) {
        int count = missCounts.computeIfAbsent(serviceId, k -> new AtomicInteger(0)).incrementAndGet();
        if (count >= SUSPECT_THRESHOLD) {
            statuses.put(serviceId, PeerStatus.SUSPECT);
        }
    }

    public void recordSuccess(String serviceId) {
        missCounts.computeIfAbsent(serviceId, k -> new AtomicInteger(0)).set(0);
        statuses.put(serviceId, PeerStatus.HEALTHY);
    }

    public PeerStatus getStatus(String serviceId) {
        return statuses.get(serviceId);
    }

    public int getMissCount(String serviceId) {
        AtomicInteger c = missCounts.get(serviceId);
        return c == null ? 0 : c.get();
    }

    public Map<String, String> getPeers() {
        return peers;
    }
}
