package com.crazycola.html.articlecard;

import com.crazycola.html.articlecard.CardTemplate.ContentRules;
import com.crazycola.html.articlecard.CardTemplate.LayoutRules;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BuiltInCardTemplateProvider implements CardTemplateProvider {

    public static final String DEFAULT_TEMPLATE_ID = "editorial-dark";
    public static final String DEFAULT_TEMPLATE_VERSION = "1.0.0";

    private static final Set<String> ALL_LAYOUTS = Set.of(
            "headline-list",
            "numbered-grid",
            "statement",
            "steps",
            "comparison");

    private final List<CardTemplate> templates = List.of(
            editorialDark(),
            warmPaper(),
            neoGrid());

    @Override
    public Collection<CardTemplate> templates() {
        return templates;
    }

    @Override
    public Map<String, String> defaultVersions() {
        return Map.of(
                "editorial-dark", "1.0.0",
                "warm-paper", "1.0.0",
                "neo-grid", "1.0.0");
    }

    public static CardTemplate editorialDark() {
        return new CardTemplate(
                "editorial-dark",
                "1.0.0",
                "Editorial Dark",
                "深色杂志编辑风：克制、权威、强层级，适合案例拆解、商业观点与知识卡片。",
                List.of("business", "editorial", "premium", "dark"),
                new ContentRules("克制、权威、杂志化，不喊口号", "balanced", "短句判断式标题", 32, 18, 52),
                new LayoutRules(ALL_LAYOUTS,
                        List.of("headline-list", "numbered-grid", "statement", "comparison"), 6),
                Map.ofEntries(
                        Map.entry("bg", "#0c1726"),
                        Map.entry("surface", "rgba(255,255,255,.075)"),
                        Map.entry("text", "#f5efe3"),
                        Map.entry("muted", "#a9b3c2"),
                        Map.entry("accent", "#f0b35a"),
                        Map.entry("border", "rgba(245,239,227,.18)"),
                        Map.entry("radius", "26px"),
                        Map.entry("page-padding-x", "76px"),
                        Map.entry("page-padding-y", "76px"),
                        Map.entry("headline-size", "76px"),
                        Map.entry("item-title-size", "34px"),
                        Map.entry("body-size", "25px"),
                        Map.entry("font-family", "ui-sans-serif,system-ui,-apple-system,BlinkMacSystemFont,Segoe UI,PingFang SC,Microsoft YaHei,sans-serif")),
                loadCss("/article-card/templates/editorial-dark.css"),
                "标题像杂志封面与咨询报告的结论句；少用感叹号。强调关键经营矛盾、机制与结论。页面之间要有明显叙事推进，不堆同义信息。");
    }

    public static CardTemplate warmPaper() {
        return new CardTemplate(
                "warm-paper",
                "1.0.0",
                "Warm Paper",
                "暖纸张咨询风：温和、理性、留白充足，适合品牌故事、方法论与人文商业内容。",
                List.of("story", "consulting", "warm", "paper"),
                new ContentRules("温和、理性、可信，有人文感但不过度抒情", "airy", "陈述式或轻问句标题", 30, 18, 46),
                new LayoutRules(ALL_LAYOUTS,
                        List.of("statement", "headline-list", "steps", "comparison"), 5),
                Map.ofEntries(
                        Map.entry("bg", "#f3eee5"),
                        Map.entry("surface", "#fffaf2"),
                        Map.entry("text", "#24221f"),
                        Map.entry("muted", "#766f66"),
                        Map.entry("accent", "#b86142"),
                        Map.entry("border", "rgba(36,34,31,.16)"),
                        Map.entry("radius", "22px"),
                        Map.entry("page-padding-x", "82px"),
                        Map.entry("page-padding-y", "82px"),
                        Map.entry("headline-size", "70px"),
                        Map.entry("item-title-size", "32px"),
                        Map.entry("body-size", "24px"),
                        Map.entry("font-family", "ui-serif,Georgia,STSong,Songti SC,serif")),
                loadCss("/article-card/templates/warm-paper.css"),
                "强调留白与阅读节奏。避免过密列表；优先使用一句主判断加少量支撑点。语言可温暖，但必须保持事实边界与专业感。");
    }

    public static CardTemplate neoGrid() {
        return new CardTemplate(
                "neo-grid",
                "1.0.0",
                "Neo Grid",
                "现代网格信息风：高对比、直接、数据导向，适合清单、流程、对比与操作型内容。",
                List.of("data", "modern", "grid", "sharp"),
                new ContentRules("直接、锐利、现代，偏数据与操作语言", "dense", "高信息量结论式标题", 28, 16, 40),
                new LayoutRules(ALL_LAYOUTS,
                        List.of("numbered-grid", "steps", "comparison", "headline-list"), 6),
                Map.ofEntries(
                        Map.entry("bg", "#f4f7f0"),
                        Map.entry("surface", "#ffffff"),
                        Map.entry("text", "#111411"),
                        Map.entry("muted", "#596057"),
                        Map.entry("accent", "#a4ef55"),
                        Map.entry("border", "#161a16"),
                        Map.entry("radius", "8px"),
                        Map.entry("page-padding-x", "68px"),
                        Map.entry("page-padding-y", "68px"),
                        Map.entry("headline-size", "72px"),
                        Map.entry("item-title-size", "32px"),
                        Map.entry("body-size", "23px"),
                        Map.entry("font-family", "ui-sans-serif,system-ui,-apple-system,BlinkMacSystemFont,Segoe UI,PingFang SC,Microsoft YaHei,sans-serif")),
                loadCss("/article-card/templates/neo-grid.css"),
                "信息组织要像清晰的操作面板：编号、步骤、对比关系优先。避免抒情修辞，尽量让每个信息块可独立扫描。");
    }

    private static String loadCss(String path) {
        try (InputStream in = BuiltInCardTemplateProvider.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to load template css " + path, e);
        }
    }
}
