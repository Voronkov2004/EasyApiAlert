package io.github.voronkov.easyapialert;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "easyapialert")
public record AlertProperties(
        Duration window,
        Duration cooldown,
        Long minRequests,
        Double errorRateThresholdPercent,
        List<String> excludePaths,
        Integer warningSeverityThresholdPercent,
        Long latencyP95ThresholdMs,
        Long latencyP95RecoverThresholdMs,
        Long latencyP99ThresholdMs,
        Double errorRateRecoverThresholdPercent,
        Integer consecutiveBadWindowsToAlert,
        Integer consecutiveGoodWindowsToRecover
) {
    public AlertProperties {
        if (window == null) {
            window = Duration.ofMinutes(1);
        }
        if (cooldown == null) {
            cooldown = Duration.ofMinutes(5);
        }
        if (excludePaths == null) {
            excludePaths = List.of();
        }
        if (minRequests == null || minRequests <= 0) {
            minRequests = 10L;
        }
        if (errorRateThresholdPercent == null || errorRateThresholdPercent <= 0) {
            errorRateThresholdPercent = 5.0;
        }
        if (warningSeverityThresholdPercent == null || warningSeverityThresholdPercent <= 0) {
            warningSeverityThresholdPercent = 10;
        }
        if (latencyP95ThresholdMs == null || latencyP95ThresholdMs <= 0) {
            latencyP95ThresholdMs = 1000L;
        }
        if (latencyP95RecoverThresholdMs == null || latencyP95RecoverThresholdMs <= 0)
            latencyP95RecoverThresholdMs = Math.round(latencyP95ThresholdMs * 0.8);

        if (latencyP99ThresholdMs == null || latencyP99ThresholdMs <= 0) {
            latencyP99ThresholdMs = 2000L;
        }

        if (errorRateRecoverThresholdPercent == null || errorRateRecoverThresholdPercent <= 0){
            errorRateRecoverThresholdPercent = errorRateThresholdPercent * 0.8;
        }
        if (consecutiveBadWindowsToAlert == null || consecutiveBadWindowsToAlert <= 0) {
            consecutiveBadWindowsToAlert = 1;
        }
        if (consecutiveGoodWindowsToRecover == null || consecutiveGoodWindowsToRecover <= 0) {
            consecutiveGoodWindowsToRecover = 3;
        }
    }

    public double errorRateThreshold()  {
        return errorRateThresholdPercent / 100.0;
    }

    public double errorRateRecoverThreshold() {
        return errorRateRecoverThresholdPercent / 100.0;
    }
}
