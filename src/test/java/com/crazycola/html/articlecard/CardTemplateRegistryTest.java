package com.crazycola.html.articlecard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.crazycola.html.articlecard.CardTemplate.ContentRules;
import com.crazycola.html.articlecard.CardTemplate.LayoutRules;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CardTemplateRegistryTest {

    @Test
    void resolvesPinnedDefaultAndExactVersion() {
        CardTemplateRegistry registry = CardTemplateRegistry.builtInOnly();

        CardTemplate defaultTemplate = registry.resolve(null, null, null);
        CardTemplate explicit = registry.resolve("editorial-dark", "1.0.0", defaultTemplate.fingerprint());

        assertEquals("editorial-dark", defaultTemplate.id());
        assertEquals("1.0.0", defaultTemplate.version());
        assertEquals(defaultTemplate.fingerprint(), explicit.fingerprint());
        assertEquals(CardTemplate.FINGERPRINT_ALGORITHM, explicit.snapshot().fingerprintAlgorithm());
    }

    @Test
    void rejectsFingerprintDrift() {
        CardTemplateRegistry registry = CardTemplateRegistry.builtInOnly();

        assertThrows(
                IllegalArgumentException.class,
                () -> registry.resolve("warm-paper", "1.0.0", "deadbeef"));
    }

    @Test
    void rejectsVersionWithoutTemplateId() {
        CardTemplateRegistry registry = CardTemplateRegistry.builtInOnly();

        assertThrows(
                IllegalArgumentException.class,
                () -> registry.resolve(null, "1.0.0", null));
    }

    @Test
    void rejectsDuplicateProviderRef() {
        CardTemplate template = BuiltInCardTemplateProvider.editorialDark();

        assertThrows(
                IllegalArgumentException.class,
                () -> new CardTemplateRegistry(List.of(
                        () -> List.of(template),
                        () -> List.of(template))));
    }

    @Test
    void canonicalFingerprintIgnoresMapAndTagInsertionOrder() {
        LinkedHashMap<String, String> firstTokens = new LinkedHashMap<>();
        firstTokens.put("bg", "#ffffff");
        firstTokens.put("text", "#111111");

        LinkedHashMap<String, String> secondTokens = new LinkedHashMap<>();
        secondTokens.put("text", "#111111");
        secondTokens.put("bg", "#ffffff");

        CardTemplate first = template("A", "description", List.of("business", "editorial"), firstTokens, ".card-page{color:#111111}\r\n");
        CardTemplate second = template("A", "description", List.of("editorial", "business"), secondTokens, ".card-page{color:#111111}\n");

        assertEquals(first.fingerprint(), second.fingerprint());
    }

    @Test
    void canonicalFingerprintDoesNotHaveDelimiterBoundaryCollisions() {
        CardTemplate first = template("A\nB", "C", List.of("a"), Map.of("text", "#111111"), "");
        CardTemplate second = template("A", "B\nC", List.of("a"), Map.of("text", "#111111"), "");

        assertNotEquals(first.fingerprint(), second.fingerprint());
    }

    @Test
    void rejectsMovingAliasAndNetworkToken() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CardTemplate(
                        "brand",
                        "latest",
                        "Brand",
                        "",
                        List.of(),
                        rules(),
                        layouts(),
                        Map.of("text", "#111111"),
                        "",
                        ""));

        assertThrows(
                IllegalArgumentException.class,
                () -> template(
                        "Brand",
                        "",
                        List.of(),
                        Map.of("bg", "url(https://example.invalid/a.png)"),
                        ""));
    }

    private CardTemplate template(
            String displayName,
            String description,
            List<String> tags,
            Map<String, String> tokens,
            String css) {
        return new CardTemplate(
                "test-template",
                "1.0.0",
                displayName,
                description,
                tags,
                rules(),
                layouts(),
                tokens,
                css,
                "guidance");
    }

    private ContentRules rules() {
        return new ContentRules("neutral", "balanced", "concise", 32, 18, 52);
    }

    private LayoutRules layouts() {
        return new LayoutRules(Set.of("headline-list", "statement"), List.of("headline-list"), 6);
    }
}
