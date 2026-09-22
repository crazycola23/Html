package com.crazycola.html.articlecard;

import com.crazycola.html.articlecard.ArticleCardContracts.TemplateSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CardTemplateRegistry {

    private final Map<String, CardTemplate> byRef;
    private final Map<String, String> defaultVersions;

    public CardTemplateRegistry(List<CardTemplateProvider> providers) {
        Map<String, CardTemplate> templates = new LinkedHashMap<>();
        Map<String, String> defaults = new LinkedHashMap<>();

        for (CardTemplateProvider provider : providers == null ? List.<CardTemplateProvider>of() : providers) {
            Objects.requireNonNull(provider, "template provider");
            if (provider.templates() != null) {
                for (CardTemplate template : provider.templates()) {
                    Objects.requireNonNull(template, "template provider returned null template");
                    String ref = template.ref();
                    CardTemplate existing = templates.putIfAbsent(ref, template);
                    if (existing != null) {
                        throw new IllegalArgumentException("duplicate template ref: " + ref);
                    }
                }
            }

            Map<String, String> providerDefaults = provider.defaultVersions();
            if (providerDefaults != null) {
                for (Map.Entry<String, String> entry : providerDefaults.entrySet()) {
                    String existing = defaults.putIfAbsent(entry.getKey(), entry.getValue());
                    if (existing != null && !existing.equals(entry.getValue())) {
                        throw new IllegalArgumentException(
                                "conflicting default template versions for " + entry.getKey());
                    }
                }
            }
        }

        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            if (!templates.containsKey(ref(entry.getKey(), entry.getValue()))) {
                throw new IllegalArgumentException(
                        "default version points to unknown template: " + ref(entry.getKey(), entry.getValue()));
            }
        }

        this.byRef = Map.copyOf(templates);
        this.defaultVersions = Map.copyOf(defaults);
    }

    public static CardTemplateRegistry builtInOnly() {
        return new CardTemplateRegistry(List.of(new BuiltInCardTemplateProvider()));
    }

    public CardTemplate resolve(String templateId, String templateVersion, String expectedFingerprint) {
        boolean hasId = templateId != null && !templateId.isBlank();
        boolean hasVersion = templateVersion != null && !templateVersion.isBlank();

        if (!hasId && hasVersion) {
            throw new IllegalArgumentException("templateId is required when templateVersion is supplied");
        }

        String id = hasId
                ? templateId.trim()
                : BuiltInCardTemplateProvider.DEFAULT_TEMPLATE_ID;

        String version;
        if (!hasVersion) {
            version = defaultVersions.get(id);
            if (version == null) {
                throw new IllegalArgumentException(
                        "templateVersion is required because template has no pinned default: " + id);
            }
        }
        else {
            version = templateVersion.trim();
        }

        CardTemplate template = byRef.get(ref(id, version));
        if (template == null) {
            throw new IllegalArgumentException("unknown template: " + ref(id, version));
        }

        if (expectedFingerprint != null && !expectedFingerprint.isBlank()
                && !template.fingerprint().equalsIgnoreCase(expectedFingerprint.trim())) {
            throw new IllegalArgumentException(
                    "template fingerprint mismatch for " + template.ref()
                            + "; requested " + expectedFingerprint.trim()
                            + " but resolved " + template.fingerprint());
        }
        return template;
    }

    public List<TemplateSnapshot> catalog() {
        List<TemplateSnapshot> result = new ArrayList<>();
        for (CardTemplate template : byRef.values()) {
            result.add(template.snapshot());
        }
        result.sort(Comparator.comparing(TemplateSnapshot::id).thenComparing(TemplateSnapshot::version));
        return List.copyOf(result);
    }

    private static String ref(String id, String version) {
        return id + "@" + version;
    }
}
