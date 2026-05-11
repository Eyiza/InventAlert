package com.inventalert.inventoryService.multicompany;

import com.inventalert.inventoryService.config.DataSourceConfig;
import lombok.RequiredArgsConstructor;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Service
@RequiredArgsConstructor
public class CompanySchemaService {

    @Value("${spring.datasource.url}")
    private String masterUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    private final DataSourceConfig dataSourceConfig;

    public void provisionSchema(String companyId) {
        String schemaName = "company_" + companyId;

        try (Connection conn = DriverManager.getConnection(masterUrl, username, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE DATABASE IF NOT EXISTS `" + schemaName + "`");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create schema " + schemaName, e);
        }

        String schemaUrl = buildSchemaUrl(schemaName);
        Flyway flyway = Flyway.configure()
                .dataSource(schemaUrl, username, password)
                .locations("classpath:db/migration/company")
                .load();
        flyway.migrate();

        dataSourceConfig.registerCompanyDataSource(companyId);
    }

    public void dropSchema(String companyId) {
        String schemaName = "company_" + companyId;
        try (Connection conn = DriverManager.getConnection(masterUrl, username, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP DATABASE IF EXISTS `" + schemaName + "`");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to drop schema " + schemaName, e);
        }
    }

    private String buildSchemaUrl(String schemaName) {
        int qIndex = masterUrl.indexOf('?');
        String path = qIndex >= 0 ? masterUrl.substring(0, qIndex) : masterUrl;
        String query = qIndex >= 0 ? masterUrl.substring(qIndex) : "";
        int lastSlash = path.lastIndexOf('/');
        return path.substring(0, lastSlash + 1) + schemaName + query;
    }
}
