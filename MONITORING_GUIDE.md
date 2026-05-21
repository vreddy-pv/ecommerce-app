# Monitoring & Logging Guide

**Complete production-ready monitoring for the autonomous agent.**

Covers: structured logging, metrics collection, error tracking, dashboards, alerts.

---

## Part 1: Backend Logging (Python)

### Setup

The logging system is already integrated. Just initialize it in your app startup:

**In `app/main.py` (already done):**
```python
from .logging_config import setup_logging, agent_logger

# On startup
setup_logging(level="INFO")  # or DEBUG for verbose
logger = agent_logger()
```

### Log Destinations

```
Logs go to:
├── Console (stdout) — real-time in Docker
├── /var/log/agent/agent.log — all logs
└── /var/log/agent/agent-errors.log — errors only
```

### Log Format (JSON)

Every log is JSON, ready for parsing:

```json
{
  "timestamp": "2025-05-16T10:30:45.123Z",
  "level": "INFO",
  "logger": "app.agents",
  "message": "Fraud detection",
  "module": "agents",
  "function": "run_agent",
  "line": 156,
  "session_id": "uuid-123",
  "order_id": "ORD-456",
  "fraud_score": 78.5,
  "risk_level": "HIGH"
}
```

### Structured Logging Example

**In agent code:**
```python
from .logging_config import agent_logger

logger = agent_logger()

# Log with context
logger.fraud_detection(
    session_id="uuid-123",
    order_id="ORD-456",
    fraud_score=78.5,
    risk_level="HIGH"
)

# Or custom context
logger.info(
    "Order processed",
    session_id="uuid-123",
    user_id="user-789",
    tool_name="cancel_order",
    duration_ms=234
)
```

### View Logs

**Docker:**
```bash
# Real-time logs
docker logs -f ecommerce-mcp-server

# Filter by level
docker logs ecommerce-mcp-server | grep '"level":"ERROR"'

# Filter by session
docker logs ecommerce-mcp-server | grep "session_id"
```

**Files (if mounted):**
```bash
# All logs
tail -f /var/log/agent/agent.log

# Errors only
tail -f /var/log/agent/agent-errors.log

# Pretty-print JSON logs
tail -f /var/log/agent/agent.log | jq '.'
```

---

## Part 2: Metrics Collection

### Setup (Already Done)

Metrics are automatically collected. Access via `/metrics` endpoint:

```bash
curl http://localhost:8090/metrics | jq '.'
```

### Metrics Available

```json
{
  "requests": {
    "total": 42,
    "errors": 2,
    "error_rate": 4.76
  },
  "response_times": {
    "average_ms": 2345.67,
    "min_ms": 1200,
    "max_ms": 5000
  },
  "approvals": {
    "approved": 8,
    "rejected": 2,
    "total": 10
  },
  "tools": {
    "calls_by_tool": {
      "get_order_details": 45,
      "check_fraud_score": 40,
      "cancel_order": 8
    },
    "errors_by_tool": {
      "get_order_details": 1
    }
  },
  "fraud_detection": {
    "total_checks": 40,
    "high_risk": 8,
    "medium_risk": 12,
    "high_risk_percentage": 20.0
  },
  "sessions": {
    "active": 3,
    "total_messages": 156
  },
  "guardrails": {
    "pii_masked": 23,
    "injections_blocked": 1,
    "messages_truncated": 0
  }
}
```

### Monitor Key Metrics

**Error Rate (should stay < 5%):**
```bash
curl http://localhost:8090/metrics | jq '.requests.error_rate'
```

**Fraud Detection (track HIGH risk %):**
```bash
curl http://localhost:8090/metrics | jq '.fraud_detection.high_risk_percentage'
```

**Tool Performance (response times):**
```bash
curl http://localhost:8090/metrics | jq '.tools.avg_execution_times_ms'
```

**Active Sessions:**
```bash
curl http://localhost:8090/metrics | jq '.sessions.active'
```

---

## Part 3: Frontend Error Tracking

### Setup

**Wrap your app with ErrorBoundary in `App.tsx`:**

```tsx
import { ErrorBoundary } from './components/ErrorBoundary'
import { AgentProvider } from './context/AgentContext'

export default function App() {
  return (
    <ErrorBoundary>
      <AgentProvider>
        {/* Your app content */}
      </AgentProvider>
    </ErrorBoundary>
  )
}
```

### How It Works

1. **Catches React errors** — Component failures don't crash entire app
2. **Logs to backend** — Sends error details to `/api/errors/log`
3. **Shows user message** — "Something Went Wrong" dialog
4. **Provides reload button** — User can recover

### Example: Error Flow

