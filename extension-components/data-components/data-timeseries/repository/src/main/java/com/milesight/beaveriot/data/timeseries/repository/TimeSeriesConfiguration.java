package com.milesight.beaveriot.data.timeseries.repository;

import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.context.support.PackagesScanner;
import com.milesight.beaveriot.data.api.SupportTimeSeries;
import com.milesight.beaveriot.data.api.TimeSeriesRepository;
import com.milesight.beaveriot.data.support.TimeSeriesDataConverter;
import com.milesight.beaveriot.data.timeseries.common.TimeSeriesProperty;
import com.milesight.beaveriot.data.timeseries.influxdb.DynamoDbTimeSeriesRepository;
import com.milesight.beaveriot.data.timeseries.influxdb.InfluxDbTimeSeriesRepository;
import com.milesight.beaveriot.data.timeseries.jpa.JpaTimeSeriesRepository;
import jakarta.persistence.Table;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

/**
 * TimeSeriesConfiguration class.
 *
 * @author simon
 * @date 2025/10/11
 */
@Slf4j
@Configuration
public class TimeSeriesConfiguration implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private Environment environment;

    private SupportTimeSeries getAnnotation(Class<?> entity) {
        SupportTimeSeries[] annotations = entity.getAnnotationsByType(SupportTimeSeries.class);
        if (annotations.length > 0) {
            return annotations[0];
        }

        return null;
    }

    private String getTableName(SupportTimeSeries supportTimeSeries, Class<?> entityClass) {
        // use specified table name first
        if (!supportTimeSeries.tableName().isEmpty()) {
            return supportTimeSeries.tableName();
        }

        // auto find table name from entity po
        Table[] annotations = entityClass.getAnnotationsByType(Table.class);
        if (annotations.length == 0) {
            throw new IllegalArgumentException(entityClass.getName() + " cannot find @Table to get table name");
        }

        String tableName = annotations[0].name();
        if (ObjectUtils.isEmpty(tableName)) {
            throw new IllegalArgumentException(entityClass.getName() + " has an invalid table name");
        }

        return tableName;
    }

    private TimeSeriesDataConverter createConverterInstance(SupportTimeSeries supportTimeSeries) {
        try {
            Constructor<?> constructor = supportTimeSeries.converter().getConstructor();
            return (TimeSeriesDataConverter) constructor.newInstance();
        } catch (Exception e) {
           throw new IllegalArgumentException("Illegal converter class");
        }
    }

    private String getTimeColumnName(SupportTimeSeries supportTimeSeries) {
        return StringUtils.toSnakeCase(supportTimeSeries.timeColumn());
    }

    private List<String> getIndexedColumnNameList(SupportTimeSeries supportTimeSeries) {
        return Arrays.stream(supportTimeSeries.indexedColumns()).toList();
    }

    @Override
    public void postProcessBeanFactory(@NotNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }

    @Override
    public void postProcessBeanDefinitionRegistry(@NotNull BeanDefinitionRegistry registry) throws BeansException {
        PackagesScanner packagesScanner = new PackagesScanner();
        packagesScanner.doScan("com.milesight.beaveriot.**.*.po", entityClass -> {
            if (!entityClass.getSimpleName().endsWith("PO")) {
                return;
            }

            SupportTimeSeries supportTimeSeries = getAnnotation(entityClass);
            if (supportTimeSeries == null) {
                return;
            }

            String tableName = getTableName(supportTimeSeries, entityClass);
            RootBeanDefinition rootBeanDefinition = new RootBeanDefinition();


            String databaseType = environment.getProperty(TimeSeriesProperty.TIMESERIES_DATABASE);
            if ("influxdb".equals(databaseType)) {
                rootBeanDefinition.setTargetType(ResolvableType.forClassWithGenerics(TimeSeriesRepository.class, entityClass));
                rootBeanDefinition.setBeanClass(InfluxDbTimeSeriesRepository.class);
                ConstructorArgumentValues cav = rootBeanDefinition.getConstructorArgumentValues();
                cav.addIndexedArgumentValue(0, supportTimeSeries.category());
                cav.addIndexedArgumentValue(1, tableName);
                cav.addIndexedArgumentValue(2, getTimeColumnName(supportTimeSeries));
                cav.addIndexedArgumentValue(3, getIndexedColumnNameList(supportTimeSeries).stream().map(StringUtils::toSnakeCase));
                cav.addIndexedArgumentValue(4, createConverterInstance(supportTimeSeries));
                cav.addIndexedArgumentValue(5, entityClass);
            } else if ("dynamodb".equals(databaseType)) {
                rootBeanDefinition.setTargetType(ResolvableType.forClassWithGenerics(TimeSeriesRepository.class, entityClass));
                rootBeanDefinition.setBeanClass(DynamoDbTimeSeriesRepository.class);
                ConstructorArgumentValues cav = rootBeanDefinition.getConstructorArgumentValues();
                cav.addIndexedArgumentValue(0, supportTimeSeries.category());
                cav.addIndexedArgumentValue(1, tableName);
                cav.addIndexedArgumentValue(2, getTimeColumnName(supportTimeSeries));
                cav.addIndexedArgumentValue(3, getIndexedColumnNameList(supportTimeSeries).stream().map(StringUtils::toSnakeCase));
                cav.addIndexedArgumentValue(4, createConverterInstance(supportTimeSeries));
                cav.addIndexedArgumentValue(5, entityClass);
            } else {
                rootBeanDefinition.setTargetType(ResolvableType.forClassWithGenerics(TimeSeriesRepository.class, entityClass));
                rootBeanDefinition.setBeanClass(JpaTimeSeriesRepository.class);
                ConstructorArgumentValues cav = rootBeanDefinition.getConstructorArgumentValues();
                cav.addIndexedArgumentValue(0, entityClass);
                cav.addIndexedArgumentValue(1, supportTimeSeries.timeColumn());
                cav.addIndexedArgumentValue(2, getIndexedColumnNameList(supportTimeSeries));
                cav.addIndexedArgumentValue(3, createConverterInstance(supportTimeSeries));
            }

            String beanName = entityClass.getSimpleName() + "TimeSeriesRepository";
            registry.registerBeanDefinition(beanName, rootBeanDefinition);
        });
    }

    @Override
    public void setEnvironment(@NotNull Environment environment) {
        this.environment = environment;
    }
}
