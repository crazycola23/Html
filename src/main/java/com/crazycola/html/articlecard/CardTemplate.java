package com.crazycola.html.articlecard;

import com.crazycola.html.articlecard.ArticleCardContracts.TemplateSnapshot;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public record CardTemplate(
        String id,
        String version,
        String displayName,
        String description,
        List<String> tags,
        ContentRules content,
        LayoutRules layouts,
        Map<String, String> tokens,
        String css,
        String plannerGuidance) {

    public CardTemplate {
        id = requireText(id, "id");
        version = requireText(version, "version");
        displayName = requireText(displayName, "displayName");
        description = description == null ? "" : description.trim();
        tags = List.copyOf(tags == null ? List.of() : tags);
        content = Objects.requireNonNull(content, "content");
        layouts = Objects.requireNonNull(layouts, "layouts");
        tokens = Map.copyOf(tokens == null ? Map.of() : tokens);
        css = css == null ? "" : css;
        plannerGuidance = plannerGuidance == null ? "" : plannerGuidance.trim();

        for (Map.Entry<String, String> entry : tokens.entrySet()) {
            if (!entry.getKey().matches("[a-z0-9-]+")) {
                throw new IllegalArgumentException("invalid template token name: " + entry.getKey());
            }
            String value = Objects.requireNonNull(entry.getValue(), "template token value");
            if (value.contains(";") || value.contains("{") || value.contains("}")
                    || value.toLowerCase(Locale.ROOT).contains("</style")) {
                throw new IllegalArgumentException("unsafe template token value for " + entry.getKey());
            }
        }

        String lowerCss = css.toLowerCase(Locale.ROOT);
        if (lowerCss.contains("</style") || lowerCss.contains("@import") || lowerCss.contains("url(")) {
            throw new IllegalArgumentException("template css must be self-contained and must not load external resources");
        }
    }

    public String ref() {
        return id + "@" + version;
    }

    public String fingerprint() {
        String canonical = String.join("\n",
                id,
                version,
                displayName,
                description,
                String.join(",", tags),
                content.toString(),
                new TreeSet<>(layouts.allowedLayouts()).toString(),
                layouts.preferredLayouts().toString(),
                Integer.toString(layouts.maxItemsPerPage()),
                new TreeMap<>(tokens).toString(),
                css,
                plannerGuidance);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    public TemplateSnapshot snapshot() {
        return new TemplateSnapshot(id, version, fingerprint(), displayName);
    }

    public String plannerStyleContract() {
        return """
                TEMPLATE STYLE CONTRACT (mandatory):
                - templateRef: %s
                - templateFingerprint: %s
                - tone: %s
                - density: %s
                - headlineStyle: %s
                - headlineMaxChars: %d
                - itemTitleMaxChars: %d
                - itemBodyMaxChars: %d
                - maxItemsPerPage: %d
                - allowedLayouts: %s
                - preferredLayouts: %s
                - templateGuidance: %s

                The template contract is part of the output contract. Keep the writing cadence,
                information density, hierarchy, and layout choices consistent with it. Do not
                invent a new visual or editorial style.
                """.formatted(
                ref(),
                fingerprint(),
                content.tone(),
                content.density(),
                content.headlineStyle(),
                content.headlineMaxChars(),
                content.itemTitleMaxChars(),
                content.itemBodyMaxChars(),
                layouts.maxItemsPerPage(),
                new TreeSet<>(layouts.allowedLayouts()),
                layouts.preferredLayouts(),
                plannerGuidance.isBlank() ? "(none)" : plannerGuidance);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    public record ContentRules(
            String tone,
            String density,
            String headlineStyle,
            int headlineMaxChars,
            int itemTitleMaxChars,
            int itemBodyMaxChars) {

        public ContentRules {
            tone = requireText(tone, "content.tone");
            density = requireText(density, "content.density");
            headlineStyle = requireText(headlineStyle, "content.headlineStyle");
            if (headlineMaxChars < 8 || itemTitleMaxChars < 4 || itemBodyMaxChars < 8) {
                throw new IllegalArgumentException("template content length limits are too small");
            }
        }
    }

    public record LayoutRules(
            Set<String> allowedLayouts,
            List<String> preferredLayouts,
            int maxItemsPerPage) {

        public LayoutRules {
            allowedLayouts = Set.copyOf(allowedLayouts == null ? Set.of() : allowedLayouts);
            preferredLayouts = List.copyOf(preferredLayouts == null ? List.of() : preferredLayouts);
            if (allowedLayouts.isEmpty()) {
                throw new IllegalArgumentException("layouts.allowedLayouts must not be empty");
            }
            if (!allowedLayouts.containsAll(preferredLayouts)) {
                throw new IllegalArgumentException("preferredLayouts must be a subset of allowedLayouts");
            }
            if (maxItemsPerPage < 0 || maxItemsPerPage > 6) {
                throw new IllegalArgumentException("maxItemsPerPage must be between 0 and 6");
            }
        }
    }
}
