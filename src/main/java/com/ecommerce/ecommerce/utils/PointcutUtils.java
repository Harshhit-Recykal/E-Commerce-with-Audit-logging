package com.ecommerce.ecommerce.utils;

import org.aspectj.lang.annotation.Pointcut;

public class PointcutUtils {

    @Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void logAroundBasedOnRequestMapping() {
    };

    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void logAroundBasedOnServiceClass() {

    }

    @Pointcut("@annotation(com.ecommerce.ecommerce.annotations.Auditable)")
    public void logAroundBasedOnAuditableAnnotation() {

    }
}

