package com.crazycola.html.articlecard;

import com.crazycola.html.articlecard.ArticleCardContracts.ArticleCardRequest;
import com.crazycola.html.articlecard.ArticleCardContracts.ArticleCardResult;
import com.crazycola.html.articlecard.ArticleCardContracts.CardDeck;
import com.crazycola.html.articlecard.ArticleCardContracts.RenderResult;
import java.util.List;

public final class ArticleCardSkill {

    public static final String SKILL_ID = "article_card_composer";
    public static final String SKILL_VERSION = "1.1.0";

    private final ArticleCardPlanner planner;
    private final CardDeckValidator validator;
    private final CardHtmlRenderer renderer;
    private final CardTemplateSelector templateSelector;

    public ArticleCardSkill(
            ArticleCardPlanner planner,
            CardDeckValidator validator,
            CardHtmlRenderer renderer) {
        this(
                planner,
                validator,
                renderer,
                CardTemplateSelector.deterministic(CardTemplateRegistry.builtInOnly()));
    }

    public ArticleCardSkill(
            ArticleCardPlanner planner,
            CardDeckValidator validator,
            CardHtmlRenderer renderer,
            CardTemplateSelector templateSelector) {
        this.planner = planner;
        this.validator = validator;
        this.renderer = renderer;
        this.templateSelector = templateSelector;
    }

    public ArticleCardResult compose(ArticleCardRequest input) {
        ArticleCardRequest request = input.normalized();
        CardTemplate template = templateSelector.select(request);

        CardDeck deck = planner.plan(request, template);
        List<String> warnings = validator.validate(deck, request, template);
        RenderResult render = renderer.render(
                deck,
                request.width(),
                request.height(),
                request.language(),
                template);

        if (render.pageCount() != deck.pages().size()) {
            throw new IllegalStateException("rendered page count does not match deck page count");
        }

        return new ArticleCardResult(
                SKILL_ID,
                SKILL_VERSION,
                template.snapshot(),
                deck,
                render,
                warnings);
    }
}
