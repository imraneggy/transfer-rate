---
name: Compose stringResource — never write a local shim
description: When refactoring Android Compose code to use stringResource(), import the real androidx.compose.ui.res.stringResource — never define a private local shim, because it silently shadows the vararg formatArgs overload.
type: feedback
originSessionId: 76d9bf25-7e54-4c08-a667-1570a1f76389
---
When refactoring Android Compose UI code to use `stringResource()` for i18n, **always** import the real function from `androidx.compose.ui.res.stringResource` directly. Never define a private/local `@Composable fun stringResource(id: Int): String = ...` shim, even if it makes call sites terser.

**Why:** The real Compose `stringResource` is overloaded — there is `stringResource(id: Int): String` AND `stringResource(id: Int, vararg formatArgs: Any): String`. A local function with the same name shadows BOTH from name resolution. Single-arg call sites still compile (they resolve to the local shim), but format-arg call sites like `stringResource(R.string.last_updated, relativeTime(completedAt))` fail at compile time with:

> `Too many arguments for 'fun stringResource(id: Int): String'`

This bit on Transfer Rate v0.30.0 — a leftover shim in `RatesScreen.kt` from earlier in the file's history made the v0.30.0 tag-push CI build fail at the `Build release APK` step, requiring a v0.30.1 hotfix to recover. The fix was 1 import added + 3 lines deleted; trivial once spotted, but the symptom (compile error in code that "looks identical to working examples nearby") is misleading enough to waste 10+ minutes of investigation if you don't know to grep for shadowing first.

**How to apply:**

* When you start a localization pass on an Android Compose file, first check whether a local `stringResource` shim exists (`grep -n "fun stringResource" RatesScreen.kt`). If it does, delete it and add `import androidx.compose.ui.res.stringResource` before refactoring call sites.
* When CI fails with `Too many arguments for 'fun stringResource(id: Int)'`, the cause is almost always shadowing — not a missing import. Don't try to add a second overload of the shim; remove the shim entirely.
* The same shadowing risk applies to any Compose helper that has both a no-arg and a vararg form (`painterResource` does NOT, but `pluralStringResource` does — has `(id, count)` and `(id, count, vararg)` overloads).
