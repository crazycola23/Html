package com.crazycola.html.articlecard;

import java.util.Collection;
import java.util.Map;

public interface CardTemplateProvider {

    Collection<CardTemplate> templates();

    default Map<String, String> defaultVersions() {
        return Map.of();
    }
}
