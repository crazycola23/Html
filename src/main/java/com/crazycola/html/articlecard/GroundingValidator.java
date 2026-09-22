package com.crazycola.html.articlecard;

import com.crazycola.html.articlecard.ArticleCardContracts.CardDeck;
import com.crazycola.html.articlecard.ArticleCardContracts.CardItem;
import com.crazycola.html.articlecard.ArticleCardContracts.CardPage;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GroundingValidator {

    private static final Pattern NUMERIC_CLAIM = Pattern.compile(
            "(?:[$¥￥€£])?\\d+(?:[,.]\\d+)*(?:\\s*[%％])?");

    public void validate(CardDeck deck, String source) {
        Set<String> sourceNumbers = extract(source);
        Set<String> outputNumbers = new LinkedHashSet<>();

        add(outputNumbers, deck.deckTitle());
        for (CardPage page : deck.pages()) {
            add(outputNumbers, page.eyebrow());
            add(outputNumbers, page.headline());
            add(outputNumbers, page.subheadline());
            for (CardItem item : page.items()) {
                // item.number is presentation sequencing ("01", "02"), not a source claim.
                add(outputNumbers, item.title());
                add(outputNumbers, item.body());
                add(outputNumbers, item.emphasis());
            }
            add(outputNumbers, page.summary());
        }

        outputNumbers.removeAll(sourceNumbers);
        if (!outputNumbers.isEmpty()) {
            throw new IllegalArgumentException(
                    "planner introduced numeric claims not present in source: " + outputNumbers);
        }
    }

    private static void add(Set<String> values, String text) {
        values.addAll(extract(text));
    }

    private static Set<String> extract(String text) {
        Set<String> result = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        Matcher matcher = NUMERIC_CLAIM.matcher(text);
        while (matcher.find()) {
            result.add(normalize(matcher.group()));
        }
        return result;
    }

    private static String normalize(String value) {
        return value
                .replace(" ", "")
                .replace(",", "")
                .replace("％", "%");
    }
}
