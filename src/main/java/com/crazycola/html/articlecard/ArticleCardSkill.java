package com.crazycola.html.articlecard;

import com.crazycola.html.articlecard.ArticleCardContracts.ArticleCardRequest;
import com.crazycola.html.articlecard.ArticleCardContracts.ArticleCardResult;
import com.crazycola.html.articlecard.ArticleCardContracts.CardDeck;
import com.crazycola.html.articlecard.ArticleCardContracts.CardExecutionContext;
import com.crazycola.html.articlecard.ArticleCardContracts.RenderResult;
import java.util.List;

public final class ArticleCardSkill {

    public static final String SKILL_ID = "article_card_composer";
    public static final String SKILL_VERSION = "1.1.0";
    private static final int MAX_PLANNING_ATTEMPTS = 2;

    private final ArticleCardPlanner planner;
    private final CardDeckValidator validator;
    private final GroundingValidator groundingValidator;
    private final CardHtmlRenderer renderer;
    private final CardTemplateSelector templateSelector;

    public ArticleCardSkill(
            ArticleCardPlanner planner,
            CardDeckValidator validator,
            CardHtmlRenderer renderer) {
        this(
                planner,
                validator,
                new GroundingValidator(),
                renderer,
                CardTemplateSelector.deterministic(CardTemplateRegistry.builtInOnly()));
    }

    public ArticleCardSkill(
            ArticleCardPlanner planner,
            CardDeckValidator validator,
            CardHtmlRenderer renderer,
            CardTemplateSelector templateSelector) {
        this(planner, validator, new GroundingValidator(), renderer, templateSelector);
    }

    public ArticleCardSkill(
            ArticleCardPlanner planner,
            CardDeckValidator validator,
            GroundingValidator groundingValidator,
            CardHtmlRenderer renderer,
            CardTemplateSelector templateSelector) {
        this.planner = planner;
        this.validator = validator;
        this.groundingValidator = groundingValidator;
        this.renderer = renderer;
        this.templateSelector = templateSelector;
    }

    public ArticleCardResult compose(ArticleCardRequest input) {
        return compose(input, CardExecutionContext.empty());
    }

    public ArticleCardResult compose(ArticleCardRequest input, CardExecutionContext context) {
        ArticleCardRequest request = input.normalized();
        CardExecutionContext executionContext = context == null ? CardExecutionContext.empty() : context;
        CardTemplate template = templateSelector.select(request, executionContext);

        CardDeck deck = null;
        List<String> warnings = List.of();
        RuntimeException lastValidationFailure = null;
        String feedback = null;

        for (int attempt = 1; attempt <= MAX_PLANNING_ATTEMPTS; attempt++) {
            try {
                deck = planner.plan(request, template, feedback);
                warnings = validator.validate(deck, request, template);
                groundingValidator.validate(deck, request.content());
                lastValidationFailure = null;
                break;
            }
            catch (ArticleCardPlanner.PlannerOutputException | IllegalArgumentException e) {
                lastValidationFailure = e;
                feedback = "attempt " + attempt + " rejected: " + safeMessage(e);
            }
        }

        if (lastValidationFailure != null || deck == null) {
            throw new IllegalStateException(
                    "planner output failed deterministic validation after "
                            + MAX_PLANNING_ATTEMPTS + " attempts",
                    lastValidationFailure);
        }

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

    private static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return error.getClass().getSimpleName();
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
