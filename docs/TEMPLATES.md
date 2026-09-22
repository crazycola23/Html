# Template system

## 1. Design goal

The template system exists to solve three different problems with one versioned contract:

1. **aesthetic quality** — pages should look intentionally designed rather than like generic generated cards;
2. **repeatability** — the same template should keep the same design language months later;
3. **evolution** — new templates can be introduced without rewriting the composer or coupling all templates to this repository.

A template therefore owns both editorial and visual style.

---

## 2. Canonical template structure

The Java runtime contract is `CardTemplate`.

Portable/external definitions should follow `schemas/card-template.schema.json`.

Conceptually:

```json
{
  "id": "editorial-dark",
  "version": "1.0.0",
  "displayName": "Editorial Dark",
  "description": "...",
  "tags": ["business", "editorial", "premium"],
  "content": {
    "tone": "克制、权威、杂志化，不喊口号",
    "density": "balanced",
    "headlineStyle": "短句判断式标题",
    "headlineMaxChars": 32,
    "itemTitleMaxChars": 18,
    "itemBodyMaxChars": 52
  },
  "layouts": {
    "allowed": ["headline-list", "numbered-grid", "statement", "comparison"],
    "preferred": ["headline-list", "statement"],
    "maxItemsPerPage": 6
  },
  "visual": {
    "tokens": {
      "bg": "#0c1726",
      "surface": "rgba(255,255,255,.075)",
      "text": "#f5efe3",
      "muted": "#a9b3c2",
      "accent": "#f0b35a",
      "border": "rgba(245,239,227,.18)",
      "radius": "26px",
      "page-padding-x": "76px",
      "page-padding-y": "76px",
      "headline-size": "76px",
      "item-title-size": "34px",
      "body-size": "25px",
      "font-family": "..."
    },
    "css": "optional additive theme CSS"
  },
  "plannerGuidance": "..."
}
```

### Identity fields

- `id`: stable family name. Do not encode dates or "latest".
- `version`: immutable version of the complete style contract.
- `displayName`: user-facing name.
- `description`: what visual/editorial problem it solves.
- `tags`: discovery metadata for catalogs or future selector agents.

### Content fields

These are injected into the planner and also used by validation.

- `tone`: editorial voice.
- `density`: e.g. airy / balanced / dense.
- `headlineStyle`: title cadence.
- `headlineMaxChars`: target page headline length.
- `itemTitleMaxChars`: target item-title length.
- `itemBodyMaxChars`: target body length.

### Layout fields

- `allowed`: hard allowlist.
- `preferred`: selection preference exposed to planner.
- `maxItemsPerPage`: hard structural limit.

A template should keep structural decisions here instead of burying them only inside prose prompts.

### Visual fields

The renderer injects visual tokens as CSS custom properties prefixed with `--ac-`. The shared structural CSS consumes these tokens. Optional additive theme CSS can create stronger identity with gradients, borders, geometry, shadows and layout-specific refinements.

Theme CSS must be self-contained. The reference runtime rejects `@import`, `url(...)`, and style-tag breakout.

---

## 3. Selection mechanism

### Deterministic core selection

The composer uses `CardTemplateSelector`. The default selector is deliberately deterministic:

```
request.templateId + request.templateVersion
                   |
                   v
            CardTemplateRegistry
                   |
                   v
      exact immutable CardTemplate
```

Rules:

- no template supplied -> `editorial-dark@1.0.0`;
- id + version -> exact lookup;
- id only -> provider's explicitly pinned default version;
- unknown id/version -> error;
- no pinned version -> require version;
- fingerprint supplied -> exact fingerprint match required;
- never silently fallback;
- never resolve a moving `latest`.

### Why template selection is not delegated to the planner

If the same LLM call both chooses and executes a style, style can drift as prompts/models change.

Template choice should happen **before** semantic card planning. The planner receives a frozen template.

### Future Agent selection

A future AI Agent can recommend templates using `CardTemplateRegistry.catalog()` plus richer provider metadata. The Agent may use user intent, channel, brand, content type, audience, or campaign rules.

The safe pattern is:

```
catalog -> agent recommendation -> exact template snapshot -> persist snapshot
                                           |
                                           v
                                  article_card_composer
```

The recommendation may be probabilistic. The execution must not be.

---

## 4. Style consistency guarantee

"Same style" has two layers.

### Visual consistency

For a fixed:

```
CardDeck + template id/version/fingerprint + dimensions
```

the renderer produces deterministic HTML.

The generated HTML contains:

