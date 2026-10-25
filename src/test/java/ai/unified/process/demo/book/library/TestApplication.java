package ai.unified.process.demo.book.library;

import org.springframework.boot.SpringApplication;

public class TestApplication {

	public static void main(String[] args) {
		// The "testdata" profile also loads the test data of the use case tests
		// (db/testdata),
		// so the app you click through shows the same books, members, and loans as the
		// tests.
		SpringApplication.from(Application::main)
			.with(TestcontainersConfiguration.class)
			.withAdditionalProfiles("testdata")
			.run(args);
	}

}
