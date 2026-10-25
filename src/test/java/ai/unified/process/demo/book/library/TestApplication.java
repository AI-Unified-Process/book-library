package ai.unified.process.demo.book.library;

import org.springframework.boot.SpringApplication;

public class TestApplication {

	public static void main(String[] args) {
		// Also load the test data of the use case tests (db/testdata), so the app you
		// click through shows the same books, members, and loans as the tests.
		String[] arguments = new String[args.length + 1];
		arguments[0] = "--spring.flyway.locations=classpath:db/migration,classpath:db/testdata";
		System.arraycopy(args, 0, arguments, 1, args.length);
		SpringApplication.from(Application::main).with(TestcontainersConfiguration.class).run(arguments);
	}

}
