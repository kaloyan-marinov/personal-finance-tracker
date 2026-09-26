package com.kaloyan_marinov.personal_finance_tracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
	properties = {
		"spring.datasource.url=jdbc:h2:mem:test",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.mysql.sql"
	}
)
class PersonalFinanceTrackerApplicationTests {

	@Test
	void contextLoads() {
	}

}
