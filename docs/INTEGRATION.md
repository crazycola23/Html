# Integration guide — Spring AI 1.1.7

This repository is intentionally provider-agnostic. It depends on Spring AI's chat client API but does not choose OpenAI, Anthropic, Ollama, or another model provider.

## 1. Integrate into an existing project

A coding agent should first inspect the target project and then either:

- copy/adapt the package under `src/main/java/com/crazycola/html/articlecard` plus resources; or
- publish this module internally and depend on it as a library.

Do not replace the host project's Spring Boot parent, model provider starter, security configuration, or Agent registry just to use this skill.

Import the reference configuration:

```java
@Import(ArticleCardSkillConfiguration.class)
@Configuration
class AgentFeatures {
}
```

The host application must already provide a Spring AI `ChatModel` bean.

## 2. Register the tool

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

If the host already registers other tool objects, add `articleCardTools` to the same registry.

## 3. Template choice

Built-ins:

- `editorial-dark@1.0.0`
- `warm-paper@1.0.0`
- `neo-grid@1.0.0`

For repeatable output, provide an exact template:

```java
ArticleCardRequest request = new ArticleCardRequest(
        content,
        title,
        4,
        6,
        1080,
        1440,
        true,
        "zh-CN",
        "editorial-dark",
        "1.0.0",
        storedFingerprint);
```

Persist `result.template()` with the job. On future regeneration, pass its id/version/fingerprint back into the request.

Do not build application logic around "latest template".

## 4. External template providers

Add templates without modifying this module by registering another `CardTemplateProvider` bean.

```java
@Bean
CardTemplateProvider brandTemplates(BrandTemplateRepository repository) {
    return new DatabaseBrandTemplateProvider(repository);
}
```

The provider compiles source definitions into validated `CardTemplate` objects. The registry rejects duplicate `id@version` refs.

This allows application-owned, tenant-owned, or remotely managed templates to coexist with the built-in catalog.

## 5. Agent-driven recommendation

Keep recommendation separate from execution.

An Agent can read `CardTemplateRegistry.catalog()` or a richer external catalog, recommend an exact template, then pass the frozen id/version/fingerprint to `article_card_composer`.

This keeps intelligent selection flexible without allowing rendering style to drift inside the composer itself.

## 6. ToolContext

Runtime metadata belongs in `ToolContext`, not model-visible arguments:

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

The reference tool deliberately does not serialize these values into `ArticleCardResult`.

## 7. Typed workflow integration

If the orchestration layer already supports typed state, prefer calling the service directly:

```java
ArticleCardResult result = articleCardSkill.compose(request);
workflowState.put("cardDeck", result.deck());
workflowState.put("cardHtml", result.render().html());
workflowState.put("cardTemplate", result.template());
```

This avoids feeding a potentially large HTML string back into an orchestration LLM.

Recommended production flow:

```
content/research
      |
      v
template recommendation/policy
      |
      v
freeze exact template snapshot
      |
      v
article_card_composer
      |
      +--> CardDeck
      +--> template snapshot
      |
      v
HTML renderer
      |
      v
html_to_image
      |
      v
publish/storage
```

## 8. Downstream image renderer

The image service only needs:

- `render.html`
- `render.width`
- `render.height`
- `render.pageSelector`

For Playwright/Chromium, load the HTML, enumerate `.card-page`, and screenshot each element separately.

Production systems should also detect DOM overflow before export.

## 9. Verification checklist

Before merging into a real application:

1. Keep Spring AI at 1.1.7.
2. Run `mvn test`.
3. Verify the Agent exposes `article_card_composer`.
4. Verify the default template resolves to an exact pinned version.
5. Persist the returned template snapshot.
6. Verify fingerprint mismatch fails.
7. Run each built-in template on representative content.
8. Verify source facts and numeric claims remain grounded.
9. Verify output page count matches `CardDeck.pages.size()`.
10. Verify existing tools remain registered.


## 10. Execution context and policy

`ArticleCardTools` maps non-model `ToolContext` keys into `CardExecutionContext`:

- `tenantId`
- `brandId`
- `projectId`
- `userId`
- `channel`
- `traceId`

The built-in deterministic selector ignores these values, but a host application can replace `CardTemplateSelector` to enforce tenant/brand/channel policy without adding sensitive policy metadata to model-visible tool parameters.

## 11. Tool result handling

The declarative tool uses `returnDirect = true`. Full HTML is therefore treated as a terminal artifact result rather than being sent through another orchestration-model round trip.

If an Agent must continue reasoning after composition, prefer calling `ArticleCardSkill` from typed workflow state and store HTML outside the LLM context. A larger production system can replace the HTML field at its orchestration boundary with an artifact reference.

## 12. Browser QA contract

The Java renderer guarantees deterministic HTML for fixed Java inputs and render contract. It does not guarantee browser layout or screenshot pixels.

A production Playwright/Chromium step should:

1. run in a pinned container with a pinned Chromium/font set;
2. deny all network requests;
3. load the HTML and wait for `document.fonts.ready`;
4. measure each `.card-page` and important content container;
5. reject `scrollHeight > clientHeight` or `scrollWidth > clientWidth`;
6. capture screenshots only after QA passes;
7. persist browser/container/font/device-scale metadata with the artifact.

1080 × 1440 is the built-in visual QA reference size. Other dimensions produce a warning until they pass downstream browser QA.
