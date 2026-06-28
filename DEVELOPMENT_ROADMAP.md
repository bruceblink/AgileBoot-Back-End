# Keystone Development Roadmap

This roadmap tracks the framework-focused direction for Keystone after removing the external identity-provider integration.

## Current Focus

| Area | Status | Notes |
| :--- | :--- | :--- |
| Framework modules | Active | Keep `keystone-common`, `keystone-infrastructure`, `keystone-framework-domain`, `keystone-framework-admin`, and `keystone-framework-spring-boot-starter` publishable as reusable artifacts. |
| Authentication | Active | Keystone local username/password login, RSA password encryption, RS256 JWT access tokens, refresh tokens, and single-account session control. |
| System management | Active | User, role, menu, department, post, dictionary, config, notice, log, online-user, and scheduled-job management. |
| Maven publishing | In progress | Ensure Gradle publications include complete metadata and can be validated before pushing to a remote Maven repository. |
| Static analysis | Ongoing | `check` runs tests, JaCoCo, Checkstyle, and SpotBugs. Checkstyle and SpotBugs are currently report-only, so XML reports must be reviewed during audits. |

## Near-Term Work

1. Harden Maven publication metadata, signing, source jars, and javadoc jars.
2. Keep framework modules free of application-specific or external-provider-specific code.
3. Raise static-analysis gates gradually after report-only findings are reduced.
4. Expand starter integration tests so downstream applications can consume the framework with minimal manual configuration.
5. Keep database migrations additive and compatible with existing deployments.

## Validation Gates

Recommended local gates before release work:

```powershell
.\gradlew.bat test
.\gradlew.bat check
.\gradlew.bat publishToMavenLocal
```

For database migrations:

```powershell
.\gradlew.bat :keystone-domain:integrationTest :keystone-admin:dbIntegrationTest
```
