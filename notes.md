Prometheus is healthy. Here's how to use Grafana locally whenever you need dashboards:

  ---
  Step 1 — Port-forward Prometheus from the cluster (keep this terminal open):
  kubectl port-forward svc/prometheus 9090:9090 -n inventalert-prod

  Step 2 — Run Grafana locally via Docker:
  docker run -d --name grafana -p 3000:3000 grafana/grafana:11.1.0

  Step 3 — Connect Grafana to Prometheus:
  1. Open http://localhost:3000 → login admin / admin
  2. Go to Connections → Data Sources → Add data source → Prometheus
  3. Set URL to http://host.docker.internal:9090
  4. Click Save & Test — should show "Successfully queried the Prometheus API"

  Step 4 — Import the InventAlert dashboard:
  1. Go to Dashboards → Import
  2. Upload monitoring/grafana/dashboards/inventalert.json from this repo




  ---
  When you're done, stop with docker stop grafana. Next time just run docker start grafana and re-open the port-forward
  http://a05931e33d54f4090af5128ef80f5db1-1679660142.eu-west-1.elb.amazonaws.com/identity/swagger-ui.html
  http://a05931e33d54f4090af5128ef80f5db1-1679660142.eu-west-1.elb.amazonaws.com/inventory/swagger-ui.html
  http://a05931e33d54f4090af5128ef80f5db1-1679660142.eu-west-1.elb.amazonaws.com/notification/swagger-ui.html
  http://a05931e33d54f4090af5128ef80f5db1-1679660142.eu-west-1.elb.amazonaws.com/analytics/swagger-ui.htm