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

import java.lang.reflect.Field;
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
        String methodName = methodSignature.getMethod().getName().toLowerCase();

        try {
            result = joinPoint.proceed();
        } catch (Throwable ignored) {
        }

        Enum<ActionType> actionType = isCrudMethod(methodName);
        if (!Objects.equals(actionType, ActionType.UNKNOWN)) {

            String entityId = extractEntityId(joinPoint.getArgs());
            String entityName = extractEntityName(joinPoint.getArgs());

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

    private Enum<ActionType> isCrudMethod(String methodName) {

        if(methodName.contains("insert") || methodName.contains("create") ) {
            return ActionType.CREATE;
        }
        if(methodName.contains("update")) {
            return ActionType.UPDATE;
        }
        if(methodName.contains("delete") || methodName.contains("remove")) {
            return ActionType.DELETE;
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
        return "UNKNOWN";
    }

    private String extractEntityName(Object[] args) {
        for (Object arg : args) {
            if (arg != null && arg.getClass().getPackageName().contains("entity")) {
                return arg.getClass().getSimpleName();
            }
        }
        return "UNKNOWN";
    }


    private Map<String, List<Object>> calculateFieldDiffs(Object oldObj, Object newObj) {
        Map<String, List<Object>> diffs = new HashMap<>();
        if (oldObj == null || newObj == null) return diffs;

        for (Field field : oldObj.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object oldVal = field.get(oldObj);
                Object newVal = field.get(newObj);
                if (!Objects.equals(oldVal, newVal)) {
                    diffs.put(field.getName(), List.of(oldVal, newVal));
                }
            } catch (IllegalAccessException ignored) {
            }
        }

        return diffs;
    }

}
