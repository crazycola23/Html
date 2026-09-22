package com.crazycola.html.articlecard;

import java.util.List;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ArticleCardSkillConfiguration {

    @Bean
    ArticleCardPlanner articleCardPlanner(ChatModel chatModel) {
        return new ArticleCardPlanner(chatModel);
    }

    @Bean
    CardDeckValidator cardDeckValidator() {
        return new CardDeckValidator();
    }

    @Bean
    GroundingValidator groundingValidator() {
        return new GroundingValidator();
    }

    @Bean
    CardHtmlRenderer cardHtmlRenderer() {
        return new CardHtmlRenderer();
    }

    @Bean
    BuiltInCardTemplateProvider builtInCardTemplateProvider() {
        return new BuiltInCardTemplateProvider();
    }

    @Bean
    CardTemplateRegistry cardTemplateRegistry(List<CardTemplateProvider> providers) {
        return new CardTemplateRegistry(providers);
    }

    @Bean
    CardTemplateSelector cardTemplateSelector(CardTemplateRegistry registry) {
        return CardTemplateSelector.deterministic(registry);
    }

    @Bean
    ArticleCardSkill articleCardSkill(
            ArticleCardPlanner planner,
            CardDeckValidator validator,
            GroundingValidator groundingValidator,
            CardHtmlRenderer renderer,
            CardTemplateSelector templateSelector) {
        return new ArticleCardSkill(planner, validator, groundingValidator, renderer, templateSelector);
    }

    @Bean
    ArticleCardTools articleCardTools(ArticleCardSkill skill) {
        return new ArticleCardTools(skill);
    }
}
