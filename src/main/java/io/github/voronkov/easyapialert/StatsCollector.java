package io.github.voronkov.easyapialert;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class StatsCollector {

    private static final long[] LAT_BINS = new long[] {
            5, 10, 25, 50, 75, 100, 150, 200, 300, 400, 500,
            750, 1000, 1500, 2000, 3000, 5000, 10000
    };

    public record Key(String method, String route) {}

    public record CountersSnapshot(
            long total,
            long errors5xx,
            long totalLatencyMs,
            long p95LatencyMs,
            long p99LatencyMs
    ) {}

    private static final class BucketCounters {
        final LongAdder total = new LongAdder();
        final LongAdder errors5xx = new LongAdder();
        final LongAdder totalLatencyMs = new LongAdder();

        final LongAdder[] latencyBins = new LongAdder[LAT_BINS.length];

        BucketCounters() {
            for (int i = 0; i < latencyBins.length; i++) latencyBins[i] = new LongAdder();
        }
    }

    private static final long BUCKET_SECONDS = 10;

    private final ConcurrentHashMap<Key, ConcurrentHashMap<Long, BucketCounters>> data = new ConcurrentHashMap<>();

    private final Clock clock = Clock.systemUTC();

    public void record(String method, String route, int status, long durationMs) {
        long bucket = currentBucket();

        var perKey = data.computeIfAbsent(new Key(method, route), k -> new ConcurrentHashMap<>());
        var counters = perKey.computeIfAbsent(bucket, b -> new BucketCounters());

        int idx = latencyBinIndex(durationMs);
        counters.latencyBins[idx].increment();

        counters.total.increment();
        counters.totalLatencyMs.add(durationMs);
        if (status >= 500) counters.errors5xx.increment();
    }

    public Map<Key, CountersSnapshot> snapshotWindow(Duration window) {
        long windowSeconds = Math.max(1, window.getSeconds());
        long bucketsToKeep = (windowSeconds + BUCKET_SECONDS - 1) / BUCKET_SECONDS;
        bucketsToKeep = Math.max(1, bucketsToKeep);

        long nowBucket = currentBucket();
        long fromBucket = nowBucket - bucketsToKeep + 1;

        Map<Key, CountersSnapshot> out = new HashMap<>();

        data.forEach((key, buckets) -> {
            long total = 0;
            long errors = 0;
            long latency = 0;

            long[] bins = new long[LAT_BINS.length];

            for (long b = fromBucket; b <= nowBucket; b++) {
                BucketCounters c = buckets.get(b);
                if (c == null) continue;

                total += c.total.sum();
                errors += c.errors5xx.sum();
                latency += c.totalLatencyMs.sum();

                for (int i = 0; i < LAT_BINS.length; i++) {
                    bins[i] += c.latencyBins[i].sum();
                }
            }

            if (total > 0) {
                long p95 = percentileFromBins(bins, total, 0.95);
                long p99 = percentileFromBins(bins, total, 0.99);
                out.put(key, new CountersSnapshot(total, errors, latency, p95, p99));
            }

            long keepFrom = fromBucket - 1;
            buckets.keySet().removeIf(idx -> idx < keepFrom);

            if (buckets.isEmpty()) {
                data.remove(key, buckets);
            }
        });

        return out;
    }

    private long currentBucket() {
        return clock.instant().getEpochSecond() / BUCKET_SECONDS;
    }

    private static int latencyBinIndex(long ms) {
        for (int i = 0; i < LAT_BINS.length; i++) {
            if (ms <= LAT_BINS[i]) return i;
        }
        return LAT_BINS.length - 1;
    }

    private static long percentileFromBins(long[] bins, long total, double p) {
        if (total <= 0) return 0;
        long need = (long) Math.ceil(total * p);
        long acc = 0;
        for (int i = 0; i < bins.length; i++) {
            acc += bins[i];
            if (acc >= need) return LAT_BINS[i];
        }
        return LAT_BINS[LAT_BINS.length - 1];
    }
}
