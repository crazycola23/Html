package com.crazycola.html.articlecard;

import com.crazycola.html.articlecard.ArticleCardContracts.ArticleCardRequest;
import com.crazycola.html.articlecard.ArticleCardContracts.CardDeck;
import com.crazycola.html.articlecard.ArticleCardContracts.CardItem;
import com.crazycola.html.articlecard.ArticleCardContracts.CardPage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class CardDeckValidator {

    private static final Set<String> LAYOUTS = Set.of(
            "headline-list",
            "numbered-grid",
            "statement",
            "steps",
            "comparison");

    public List<String> validate(CardDeck deck, ArticleCardRequest request) {
        if (deck == null || deck.pages() == null) {
            throw new IllegalArgumentException("deck/pages must not be null");
        }

        int count = deck.pages().size();
        if (count < request.minPages() || count > request.maxPages()) {
            throw new IllegalArgumentException(
                    "deck page count " + count + " is outside requested range "
                            + request.minPages() + ".." + request.maxPages());
        }

        List<String> warnings = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            CardPage page = deck.pages().get(i);
            if (page == null) {
                throw new IllegalArgumentException("page " + (i + 1) + " must not be null");
            }
            if (page.headline() == null || page.headline().isBlank()) {
                throw new IllegalArgumentException("page " + (i + 1) + " headline must not be blank");
            }
            if (page.layout() == null || !LAYOUTS.contains(page.layout())) {
                throw new IllegalArgumentException(
                        "page " + (i + 1) + " has unsupported layout: " + page.layout());
            }

            List<CardItem> items = page.items() == null ? List.of() : page.items();
            if (items.size() > 6) {
                throw new IllegalArgumentException("page " + (i + 1) + " has more than 6 items");
            }

            for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
                CardItem item = items.get(itemIndex);
                if (item == null || item.title() == null || item.title().isBlank()) {
                    throw new IllegalArgumentException(
                            "page " + (i + 1) + " item " + (itemIndex + 1) + " title must not be blank");
                }
                if (item.body() != null && item.body().length() > 140) {
                    warnings.add("page " + (i + 1) + " item " + (itemIndex + 1)
                            + " body is long for a fixed-size card");
                }
            }

            if (page.headline().length() > 70) {
                warnings.add("page " + (i + 1) + " headline is long for a fixed-size card");
            }
        }
        return List.copyOf(warnings);
    }
}
