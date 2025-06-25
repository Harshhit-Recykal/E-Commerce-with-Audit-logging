package com.ecommerce.ecommerce.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
@Getter
@Setter
@Component
public class EntityMatching {
     private final Map<String, String> entityMappings = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        entityMappings.put("products", "Product");
        // Add more URL-EntityName mappings as needed
    }

     public void addEntity(String url , String entityName){
         entityMappings.put(url, entityName);
     }

     public String getEntityName(String url){
        return entityMappings.get(url);
     }
}
