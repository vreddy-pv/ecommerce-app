# Grafana & Prometheus Setup Guide

**Complete visual monitoring dashboard for your microservices.**

---

## Overview

This guide sets up Prometheus + Grafana for real-time monitoring and visualization of all microservices, including the autonomous AI agent.

```
Services emit metrics
  ↓
Prometheus collects & stores (every 15s)
  ↓
Grafana queries & visualizes
  ↓
http://localhost:3001 (dashboards, alerts, insights)
```

---

## Quick Start

### 1. Start the Monitoring Stack

```bash
docker-compose up -d prometheus grafana
```

This starts:
- **Prometheus** on `http://localhost:9090` — metrics database
- **Grafana** on `http://localhost:3001` — visualization & dashboards

### 2. Access Grafana

```
URL: http://localhost:3001
Username: admin
Password: admin (change in .env: GRAFANA_ADMIN_PASSWORD)
```

### 3. View Dashboard

The system comes with pre-built dashboard: **E-Commerce System Overview**

- Click **Home** → **Dashboards** → **E-Commerce System Overview**
- Or navigate directly: `/d/ecommerce-overview`

---

## What Gets Monitored

### Spring Boot Services (API Gateway, User, Catalog, Inventory, Order, etc.)

Metrics collected from `/actuator/prometheus`:
- **Request Rate** — HTTP requests/second by service
- **Error Rate** — 5xx errors as % of total requests
- **Response Time** — p95, p99 latency
- **Throughput** — database queries/second
- **Memory Usage** — heap memory by service
- **CPU Usage** — process CPU utilization
- **Database Connections** — active/idle HikariCP pool

### Python Services (MCP Server, Notification Service)

Metrics collected from `/metrics`:
- **Chat Requests** — total and errors
- **Active Sessions** — concurrent user sessions
- **Tool Calls** — by tool name
- **Agent Performance** — execution times

### System Health

- Service availability (up/down)
- Instance counts by job
- Network connectivity

---

## Dashboard Panels

The **E-Commerce System Overview** dashboard includes:

### Row 1: Status & Request Rate
- **Service Health Status** — Table of up/down services
- **Request Rate** — Line chart (requests/sec) by service

### Row 2: Error & Response Time
- **Error Rate (5xx)** — Line chart showing % errors
- **Response Time (p95, p99)** — Latency percentiles

### Row 3: Performance Metrics
- **Database Query Throughput** — ops/sec
- **Memory Usage** — MB by service

### Row 4: Resource Utilization
- **CPU Usage** — process CPU % by service
- **Database Connection Pool** — active/idle connections

---

## Create Custom Dashboards

### Example: API Gateway Monitoring

1. Click **Dashboards** → **New** → **New Dashboard**
2. Click **Add visualization**
3. Select **Prometheus** datasource
4. Enter query:
   ```
   rate(http_requests_total{job="api-gateway"}[5m])
   ```
5. Click **Apply**, give it a name
6. Save dashboard

### Common Queries

**Total requests:**
```
rate(http_requests_total[5m])
```

**Errors by service:**
```
rate(http_requests_total{status=~"5.."}[5m])
```

**Latency (p99):**
```
histogram_quantile(0.99, rate(http_request_duration_seconds_bucket[5m]))
```

**Memory:**
```
process_resident_memory_bytes / 1024 / 1024
```

**Active database connections:**
```
hikaricp_connections_active
```

**MCP agent metrics:**
```
mcp_chat_requests_total
mcp_active_sessions
mcp_tool_calls_by_tool
```

---

## Prometheus Queries (PromQL)

### Aggregation Functions

```
# Average across all instances
avg(metric_name)

# Maximum value
max(metric_name)

# Sum by service
sum by (job) (metric_name)

# Rate of change (5 minutes)
rate(metric_name[5m])

# Increase (cumulative)
increase(metric_name[5m])
```

### Time Ranges

```
[5m]   — last 5 minutes
[1h]   — last 1 hour
[24h]  — last 24 hours
[7d]   — last 7 days
```

### Examples

**Request volume by status:**
```
sum by (status) (rate(http_requests_total[5m]))
```

**Error rate percentage:**
```
(sum(rate(http_requests_total{status=~"5.."}[5m])) / sum(rate(http_requests_total[5m]))) * 100
```

**P95 latency trend:**
```
histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[1h]))
```

**Service uptime percentage (last 24h):**
```
(count(up == 1) / count(up)) * 100
```

---

## Alerting (Optional)

### Create Alert Rule

1. In dashboard, click **Alert** icon on a panel
2. Set **Alert condition:**
   ```
   When metric > 5 (for error rate)
   For 5m (wait 5 minutes before alerting)
   ```
3. Configure notification channel (Slack, email, etc.)
4. Save

### Example Alert: High Error Rate

```
IF: rate(http_requests_total{status=~"5.."}[5m]) > 0.05
THEN: Alert "High error rate on {{ $labels.job }}"
```

---

## Storage & Retention

### Prometheus Data Retention

Default: 15 days of metrics

To change:
```yaml
# docker-compose.yml
command:
  - '--storage.tsdb.retention.time=30d'  # 30 days
```

To change in running container:
```bash
curl -X POST http://localhost:9090/-/reload
```

### Grafana Data

