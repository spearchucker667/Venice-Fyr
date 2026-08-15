# GitHub Documentation Index

This file maps general repository/community documentation. Port-specific architecture documents already present in Venice Fyr should remain separate and authoritative for their domains.

## Root/community documents

- `README.md` — project landing page
- `LICENSE` — Apache License 2.0 candidate
- `NOTICE` — project/third-party notice
- `LEGAL.md` — legal/project relationship notes
- `PRIVACY.md` — privacy model
- `SECURITY.md` — vulnerability reporting
- `CONTRIBUTING.md` — contribution workflow
- `CODE_OF_CONDUCT.md` — participation expectations
- `SUPPORT.md` — support routes
- `CHANGELOG.md` — release-facing change log

## `.github`

- `.github/CODEOWNERS`
- `.github/PULL_REQUEST_TEMPLATE.md`
- `.github/ISSUE_TEMPLATE/bug_report.yml`
- `.github/ISSUE_TEMPLATE/feature_request.yml`
- `.github/ISSUE_TEMPLATE/documentation.yml`
- `.github/ISSUE_TEMPLATE/config.yml`

These files are candidates and must be reviewed against repository settings before adoption.

## Guides

- `docs/GETTING_STARTED.md`
- `docs/USER_GUIDE.md`
- `docs/DEVELOPMENT_GUIDE.md`
- `docs/SDK_GUIDE.md`
- `docs/TROUBLESHOOTING.md`
- `docs/RELEASE_CHECKLIST.md`
- `docs/BRANDING.md`

## Existing technical docs that must not be overwritten blindly

The current repository already contains port-specific material such as:

- `AGENTS.md`
- `ANDROID_PORT_HANDOFF.md`
- `SOURCE_BASELINE.md`
- `docs/DESKTOP_SOURCE_BOOTSTRAP.md`
- `docs/ELECTRON_TO_ANDROID_MAP.md`
- `docs/FEATURE_PARITY_MATRIX.md`
- `docs/PROVIDER_PARITY.md`
- `docs/SECURITY_AND_STORAGE_CONTRACT.md`
- `docs/VENICE_API_PORT_MATRIX.md`
- `docs/superpowers/...`

The documentation-pack agent handoff explicitly requires reconciling with these files instead of replacing them.