```
User clicks button
  ↓
Component crashes (undefined property, etc.)
  ↓
ErrorBoundary catches it
  ↓
Logs to backend: POST /api/errors/log
{
  "timestamp": "2025-05-16T10:30:00Z",
  "message": "Cannot read property 'foo' of undefined",
  "stack": "at AgentChat.tsx:45",
  "componentStack": "in AgentChat (created by..."
}
  ↓
Shows error UI with reload button
```

### View Frontend Errors

```bash
# Check backend logs for POST to /api/errors/log
docker logs ecommerce-mcp-server | grep "api/errors"
```

---

## Part 4: Redis Monitoring

### Monitor Redis Memory

```bash
# Check memory usage
docker exec ecommerce-redis redis-cli info memory

# Output:
# used_memory_human:1.23M
# maxmemory:0
# eviction_policy:noeviction

# If memory is growing, consider enabling eviction
```

### Monitor Session Count

```bash
# Count active sessions
docker exec ecommerce-redis redis-cli keys "agent:session:*" | wc -l

# List all session IDs
docker exec ecommerce-redis redis-cli keys "agent:session:*"

# Check a specific session TTL
docker exec ecommerce-redis redis-cli ttl "agent:session:{id}"
# Output: 82345 (seconds remaining)
```

### Set Memory Limits (Production)

```bash
# In docker-compose.yml
redis:
  image: redis:7-alpine
  command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru
  # LRU eviction: removes least-recently-used keys when limit hit
```

---

## Part 5: Dashboard (Grafana + Prometheus)

### Quick Setup (Optional)

For production, export metrics to Prometheus and visualize in Grafana:

**Add to docker-compose.yml:**

```yaml
prometheus:
  image: prom/prometheus:latest
  volumes:
    - ./prometheus.yml:/etc/prometheus/prometheus.yml
    - prometheus-data:/prometheus
  ports:
    - "9090:9090"

grafana:
  image: grafana/grafana:latest
  ports:
    - "3001:3000"
  environment:
    - GF_SECURITY_ADMIN_PASSWORD=admin
  depends_on:
    - prometheus

volumes:
  prometheus-data:
```

**Create `prometheus.yml`:**

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'agent'
    static_configs:
      - targets: ['localhost:8090']
    metrics_path: '/metrics'
```

**Start:**

```bash
docker-compose up -d prometheus grafana
# Prometheus: http://localhost:9090
# Grafana: http://localhost:3001 (admin/admin)
```

### Grafana Dashboard Example

Create dashboard with panels:

1. **Request Rate**
   ```
   Metric: requests_total
   Chart type: Line
   ```

2. **Error Rate**
   ```
   Metric: error_rate
   Threshold: Alert if > 5%
   ```

3. **Response Time**
   ```
   Metric: response_time_ms
   Percentiles: 50th, 95th, 99th
   ```

4. **Fraud Detection**
   ```
   Metric: high_risk_percentage
   Chart type: Gauge
   ```

---

## Part 6: Alerting

### Set Up Alerts (Prometheus)

**Create `alert_rules.yml`:**

```yaml
groups:
  - name: agent_alerts
    rules:
      # Alert if error rate > 5%
      - alert: HighErrorRate
        expr: requests_error_rate > 5
        for: 5m
        annotations:
          summary: "Agent error rate is {{ $value }}%"

      # Alert if no requests for 10 minutes
      - alert: NoActivity
        expr: increase(requests_total[10m]) == 0
        for: 1m
        annotations:
          summary: "No requests in last 10 minutes"

      # Alert if high fraud detected
      - alert: HighFraudActivity
        expr: fraud_high_risk_percentage > 25
        for: 5m
        annotations:
          summary: "{{ $value }}% of orders flagged as high fraud risk"

      # Alert if response time > 10 seconds
      - alert: SlowResponses
        expr: response_time_p99_ms > 10000
        for: 5m
        annotations:
          summary: "p99 response time is {{ $value }}ms"
```

### Alert Webhooks

Send alerts to Slack, email, PagerDuty, etc.

**In `prometheus.yml`:**

```yaml
alerting:
  alertmanagers:
    - static_configs:
        - targets: ['localhost:9093']

alertmanager:
  image: prom/alertmanager:latest
  volumes:
    - ./alertmanager.yml:/etc/alertmanager/alertmanager.yml
  ports:
    - "9093:9093"
```

**Create `alertmanager.yml`:**

```yaml
global:
  resolve_timeout: 5m
  slack_api_url: 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'

route:
  receiver: 'slack'

receivers:
  - name: 'slack'
    slack_configs:
      - channel: '#alerts'
        title: 'Agent Alert'
        text: '{{ .GroupLabels }} — {{ .Alerts }}'
```

---

## Part 7: Log Aggregation (Production)

### Option A: ELK Stack (Elasticsearch + Logstash + Kibana)

```bash
# Uses Logstash to ingest JSON logs into Elasticsearch
# View and search in Kibana dashboard
```

### Option B: Cloud Providers

**AWS CloudWatch:**
```python
# Install watchtower
pip install watchtower

