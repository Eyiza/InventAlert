package com.inventalert.inventoryService;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"MYSQL_INVENTORY_USER=test_user",
		"MYSQL_INVENTORY_PASSWORD=test_pass",
		"JWT_SECRET=test-jwt-secret-key-for-testing-purposes-only-minimum-256-bits",
		// Tell Hibernate the dialect explicitly — skips live-DB dialect detection
		"spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
		// Prevent Hibernate bootstrap from opening a JDBC connection
		"spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
		// Prevent Spring's SQL initializer from trying to run scripts
		"spring.sql.init.mode=never"
})
class InventoryServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
