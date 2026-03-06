package io.github.voronkov.easyapialert;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AlertStateStore {

    public enum Status { OK, ALERTING }

    public record AlertKey(String service, String method, String route, String type) {}

    public static final class Entry {
        private volatile Status status = Status.OK;
        private volatile Instant lastSentAt = Instant.EPOCH;

        private volatile int badStreak = 0;
        private volatile int goodStreak = 0;

        public Status status() { return status; }
        public Instant lastSentAt() { return lastSentAt; }
        public int badStreak() { return badStreak; }
        public int goodStreak() { return goodStreak; }

        private synchronized void set(Status status, Instant lastSentAt) {
            this.status = status;
            this.lastSentAt = lastSentAt;
        }

        private synchronized void markBad() {
            badStreak++;
            goodStreak = 0;
        }

        private synchronized void markGood() {
            goodStreak++;
            badStreak = 0;
        }

        private synchronized void markNeutral() {
            badStreak = 0;
            goodStreak = 0;
        }

        private synchronized void resetStreaks() {
            badStreak = 0;
            goodStreak = 0;
        }
    }

    private final Map<AlertKey, Entry> map = new ConcurrentHashMap<>();

    public Entry entry(AlertKey key) {
        return map.computeIfAbsent(key, k -> new Entry());
    }

    public boolean shouldSendAlert(AlertKey key, Duration cooldown, Instant now) {
        Entry e = entry(key);
        boolean cooldownPassed = Duration.between(e.lastSentAt(), now).compareTo(cooldown) >= 0;
        return e.status() == Status.OK || cooldownPassed;
    }

    public boolean shouldSendRecovered(AlertKey key) {
        return entry(key).status() == Status.ALERTING;
    }

    public void markBad(AlertKey key) {
        entry(key).markBad();
    }

    public void markGood(AlertKey key) {
        entry(key).markGood();
    }

    public void markNeutral(AlertKey key) {
        entry(key).markNeutral();
    }

    public boolean shouldSendAlertWithStreak(AlertKey key, int requiredBad, Duration cooldown, Instant now) {
        Entry e = entry(key);

        boolean cooldownPassed = Duration.between(e.lastSentAt(), now).compareTo(cooldown) >= 0;
        boolean streakOk = e.badStreak() >= requiredBad;

        return streakOk && (e.status() == Status.OK || cooldownPassed);
    }

    public boolean shouldSendRecoveredWithStreak(AlertKey key, int requiredGood) {
        Entry e = entry(key);
        return e.status() == Status.ALERTING && e.goodStreak() >= requiredGood;
    }

    public void markAlertSent(AlertKey key, Instant now) {
        Entry e = entry(key);
        e.set(Status.ALERTING, now);
        e.resetStreaks();
    }

    public void markRecovered(AlertKey key) {
        Entry e = entry(key);
        e.set(Status.OK, e.lastSentAt());
        e.resetStreaks();
    }
}