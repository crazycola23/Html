package com.crazycola.html.articlecard;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.crazycola.html.articlecard.ArticleCardContracts.ArticleCardRequest;
import com.crazycola.html.articlecard.ArticleCardContracts.CardDeck;
import com.crazycola.html.articlecard.ArticleCardContracts.CardItem;
import com.crazycola.html.articlecard.ArticleCardContracts.CardPage;
import java.util.List;
import org.junit.jupiter.api.Test;

class CardDeckValidatorTest {

    @Test
    void rejectsMoreThanSixItemsOnOnePage() {
        List<CardItem> items = List.of(
                item("1"), item("2"), item("3"), item("4"), item("5"), item("6"), item("7"));

        CardDeck deck = new CardDeck(
                "demo",
                List.of(new CardPage(1, "numbered-grid", null, "headline", null, items, null)));

        ArticleCardRequest request = new ArticleCardRequest(
                "source", null, 1, 1, 1080, 1440, true, "zh-CN").normalized();

        assertThrows(
                IllegalArgumentException.class,
                () -> new CardDeckValidator().validate(deck, request));
    }

    private CardItem item(String value) {
        return new CardItem(value, "title", null, null);
    }
}
