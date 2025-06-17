package com.ecommerce.ecommerce.utils;

import com.ecommerce.ecommerce.annotations.Auditable;
import org.aspectj.lang.annotation.Pointcut;

public class PointcutUtils {

    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void auditLog() {
    };
}

