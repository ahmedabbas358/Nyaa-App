# AniFlow Accessibility & Localization

## 1. Accessibility Semantics
- Every interactive element (IconButton, Checkbox, Card) defines an explicit `contentDescription`.
- States do not rely on color alone; badges combine explicit labels (`Downloaded`, `Failed`, `Queued`) with icons and semantic colors.
- Touch targets strictly adhere to standard 48x48dp minimum bounds.

## 2. Dynamic Text & Font Scaling
- Typography uses scalable sp units that remain resilient under high system font scales (`FontScale > 1.3`).
- Cards avoid rigid height limits to prevent text clipping when users enlarge device fonts.

## 3. RTL & Localization
- Arabized LTR/RTL mirror layouts automatically adapt under Arabic locales.
- Monospaced numeric counters (speeds, file sizes, hashes, and download progress percentages) maintain consistent readability.
