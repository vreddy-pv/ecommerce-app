# Monitoring Stack Summary

**Complete observability setup with Prometheus + Grafana for visual dashboards.**

---

## What Was Added

### 1. Docker Compose Updates

**New services:**
- **Prometheus** (port 9090) — metrics database & scraper
- **Grafana** (port 3001) — visualization & dashboards

**New volumes:**
- `prometheus-data` — metrics storage
- `grafana-data` — dashboard configuration

### 2. Configuration Files

**`infrastructure/prometheus/prometheus.yml`**
- Scrape configuration for all 12 services
- 15s scrape interval, 15 days retention
- Job definitions for each microservice

**`infrastructure/grafana/provisioning/datasources/datasources.yml`**
- Auto-configures Prometheus as data source
- Runs on container startup

**`infrastructure/grafana/provisioning/dashboards/dashboards.yml`**
- Auto-loads dashboard JSON files
- Watches `/var/lib/grafana/dashboards` directory

### 3. Pre-built Dashboard

**`infrastructure/grafana/dashboards/ecommerce-system.json`**
- **Service Health Status** — up/down status
- **Request Rate** — requests/sec by service
- **Error Rate** — 5xx error percentage
- **Response Time** — p95, p99 latency
- **Database Throughput** — queries/second
- **Memory Usage** — heap memory by service
- **CPU Usage** — process utilization
- **Connection Pool** — active/idle database connections

---

## Quick Start

### Start Monitoring

```bash
docker-compose up -d prometheus grafana
```

Wait 30 seconds for startup:
```bash
docker logs -f ecommerce-prometheus
docker logs -f ecommerce-grafana
```

### Access Dashboards

| Service | URL | Credentials |
|---------|-----|-------------|
| **Prometheus** | http://localhost:9090 | (no auth) |
| **Grafana** | http://localhost:3001 | admin/admin |

### View System Dashboard

1. Open http://localhost:3001
2. Login: `admin` / `admin`
3. Navigate: **Dashboards** → **E-Commerce System Overview**
4. Or go directly to: http://localhost:3001/d/ecommerce-overview

---

## Metrics Collected

### From Spring Boot Services

Collected from `/actuator/prometheus` endpoint:

| Metric | What It Shows |
|--------|---------------|
| `http_requests_total` | Total HTTP requests by status |
| `http_request_duration_seconds_bucket` | Request latency histogram |
| `hikaricp_connections_active` | Active database connections |
| `hikaricp_connections_idle` | Idle database connections |
| `process_resident_memory_bytes` | Heap memory usage |
| `process_cpu_seconds_total` | CPU time |
| `jvm_memory_used_bytes` | JVM memory |

### From Python Services (MCP Server)

Collected from `/metrics` endpoint:

| Metric | What It Shows |
|--------|---------------|
| `mcp_chat_requests_total` | Total chat messages |
| `mcp_chat_errors_total` | Chat processing errors |
| `mcp_active_sessions` | Concurrent sessions |
| `mcp_tool_calls_by_tool` | Tool usage breakdown |
| `mcp_fraud_detection_total` | Fraud checks performed |
| `mcp_fraud_high_risk_percentage` | High-risk orders % |

---

## Query Examples

### Request Performance

**Request rate (requests/sec):**
```promql
rate(http_requests_total[5m])
```

**Error rate (%):**
```promql
(sum(rate(http_requests_total{status=~"5.."}[5m])) / sum(rate(http_requests_total[5m]))) * 100
```

**Response latency (p99):**
```promql
histogram_quantile(0.99, rate(http_request_duration_seconds_bucket[5m]))
```

### Resource Usage

**Memory (MB):**
```promql
process_resident_memory_bytes / 1024 / 1024
```

**CPU (%):**
```promql
rate(process_cpu_seconds_total[5m]) * 100
```

**Database connections:**
```promql
hikaricp_connections_active
```

### AI Agent Metrics

**Chat requests/sec:**
```promql
rate(mcp_chat_requests_total[5m])
```

**Active sessions:**
```promql
mcp_active_sessions
```

**Tool usage by tool:**
```promql
sum by (tool) (rate(mcp_tool_calls_total[5m]))
```

**Fraud detection accuracy:**
```promql
mcp_fraud_high_risk_percentage
```

---

## Services Monitored

| Service | Port | Endpoint | Interval |
|---------|------|----------|----------|
| API Gateway | 8080 | `/actuator/prometheus` | 10s |
| Discovery | 8761 | `/actuator/prometheus` | 15s |
| User Service | 8082 | `/actuator/prometheus` | 10s |
| Catalog Service | 8083 | `/actuator/prometheus` | 10s |
| Inventory Service | 8084 | `/actuator/prometheus` | 10s |
| Order Service | 8085 | `/actuator/prometheus` | 10s |
| Order Processing | 8086 | `/actuator/prometheus` | 10s |
| Aggregator Service | 8081 | `/actuator/prometheus` | 10s |
| Analytics Service | 8088 | `/actuator/prometheus` | 10s |
| Notification Service | 8087 | `/metrics` | 15s |
| MCP Server (AI Agent) | 8090 | `/metrics` | 10s |
| Prometheus | 9090 | (self) | — |

---

## File Structure

```
infrastructure/
├── prometheus/
│   └── prometheus.yml                    # Scrape configuration
└── grafana/
    ├── provisioning/
    │   ├── datasources/
    │   │   └── datasources.yml           # Prometheus datasource config
    │   └── dashboards/
    │       └── dashboards.yml            # Dashboard loader config
    └── dashboards/
        └── ecommerce-system.json         # Pre-built dashboard

docker-compose.yml                        # Updated with prometheus & grafana services
```

