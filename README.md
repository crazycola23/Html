# Article Card Composer Skill

A Spring AI **1.1.7** reference skill for turning article-like text into structured multi-page social cards and deterministic, template-driven HTML.

The repository separates semantic planning, template selection, validation, and rendering:

```
article / case study / comparison text
                |
                v
      versioned CardTemplate
                |
                +---- content style contract
                |        |
                v        v
       ArticleCardPlanner (LLM)
                |
                v
            CardDeck
                |
                v
       CardDeckValidator
                |
                v
        CardHtmlRenderer
     (deterministic Java + exact template)
                |
                v
     fixed-size HTML pages
                |
                v
 html_to_image (downstream)
```

## Why templates are first-class

A template is not just CSS. One exact template version owns:

- editorial tone and information density;
- headline / item length targets;
- allowed and preferred layouts;
- visual tokens;
- additive theme CSS;
- planner guidance;
- an immutable SHA-256 fingerprint.

The same resolved template object is used by the planner, validator, and renderer. This prevents a common failure mode where the model writes in one style while the renderer uses another.

## Built-in templates

| Template | Style | Good for |
| --- | --- | --- |
| `editorial-dark@1.0.0` | premium dark editorial | business cases, insights, knowledge cards |
| `warm-paper@1.0.0` | warm paper / consulting | stories, methods, human-centered business content |
| `neo-grid@1.0.0` | high-contrast modern grid | steps, checklists, comparisons, data-heavy cards |

The default is pinned to **`editorial-dark@1.0.0`**. The composer never resolves a moving `latest` alias.

## Stable style across time

For reproducible use:

1. choose `templateId` and `templateVersion`;
2. persist the returned `template.fingerprint`;
3. send all three values when regenerating later.

If a provider mutates the content of an existing template version, the fingerprint check fails instead of silently changing the design.

Visual output is deterministic for a fixed `CardDeck + template + dimensions`. LLM wording can still vary across model versions, but the template contract constrains tone, density, layout choice, and length targets.

## Agent-facing capability

The default tool name remains:

```
article_card_composer
```

New optional inputs:

- `templateId`
- `templateVersion`
- `templateFingerprint`

Existing callers that omit them keep working and use the pinned default template.

The result now includes an exact template snapshot:

```json
{
  "template": {
    "id": "editorial-dark",
    "version": "1.0.0",
    "fingerprint": "...",
    "displayName": "Editorial Dark"
  }
}
```

## Extending templates without changing this skill

Implement `CardTemplateProvider` and register it as a Spring bean. A provider may load templates from:

- application code;
- a database;
- a Git repository;
- an internal design-system service;
- a CMS / configuration service;
- a future AI-assisted template authoring pipeline.

The core skill only consumes the compiled `CardTemplate` contract. External templates do not need to be committed into this repository.

See [docs/TEMPLATES.md](docs/TEMPLATES.md) for the complete definition, selection, versioning, and future Agent design.

## Repository contents

- `SKILL.md` — coding-agent implementation contract.
- `skill.yaml` — machine-readable skill metadata.
- `schemas/` — request/output/template contracts.
- `src/main/java/` — Spring AI 1.1.7 reference implementation.
- `src/main/resources/prompts/` — planner prompt.
- `src/main/resources/article-card/` — structural CSS and built-in theme CSS.
- `examples/` — manufacturing example.
- `docs/TEMPLATES.md` — template system and extension model.
- `docs/INTEGRATION.md` — Spring AI integration guide.

## Defaults

- Spring AI: 1.1.7
- Java: 17+
- page range: 3–8
- canvas: 1080 × 1440
- language: zh-CN
- rewrite: allowed when facts and numeric claims are preserved
- template: editorial-dark@1.0.0

## Test

```bash
mvn test
```

Unit tests do not call a remote model. End-to-end planner behavior should be tested inside the host application with its configured `ChatModel`.
