package com.crazycola.html.articlecard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.crazycola.html.articlecard.ArticleCardContracts.CardDeck;
import com.crazycola.html.articlecard.ArticleCardContracts.CardItem;
import com.crazycola.html.articlecard.ArticleCardContracts.CardPage;
import com.crazycola.html.articlecard.ArticleCardContracts.RenderResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class CardHtmlRendererTest {

    @Test
    void rendersFixedPagesEscapesSourceTextAndAddsSecurityMetadata() {
        CardDeck deck = new CardDeck(
                "demo",
                List.of(
                        new CardPage(
                                1,
                                "numbered-grid",
                                "案例",
                                "利润 < 风险 & 现金",
                                null,
                                List.of(new CardItem("01", "订单", "\"不要猜数字\"", null)),
                                "结论"),
                        new CardPage(
                                2,
                                "statement",
                                null,
                                "预算先行",
                                null,
                                List.of(),
                                null)));

        CardHtmlRenderer renderer = new CardHtmlRenderer("body{}");
        RenderResult result = renderer.render(deck, 1080, 1440, "zh-CN");

        assertEquals(1080, result.width());
        assertEquals(1440, result.height());
        assertEquals(2, result.pageCount());
        assertEquals(".card-page", result.pageSelector());
        assertNotNull(result.renderFingerprint());
        assertEquals(64, result.renderFingerprint().length());

        assertEquals(2, occurrences(result.html(), "class=\"card-page\""));
        assertTrue(result.html().contains("利润 &lt; 风险 &amp; 现金"));
        assertTrue(result.html().contains("&quot;不要猜数字&quot;"));
        assertFalse(result.html().contains("利润 < 风险"));
        assertTrue(result.html().contains("Content-Security-Policy"));
        assertTrue(result.html().contains("data-render-fingerprint=\"" + result.renderFingerprint() + "\""));
    }

    @Test
    void renderFingerprintChangesWhenStructuralCssChanges() {
        CardTemplate template = BuiltInCardTemplateProvider.editorialDark();

        String first = new CardHtmlRenderer("body{}")
                .renderFingerprint(1080, 1440, "zh-CN", template);
        String second = new CardHtmlRenderer("body{margin:1px}")
                .renderFingerprint(1080, 1440, "zh-CN", template);

        assertNotEquals(first, second);
    }

    private int occurrences(String value, String needle) {
        int count = 0;
        int from = 0;
        while ((from = value.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }
}
