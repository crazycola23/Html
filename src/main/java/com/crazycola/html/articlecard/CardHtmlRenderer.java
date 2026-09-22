package com.crazycola.html.articlecard;

import com.crazycola.html.articlecard.ArticleCardContracts.CardDeck;
import com.crazycola.html.articlecard.ArticleCardContracts.CardItem;
import com.crazycola.html.articlecard.ArticleCardContracts.CardPage;
import com.crazycola.html.articlecard.ArticleCardContracts.RenderResult;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class CardHtmlRenderer {

    public static final String PAGE_SELECTOR = ".card-page";

    private final String baseCss;

    public CardHtmlRenderer() {
        this(loadDefaultCss());
    }

    public CardHtmlRenderer(String baseCss) {
        this.baseCss = baseCss == null ? "" : baseCss;
    }

    public RenderResult render(CardDeck deck, int width, int height, String language) {
        return render(deck, width, height, language, BuiltInCardTemplateProvider.editorialDark());
    }

    public RenderResult render(
            CardDeck deck,
            int width,
            int height,
            String language,
            CardTemplate template) {

        StringBuilder html = new StringBuilder(24_576);
        html.append("<!doctype html>\n<html lang=\"")
                .append(escapeAttribute(language))
                .append("\">\n<head>\n")
                .append("<meta charset=\"UTF-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n")
                .append("<style>\n")
                .append(":root{--card-width:").append(width).append("px;--card-height:")
                .append(height).append("px;");

        for (Map.Entry<String, String> token : new TreeMap<>(template.tokens()).entrySet()) {
            html.append("--ac-").append(token.getKey()).append(":").append(token.getValue()).append(";");
        }

        html.append("}\n")
                .append(baseCss)
                .append("\n")
                .append(template.css())
                .append("\n</style>\n</head>\n<body>\n")
                .append("<main class=\"card-deck\" data-template=\"")
                .append(escapeAttribute(template.ref()))
                .append("\" data-template-fingerprint=\"")
                .append(escapeAttribute(template.fingerprint()))
                .append("\">\n");

        List<CardPage> pages = deck.pages() == null ? List.of() : deck.pages();
        for (int i = 0; i < pages.size(); i++) {
            appendPage(html, pages.get(i), i + 1, pages.size(), deck.deckTitle());
        }

        html.append("</main>\n</body>\n</html>\n");

        return new RenderResult(
                width,
                height,
                pages.size(),
                PAGE_SELECTOR,
                html.toString());
    }

    private static void appendPage(
            StringBuilder html,
            CardPage page,
            int fallbackIndex,
            int totalPages,
            String deckTitle) {

        int pageIndex = page.index() == null ? fallbackIndex : page.index();
        String layout = page.layout() == null ? "headline-list" : page.layout();

        html.append("<section class=\"card-page\" data-page=\"")
                .append(pageIndex)
                .append("\" data-layout=\"")
                .append(escapeAttribute(layout))
                .append("\">\n")
                .append("<div class=\"card-inner\">\n")
                .append("<header class=\"card-meta\">\n")
                .append("<span class=\"deck-label\">")
                .append(escapeHtml(deckTitle == null ? "" : deckTitle))
                .append("</span>\n")
                .append("<span class=\"page-number\">")
                .append(String.format("%02d", pageIndex))
                .append(" / ")
                .append(String.format("%02d", totalPages))
                .append("</span>\n")
                .append("</header>\n");

        appendText(html, "eyebrow", page.eyebrow(), "div");
        appendText(html, "headline", page.headline(), "h1");
        appendText(html, "subheadline", page.subheadline(), "p");

        List<CardItem> items = page.items() == null ? List.of() : page.items();
        if (!items.isEmpty()) {
            html.append("<div class=\"card-items\">\n");
            for (CardItem item : items) {
                html.append("<article class=\"card-item\">\n");
                appendText(html, "item-number", item.number(), "div");
                appendText(html, "item-title", item.title(), "h2");
                appendText(html, "item-body", item.body(), "p");
                appendText(html, "item-emphasis", item.emphasis(), "div");
                html.append("</article>\n");
            }
            html.append("</div>\n");
        }

        appendText(html, "summary", page.summary(), "footer");
        html.append("</div>\n</section>\n");
    }

    private static void appendText(
            StringBuilder html,
            String cssClass,
            String value,
            String tag) {

        if (value == null || value.isBlank()) {
            return;
        }
        html.append("<").append(tag).append(" class=\"").append(cssClass).append("\">")
                .append(escapeHtml(value))
                .append("</").append(tag).append(">\n");
    }

    static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String escapeAttribute(String value) {
        return escapeHtml(value == null ? "" : value);
    }

    private static String loadDefaultCss() {
        String path = "/article-card/default.css";
        try (InputStream in = CardHtmlRenderer.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to load default card CSS", e);
        }
    }
}
