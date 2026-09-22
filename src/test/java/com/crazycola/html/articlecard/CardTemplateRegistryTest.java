package com.crazycola.html.articlecard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    }

    @Test
    void rejectsFingerprintDrift() {
        CardTemplateRegistry registry = CardTemplateRegistry.builtInOnly();

        assertThrows(
                IllegalArgumentException.class,
                () -> registry.resolve("warm-paper", "1.0.0", "deadbeef"));
    }
}
