package com.ecommerce.ecommerce.config;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

    @Getter
    @Setter
    @Component
    @ConfigurationProperties(prefix = "entity")
    public class EntityProperties {
        private Map<String, String> mappings;
    }
