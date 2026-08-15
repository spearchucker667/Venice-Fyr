# Agent Handoff — Review-First GitHub Documentation Integration

## Mission

Review the supplied Venice Fyr GitHub documentation package against the **current repository at execution time**, reconcile it with existing implementation and documentation, adopt only accurate/useful pieces, and reject or rewrite anything that would become stale, misleading, legally questionable, duplicative, or inconsistent with repository contracts.

**Do not copy this package over the repository wholesale.**

The package was prepared from a live review of `spearchucker667/Venice-Fyr` on 2026-08-15 around commit:

```text
f53b8d2818f0df6fc833de3c745acdcf233f9ff7
```

The repo is moving quickly. Treat that SHA only as provenance for the candidate package, not as the source of truth when you execute this handoff.

## Candidate license decision

The package proposes **Apache License 2.0** rather than MIT.

Reason: Venice Fyr contains both an Android application and a reusable `:venice-sdk` AAR. Apache-2.0 remains permissive while including an explicit patent-license grant and patent-termination language that MIT does not provide.

This is a technical/open-source-maintenance recommendation, not legal advice.

### License gate

Before adding `LICENSE`/`NOTICE`:

1. confirm the repository does not already have a license at current HEAD;
2. inspect whether any existing source/header/docs claim a different license;
3. inspect imported/copied source for incompatible licensing;
4. verify the maintainer wants Apache-2.0 for both app and SDK unless modules require different treatment;
5. identify required third-party attribution/NOTICE obligations.

If there is an existing license decision, do **not** silently replace it.

## Mandatory discovery

From the actual repo root:

```bash
pwd
git rev-parse --show-toplevel
git branch --show-current
git status --short
git log -10 --oneline
```

Read:

```text
AGENTS.md
ANDROID_PORT_HANDOFF.md
README.md
SOURCE_BASELINE.md
settings.gradle.kts
gradle/libs.versions.toml
app/build.gradle.kts
venice-sdk/build.gradle.kts
docs/DESKTOP_SOURCE_BOOTSTRAP.md
docs/ELECTRON_TO_ANDROID_MAP.md
docs/FEATURE_PARITY_MATRIX.md
docs/PROVIDER_PARITY.md
docs/SECURITY_AND_STORAGE_CONTRACT.md
docs/VENICE_API_PORT_MATRIX.md
```

Also inventory:

```text
.github/
docs/
app/src/
venice-sdk/src/
core/
```

Search before creating each proposed file:

```bash
find . -maxdepth 3 -type f | sort
rg -n "Apache|MIT|license|copyright|privacy|telemetry|security|support|contribut|code of conduct" .
```

Do not run broad replacement operations.

## Repository facts that must be re-verified

Candidate docs were authored against a repo that contained:

```text
:app
:venice-sdk
:core:common
:core:security
:core:designsystem
:core:data
```

with an app baseline around:

```text
compileSdk = 37
targetSdk = 37
minSdk = 26
JDK 17
AGP 9.3.x
Gradle 9.5.0
```

Do not preserve those numbers in the README/guides if the current source has changed.

Likewise, verify:

- Room/data implementation status;
- chat/SSE implementation status;
- current feature parity;
- credential storage implementation;
- telemetry behavior;
- Android permissions;
- SDK credential-persistence boundary;
- actual Gradle verification commands;
- release/publishing state.

## Review each candidate independently

For every file in this package, choose one:

```text
ADOPT
MERGE
REWRITE
DEFER
REJECT
```

Record the decision and reason in a temporary review report before writing the repo.

Recommended report:

```text
docs/reviews/GITHUB_DOCS_PACK_REVIEW_2026-08-15.md
```

If repository conventions say review artifacts should remain local, keep it local instead. Do not invent a tracked review directory without checking existing practice.

## README integration

The candidate `README.md` should not automatically replace the existing README.

Perform a semantic merge.

Preserve or improve:

- accurate current Android foundation;
- desktop-source bootstrap workflow;
- source-of-truth references;
- build commands;
- alpha/incomplete-parity warning;
- project/module map;
- links to existing port docs.

Remove/update:

- stale versions;
- features not actually implemented;
- candidate links to documents you decide not to adopt;
- branding claims inconsistent with current app assets.

The banner may be added only if it fits current visual direction.

Verify all relative links after integration.

## Existing docs have priority

Do not overwrite these merely because similarly scoped text exists in the package:

```text
AGENTS.md
ANDROID_PORT_HANDOFF.md
SOURCE_BASELINE.md
docs/DESKTOP_SOURCE_BOOTSTRAP.md
docs/ELECTRON_TO_ANDROID_MAP.md
docs/FEATURE_PARITY_MATRIX.md
docs/PROVIDER_PARITY.md
docs/SECURITY_AND_STORAGE_CONTRACT.md
docs/VENICE_API_PORT_MATRIX.md
docs/superpowers/
```

The package is intended to add general GitHub/community/user/developer documentation around them.

