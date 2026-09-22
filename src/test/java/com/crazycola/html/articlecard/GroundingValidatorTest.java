package com.crazycola.html.articlecard;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.crazycola.html.articlecard.ArticleCardContracts.CardDeck;
import com.crazycola.html.articlecard.ArticleCardContracts.CardItem;
import com.crazycola.html.articlecard.ArticleCardContracts.CardPage;
import java.util.List;
import org.junit.jupiter.api.Test;

class GroundingValidatorTest {

    private final GroundingValidator validator = new GroundingValidator();

    @Test
    void acceptsNumbersThatExistInSourceAndIgnoresPresentationSequenceNumbers() {
        CardDeck deck = new CardDeck(
                "12个月预算",
                List.of(new CardPage(
                        1,
                        "numbered-grid",
                        null,
                        "覆盖12个月",
                        null,
                        List.of(new CardItem("01", "预算", "利润率5%", null)),
                        null)));

        assertDoesNotThrow(() -> validator.validate(
                deck,
                "项目建立未来12个月滚动预算，当前材料明确记录利润率5%。"));
    }

    @Test
    void rejectsInventedNumericClaims() {
        CardDeck deck = new CardDeck(
                "经营改善",
                List.of(new CardPage(
                        1,
                        "statement",
                        null,
                        "利润提升30%",
                        null,
                        List.of(),
                        null)));

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(deck, "材料只说明经营机制发生变化，没有给出利润提升比例。"));
    }
}
