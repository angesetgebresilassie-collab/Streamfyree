## 2025-05-18 - Jetpack Compose Icon Button Accessibility
**Learning:** Custom UI wrappers around `Box` + `Icon` (like `CircleIcon`, `PlayerMiniButton`, `GlassIconButton`) often hardcode `contentDescription = null` on `Icon` and omit `onClickLabel` on `Modifier.clickable`. This renders icon-only buttons completely unannounced to TalkBack/screen readers.
**Action:** Always accept a `contentDescription: String? = null` parameter in custom icon component wrappers and apply it to both the `Icon` and `Modifier.clickable(onClickLabel = ...)`.
