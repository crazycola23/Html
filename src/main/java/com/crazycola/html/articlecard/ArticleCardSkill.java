package com.crazycola.html.articlecard;

import com.crazycola.html.articlecard.ArticleCardContracts.ArticleCardRequest;
import com.crazycola.html.articlecard.ArticleCardContracts.ArticleCardResult;
import com.crazycola.html.articlecard.ArticleCardContracts.CardDeck;
import com.crazycola.html.articlecard.ArticleCardContracts.RenderResult;
import java.util.List;

public final class ArticleCardSkill {

    public static final String SKILL_ID = "article_card_composer";
    public static final String SKILL_VERSION = "1.0.0";

    private final ArticleCardPlanner planner;
    private final CardDeckValidator validator;
    private final CardHtmlRenderer renderer;

    public ArticleCardSkill(
            ArticleCardPlanner planner,
            CardDeckValidator validator,
            CardHtmlRenderer renderer) {
        this.planner = planner;
        this.validator = validator;
        this.renderer = renderer;
    }

    public ArticleCardResult compose(ArticleCardRequest input) {
        ArticleCardRequest request = input.normalized();

        CardDeck deck = planner.plan(request);
        List<String> warnings = validator.validate(deck, request);
        RenderResult render = renderer.render(
                deck,
                request.width(),
                request.height(),
                request.language());

        if (render.pageCount() != deck.pages().size()) {
            throw new IllegalStateException("rendered page count does not match deck page count");
        }

        return new ArticleCardResult(
                SKILL_ID,
                SKILL_VERSION,
                deck,
                render,
                warnings);
    }
}
