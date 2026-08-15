# Legal and Project Notices

> [!NOTE]
> This document is project documentation, not legal advice. Maintainers should obtain qualified legal review before commercial distribution, paid releases, enterprise deployment, or any change that materially affects data handling, licensing, trademarks, or third-party service use.

## License

The repository license is the **Apache License 2.0**. The full license text is in [`LICENSE`](LICENSE).

Apache-2.0 is adopted for Venice Fyr because the repository contains both an application and a reusable SDK/library boundary. In addition to permissive redistribution terms, Apache-2.0 includes an explicit contributor patent license.

If the maintainer chooses a different license, update all of the following together:

- `LICENSE`
- `NOTICE`
- `README.md`
- package/release metadata
- source-file license headers if the project later adopts them
- any GitHub repository license setting or documentation

Do not mix incompatible licensing statements.

## Third-party software

Venice Fyr depends on Android, Kotlin, AndroidX, Jetpack Compose, OkHttp, Kotlin coroutines/serialization, Room and other third-party components.

Each dependency retains its own copyright and license.

Before distributing production APK/AAB/AAR artifacts:

1. inspect the resolved dependency graph;
2. identify bundled/runtime dependencies;
3. collect required license and attribution notices;
4. verify whether any dependency requires NOTICE reproduction;
5. ensure app-store/release documentation contains required notices.

Do not assume Apache-2.0 on this repository relicenses third-party code.

## Venice AI and trademarks

Venice Fyr is an independently maintained client intended to interoperate with Venice AI.

Names such as “Venice”, “Venice AI”, model names, logos, service marks and related branding may be trademarks or other protected identifiers of their respective owners.

Venice brand marks, guidelines, and Built in Venice badge assets are referenced from the official Venice.ai brand kit in accordance with Venice brand guidance. The project does not claim official endorsement, sponsorship, or corporate affiliation.

## Third-party media and character artwork

The repository includes a reference loading animation spritesheet based on `ayanami-rei.codex-pet`. Third-party character artwork remains subject to its respective copyright, trademark, and redistribution rights. Maintainers must verify redistribution rights before publishing binary packages to public distribution channels or app stores.

## API and service terms

Use of Venice AI or another configured provider is subject to that provider's current terms, pricing, availability, API behavior and account requirements.

The project's open-source license governs the repository code. It does not grant rights to:

- a third-party API account;
- paid model access;
- model weights;
- provider-hosted content;
- third-party trademarks;
- provider-specific datasets or generated media.

Users are responsible for obtaining and protecting their own credentials.

## Generated content

Generative-model outputs can be inaccurate, objectionable, infringing, unsafe, or unsuitable for a particular use.

The application should not represent generated output as guaranteed accurate, lawful, unique, or fit for a particular purpose.

Users remain responsible for evaluating outputs and complying with applicable law, platform rules, contractual obligations and third-party rights.

## No warranty

The repository is provided under the warranty disclaimer in Apache License 2.0.

Development builds may contain incomplete features, migrations, experimental API behavior or breaking changes. Do not describe an alpha/beta build as production-ready without release evidence.

## Privacy and security

See:

- [`PRIVACY.md`](PRIVACY.md)
- [`SECURITY.md`](SECURITY.md)
- [`docs/SECURITY_AND_STORAGE_CONTRACT.md`](docs/SECURITY_AND_STORAGE_CONTRACT.md)

If implementation changes conflict with these documents, update the documents or fix the implementation before release. Documentation must not promise privacy/security behavior that the code does not provide.