---

## Dashboard Navigation

### Main Dashboard

**E-Commerce System Overview** (`/d/ecommerce-overview`)

8 panels organized in 3 rows:
1. **Health & Requests** — Status + request rate
2. **Errors & Latency** — Error rate + response times
3. **Throughput & Memory** — Database ops + memory usage
4. **Resource Usage** — CPU + connection pool

### Create Custom Dashboard

1. Click **+** → **Dashboard**
2. Click **Add visualization**
3. Select **Prometheus** datasource
4. Enter PromQL query
5. Customize visualization
6. Save

### Import Dashboard

1. Click **+** → **Import**
2. Upload JSON file (from `infrastructure/grafana/dashboards/`)
3. Select Prometheus datasource
4. Save

---

## Environment Configuration

### .env Variables

```env
# Grafana Admin Credentials
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=your-secure-password

# Optional: Prometheus Retention
PROMETHEUS_RETENTION=15d
```

### Change Defaults

**Grafana:**
- First login: admin/admin
- Change password immediately: **Settings** → **Profile** → **Change password**

**Prometheus:**
- Data retention: `docker-compose.yml` → `prometheus` → `command`
- Change `--storage.tsdb.retention.time=15d` to desired value

---

## Common Tasks

### View Service Metrics

1. Go to http://localhost:3001
2. Click **Explore**
3. Select **Prometheus**
4. Type metric name: `http_requests_total`
5. Click **Run query**

### Create Alert

1. Open a dashboard panel
2. Click **Alert** icon
3. Set condition: `rate(metric[5m]) > 5`
4. Set duration: `5m` (wait 5 min before alerting)
5. Configure notification (Slack, email, etc.)
6. Save

### Check Prometheus Targets

- **URL:** http://localhost:9090/targets
- Shows which services Prometheus can reach
- Green = UP, Red = DOWN
- Debug scrape failures here

### View Metrics in Raw Format

```bash
# Query a metric
curl http://localhost:9090/api/v1/query?query=up

# Get metric metadata
curl http://localhost:9090/api/v1/metadata
```

---

## Storage & Performance

### Metrics Storage

- **Default retention:** 15 days
- **Typical size:** 1-2GB per day
- **Scrape interval:** 10-15 seconds
- **Auto-compaction:** Old data compressed

### Reduce Disk Usage

1. Lower retention: `--storage.tsdb.retention.time=7d`
2. Increase scrape interval: `scrape_interval: 30s`
3. Drop metrics: Add `metric_relabel_configs`

### Optimize Grafana

- Dashboard refresh: Set to 30s+ (not 5s)
- Query caching: Enable in data source settings
- Use recording rules for frequently used queries

---

## Troubleshooting

### Prometheus Targets DOWN

```bash
# Check if service is running
docker ps | grep ecommerce

# Check service logs
docker logs ecommerce-api-gateway

# Test endpoint manually
curl http://localhost:8080/actuator/prometheus
```

### Grafana: Data Source Error

```bash
# Check Grafana logs
docker logs ecommerce-grafana

# Verify Prometheus is running
docker ps | grep prometheus

# Test Prometheus from Grafana container
docker exec ecommerce-grafana curl http://prometheus:9090/-/healthy
```

### No Metrics Showing

1. Ensure service is running: `docker ps`
2. Check metrics endpoint: `curl http://service:port/metrics`
3. Verify prometheus.yml has correct hostname (use container name, not localhost)
4. Wait 30-60s for first scrape to complete
5. View targets: http://localhost:9090/targets

### Grafana 500 Error

```bash
docker logs ecommerce-grafana | grep -i error
```

Check datasource configuration: **Settings** → **Data Sources** → Edit Prometheus

---

## Security

### Protect Grafana

1. Change default password immediately
2. Disable anonymous access: **Settings** → **Security** → Disable
3. Add user accounts for each team member
4. Set user roles: Admin, Editor, Viewer

### Protect Prometheus

- Don't expose port 9090 to internet
- Keep behind firewall (only Grafana should access)
- No authentication in default setup (add reverse proxy if needed)

---

## Next Steps

1. **Start monitoring:**
   ```bash
   docker-compose up -d prometheus grafana
   ```

2. **View dashboard:**
   - http://localhost:3001
   - Login: admin/admin
   - Open: E-Commerce System Overview

3. **Explore metrics:**
   - http://localhost:9090
   - Try queries: `http_requests_total`, `up`, etc.

4. **Create dashboards:**
   - Build custom dashboards for your use cases
   - Set alerts for critical metrics

5. **Read full guide:**
   - See `GRAFANA_SETUP_GUIDE.md` for advanced features

---

## Integration with Other Docs

- **Monitoring basics:** See `MONITORING_GUIDE.md` (logging, metrics, frontend errors)
- **Agent metrics:** See `AGENT_README.md` (fraud detection, approvals, tools)
- **System architecture:** See `AGENT_SETUP.md` (deployment, scaling)

---

## Comparison: Logging vs Metrics vs Dashboards

| Aspect | Logs | Metrics | Dashboards |
|--------|------|---------|-----------|
| **What** | Event details | Time-series data | Visual insights |
| **Tool** | ELK, CloudWatch | Prometheus | Grafana |
| **Query** | "Find errors in last hour" | "Error rate in last 5m" | "Show trends" |
| **Storage** | Large (text) | Small (numbers) | — |
| **Use Case** | Debugging | Alerting | Trends & patterns |

All three are complementary and included in your monitoring setup!

