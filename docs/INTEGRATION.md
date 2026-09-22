# Integration guide — Spring AI 1.1.7

This repository is intentionally a provider-agnostic reference module. It depends on Spring AI's chat client API but does not choose OpenAI, Anthropic, Ollama, or another model provider.

## 1. Integrate into an existing project

A coding agent should first inspect the target project and then either:

- copy/adapt the package under `src/main/java/com/crazycola/html/articlecard` plus the two resources; or
- publish this module internally and depend on it as a library.

Do **not** replace the host project's Spring Boot parent, model provider starter, security configuration, or Agent registry just to use this skill.

Import the reference configuration:

```java
@Import(ArticleCardSkillConfiguration.class)
@Configuration
class AgentFeatures {
}
```

The configuration needs the host application to already provide a Spring AI `ChatModel` bean.

## 2. Register the tool with the existing Agent

Declarative registration is the simplest path in Spring AI 1.1.x:

```java
@Bean
ChatClient mainAgent(
        ChatClient.Builder builder,
        ArticleCardTools articleCardTools) {

    return builder
            .defaultTools(articleCardTools)
            .build();
}
```

If the host already registers other tool objects, add `articleCardTools` to the same registration rather than creating a second Agent.

For codebases using explicit callbacks, Spring AI 1.1.x can adapt the same POJO:

```java
ToolCallback[] callbacks = ToolCallbacks.from(articleCardTools);
```

Then add those callbacks using the host project's existing `toolCallbacks/defaultToolCallbacks` mechanism.

## 3. ToolContext

Runtime metadata belongs in `ToolContext`, not in model-visible arguments:

```java
String response = mainAgent.prompt()
        .user(userMessage)
        .toolContext(Map.of(
                "tenantId", tenantId,
                "jobId", jobId,
                "traceId", traceId))
        .call()
        .content();
```

The reference tool deliberately does not serialize these values back into `ArticleCardResult`.

## 4. Typed workflow integration

If your orchestration layer already supports typed workflow state, prefer calling the service directly:

```java
ArticleCardResult result = articleCardSkill.compose(request);
workflowState.put("cardDeck", result.deck());
workflowState.put("cardHtml", result.render().html());
```

This avoids feeding a potentially large HTML string back into the orchestration LLM.

Recommended production flow:

```
content/research node
       |
       v
article_card_composer
       |
       +--> CardDeck (stable semantic state)
       |
       v
HTML renderer
       |
       v
html_to_image (separate capability)
       |
       v
publish/storage node
```

## 5. Downstream image renderer contract

The future image service does not need to understand the article. It only needs:

- `render.html`
- `render.width`
- `render.height`
- `render.pageSelector` (always `.card-page` in v1)

For Playwright/Chromium, load the returned HTML, wait for fonts/assets, enumerate `.card-page`, and screenshot each element separately.

## 6. Replacing visual style

The semantic contract is independent of style. Replace:

`src/main/resources/article-card/default.css`

or instantiate `CardHtmlRenderer` with custom CSS. Keep the semantic class names and page dimensions intact.

## 7. Verification checklist

Before merging into a real application:

1. Keep Spring AI at 1.1.7.
2. Run `mvn test` for this reference module.
3. Add a host-project test proving the Agent exposes `article_card_composer`.
4. Run the manufacturing example end-to-end with the host model.
5. Verify facts and numeric claims remain grounded in the input.
6. Verify output HTML page count matches `CardDeck.pages.size()`.
7. Verify existing tools remain registered.
