package com.crazycola.html.articlecard;

import com.crazycola.html.articlecard.ArticleCardContracts.ArticleCardRequest;
import java.util.Objects;

@FunctionalInterface
public interface CardTemplateSelector {

    CardTemplate select(ArticleCardRequest request);

    static CardTemplateSelector deterministic(CardTemplateRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        return request -> registry.resolve(
                request.templateId(),
                request.templateVersion(),
                request.templateFingerprint());
    }
}
