package com.inventalert.inventoryService.config;

import com.inventalert.inventoryService.multicompany.CompanyRoutingDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String masterUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    private CompanyRoutingDataSource routingDataSource;

    @Bean
    @Primary
    public DataSource dataSource() {
        routingDataSource = new CompanyRoutingDataSource();

        Map<Object, Object> targets = new HashMap<>();
        try (Connection conn = DriverManager.getConnection(masterUrl, username, password);
             ResultSet rs = conn.getMetaData().getCatalogs()) {
            while (rs.next()) {
                String schema = rs.getString(1);
                if (schema.startsWith("company_")) {
                    targets.put(schema, buildDataSource(schema));
                }
            }
        } catch (SQLException e) {
            // No company schemas yet — this is fine on first startup
        }

        routingDataSource.initializeTargetDataSources(targets);
        routingDataSource.setDefaultTargetDataSource(buildMasterDataSource());
        routingDataSource.afterPropertiesSet();
        return routingDataSource;
    }

    public void registerCompanyDataSource(String companyId) {
        String schema = "company_" + companyId;
        routingDataSource.addTargetDataSource(schema, buildDataSource(schema));
    }

    private DataSource buildDataSource(String schema) {
        String url = masterUrl.replace("/?", "/" + schema + "?");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(5);
        config.setPoolName("pool-" + schema);
        return new HikariDataSource(config);
    }

    private DataSource buildMasterDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(masterUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(2);
        config.setPoolName("pool-master");
        config.setInitializationFailTimeout(0);
        return new HikariDataSource(config);
    }
}
