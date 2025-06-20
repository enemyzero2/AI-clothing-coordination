package com.example.aioutfitapp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;

/**
 * 端点日志监听器
 * 
 * 在应用启动时记录所有映射的端点，用于调试
 */
@Component
@Slf4j
public class EndpointLoggerListener implements ApplicationListener<ContextRefreshedEvent> {

    /**
     * 应用上下文刷新时触发
     * 
     * @param event 上下文刷新事件
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ApplicationContext applicationContext = event.getApplicationContext();
        RequestMappingHandlerMapping requestMappingHandlerMapping = applicationContext
                .getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, org.springframework.web.method.HandlerMethod> map = requestMappingHandlerMapping
                .getHandlerMethods();
        
        log.info("============================");
        log.info("应用程序所有端点列表:");
        map.forEach((key, value) -> log.info("{} => {}", key, value));
        log.info("============================");
    }
} 