# Production rendering and reproducibility contract

This document defines the boundary between the deterministic Java skill and production HTML-to-image infrastructure.

## 1. Identity layers

Persist these identities separately:

### Template identity

- template id
- immutable semantic version
- fingerprint algorithm: `card-template-canonical-v1`
- template fingerprint

The template fingerprint covers the compiled editorial rules, layout rules, visual tokens, additive CSS and planner guidance.

### Render identity

- renderer version
- markup version
- structural CSS hash
- template fingerprint
- width / height
- language
- final `renderFingerprint`

The current runtime emits `render.renderFingerprint` and `data-render-fingerprint`.

### Planner execution identity

A production host should additionally persist what the core provider-agnostic skill cannot know reliably:

- model provider
- model name/revision
- temperature/top-p/seed when supported
- system prompt artifact/hash
- Spring AI version
- retry/attempt count
- source hash

Template/render identity does not make an LLM call deterministic.

### Browser identity

For pixel reproducibility persist:

- container image digest
- Chromium build
- installed font-set hash
- locale
- device scale factor
- screenshot options

## 2. Recommended job artifacts

Persist immutable references for:

- normalized source or source hash
- selected template identity
- generated CardDeck
- rendered HTML
- render fingerprint
- QA report
- screenshots

If exact historical wording matters, regenerate screenshots from the stored CardDeck/HTML instead of re-running the planner.

## 3. Browser QA pipeline

```
HTML
  -> pinned Chromium container
  -> block network
  -> set viewport/deviceScaleFactor
  -> setContent
  -> await document.fonts.ready
  -> measure DOM
  -> overflow checks
  -> optional contrast/visual checks
  -> screenshot
  -> persist QA + runtime metadata
```

At minimum, reject a page when an expected non-scrollable region has:

```js
element.scrollHeight > element.clientHeight ||
element.scrollWidth > element.clientWidth
```

Also compare descendant bounding boxes with `.card-inner` because clipped absolutely positioned content can evade simple scroll metrics.

## 4. Network isolation

The generated HTML includes CSP and the template compiler rejects obvious resource-loading CSS constructs. The browser layer must still abort network requests. Do not rely on CSS string filtering as the sole SSRF boundary.

## 5. Fonts

System fallback stacks are useful for development, not pixel reproducibility. Production should install and pin an approved CJK/Latin font set in the screenshot container.

## 6. Canvas policy

1080 × 1440 is the built-in visual QA reference canvas.

The API still accepts other sizes for compatibility, but they must be treated as unverified until browser QA passes. As channel support grows, prefer named, tested canvas presets over assuming every width/height pair is supported.

## 7. Adaptive fitting

Do not let the LLM or renderer silently shrink arbitrary text.

If adaptive fitting is introduced, make it deterministic and bounded, for example:

```
headline XL -> L -> M
body M -> S
```

Run fitting after browser measurement and record the selected fit tier in artifact metadata.

## 8. Template publication

External/AI-authored templates should follow:

```
draft
-> schema validation
-> security/policy compilation
-> representative render fixtures
-> overflow/contrast/visual QA
-> approval
-> immutable publish
-> optional deprecation
```

Production registries should execute published compiled artifacts only.