- `data-template="id@version"`
- `data-template-fingerprint="..."`

### Editorial consistency

The same template content rules are appended to the planner system contract every time. The validator then checks that layouts and density remain compatible with the template.

Model wording is not byte-for-byte deterministic and should not be presented as such. What is locked is the **design language**: tone, density, hierarchy, layout vocabulary, visual tokens, and CSS.

### Fingerprint

The fingerprint covers the complete compiled template definition, including planner guidance and CSS.

If someone changes a published `1.0.0` template in place, old jobs can send the stored fingerprint and fail fast. The correct maintenance action is to publish `1.0.1` or `1.1.0`.

---

## 5. Template evolution

Treat a template like an API.

### Safe changes

Create a new version when changing:

- visual token values;
- CSS;
- content tone;
- density;
- layout allowlist/preferences;
- content length targets;
- planner guidance.

Do not edit old versions in place.

### Family evolution example

```
editorial-dark@1.0.0
editorial-dark@1.1.0
editorial-dark@2.0.0
```

A product may deliberately move its pinned default from 1.0.0 to 1.1.0, but previously persisted jobs should continue to request their original exact version.

---

## 6. External templates

The skill does not require all templates to live in this repository.

Implement:

```java
public final class BrandTemplateProvider implements CardTemplateProvider {
    @Override
    public Collection<CardTemplate> templates() {
        return loadBrandTemplates();
    }

    @Override
    public Map<String, String> defaultVersions() {
        return Map.of("brand-report", "3.2.0");
    }
}
```

Register it as a bean:

```java
@Bean
CardTemplateProvider brandTemplateProvider(...) {
    return new BrandTemplateProvider(...);
}
```

The existing registry automatically merges it with built-ins.

Possible provider backends:

- database / configuration center;
- Git-backed design-system repository;
- tenant-specific brand store;
- CMS;
- internal template marketplace;
- generated code/config produced by a design Agent.

This is the intended door for templates that should not be compiled into the skill.

---

## 7. Future AI-agent direction

The core composer should stay deterministic. Intelligence around it can grow.

A future architecture can add:

```
TemplateCatalogTool
        |
        v
TemplateRecommendationAgent
        |
        v
TemplatePolicy / brand guard
        |
        v
freeze(id, version, fingerprint)
        |
        v
article_card_composer
```

Useful future capabilities:

- recommend templates from channel + audience + content type;
- tenant/brand template policies;
- A/B template experiments;
- automatic overflow screenshot validation;
- template quality scoring from rendered samples;
- AI-assisted template authoring that emits the external schema;
- promotion workflow: draft -> review -> immutable published version.

The important boundary is that AI may **author or recommend** templates, but published template execution resolves an exact immutable version before generation.


---

## 8. Fingerprint canonicalization

Published templates use `card-template-canonical-v1`.

The runtime does not hash Java `toString()` output. It creates an explicit length-prefixed canonical representation, normalizes line endings and Unicode NFC, sorts unordered collections (tags, allowed layouts, token maps), and preserves ordered collections where order is semantic (preferred layouts).

The fingerprint identifies the compiled template definition only. It does not include the shared structural CSS or renderer implementation; those are represented by the separate render fingerprint.

Moving version aliases such as `latest`, `current`, `default`, or `draft` are not valid published template versions. Runtime template IDs and versions are validated independently of the JSON schema.

## 9. CSS trust boundary

`CardTemplate` rejects style-tag breakout, at-rules, backslash escapes, URL/network schemes and common resource-loading constructs in additive CSS. Token values are also prevented from introducing declarations or network resources.

This is defense in depth, not a claim that arbitrary untrusted CSS is a safe public authoring language. A production template-authoring system should compile a restricted/typed template source into a published `CardTemplate`, and the screenshot browser must deny network access.

AI-authored templates should enter the lifecycle as draft source:

```
draft -> schema/policy validation -> render QA -> human/policy approval -> immutable publish
```

The authoring Agent should never publish raw CSS directly into the production registry.

## 10. Style reproducibility levels

Do not use "same style" as one undifferentiated guarantee:

- same template definition: enforced by template fingerprint;
- same HTML rendering contract: strengthened by render fingerprint;
- same browser layout: requires pinned fonts and Chromium runtime;
- same LLM wording: not guaranteed;
- same screenshot pixels: requires pinned container/browser/fonts/device scale and screenshot options.

For exact historical regeneration, store the generated `CardDeck` as an artifact instead of assuming a future model call will reproduce it.
