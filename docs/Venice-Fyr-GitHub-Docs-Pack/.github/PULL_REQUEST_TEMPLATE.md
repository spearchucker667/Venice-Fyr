## Summary

<!-- What does this change do? Keep this concrete. -->

## Why

<!-- What problem/parity gap does it solve? Link an issue/spec when applicable. -->

## Source / contract checked

- [ ] Android repository contracts (`AGENTS.md`, handoff, relevant docs)
- [ ] Venice Forge desktop behavior (when porting existing behavior)
- [ ] Venice API source material (when API behavior changes)
- [ ] Live capability metadata considered where relevant

Desktop source HEAD used, if applicable:

```text
<sha or N/A>
```

## Validation

Commands run:

```text
<exact commands>
```

- [ ] relevant unit/JVM tests
- [ ] affected module build
- [ ] lint where relevant
- [ ] instrumentation tests where relevant
- [ ] migration/isolation tests where relevant

## Security / privacy

- [ ] no secrets or sensitive fixtures added
- [ ] no raw credential/prompt/response logging introduced
- [ ] profile isolation preserved
- [ ] credential ownership boundary preserved
- [ ] permissions/exported components reviewed if changed
- [ ] paid/mutating actions retain approval + duplicate-submit defenses

## Documentation / parity

- [ ] `docs/FEATURE_PARITY_MATRIX.md` updated if parity changed
- [ ] user/developer docs updated if behavior/setup changed
- [ ] no planned-state feature is represented as already implemented

## Screenshots / recordings

<!-- UI changes only; redact private content/API keys. -->

## Reviewer notes

<!-- Risks, deliberate deviations, migration concerns, follow-up work. -->
