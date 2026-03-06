package io.github.voronkov.easyapialert;

import io.github.voronkov.easyapialert.AlertStateStore.AlertKey;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.Instant;

@RequiredArgsConstructor
public class AlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlertScheduler.class);

    private final StatsCollector statsCollector;
    private final TelegramNotifier notifier;
    private final AlertStateStore stateStore;
    private final AlertProperties props;
    private final AppProperties appProperties;

    @Scheduled(fixedDelay = 10_000)
    public void evaluate() {
        Instant now = Instant.now();

        String appName = appProperties.get();
        Duration window = props.window();
        long minRequests = props.minRequests();
        Duration cooldown = props.cooldown();

        double errorAlertThr = props.errorRateThreshold();
        double errorRecoverThr = props.errorRateRecoverThreshold();

        long p95AlertThr = props.latencyP95ThresholdMs();
        long p95RecoverThr = props.latencyP95RecoverThresholdMs();

        int badToAlert = props.consecutiveBadWindowsToAlert();
        int goodToRecover = props.consecutiveGoodWindowsToRecover();

        for (var e : statsCollector.snapshotWindow(window).entrySet()) {
            var key = e.getKey();
            var s = e.getValue();

            if (isExcluded(key.route())) continue;

            long total = s.total();
            long errors = s.errors5xx();
            long avgLatencyMs = total == 0 ? 0 : (s.totalLatencyMs() / total);
            double rate = total == 0 ? 0.0 : (double) errors / (double) total;

            long p95 = s.p95LatencyMs();
            long p99 = s.p99LatencyMs();

            log.info("stats window={} method={} route={} total={} errors5xx={} errorRate={} avgLatencyMs={} p95LatencyMs={} p99LatencyMs={}",
                    window,
                    key.method(),
                    key.route(),
                    total,
                    errors,
                    String.format("%.4f", rate),
                    avgLatencyMs,
                    p95,
                    p99
            );

            AlertKey errorRateKey = new AlertKey(appName, key.method(), key.route(), "ERROR_RATE");
            AlertKey latencyKey   = new AlertKey(appName, key.method(), key.route(), "LATENCY_P95");

            if (total < minRequests) {
                stateStore.markNeutral(errorRateKey);
            } else if (rate > errorAlertThr) {
                stateStore.markBad(errorRateKey);

                if (stateStore.shouldSendAlertWithStreak(errorRateKey, badToAlert, cooldown, now)) {
                    notifier.send("ALERT:\n" +
                            "Severity: " + calculateSeverity(rate * 100) + "\n" +
                            key.method() + " " + key.route() +
                            " errorRate=" + String.format("%.2f", rate * 100) + "% " +
                            "(threshold " + props.errorRateThresholdPercent() + "%, window " + window + ", n=" + total + ")"
                    );
                    stateStore.markAlertSent(errorRateKey, now);
                }
            } else if (rate <= errorRecoverThr) {
                stateStore.markGood(errorRateKey);

                if (stateStore.shouldSendRecoveredWithStreak(errorRateKey, goodToRecover)) {
                    notifier.send("RECOVERED: " + key.method() + " " + key.route() +
                            " errorRate=" + String.format("%.2f", rate * 100) + "% " +
                            "(recoverThreshold " + props.errorRateRecoverThresholdPercent() + "%, window " + window + ", n=" + total + ")"
                    );
                    stateStore.markRecovered(errorRateKey);
                }
            } else {
                stateStore.markNeutral(errorRateKey);
            }

            if (total < minRequests) {
                stateStore.markNeutral(latencyKey);
            } else if (p95 > p95AlertThr) {
                stateStore.markBad(latencyKey);

                if (stateStore.shouldSendAlertWithStreak(latencyKey, badToAlert, cooldown, now)) {
                    String sev = (props.latencyP99ThresholdMs() != null && props.latencyP99ThresholdMs() > 0 && p99 > props.latencyP99ThresholdMs())
                            ? "Critical"
                            : "Warning";

                    notifier.send("ALERT:\n" +
                            "Severity: " + sev + "\n" +
                            key.method() + " " + key.route() +
                            " p95=" + p95 + "ms (threshold " + p95AlertThr + "ms, window " + window + ", n=" + total + ")"
                    );
                    stateStore.markAlertSent(latencyKey, now);
                }
            } else if (p95 <= p95RecoverThr) {
                stateStore.markGood(latencyKey);

                if (stateStore.shouldSendRecoveredWithStreak(latencyKey, goodToRecover)) {
                    notifier.send("RECOVERED: " + key.method() + " " + key.route() +
                            " p95=" + p95 + "ms (recoverThreshold " + p95RecoverThr + "ms, window " + window + ", n=" + total + ")"
                    );
                    stateStore.markRecovered(latencyKey);
                }
            } else {
                stateStore.markNeutral(latencyKey);
            }
        }
    }

    private boolean isExcluded(String route) {
        if (route == null) return false;

        for (String p : props.excludePaths()) {
            if (p == null || p.isBlank()) continue;

            if (p.endsWith("/**")) {
                String prefix = p.substring(0, p.length() - 3);
                if (route.startsWith(prefix)) return true;
            } else {
                if (route.equals(p)) return true;
            }
        }
        return false;
    }

    private String calculateSeverity(double errorRatePercent) {
        if (errorRatePercent <= props.warningSeverityThresholdPercent()) {
            return "Warning";
        }
        return "Critical";
    }
}