# Backend Security Remediation Report

## Scope
This report records the backend continuation work completed from the previous half-finished state. The scope covers source code, deployment templates, deployment scripts, validation behavior, and refreshed delivery artifacts.

## Completed Work
- Completed the `BizException` path and wired validation-focused request handling through controllers that accept external input.
- Added startup security validation via `SecurityConfigurationValidator` so missing sensitive environment variables fail fast.
- Replaced the risky `SysUser.password transient` approach with `@JsonIgnore`, preserving ORM field mapping while preventing JSON exposure.
- Standardized DTO validation for login, flow approval, project, user, department, role, and frontend monitor request payloads.
- Removed silent `catch ignored` behavior from audit, file, controller, and monitor paths; fallback paths now log context before degrading.
- Migrated cleanup tasks from `Calendar` to `java.time`.
- Removed default demo secrets from backend configuration, SM4 handling, and deployment shell scripts.
- Added a lightweight gate script to detect sensitive defaults, `throw new RuntimeException`, and `catch (Exception ignored)`.
- Refreshed `deploy-output/gov4` after the backend and frontend packaging flow completed.

## Verification Results
- Security gate: PASS
  - Command: `powershell -ExecutionPolicy Bypass -File .\scripts\security-remediation-check.ps1`
- Backend tests: PASS
  - Command: `powershell -ExecutionPolicy Bypass -File .\scripts\mvn-jdk8.ps1 test`
  - Result: `45` tests passed
- Backend package: PASS
  - Command: `powershell -ExecutionPolicy Bypass -File .\scripts\package-kylin-arm.ps1`
  - Result: refreshed backend jar and deployment package
- Validation behavior: PASS
  - Parameter validation, illegal argument handling, and upload-size violations now return HTTP `400` instead of HTTP `200` with only a business error body.

## Deployment Template Status
- `deploy/kylin-arm/backend.env.example` now uses required placeholders:
  - `GOV_DB_PASSWORD=CHANGE_ME`
  - `MYSQL_ROOT_PASSWORD=CHANGE_ME`
  - `GOV_MINIO_ACCESS_KEY=CHANGE_ME`
  - `GOV_MINIO_SECRET_KEY=CHANGE_ME`
  - `GOV_SM4_KEY=CHANGE_ME_16_CHARS`
- `run-mariadb.sh`, `wait-mariadb-and-init.sh`, and `run-minio.sh` no longer fall back to demo credentials.
- Missing sensitive values now fail deployment instead of silently running with insecure defaults.

## Delivery Artifacts
- Source changes: backend repository working tree
- Deployment templates: `deploy/kylin-arm/`
- Delivery package: `deploy-output/gov4/`
- Main backend artifact: `deploy-output/gov4/backend/app.jar`

## Remaining Risks
- Existing business exceptions still use wrapped `R` responses for some permission and business-denial flows; this round only enforced real HTTP `400` for parameter and upload validation paths.
- Deployment now depends on explicit secret provisioning. Operations must update `/opt/gov4/backend/backend.env` before restart.
- Empty `deploy-output/gov4/frontend/runtime/` is expected because `run-frontend.sh` generates `env.js` on the target host from `frontend.env`.

## Commit
- Backend commit: pending
