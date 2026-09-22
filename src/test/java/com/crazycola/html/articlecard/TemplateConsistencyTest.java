package com.crazycola.html.articlecard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.crazycola.html.articlecard.ArticleCardContracts.CardDeck;
import com.crazycola.html.articlecard.ArticleCardContracts.CardItem;
import com.crazycola.html.articlecard.ArticleCardContracts.CardPage;
import java.util.List;
import org.junit.jupiter.api.Test;

class TemplateConsistencyTest {

    @Test
    void sameTemplateProducesSameHtmlAndCarriesStyleIdentity() {
        CardTemplate template = BuiltInCardTemplateProvider.neoGrid();
        CardDeck deck = new CardDeck(
                "运营复盘",
                List.of(new CardPage(
                        1,
                        "numbered-grid",
                        "关键动作",
                        "三步把信息变成行动",
                        null,
                        List.of(
                                new CardItem("01", "识别问题", "先找到真正影响结果的变量", null),
                                new CardItem("02", "建立节奏", "让关键角色按同一节奏复盘", null)),
                        "结论先于装饰。")));

        CardHtmlRenderer renderer = new CardHtmlRenderer();
        String first = renderer.render(deck, 1080, 1440, "zh-CN", template).html();
        String second = renderer.render(deck, 1080, 1440, "zh-CN", template).html();

        assertEquals(first, second);
        assertTrue(first.contains("data-template=\"neo-grid@1.0.0\""));
        assertTrue(first.contains("data-template-fingerprint=\"" + template.fingerprint() + "\""));
    }
}
