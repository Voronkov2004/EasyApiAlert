# EasyApiAlert

Lightweight developer-oriented monitoring and alerting library for Java Spring Boot APIs.

EasyApiAlert provides request-level monitoring, latency tracking, error-rate alerting, and Telegram notifications with minimal setup effort. The library is designed for situations where developers need fast operational visibility without deploying a full observability stack.

---

## Features

- Automatic integration into Spring Boot applications
- Request-level monitoring
- Error-rate-based alerting
- Latency monitoring using p95 and p99 indicators
- Telegram alert notifications
- Configurable monitoring windows and thresholds
- Route normalization
- Cooldown and recovery logic
- Excluded path support
- Minimal external infrastructure requirements

---

# Why EasyApiAlert?

Modern observability platforms such as Prometheus, Grafana, and Alertmanager provide powerful capabilities, but they usually require multiple external components, configuration steps, dashboards, metric storage, and alert-routing infrastructure.

EasyApiAlert focuses on a different goal:

> Fast adoption and immediate operational visibility for Spring Boot APIs.

The library is intended for:

- small internal APIs
- educational projects
- prototypes
- developer-managed services
- lightweight production environments

Instead of deploying a full monitoring stack, developers can add a dependency, configure Telegram credentials, and start receiving alerts within minutes.

---

# What the Library Monitors

EasyApiAlert collects request-level runtime statistics, including:

- HTTP method
- request route
- response status code
- request duration

The library aggregates metrics over configurable time windows and evaluates:

- 5xx error rates
- p95 latency
- p99 latency

---

# Example Alerts

## Error Rate Alert

```text
[CRITICAL] High Error Rate Detected

Service: example-api
Route: GET /api/users/{id}

Error rate: 35.0%
Threshold: 5.0%
Requests: 120
Window: 1m
```

## Latency Alert

```text
[WARNING] High Latency Detected

Service: example-api
Route: POST /api/orders

p95 latency: 1800 ms
Threshold: 1000 ms
Requests: 95
Window: 1m
```

## Recovery Alert

```text
[RECOVERED] Route Stabilized

Service: example-api
Route: GET /api/users/{id}
```

---

# Installation

## Gradle

```groovy
dependencies {
    implementation 'YOUR_GROUP_ID:easy-api-alert:VERSION'
}
```

## Maven

```xml
<dependency>
    <groupId>YOUR_GROUP_ID</groupId>
    <artifactId>easy-api-alert</artifactId>
    <version>VERSION</version>
</dependency>
```

---

# Configuration

```yaml
spring:
  application:
    name: example-api

telegram:
  bot:
    token: ${ALERT_TELEGRAM_BOT_TOKEN}
    chat-id: ${ALERT_TELEGRAM_CHAT_ID}

easyapialert:
  enabled: true

  window: 1m
  cooldown: 30s

  min-requests: 5

  error-rate-threshold-percent: 5

  latency-p95-threshold-ms: 1000
  latency-p99-threshold-ms: 2000

  exclude-paths:
    - /error
    - /actuator
    - /actuator/**
    - /swagger-ui/**
```

---

# Usage

After startup, EasyApiAlert automatically begins collecting request-level metrics and evaluating alert conditions.

No external monitoring infrastructure is required.

---

# Monitoring Logic

EasyApiAlert performs:

- request interception using Spring MVC interceptor mechanisms
- in-memory metric aggregation
- periodic threshold evaluation
- route-level alerting
- cooldown handling
- recovery detection

Metrics are aggregated in configurable time windows using lightweight in-memory structures.

---

# Route Normalization

Dynamic path segments are normalized to reduce metric cardinality.

Example:

```text
/api/v1/users/123/orders/42
```

becomes:

```text
/api/v1/users/{id}/orders/{id}
```

Supported identifier detection includes:

- numeric IDs
- UUIDs
- MongoDB ObjectIds
- ULIDs

---

# Configuration Options

| Property | Description | Default |
|---|---|---|
| `window` | Monitoring window | `1m` |
| `cooldown` | Alert cooldown duration | `1m` |
| `min-requests` | Minimum requests before evaluation | `10` |
| `error-rate-threshold-percent` | Error-rate alert threshold | `5` |
| `latency-p95-threshold-ms` | p95 latency threshold | `1000` |
| `latency-p99-threshold-ms` | p99 latency threshold | `2000` |
| `exclude-paths` | Excluded monitoring paths | predefined defaults |

---

# Design Goals

EasyApiAlert intentionally prioritizes:

- simplicity
- fast adoption
- low runtime overhead
- minimal infrastructure dependence
- actionable alerts

The library is not intended to replace full observability platforms such as Prometheus or OpenTelemetry. Instead, it acts as a lightweight alternative for situations where operational simplicity matters more than analytical breadth.

---

# Comparison with Traditional Monitoring Stack

| Criterion | Prometheus + Grafana + Alertmanager | EasyApiAlert |
|---|---|---|
| Metrics collection | Yes | Yes |
| Long-term storage | Yes | No |
| Dashboards | Yes | No |
| External infrastructure | Required | Not required |
| Alert routing | Advanced | Basic |
| Telegram integration | Additional setup | Built-in |
| Setup complexity | Medium to High | Very Low |
| Time to first alert | Often hours in greenfield setups | Usually minutes |

---

# Limitations

EasyApiAlert intentionally trades breadth for simplicity.

Current limitations include:

- no long-term metrics persistence
- no built-in dashboards
- no distributed tracing
- no advanced notification routing
- single-application focus
- approximate percentile calculations

---

# Suitable Use Cases

EasyApiAlert is especially suitable for:

- internal APIs
- student projects
- prototypes
- developer-owned services
- lightweight production applications

---

# Future Improvements

Potential future extensions include:

- additional notification channels
- persistent metrics storage
- dashboard integration
- distributed tracing support
- richer alert formatting
- deeper OpenTelemetry integration
