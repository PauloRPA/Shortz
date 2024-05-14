package com.prpa.Shortz;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ShortzApplicationTests {

	@Test
	void contextLoads() {
		System.out.println(System.getProperties().getProperty("java.version"));
	}

}
