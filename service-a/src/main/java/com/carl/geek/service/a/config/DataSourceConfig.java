package com.carl.geek.service.a.config;


import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * @author carl.che
 */
@Configuration
public class DataSourceConfig {

    @Bean("masterDataSourceProperties")
    @Primary
    @ConfigurationProperties("service-a.datasource.master")
    public DataSourceProperties masterDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean("masterDatasource")
    @Primary
    @ConfigurationProperties("service-a.datasource.master.configuration")
    public HikariDataSource masterDataSource(@Qualifier("masterDataSourceProperties") DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Primary
    @Bean("masterSqlSessionFactory")
    @ConditionalOnBean(name = "masterDatasource")
    public SqlSessionFactory masterSqlSessionFactory(@Qualifier("masterDatasource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sqlSessionFactoryBean.setMapperLocations(resolver.getResources("com/carl/geek/service/a/dao/mybatis/mappers/*.xml"));
        return sqlSessionFactoryBean.getObject();
    }

    @Bean("masterMapperScannerConfig")
    public MapperScannerConfigurer masterMapperScannerConfig() {
        MapperScannerConfigurer mapperScannerConfigurer = new MapperScannerConfigurer();
        mapperScannerConfigurer.setSqlSessionFactoryBeanName("masterSqlSessionFactory");
        mapperScannerConfigurer.setBasePackage("com.carl.geek.service.a.dao");
        mapperScannerConfigurer.setAnnotationClass(Repository.class);
        return mapperScannerConfigurer;
    }

    @Bean("masterConfigurationCustomizer")
    public ConfigurationCustomizer masterMybatisConfigurationCustomizer() {
        return configuration -> {
            configuration.setLazyLoadingEnabled(true);
            configuration.setDefaultStatementTimeout(20000);
            configuration.setMapUnderscoreToCamelCase(true);
        };
    }


    @Bean("masterSqlSessionTemplate")
    @ConditionalOnBean(name = "masterSqlSessionFactory")
    public SqlSessionTemplate masterSqlSessionTemplate(@Qualifier("masterSqlSessionFactory") SqlSessionFactory sqlSessionFactory){
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean("masterTransaction")
    @ConditionalOnBean(name = "masterDatasource")
    public PlatformTransactionManager transactionManager(@Qualifier("masterDatasource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

}