# In logging_config.py
import watchtower
handler = watchtower.CloudWatchLogHandler(
    log_group='/agent/logs',
    stream_name='mcp-server'
)
logger.addHandler(handler)
```

**Google Cloud Logging:**
```python
from google.cloud import logging as cloud_logging

cloud_logger = cloud_logging.Client().logger("agent")
cloud_logger.log_struct({"message": "...", "session_id": "..."})
```

**DataDog:**
```python
from datadog import api
api.api_key = "YOUR_API_KEY"
api.app_key = "YOUR_APP_KEY"

# Logs sent automatically to DataDog
```

### Option C: Self-Hosted Loki (Simple)

```bash
# Lightweight log aggregation (alternative to ELK)
# Uses same promtail agent to scrape logs

# Add to docker-compose.yml:
loki:
  image: grafana/loki:latest
  ports:
    - "3100:3100"

promtail:
  image: grafana/promtail:latest
  volumes:
    - /var/log/agent:/var/log/agent
    - ./promtail-config.yml:/etc/promtail/config.yml
  depends_on:
    - loki
```

---

## Part 8: Monitoring Checklist

### Daily Checks

- [ ] **Error Rate** — Should be < 2%
  ```bash
  curl http://localhost:8090/metrics | jq '.requests.error_rate'
  ```

- [ ] **Active Sessions** — Normal range for your traffic
  ```bash
  curl http://localhost:8090/metrics | jq '.sessions.active'
  ```

- [ ] **Redis Memory** — Should not be growing unbounded
  ```bash
  docker exec ecommerce-redis redis-cli info memory | grep used_memory
  ```

### Weekly Checks

- [ ] **Fraud Detection Accuracy** — Review flagged orders
  ```bash
  curl http://localhost:8090/metrics | jq '.fraud_detection'
  ```

- [ ] **Response Times** — Should stay stable
  ```bash
  curl http://localhost:8090/metrics | jq '.response_times'
  ```

- [ ] **Tool Performance** — Identify slow tools
  ```bash
  curl http://localhost:8090/metrics | jq '.tools.avg_execution_times_ms'
  ```

### Monthly Checks

- [ ] **Log Archive** — Rotate old logs (> 30 days)
- [ ] **Metric Retention** — Configure Prometheus retention (default: 15d)
- [ ] **Cost Review** — Track API usage and costs
- [ ] **Security Audit** — Review error logs for suspicious patterns

---

## Part 9: Quick Queries

### Log Queries (with jq)

**Find all orders with high fraud risk:**
```bash
docker logs ecommerce-mcp-server | jq 'select(.fraud_score > 70)'
```

**Count tool calls by type:**
```bash
docker logs ecommerce-mcp-server | jq 'select(.tool_name) | .tool_name' | sort | uniq -c
```

**Find errors in last 100 logs:**
```bash
docker logs ecommerce-mcp-server | tail -100 | jq 'select(.level == "ERROR")'
```

**Get session metrics:**
```bash
curl http://localhost:8090/metrics | jq '.sessions'
```

**Find slow requests (> 5 seconds):**
```bash
curl http://localhost:8090/metrics | jq '.response_times | select(.max_ms > 5000)'
```

### Prometheus Queries (in Grafana)

**Error rate over time:**
```
rate(requests_total[5m])
```

**p99 response time:**
```
histogram_quantile(0.99, request_duration_ms)
```

**Fraud detection trend:**
```
increase(fraud_high_risk[1h])
```

**Tool success rate:**
```
(tool_calls - tool_errors) / tool_calls * 100
```

---

## Part 10: Troubleshooting

| Issue | Solution |
|-------|----------|
| **Logs not appearing** | Check log level: `setup_logging(level="DEBUG")` |
| **Metrics endpoint 404** | Ensure `/metrics` route in main.py (already added) |
| **Redis memory growing** | Set `--maxmemory` and `--maxmemory-policy` |
| **Errors not logged to frontend** | Ensure ErrorBoundary wraps app, check browser console |
| **Prometheus scrape failing** | Verify mcp-server is running on port 8090 |
| **Grafana can't reach Prometheus** | Check docker network: `docker network ls` |
| **Alerts not firing** | Check Prometheus targets: http://localhost:9090/targets |

---

## Summary

You now have:

✅ **Structured Logging** — JSON logs, console + file outputs
✅ **Metrics Collection** — `/metrics` endpoint with detailed stats
✅ **Frontend Error Tracking** — ErrorBoundary + backend logging
✅ **Redis Monitoring** — Memory & session tracking
✅ **Dashboard Ready** — Grafana templates provided
✅ **Alerting** — Prometheus rules for high-risk events
✅ **Log Aggregation** — ELK / Loki / Cloud provider options

**Next step:** Visit http://localhost:8090/metrics and set up Grafana for visualization!
