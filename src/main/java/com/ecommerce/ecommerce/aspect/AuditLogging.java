package com.ecommerce.ecommerce.aspect;

import com.ecommerce.ecommerce.annotations.Auditable;
import com.ecommerce.ecommerce.constants.ConfigConstants;
import com.ecommerce.ecommerce.dto.ApiResponse;
import com.ecommerce.ecommerce.dto.AuditEvent;
import com.ecommerce.ecommerce.enums.ActionType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    @Around("com.ecommerce.ecommerce.utils.PointcutUtils.logAroundBasedOnRequestMapping()")
    public Object logAudit(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = null;

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        String entityId = extractEntityId(joinPoint.getArgs());
        Enum<ActionType> actionType = determineAction(method, entityId);

        try {
            result = joinPoint.proceed();
        } catch (Throwable ignored) {
        }

        if (!Objects.equals(actionType, ActionType.UNKNOWN)) {

            LocalDateTime timeStamp = LocalDateTime.now();
            Object response = extractResponseBody(result);
            String entityName = extractEntityName(joinPoint.getArgs());
            entityId = extractEntityId( new Object[] {response});

            if(actionType == ActionType.CREATE) {
                Method getCreatedTime = response.getClass().getMethod("getCreatedAt");
                timeStamp = (LocalDateTime) getCreatedTime.invoke(response);
            }
            else if(actionType == ActionType.UPDATE) {
                Method getUpdatedTime = response.getClass().getMethod("getUpdatedAt");
                timeStamp = (LocalDateTime) getUpdatedTime.invoke(response);
            }

            AuditEvent event = AuditEvent.builder()
                    .entityName(entityName)
                    .entityId(entityId)
                    .action(actionType.name())
                    .timestamp(timeStamp)
                    .rawDataAfter(response)
                    .requestId(UUID.randomUUID().toString())
                    .changedBy("USER")
                    .build();

            rabbitTemplate.convertAndSend(ConfigConstants.TOPIC_NAME, ConfigConstants.ROUTING_KEY, event);
        }

        return result;
    }

    private Enum<ActionType> determineAction(Method method, String entityId) {

        if (method.isAnnotationPresent(DeleteMapping.class) || method.getName().toUpperCase().contains(ActionType.DELETE.name())) {
            return ActionType.DELETE;
        }
        if (method.isAnnotationPresent(PutMapping.class) || method.getName().toUpperCase().contains(ActionType.UPDATE.name())) {
            return ActionType.UPDATE;
        }
        if (method.isAnnotationPresent(PostMapping.class) || method.getName().toUpperCase().contains(ActionType.CREATE.name())) {
            boolean hasEntityId = !Objects.isNull(entityId);

            return hasEntityId ? ActionType.UPDATE : ActionType.CREATE;
        }
        if (method.isAnnotationPresent(Auditable.class)) {
            Auditable auditable = method.getAnnotation(Auditable.class);
            return auditable.actionType();
        }

        return ActionType.UNKNOWN;
    }

    private String extractEntityId(Object[] args) {

        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            if (arg instanceof Long || arg instanceof String) {
                return String.valueOf(arg);
            }
            try {
                Method method = arg.getClass().getMethod("getId");
                Object id = method.invoke(arg);
                if (id != null) {
                    return String.valueOf(id);
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }


    private String extractEntityName(Object[] args) {
        for (Object arg : args) {
            if (arg == null) continue;

            Class<?> clazz = arg.getClass();
            String className = clazz.getSimpleName();

            String packageName = clazz.getPackageName();

            if (packageName.startsWith("java.") || packageName.startsWith("javax.") || packageName.startsWith("org.springframework")) {
                continue;
            }

            return className.replaceAll("(?i)(Dto)$", "");
        }
        return "UNKNOWN";
    }

    private Object extractResponseBody(Object result) {
        if (result instanceof ResponseEntity) {
            Object response = ((ResponseEntity<?>) result).getBody();
            if (response instanceof ApiResponse<?>) {
                return ((ApiResponse<?>) response).getData();
            }
            return response;
        }
        return result;
    }

}
