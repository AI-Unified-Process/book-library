package ai.unified.process.demo.book.library.core.configuration;

import org.jooq.impl.DefaultConfiguration;
import org.springframework.boot.jooq.autoconfigure.DefaultConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VjJooqConfiguration {

	@Bean
	public DefaultConfigurationCustomizer configurationCustomizer() {
		// Enable optimistic locking
		return (DefaultConfiguration c) -> c.settings().withExecuteWithOptimisticLocking(true);
	}

}
