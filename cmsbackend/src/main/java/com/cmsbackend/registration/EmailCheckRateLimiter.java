package com.cmsbackend.registration;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailCheckRateLimiter {
    private static final long WINDOW_MILLIS = 60_000;
    private static final int MAX_TRACKED_CLIENTS = 10_000;
    private final Clock clock;
    private final int perClientLimit;
    private final int globalLimit;
    private final Map<String, Window> clients = new HashMap<>();
    private Window global;

    private record Window(long startedAt, int count) { }

    public EmailCheckRateLimiter(Clock clock,
            @Value("${cms.registration.email-check.per-client-per-minute:10}") int perClientLimit,
            @Value("${cms.registration.email-check.global-per-minute:300}") int globalLimit) {
        this.clock = clock;
        this.perClientLimit = perClientLimit;
        this.globalLimit = globalLimit;
    }

    public synchronized boolean allow(String remoteAddress) {
        long now = clock.millis();
        String client = remoteAddress == null ? "unknown" : remoteAddress;
        if (global == null || now - global.startedAt >= WINDOW_MILLIS) global = new Window(now, 0);
        if (global.count >= globalLimit) return false;
        Window current = clients.get(client);
        if (current == null || now - current.startedAt >= WINDOW_MILLIS) current = new Window(now, 0);
        if (current.count >= perClientLimit) return false;
        if (!clients.containsKey(client) && clients.size() >= MAX_TRACKED_CLIENTS) {
            clients.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= WINDOW_MILLIS);
            if (clients.size() >= MAX_TRACKED_CLIENTS) return false;
        }
        clients.put(client, new Window(current.startedAt, current.count + 1));
        global = new Window(global.startedAt, global.count + 1);
        return true;
    }
}
