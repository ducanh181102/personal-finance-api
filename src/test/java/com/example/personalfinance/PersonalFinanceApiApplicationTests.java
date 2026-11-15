package com.example.personalfinance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:application-context-test",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PersonalFinanceApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
