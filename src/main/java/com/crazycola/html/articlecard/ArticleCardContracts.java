package com.crazycola.html.articlecard;

import java.util.List;

public final class ArticleCardContracts {

    private ArticleCardContracts() {
    }

    public record ArticleCardRequest(
            String content,
            String title,
            Integer minPages,
            Integer maxPages,
            Integer width,
            Integer height,
            Boolean allowRewrite,
            String language,
            String templateId,
            String templateVersion,
            String templateFingerprint) {

        public ArticleCardRequest(
                String content,
                String title,
                Integer minPages,
                Integer maxPages,
                Integer width,
                Integer height,
                Boolean allowRewrite,
                String language) {
            this(content, title, minPages, maxPages, width, height, allowRewrite, language, null, null, null);
        }

        public ArticleCardRequest normalized() {
            if (content == null || content.isBlank()) {
                throw new IllegalArgumentException("content must not be blank");
            }

            int min = minPages == null ? 3 : minPages;
            int max = maxPages == null ? 8 : maxPages;
            int w = width == null ? 1080 : width;
            int h = height == null ? 1440 : height;
            boolean rewrite = allowRewrite == null || allowRewrite;
            String lang = language == null || language.isBlank() ? "zh-CN" : language.trim();

            if (min < 1 || min > 12) {
                throw new IllegalArgumentException("minPages must be between 1 and 12");
            }
            if (max < 1 || max > 12) {
                throw new IllegalArgumentException("maxPages must be between 1 and 12");
            }
            if (min > max) {
                throw new IllegalArgumentException("minPages must not exceed maxPages");
            }
            if (w < 320 || w > 4096 || h < 320 || h > 4096) {
                throw new IllegalArgumentException("width/height must be between 320 and 4096");
            }

            return new ArticleCardRequest(
                    content.trim(),
                    blankToNull(title),
                    min,
                    max,
                    w,
                    h,
                    rewrite,
                    lang,
                    blankToNull(templateId),
                    blankToNull(templateVersion),
                    blankToNull(templateFingerprint));
        }

        private static String blankToNull(String value) {
            return value == null || value.isBlank() ? null : value.trim();
        }
    }

    public record CardExecutionContext(
            String tenantId,
            String brandId,
            String projectId,
            String userId,
            String channel,
            String traceId) {

        public CardExecutionContext {
            tenantId = blankToNull(tenantId);
            brandId = blankToNull(brandId);
            projectId = blankToNull(projectId);
            userId = blankToNull(userId);
            channel = blankToNull(channel);
            traceId = blankToNull(traceId);
        }

        public static CardExecutionContext empty() {
            return new CardExecutionContext(null, null, null, null, null, null);
        }

        private static String blankToNull(String value) {
            return value == null || value.isBlank() ? null : value.trim();
        }
    }

    public record CardItem(
            String number,
            String title,
            String body,
            String emphasis) {
    }

    public record CardPage(
            Integer index,
            String layout,
            String eyebrow,
            String headline,
            String subheadline,
            List<CardItem> items,
            String summary) {

        public CardPage {
            items = List.copyOf(items == null ? List.of() : items);
        }
    }

    public record CardDeck(
            String deckTitle,
            List<CardPage> pages) {

        public CardDeck {
            pages = List.copyOf(pages == null ? List.of() : pages);
        }
    }

    public record TemplateSnapshot(
            String id,
            String version,
            String fingerprint,
            String displayName,
            String fingerprintAlgorithm) {

        public TemplateSnapshot(String id, String version, String fingerprint, String displayName) {
            this(id, version, fingerprint, displayName, null);
        }
    }

    public record RenderResult(
            int width,
            int height,
            int pageCount,
            String pageSelector,
            String html,
            String renderFingerprint) {

        public RenderResult(int width, int height, int pageCount, String pageSelector, String html) {
            this(width, height, pageCount, pageSelector, html, null);
        }
    }

    public record ArticleCardResult(
            String skill,
            String skillVersion,
            TemplateSnapshot template,
            CardDeck deck,
            RenderResult render,
            List<String> warnings) {

        public ArticleCardResult {
            warnings = List.copyOf(warnings == null ? List.of() : warnings);
        }

        public ArticleCardResult(
                String skill,
                String skillVersion,
                CardDeck deck,
                RenderResult render,
                List<String> warnings) {
            this(skill, skillVersion, null, deck, render, warnings);
        }
    }
}
