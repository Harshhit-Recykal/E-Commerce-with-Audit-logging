package com.ecommerce.ecommerce.aspect;

import com.ecommerce.ecommerce.annotations.Auditable;
import com.ecommerce.ecommerce.constants.ConfigConstants;
import com.ecommerce.ecommerce.dto.ApiResponse;
import com.ecommerce.ecommerce.dto.AuditEvent;
import com.ecommerce.ecommerce.enums.ActionType;
import com.ecommerce.ecommerce.utils.CloneUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.metamodel.EntityType;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;

@Aspect
@Component
public class AuditLogging {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogging.class);

    private final RabbitTemplate rabbitTemplate;

    private final EntityManager entityManager;

    private final ObjectMapper objectMapper;

    @Autowired
    public AuditLogging(RabbitTemplate rabbitTemplate, EntityManager entityManager, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
    }

    @Around("com.ecommerce.ecommerce.utils.PointcutUtils.logAroundBasedOnRequestMapping()")
    public Object logAudit(ProceedingJoinPoint joinPoint) throws Throwable {

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        Object result = null;
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();

        logger.info("Method: {} is intercepted by audit logging aspect", method.getName());

        String uri = request.getRequestURI();
        String entityName = extractEntityName(joinPoint.getArgs());
        String entityId = extractEntityId(joinPoint.getArgs());

        // Updation of entityName if we couldn't find it from the arguement of the method
        if (entityName.equals("UNKNOWN") && uri != null) {
            String[] parts = uri.split("/");
            // parts[0] might be empty if URI starts with "/"
            for (int i = 0; i < parts.length; i++) {
                if ("api".equals(parts[i]) && i + 1 < parts.length) {
                    entityName = parts[i + 1];
                    break;
                }
            }
        }

        ActionType actionType = determineAction(method, entityId);
        Object rawDataBefore = null;

        if (entityId != null) {
            rawDataBefore = CloneUtils.deepCopy(objectMapper, getEntityByNameAndId(entityName, entityId, entityManager));
        }

        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            logger.error("Exception occurred while proceeding with method: {}, entityName = {}, entityId = {}, e = {}", method.getName(), entityName, entityId, ex.getMessage());
            throw ex;
        }

        Object responseBody = extractResponseBody(result);

        if (!Objects.equals(actionType, ActionType.UNKNOWN)) {

            logger.debug("Building audit event for method: {}",  method.getName());

            LocalDateTime timeStamp = extractTimestamp(responseBody, actionType).orElse(LocalDateTime.now());

            AuditEvent event = AuditEvent.builder()
                    .entityName(entityName)
                    .entityId(entityId)
                    .action(actionType.name())
                    .timestamp(timeStamp)
                    .rawDataBefore(rawDataBefore)
                    .rawDataAfter(responseBody)
                    .requestId(UUID.randomUUID().toString())
                    .changedBy("USER")
                    .build();

            logger.debug("Publishing audit event to rabbitmq for processing with requestId: {}", event.getRequestId());

            rabbitTemplate.convertAndSend(ConfigConstants.TOPIC_NAME, ConfigConstants.ROUTING_KEY, event);
        }

        return result;
    }

    private ActionType determineAction(Method method, String entityId) {

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
            if (arg == null) continue;

            if (arg instanceof Long || arg instanceof String) {
                return String.valueOf(arg);
            }

            try {
                Method getIdMethod = arg.getClass().getMethod("getId");
                Object id = getIdMethod.invoke(arg);
                if (id != null) return String.valueOf(id);
            } catch (NoSuchMethodException e) {
                logger.error("No method found for extracting entity Id, e = {}", e.getMessage());
            } catch (Exception e) {
                logger.error("Exception occurred while extracting entity Id, e = {}", e.getMessage());
                throw new RuntimeException("Error extracting entity ID", e);
            }
        }
        return null;
    }


    private String extractEntityName(Object[] args) {
        return Arrays.stream(args)
                .filter(Objects::nonNull)
                .filter(arg -> {
                    String pkg = arg.getClass().getPackageName();
                    return !(pkg.startsWith("java.") || pkg.startsWith("javax.") || pkg.startsWith("org.springframework"));
                })
                .map(arg -> arg.getClass().getSimpleName().replaceAll("(?i)Dto$", ""))
                .findFirst()
                .orElse("UNKNOWN");
    }

    private Object extractResponseBody(Object result) {
        if (result instanceof Optional<?> optional) {
            return optional.orElse(null);
        }

        if (result instanceof ResponseEntity<?> responseEntity) {
            Object body = responseEntity.getBody();
            return (body instanceof ApiResponse<?> apiResponse) ? apiResponse.getData() : body;
        }

        return result;
    }

    private Object getEntityByNameAndId(String entityName, Object id, EntityManager entityManager) {
        Class<?> entityClass = entityManager.getMetamodel()
                .getEntities()
                .stream()
                .filter(e -> e.getName().equals(entityName))
                .findFirst()
                .map(EntityType::getJavaType)
                .orElseThrow(() -> new IllegalArgumentException("No such entity: " + entityName));

        return entityManager.find(entityClass, id);
    }

    private Optional<LocalDateTime> extractTimestamp(Object response, ActionType actionType) {
        if (response == null) return Optional.empty();

        try {
            Method timeMethod = switch (actionType) {
                case CREATE -> response.getClass().getMethod("getCreatedAt");
                case UPDATE -> response.getClass().getMethod("getUpdatedAt");
                default -> null;
            };

            if (timeMethod != null) {
                return Optional.ofNullable((LocalDateTime) timeMethod.invoke(response));
            }
        } catch (Exception e) {
            logger.error("Failed to extract time stamp, e = {}", e.getMessage());
        }

        return Optional.empty();
    }

}
