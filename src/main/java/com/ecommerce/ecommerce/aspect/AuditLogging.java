package com.ecommerce.ecommerce.aspect;

import com.ecommerce.ecommerce.constants.ConfigConstants;
import com.ecommerce.ecommerce.dto.AuditEvent;
import com.ecommerce.ecommerce.enums.ActionType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;

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
        Object result = null;

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        String entityId = extractEntityId(joinPoint.getArgs());
        String entityName = extractEntityName(joinPoint.getArgs());

        try {
            result = joinPoint.proceed();
        } catch (Throwable ignored) {
        }

        Enum<ActionType> actionType = determineAction(method, entityId);
        if (!Objects.equals(actionType, ActionType.UNKNOWN)) {
            entityId = extractEntityId(joinPoint.getArgs());

            AuditEvent event = AuditEvent.builder()
                    .entityName(entityName)
                    .entityId(entityId)
                    .action(actionType.name())
                    .timestamp(LocalDateTime.now())
                    .rawDataAfter(result)
                    .requestId(UUID.randomUUID().toString())
                    .changedBy("USER")
                    .build();

            rabbitTemplate.convertAndSend(ConfigConstants.TOPIC_NAME, ConfigConstants.ROUTING_KEY, event);
        }

        return result;
    }

    private Enum<ActionType> determineAction(Method method, String entityId) {

        if (method.isAnnotationPresent(DeleteMapping.class)) {
            return ActionType.DELETE;
        }
        if (method.isAnnotationPresent(PutMapping.class)) {
            return ActionType.UPDATE;
        }
        if (method.isAnnotationPresent(PostMapping.class)) {
            boolean hasEntityId = !Objects.isNull(entityId);

            return hasEntityId ? ActionType.UPDATE : ActionType.CREATE;
        }

        return ActionType.UNKNOWN;
    }

    private String extractEntityId(Object[] args) {
        for (Object arg : args) {
            try {
                Method method = arg.getClass().getMethod("getId");
                Object id = method.invoke(arg);
                if (id != null) {
                    return String.valueOf(id);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String extractEntityName(Object[] args) {
        for (Object arg : args) {
            if (arg != null && arg.getClass().getPackageName().contains("entity")) {
                return arg.getClass().getSimpleName();
            }
        }
        return null;
    }

}
