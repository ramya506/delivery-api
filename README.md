# delivery-api

Spring Boot 3 REST API for the **Last-Mile Delivery Delay Intelligence** dashboard.
Uses an in-memory H2 database that is filled with ~120,000 generated deliveries on every start
(same data each time), so there is nothing to install or configure.

## Open in STS

1. **File -> Import -> Maven -> Existing Maven Projects** -> choose this folder.
2. Right-click the project -> **Run As -> Spring Boot App**.
3. Open http://localhost:8080/api/health  (should show `"status":"UP"`).
4. Open http://localhost:8080/api/summary

Requires JDK 17 or newer.

Or from a terminal: `mvn spring-boot:run`

## Endpoints

All of these accept optional `from`, `to` (yyyy-MM-dd), `partner` (e.g. `P03`) and `district` query params.

| Endpoint | Returns |
|---|---|
| `GET /api/health` | service status |
| `GET /api/filters` | partners, districts, min/max date |
| `GET /api/summary` | orders, late %, first-attempt %, revenue at risk, re-attempt cost |
| `GET /api/pincodes/risk` | per pin code risk (for the map), sorted by revenue at risk |
| `GET /api/trend` | daily series with rolling 7-day late %, first-attempt %, heavy-rain share |
| `GET /api/partners/leaderboard` | late % per delivery partner (ignores the partner filter) |
| `GET /api/hubs/leaderboard` | late % per hub |
| `GET /api/factors` | late % by weather and festival vs normal days |

**Revenue at risk** = total order value x delay probability (share of orders delivered later than promised).
**Re-attempt cost** = (total attempts - orders) x Rs 60 (change with `REATTEMPT_COST`).

## Configuration (environment variables)

| Variable | Default | Meaning |
|---|---|---|
| `PORT` | 8080 | Set automatically by Cloud Run |
| `CORS_ORIGINS` | `http://localhost:5173,http://localhost:4173` | Comma-separated frontend URLs allowed to call the API |
| `SEED_ROWS` | 120000 | Number of generated deliveries |
| `REATTEMPT_COST` | 60 | Rs per extra delivery attempt |
| `H2_CONSOLE` | false | `true` enables http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:deliverydb`) |

## Deploy to Google Cloud Run

```bash
gcloud config set project YOUR_PROJECT_ID
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com

# run from this folder; builds the Dockerfile in the cloud and deploys it
gcloud run deploy delivery-api \
  --source . \
  --region asia-south1 \
  --allow-unauthenticated \
  --memory 1Gi \
  --set-env-vars CORS_ORIGINS=https://YOUR-FRONTEND-URL
```

Billing must be enabled on the project. After the frontend is deployed, re-run with its real URL in
`CORS_ORIGINS` (or update it later with `gcloud run services update delivery-api --update-env-vars CORS_ORIGINS=...`).

Test: `curl https://YOUR-CLOUD-RUN-URL/api/health`

## Notes

- Pin codes and coordinates in `DataSeeder` are illustrative. Replace them with real ones if you need accuracy.
- Because data lives in memory, every cold start regenerates it (a few seconds). To use a real database later,
  swap H2 for Cloud SQL (PostgreSQL) in `pom.xml` and `application.properties`.