Dashboards & datasources stored in `grafana-data/` volume (persistent).

To backup:
```bash
docker cp ecommerce-grafana:/var/lib/grafana ./grafana-backup
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| **Grafana 500 error** | Check logs: `docker logs ecommerce-grafana` |
| **No metrics appearing** | Ensure services expose `/actuator/prometheus` or `/metrics` |
| **Prometheus target DOWN** | Check service is running: `docker ps` |
| **Dashboard blank** | Verify datasource: **Settings** → **Data Sources** → **Prometheus** → Test |
| **Very high memory usage** | Reduce retention: `--storage.tsdb.retention.time=7d` |
| **Metrics gaps** | Check scrape interval: prometheus.yml `scrape_interval: 15s` |

---

## Performance Tips

### Optimize Metric Collection

1. **Increase scrape interval** for less critical metrics:
   ```yaml
   - job_name: 'notification-service'
     scrape_interval: 30s  # Instead of 10s
   ```

2. **Drop high-cardinality metrics:**
   ```yaml
   metric_relabel_configs:
     - source_labels: [__name__]
       regex: 'request_duration_.*'
       action: drop
   ```

3. **Sample metrics** (reduce volume):
   ```yaml
   - job_name: 'api-gateway'
     sample_limit: 10000
   ```

### Storage Optimization

- Retention: 15 days (change as needed)
- Compression: Prometheus auto-compresses old data
- Typical size: ~1-2GB per day per 10 services

---

## Integration with Existing Services

### MCP Server Metrics

The MCP server (AI agent) exposes metrics at:
```
http://localhost:8090/metrics
```

Metrics tracked:
- `mcp_chat_requests_total` — total chat messages processed
- `mcp_active_sessions` — concurrent sessions
- `mcp_chat_errors_total` — errors
- `mcp_tool_calls_by_tool` — tool usage breakdown
- `mcp_fraud_detection_total` — fraud checks performed
- `mcp_fraud_high_risk_percentage` — high-risk order %
- `mcp_response_time_ms` — agent response latency

### Query Examples

**Active chat sessions:**
```
mcp_active_sessions
```

**Fraud detection accuracy:**
```
mcp_fraud_high_risk_percentage
```

**Tool usage:**
```
sum by (tool) (rate(mcp_tool_calls_total[5m]))
```

**Error rate:**
```
rate(mcp_chat_errors_total[5m]) / rate(mcp_chat_requests_total[5m]) * 100
```

---

## Advanced: Custom Exporter

To add additional metrics (e.g., custom business metrics):

### Python Service Example

```python
from prometheus_client import Counter, Gauge, start_http_server

# Define metrics
orders_total = Counter('orders_total', 'Total orders', ['status'])
fraud_score = Gauge('fraud_score', 'Current fraud score', ['order_id'])

# Record metrics
orders_total.labels(status='completed').inc()
fraud_score.labels(order_id='ORD-123').set(78.5)

# Start metrics server
start_http_server(8000)
```

Then add to prometheus.yml:
```yaml
- job_name: 'custom-service'
  static_configs:
    - targets: ['localhost:8000']
```

---

## Environment Variables

Add to `.env`:

```env
# Grafana
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=your-secure-password

# Prometheus (optional)
PROMETHEUS_RETENTION=15d  # Data retention period
```

---

## Security

### Protect Grafana

1. **Change default password:**
   - Login → **Settings** → **Security** → **Change password**

2. **Disable anonymous access:**
   - **Settings** → **Security** → **Allow anonymous access** → OFF

3. **Enable authentication:**
   - **Settings** → **Users** → Add users with specific roles

4. **Reverse proxy (production):**
   ```nginx
   location /grafana {
     auth_basic "Grafana";
     auth_basic_user_file /etc/nginx/.htpasswd;
     proxy_pass http://grafana:3000;
   }
   ```

### Protect Prometheus

Keep Prometheus behind firewall (only accessible from Grafana):
- Don't expose port 9090 to internet
- Only Grafana should query Prometheus

---

## Dashboards to Explore

### Built-in Prometheus Dashboard

- **URL:** http://localhost:9090/graph
- **Purpose:** Raw metric queries & debugging
- **Query:** `up`, `rate(...)`, `histogram_quantile(...)`

### Node Exporter (Optional)

For system metrics (CPU, disk, network):

```bash
docker run -d \
  --name=node-exporter \
  --network=ecommerce-network \
  prom/node-exporter:latest
```

Then add to prometheus.yml:
```yaml
- job_name: 'node'
  static_configs:
    - targets: ['node-exporter:9100']
```

---

## Summary

You now have:

✅ **Real-time metrics** — Prometheus collects from all services
✅ **Beautiful dashboards** — Grafana visualizes system health
✅ **E-Commerce Overview** — Pre-built dashboard with key metrics
✅ **AI Agent Monitoring** — MCP server metrics tracked
✅ **Custom dashboards** — Create your own
✅ **Alerting ready** — Configure alerts for anomalies
✅ **Long-term storage** — 15+ days of historical data

**Next steps:**
1. Start: `docker-compose up -d prometheus grafana`
2. Login: http://localhost:3001 (admin/admin)
3. View: **E-Commerce System Overview** dashboard
4. Customize: Create dashboards for your specific needs
5. Alert: Set up notifications for critical metrics

