package com.crazycola.html.articlecard;

import com.crazycola.html.articlecard.ArticleCardContracts.ArticleCardRequest;
import com.crazycola.html.articlecard.ArticleCardContracts.CardExecutionContext;
import java.util.Objects;

@FunctionalInterface
public interface CardTemplateSelector {

    CardTemplate select(ArticleCardRequest request, CardExecutionContext context);

    default CardTemplate select(ArticleCardRequest request) {
        return select(request, CardExecutionContext.empty());
    }

    static CardTemplateSelector deterministic(CardTemplateRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        return (request, context) -> registry.resolve(
                request.templateId(),
                request.templateVersion(),
                request.templateFingerprint());
    }
}
