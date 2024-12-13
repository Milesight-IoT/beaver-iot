package com.milesight.beaveriot.permission.initializer;

import com.milesight.beaveriot.permission.inspector.DataAspectStatementInspector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * @author loong
 * @date 2024/12/6 8:55
 */
@Slf4j
public class JPAApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

    String JSQLPARSER_CLASS_NAME = "net.sf.jsqlparser.statement.Statement";
    String JPA_ENTITYMANAGER_CLASS_NAME = "jakarta.persistence.EntityManager";

    String JPA_STATEMENT_INSPECTOR_KEY = "spring.jpa.properties.hibernate.session_factory.statement_inspector";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {

        boolean entityManagerPresent = ClassUtils.isPresent(JPA_ENTITYMANAGER_CLASS_NAME, this.getClass().getClassLoader());
        boolean jsqlparserPresent = ClassUtils.isPresent(JSQLPARSER_CLASS_NAME, this.getClass().getClassLoader());
        if(entityManagerPresent && jsqlparserPresent){
            String oldValue = applicationContext.getEnvironment().getProperty(JPA_STATEMENT_INSPECTOR_KEY);
            if(StringUtils.hasText(oldValue)){
                log.warn("The existing statement inspector interceptor configuration, please manually add JPADataAspectStatementInspector interceptors");
            }else{
                log.info("Enable JPA DataAspect function, and dynamically add JPA STATEMENT_INSPECTOR");
                System.setProperty(JPA_STATEMENT_INSPECTOR_KEY, DataAspectStatementInspector.class.getName());
            }
        }

    }


    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 9;
    }

}
