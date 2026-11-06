# CI-only Chinese glyph fallback

`GardeniaLabCJK-Subset.otf` is a renamed, local subset of Noto Sans CJK SC Regular from the execution environment's `/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc` (font index 2), licensed under SIL OFL 1.1. Original copyright notices and the full license are preserved in `OFL.txt` and the font metadata.

It includes the Chinese codepoints and supported Unicode punctuation actually used by `index.html`, `app.js`, and `styles.css`. CI installs it into its system font directory before launching Chrome, because the stock hosted runner does not include CJK coverage. It is **not a webfont**, is not referenced by CSS, and is excluded from the Pages artifact together with all test tooling. The website still uses visitors' system fonts, without external requests.

Regenerate after adding Chinese UI copy using fontTools subset tools and rename the derivative family to `Gardenia UI Lab CJK`, preserving OFL metadata. Browser acceptance inspects Chrome's actual platform font used for Chinese page titles so missing glyphs cannot silently pass.
