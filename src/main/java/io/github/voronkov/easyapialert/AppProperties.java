package io.github.voronkov.easyapialert;

public class AppProperties {
    private final String serviceName;

    public AppProperties(String serviceName) {
        this.serviceName = (serviceName == null || serviceName.isBlank()) ? "application" : serviceName;
    }

    public String get() { return serviceName; }
}
