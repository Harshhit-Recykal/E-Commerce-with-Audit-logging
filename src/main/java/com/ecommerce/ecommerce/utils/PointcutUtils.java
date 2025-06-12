package com.ecommerce.ecommerce.utils;

import org.aspectj.lang.annotation.Pointcut;

public class PointcutUtils {

    @Pointcut("@annotation(com.ecommerce.ecommerce.annotations.Auditable)")
    public void auditLog() {
    };
}

