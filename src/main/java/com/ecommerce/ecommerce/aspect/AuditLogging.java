package com.ecommerce.ecommerce.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditLogging {

    @Around("com.ecommerce.ecommerce.utils.PointcutUtils.auditLog()")
    public Object logAudit(ProceedingJoinPoint joinPoint) throws Throwable {
        return null;
    }
}
