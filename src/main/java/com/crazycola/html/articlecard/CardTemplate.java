package com.crazycola.html.articlecard;

import com.crazycola.html.articlecard.ArticleCardContracts.TemplateSnapshot;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

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

    public static final String FINGERPRINT_ALGORITHM = "card-template-canonical-v1";

    private static final Pattern ID_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]*$");
    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)"
                    + "(?:-[0-9A-Za-z.-]+)?(?:\\+[0-9A-Za-z.-]+)?$");
    private static final Set<String> FORBIDDEN_VERSION_ALIASES = Set.of(
            "latest", "current", "default", "draft", "head");

    public CardTemplate {
        id = requireText(id, "id");
        version = requireText(version, "version");
        displayName = requireText(displayName, "displayName");
        description = description == null ? "" : description.trim();
        tags = normalizeTags(tags);
        content = Objects.requireNonNull(content, "content");
        layouts = Objects.requireNonNull(layouts, "layouts");
        tokens = Map.copyOf(tokens == null ? Map.of() : tokens);
        css = normalizeLineEndings(css == null ? "" : css);
        plannerGuidance = plannerGuidance == null ? "" : plannerGuidance.trim();

        if (!ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("template id must match " + ID_PATTERN.pattern() + ": " + id);
        }
        if (!VERSION_PATTERN.matcher(version).matches()
                || FORBIDDEN_VERSION_ALIASES.contains(version.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("template version must be an immutable semantic version: " + version);
        }

        for (Map.Entry<String, String> entry : tokens.entrySet()) {
            if (!entry.getKey().matches("[a-z0-9-]+")) {
                throw new IllegalArgumentException("invalid template token name: " + entry.getKey());
            }
            String value = Objects.requireNonNull(entry.getValue(), "template token value");
            assertSafeCssFragment(value, "template token " + entry.getKey(), false);
        }

        assertSafeCssFragment(css, "template css", true);
    }

    public String ref() {
        return id + "@" + version;
    }

    public String fingerprint() {
        return sha256(canonicalDefinition());
    }

    public TemplateSnapshot snapshot() {
        return new TemplateSnapshot(id, version, fingerprint(), displayName, FINGERPRINT_ALGORITHM);
    }

    public String plannerStyleContract() {
        return """
                TEMPLATE STYLE CONTRACT (mandatory):
                - templateRef: %s
                - templateFingerprint: %s
                - fingerprintAlgorithm: %s
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
                FINGERPRINT_ALGORITHM,
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

    private String canonicalDefinition() {
        StringBuilder out = new StringBuilder(4096);
        appendCanonical(out, "algorithm", FINGERPRINT_ALGORITHM);
        appendCanonical(out, "id", id);
        appendCanonical(out, "version", version);
        appendCanonical(out, "displayName", displayName);
        appendCanonical(out, "description", description);

        List<String> canonicalTags = new ArrayList<>(tags);
        canonicalTags.sort(String::compareTo);
        appendCanonical(out, "tags.count", Integer.toString(canonicalTags.size()));
        for (int i = 0; i < canonicalTags.size(); i++) {
            appendCanonical(out, "tags." + i, canonicalTags.get(i));
        }

        appendCanonical(out, "content.tone", content.tone());
        appendCanonical(out, "content.density", content.density());
        appendCanonical(out, "content.headlineStyle", content.headlineStyle());
        appendCanonical(out, "content.headlineMaxChars", Integer.toString(content.headlineMaxChars()));
        appendCanonical(out, "content.itemTitleMaxChars", Integer.toString(content.itemTitleMaxChars()));
        appendCanonical(out, "content.itemBodyMaxChars", Integer.toString(content.itemBodyMaxChars()));

        List<String> allowed = new ArrayList<>(layouts.allowedLayouts());
        allowed.sort(String::compareTo);
        appendCanonical(out, "layouts.allowed.count", Integer.toString(allowed.size()));
        for (int i = 0; i < allowed.size(); i++) {
            appendCanonical(out, "layouts.allowed." + i, allowed.get(i));
        }

        appendCanonical(out, "layouts.preferred.count", Integer.toString(layouts.preferredLayouts().size()));
        for (int i = 0; i < layouts.preferredLayouts().size(); i++) {
            appendCanonical(out, "layouts.preferred." + i, layouts.preferredLayouts().get(i));
        }
        appendCanonical(out, "layouts.maxItemsPerPage", Integer.toString(layouts.maxItemsPerPage()));

        TreeMap<String, String> canonicalTokens = new TreeMap<>(tokens);
        appendCanonical(out, "tokens.count", Integer.toString(canonicalTokens.size()));
        for (Map.Entry<String, String> entry : canonicalTokens.entrySet()) {
            appendCanonical(out, "token." + entry.getKey(), entry.getValue());
        }

        appendCanonical(out, "css", css);
        appendCanonical(out, "plannerGuidance", plannerGuidance);
        return out.toString();
    }

    private static void appendCanonical(StringBuilder out, String field, String value) {
        String canonicalField = normalizeCanonical(field);
        String canonicalValue = normalizeCanonical(value == null ? "" : value);
        out.append(canonicalField.length()).append(':').append(canonicalField)
                .append(canonicalValue.length()).append(':').append(canonicalValue).append('\n');
    }

    private static String normalizeCanonical(String value) {
        return Normalizer.normalize(normalizeLineEndings(value), Normalizer.Form.NFC);
    }

    private static String normalizeLineEndings(String value) {
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static List<String> normalizeTags(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>(values.size());
        Set<String> seen = new TreeSet<>();
        for (String tag : values) {
            String normalized = requireText(tag, "tag");
            if (!seen.add(normalized)) {
                throw new IllegalArgumentException("duplicate template tag: " + normalized);
            }
            result.add(normalized);
        }
        return List.copyOf(result);
    }

    private static void assertSafeCssFragment(String value, String field, boolean allowDeclarations) {
        String normalized = normalizeCanonical(value);
        String lower = normalized.toLowerCase(Locale.ROOT);

        if (normalized.indexOf('\0') >= 0 || normalized.indexOf('\\') >= 0
                || lower.contains("</style") || lower.contains("url(")
                || lower.contains("expression(") || lower.contains("image-set(")
                || lower.contains("src(") || lower.contains("http:")
                || lower.contains("https:") || lower.contains("file:")
                || lower.contains("javascript:") || lower.contains("data:")
                || lower.contains("@")) {
            throw new IllegalArgumentException(field + " contains a forbidden CSS/network construct");
        }

        if (!allowDeclarations && (normalized.contains(";") || normalized.contains("{") || normalized.contains("}"))) {
            throw new IllegalArgumentException(field + " must be a single CSS token value");
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
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
