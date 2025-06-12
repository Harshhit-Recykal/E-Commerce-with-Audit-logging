package com.ecommerce.ecommerce.aspect;

import com.ecommerce.ecommerce.constants.ConfigConstants;
import com.ecommerce.ecommerce.dto.AuditEvent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
public class AuditLogging {

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public AuditLogging(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Around("com.ecommerce.ecommerce.utils.PointcutUtils.auditLog()")
    public Object logAudit(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result;

        try {
            result = joinPoint.proceed();
        }
        catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();
        String className = method.getDeclaringClass().getName();

        AuditEvent event = AuditEvent.builder()
                .entityName(className)
                .action(methodName)
                .timestamp(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(ConfigConstants.TOPIC_NAME, ConfigConstants.ROUTING_KEY, event);

        return result;
    }
}