If package wording conflicts with a verified existing contract, the verified contract wins.

## Security/privacy review

Before adopting `SECURITY.md` or `PRIVACY.md`, inspect actual code/config for:

- manifest permissions;
- exported Android components;
- network-security config;
- credential store implementation;
- logs/redaction;
- telemetry/analytics/crash SDKs;
- Room entities/repositories;
- profile scoping;
- provider request boundaries;
- file/document access;
- WebView usage;
- third-party SDKs.

Do not publish a privacy promise the implementation does not satisfy.

Security-reporting wording must match GitHub repository settings. If Private Vulnerability Reporting is not enabled, either enable/configure it separately with maintainer approval or rewrite the reporting instructions to the actual available private contact path.

Never put a personal email in public docs unless it is already intentionally published for this project.

## `.github` review

The package proposes:

```text
.github/CODEOWNERS
.github/PULL_REQUEST_TEMPLATE.md
.github/ISSUE_TEMPLATE/bug_report.yml
.github/ISSUE_TEMPLATE/feature_request.yml
.github/ISSUE_TEMPLATE/documentation.yml
.github/ISSUE_TEMPLATE/config.yml
```

Before adding:

1. inventory existing `.github`;
2. inspect current labels;
3. ensure labels referenced by issue forms exist or decide whether GitHub's missing-label behavior is acceptable;
4. verify repository contribution workflow;
5. verify CODEOWNERS is desired;
6. verify the security/support URLs remain correct.

Do not add workflows, Dependabot, release automation, branch-protection changes, or GitHub Apps merely because a general “GitHub docs” task might suggest them. Those are operational changes and outside this package unless independently requested.

## Branding assets

Candidate assets:

```text
docs/assets/venice-fyr-banner.png
docs/assets/venice-fyr-banner.svg
docs/assets/venice-fyr-social-preview.png
docs/assets/venice-fyr-mark.png
docs/assets/venice-fyr-mark.svg
```

Review them against current app/repo branding.

Do not replace existing launcher icons/app assets automatically.

The social preview file is only a candidate image. GitHub repository social-preview configuration is a separate repository setting and should not be assumed merely because the image is committed.

Check:

- legibility in GitHub dark/light layouts;
- file size;
- trademark conflicts;
- accessibility alt text;
- visual consistency with app theme;
- whether PNG or SVG should be referenced from README.

## Legal review

Inspect all dependency and source provenance before adopting Apache-2.0.

At minimum:

```bash
./gradlew :app:dependencies
./gradlew :venice-sdk:dependencies
```

Use more targeted dependency reports if those are too noisy.

Do not claim the root Apache license changes third-party licenses.

`NOTICE` contains a deliberate release-time instruction to review third-party notices. Improve it if the current dependency graph yields concrete required notices.

If the project will be monetized, distributed through an app store, or presented as an official Venice product, escalate the legal/trademark text to the maintainer rather than guessing.

## Validation after integration

Run documentation/static checks available in the repo.

At minimum:

```bash
git diff --check
```

Validate Markdown links with an existing repo script if one exists.

Validate YAML syntax for issue forms.

Validate that the README references only files actually adopted.

Then run the normal repository gates required by `AGENTS.md`, because documentation changes can still affect Gradle metadata or packaging if the integration touched more than docs.

Known historical release-candidate command:

```bash
./gradlew test lint :app:assembleDebug :venice-sdk:assembleRelease
```

Use the current `AGENTS.md` command if it has changed.

## Git discipline

- Work on the current intended branch per repo/user instructions.
- Do not create a PR unless requested.
- Do not push unless requested.
- Do not rewrite unrelated files.
- Do not normalize every Markdown file.
- Do not delete existing technical docs as “duplicates.”
- Keep the integration reviewable.

Prefer logical commits, for example:

```text
docs: refresh repository landing page and guides
community: add contribution and issue templates
legal: add Apache-2.0 project licensing docs
docs(brand): add Venice Fyr repository artwork
```

Do not force this exact split if repository history favors another convention.

## Deliverable

At completion report:

1. current HEAD reviewed;
2. candidate file decision table;
3. files adopted/merged/rewritten;
4. files rejected/deferred and why;
5. license decision and evidence;
6. privacy/security verification performed;
7. commands/tests run;
8. broken/stale links found and fixed;
9. Git status;
10. commits created;
11. whether anything was pushed.

Most importantly, explicitly state any candidate claim that was removed because the current implementation did not support it.

## Do not

- Do not blindly copy this zip into the repo.
- Do not blindly replace README.md.
- Do not replace `AGENTS.md`.
- Do not rewrite parity matrices from package assumptions.
- Do not claim “production ready.”
- Do not invent release downloads.
- Do not invent Play Store availability.
- Do not invent Maven publishing.
- Do not invent security contact information.
- Do not publish secrets.
- Do not make Venice.ai affiliation claims without evidence.
- Do not add CI or operational automation as a side effect.
- Do not silently choose a license different from the maintainer's verified intent.
