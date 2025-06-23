package com.ecommerce.ecommerce.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CloneUtils {

    private CloneUtils() {
    }

    public static <T> T deepCopy(ObjectMapper mapper, Object source, Class<T> clazz) {
        if (source == null) return null;
        return mapper.convertValue(source, clazz);
    }

    public static Object deepCopy(ObjectMapper mapper, Object source) {
        if (source == null) return null;
        return mapper.convertValue(source, source.getClass());
    }
}
