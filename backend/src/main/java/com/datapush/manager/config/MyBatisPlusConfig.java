package com.datapush.manager.config;

import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
@MapperScan("com.datapush.manager.mapper")
public class MyBatisPlusConfig {

    /*
     * 分页插件，自动识别数据库类型
     * 多租户，请参考官网【插件扩展】
     */
    @Bean
    public PaginationInterceptor paginationInterceptor() {
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
        //分页单页最大条数，默认500
        paginationInterceptor.setLimit(10000);
        return paginationInterceptor;
    }

    @Bean
    public DatabaseIdProvider databaseIdProvider() {
        System.out.println("databaseIdProvider");
        DatabaseIdProvider databaseIdProvider = new VendorDatabaseIdProvider();
        Properties p = new Properties();
        p.setProperty("Oracle", "oracle");
        p.setProperty("MySQL", "mysql");
        p.setProperty("PostgreSQL", "postgresql");
        p.setProperty("DB2", "db2");
        p.setProperty("SQL Server", "sqlserver");
        p.setProperty("DM DBMS", "dm");
        p.setProperty("KingbaseES", "kingbase8");
        databaseIdProvider.setProperties(p);
        return databaseIdProvider;
    }
}
