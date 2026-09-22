# Article Card Composer Skill

A Spring AI **1.1.7** reference skill for turning article-like text into structured multi-page social card content and deterministic HTML.

The repository separates semantic content planning from visual rendering:

```
article / case study / comparison text
                |
                v
       ArticleCardPlanner
          (LLM only)
                |
                v
            CardDeck
                |
                v
       CardDeckValidator
                |
                v
        CardHtmlRenderer
     (deterministic Java)
                |
                v
     fixed-size HTML pages
                |
                v
 html_to_image (downstream)
```

## What this repository contains

- `SKILL.md` — coding-agent implementation contract.
- `skill.yaml` — machine-readable skill metadata.
- `schemas/` — stable JSON input/output contracts.
- `src/main/java/` — Spring AI 1.1.7 reference implementation.
- `src/main/resources/prompts/` — planner prompt.
- `src/main/resources/article-card/default.css` — replaceable default visual theme.
- `examples/` — manufacturing case input and expected semantic deck example.
- `docs/INTEGRATION.md` — how an Agent should integrate this capability into a real Spring AI project.

## Agent-facing capability

The default tool name is:

```
article_card_composer
```

It accepts source content plus optional page/canvas settings and returns:

- a structured `CardDeck` suitable for further workflow processing;
- deterministic HTML;
- page count, dimensions, and `.card-page` screenshot selector;
- validation warnings.

The model does **not** generate the final HTML. It generates only the semantic deck.

## Defaults

- Spring AI: 1.1.7
- Java: 17+
- page range: 3–8
- canvas: 1080 × 1440
- language: zh-CN
- rewrite: allowed when facts and numeric claims are preserved

## Use in your project

Read [docs/INTEGRATION.md](docs/INTEGRATION.md). In an existing application, keep your current Spring Boot/model-provider setup and register the `ArticleCardTools` bean with the Agent's existing tool registry.

For a coding Agent, the intended instruction is simply:

> Read `SKILL.md` and `docs/INTEGRATION.md`, inspect the host Spring AI 1.1.7 project, and integrate `article_card_composer` using the project's existing Agent/Tool registration mechanism. Do not upgrade Spring AI and do not replace unrelated infrastructure.

## Test

```bash
mvn test
```

The unit tests do not call a remote model. End-to-end planner behavior must be tested inside the host application with its configured `ChatModel`.
