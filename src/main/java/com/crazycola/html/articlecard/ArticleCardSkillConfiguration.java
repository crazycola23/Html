package com.crazycola.html.articlecard;

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
    CardHtmlRenderer cardHtmlRenderer() {
        return new CardHtmlRenderer();
    }

    @Bean
    ArticleCardSkill articleCardSkill(
            ArticleCardPlanner planner,
            CardDeckValidator validator,
            CardHtmlRenderer renderer) {
        return new ArticleCardSkill(planner, validator, renderer);
    }

    @Bean
    ArticleCardTools articleCardTools(ArticleCardSkill skill) {
        return new ArticleCardTools(skill);
    }
}
