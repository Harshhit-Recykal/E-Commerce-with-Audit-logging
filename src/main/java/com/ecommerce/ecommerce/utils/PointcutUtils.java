package com.ecommerce.ecommerce.utils;

import com.ecommerce.ecommerce.annotations.Auditable;
import org.aspectj.lang.annotation.Pointcut;

public class PointcutUtils {

    @Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void auditLog() {
    };
}

