package com.inventalert.inventoryService.multicompany;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class CompanyRoutingDataSource extends AbstractRoutingDataSource {

    private final Map<Object, Object> targetDataSourcesMap = new HashMap<>();

    @Override
    protected Object determineCurrentLookupKey() {
        return CompanyContext.get();
    }

    public void addTargetDataSource(String key, DataSource dataSource) {
        targetDataSourcesMap.put(key, dataSource);
        setTargetDataSources(new HashMap<>(targetDataSourcesMap));
        afterPropertiesSet();
    }

    public void initializeTargetDataSources(Map<Object, Object> dataSources) {
        targetDataSourcesMap.putAll(dataSources);
        setTargetDataSources(new HashMap<>(targetDataSourcesMap));
    }
}
