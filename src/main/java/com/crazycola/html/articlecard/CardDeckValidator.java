package com.crazycola.html.articlecard;

import com.crazycola.html.articlecard.ArticleCardContracts.ArticleCardRequest;
import com.crazycola.html.articlecard.ArticleCardContracts.CardDeck;
import com.crazycola.html.articlecard.ArticleCardContracts.CardItem;
import com.crazycola.html.articlecard.ArticleCardContracts.CardPage;
import java.util.ArrayList;
import java.util.List;

public final class CardDeckValidator {

    public List<String> validate(CardDeck deck, ArticleCardRequest request) {
        return validate(deck, request, BuiltInCardTemplateProvider.editorialDark());
    }

    public List<String> validate(CardDeck deck, ArticleCardRequest request, CardTemplate template) {
        if (deck == null || deck.pages() == null) {
            throw new IllegalArgumentException("deck/pages must not be null");
        }
        if (deck.deckTitle() == null || deck.deckTitle().isBlank()) {
            throw new IllegalArgumentException("deckTitle must not be blank");
        }

        int count = deck.pages().size();
        if (count < request.minPages() || count > request.maxPages()) {
            throw new IllegalArgumentException(
                    "deck page count " + count + " is outside requested range "
                            + request.minPages() + ".." + request.maxPages());
        }

        List<String> warnings = new ArrayList<>();
        if (request.width() != 1080 || request.height() != 1440) {
            warnings.add("canvas " + request.width() + "x" + request.height()
                    + " is not the built-in QA reference size 1080x1440; downstream browser overflow validation is required");
        }

        for (int i = 0; i < count; i++) {
            CardPage page = deck.pages().get(i);
            if (page == null) {
                throw new IllegalArgumentException("page " + (i + 1) + " must not be null");
            }
            if (page.index() != null && page.index() != i + 1) {
                throw new IllegalArgumentException(
                        "page " + (i + 1) + " index must equal its 1-based list position");
            }
            if (page.headline() == null || page.headline().isBlank()) {
                throw new IllegalArgumentException("page " + (i + 1) + " headline must not be blank");
            }
            if (page.layout() == null || !template.layouts().allowedLayouts().contains(page.layout())) {
                throw new IllegalArgumentException(
                        "page " + (i + 1) + " has unsupported layout for template "
                                + template.ref() + ": " + page.layout());
            }

            List<CardItem> items = page.items();
            if (items.size() > template.layouts().maxItemsPerPage()) {
                throw new IllegalArgumentException(
                        "page " + (i + 1) + " has more than "
                                + template.layouts().maxItemsPerPage() + " items");
            }
            if ("statement".equals(page.layout()) && !items.isEmpty()) {
                throw new IllegalArgumentException(
                        "page " + (i + 1) + " uses statement layout and must not contain items");
            }

            for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
                CardItem item = items.get(itemIndex);
                if (item == null || item.title() == null || item.title().isBlank()) {
                    throw new IllegalArgumentException(
                            "page " + (i + 1) + " item " + (itemIndex + 1) + " title must not be blank");
                }
                if (length(item.title()) > template.content().itemTitleMaxChars()) {
                    warnings.add("page " + (i + 1) + " item " + (itemIndex + 1)
                            + " title exceeds template target of "
                            + template.content().itemTitleMaxChars() + " chars");
                }
                if (item.body() != null && length(item.body()) > template.content().itemBodyMaxChars()) {
                    warnings.add("page " + (i + 1) + " item " + (itemIndex + 1)
                            + " body exceeds template target of "
                            + template.content().itemBodyMaxChars() + " chars");
                }
            }

            if (length(page.headline()) > template.content().headlineMaxChars()) {
                warnings.add("page " + (i + 1) + " headline exceeds template target of "
                        + template.content().headlineMaxChars() + " chars");
            }
        }
        return List.copyOf(warnings);
    }

    private static int length(String value) {
        if (value == null) {
            return 0;
        }
        return value.codePointCount(0, value.length());
    }
}
