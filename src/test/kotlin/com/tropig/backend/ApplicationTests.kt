package com.tropig.backend

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = ["classpath:application-test.yml"])
class ApplicationTests {

	@Test
	fun contextLoads() {
		// Spring 컨텍스트가 정상적으로 로드되는지 확인
		println("TEST")
	}
}
