package ai.unified.process.demo.book.library;

import org.springframework.boot.SpringApplication;

public class TestApplication {

	public static void main(String[] args) {
		// The test classpath also holds the test data migrations of the use case tests
		// (src/test/resources/db/migration/R__*.sql), so the app you click through shows
		// the same books, members, and loans as the tests.
		SpringApplication.from(Application::main).with(TestcontainersConfiguration.class).run(args);
	}

}
