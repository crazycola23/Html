package com.crazycola.html.articlecard;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        ArticleCardRequest request = request(1080, 1440);

        assertThrows(
                IllegalArgumentException.class,
                () -> new CardDeckValidator().validate(deck, request));
    }

    @Test
    void rejectsPageIndexThatDisagreesWithListPosition() {
        CardDeck deck = new CardDeck(
                "demo",
                List.of(new CardPage(2, "statement", null, "headline", null, List.of(), null)));

        assertThrows(
                IllegalArgumentException.class,
                () -> new CardDeckValidator().validate(deck, request(1080, 1440)));
    }

    @Test
    void warnsWhenCanvasIsOutsideBuiltInQaReferenceSize() {
        CardDeck deck = new CardDeck(
                "demo",
                List.of(new CardPage(1, "statement", null, "headline", null, List.of(), null)));

        List<String> warnings = new CardDeckValidator().validate(deck, request(1080, 1080));

        assertTrue(warnings.stream().anyMatch(value -> value.contains("browser overflow validation")));
    }

    private ArticleCardRequest request(int width, int height) {
        return new ArticleCardRequest(
                "source", null, 1, 1, width, height, true, "zh-CN").normalized();
    }

    private CardItem item(String value) {
        return new CardItem(value, "title", null, null);
    }
}
