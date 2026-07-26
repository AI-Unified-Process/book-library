package ai.unified.process.demo.book.library.core.ui;

import ai.unified.process.demo.book.library.TestcontainersConfiguration;
import com.vaadin.browserless.SpringBrowserlessTest;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.Locale;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
public abstract class AbstractBrowserlessTest extends SpringBrowserlessTest {

	@BeforeAll
	static void setDefaultLocale() {
		Locale.setDefault(Locale.ENGLISH);
	}

}
