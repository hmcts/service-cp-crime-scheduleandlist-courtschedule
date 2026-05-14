## Repo: service-cp-crime-scheduleandlist-courtschedule

Spring Boot service that retrieves court schedule data for a case URN by calling the CP backend, applies a hearing response filter to normalise the result, and returns it as an API response.

**Pattern**: Stateless proxy with response filtering
**Spring Boot version**: 4.0.1 (target 4.0.6+ per upgrade cycle)
**Implements**: `api-cp-crime-schedulingandlisting-courtschedule`

## Infrastructure

| Component | Technology | Purpose |
|---|---|---|
| CP Backend | External HTTP | Source of court schedule data |
| WireMock | 3.6.0 (docker) | Backend stub for local dev and API tests |

## Source Structure

```
uk.gov.hmcts.cp/
  Application.java                          @SpringBootApplication
  clients/
    CourtScheduleClient                     RestTemplate → CP backend; sets CJSCPPUID header
  config/
    AppConfig                               @Bean RestTemplate
    AppPropertiesBackend                    @Value AMP_BACKEND_URL, CP_BACKEND_URL
  controllers/
    CourtScheduleController                 Implements generated API; delegates to CourtScheduleService
    GlobalExceptionHandler                  @RestControllerAdvice; maps exceptions to HTTP codes
    RootController                          Returns 200 on GET /
  domain/
    CaseMapperResponse                      Internal DTO: case URN → case ID
    HearingResponse                         Internal DTO: raw hearing data from backend
  exceptions/
    GlobalExceptionHandler                  Maps domain exceptions to HTTP status codes
  filters/
    HearingResponseFilter                   Strips or transforms backend hearing fields before mapping
    TracingFilter                           Reads/generates X-Correlation-Id; propagates via MDC
  mappers/
    HearingsMapper                          Maps HearingResponse domain DTO to API response model
  services/
    CaseUrnMapperService                    Resolves case URN to case ID via CP backend
    CourtScheduleService                    Calls CourtScheduleClient → applies filter → maps response
```

## Environment Variables

| Variable | Purpose | Default |
|---|---|---|
| `CP_BACKEND_URL` | Base URL of the CP backend (court schedule data) | `http://localhost:8081` |
| `AMP_BACKEND_URL` | Alternative backend URL (used by CaseUrnMapperService) | `http://localhost:8081` |
| `CJSCPPUID` | User UUID header on all backend calls | `00000000-0000-0000-0000-000000000000` |
| `rpe.AppInsightsInstrumentationKey` | Azure Application Insights key | `00000000-0000-0000-0000-000000000000` |

## Repo-Specific Architecture Rules

- **HearingResponseFilter**: Applied before `HearingsMapper` — it normalises or removes backend fields that should not appear in the API response. Modify the filter (not the mapper) to change what fields are included.
- **CJSCPPUID header**: `CourtScheduleClient` sets `CJSCPPUID` on every backend request. Never remove this header.
- **Mapper is pure**: `HearingsMapper` only transforms types; no filtering or business logic.

## Debugging

| Symptom | Cause / Fix |
|---|---|
| Missing hearings in response | Check `HearingResponseFilter` — fields may be stripped; review filter logic |
| 404 for valid URN | Backend does not have schedule for that case; check `CP_BACKEND_URL` and data |
| Null fields in response | `@JsonInclude(NON_NULL)` suppresses nulls; check whether backend returned the field |

## Repo-Specific Notes

- `ci-build-publish.yml` present alongside standard `ci-draft.yml` / `ci-released.yml`.
- No database; fully stateless.
