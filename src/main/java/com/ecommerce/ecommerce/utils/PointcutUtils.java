package com.ecommerce.ecommerce.utils;

import com.ecommerce.ecommerce.annotations.Auditable;
import org.aspectj.lang.annotation.Pointcut;

public class PointcutUtils {

    @Pointcut("@annotation(com.ecommerce.ecommerce.annotations.Auditable)")
    public void auditLog() {
    };
}

