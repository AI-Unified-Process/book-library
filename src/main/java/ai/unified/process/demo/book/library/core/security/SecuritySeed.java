package ai.unified.process.demo.book.library.core.security;

import org.jooq.DSLContext;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import static ai.unified.process.demo.book.library.db.Tables.APP_USER;

/**
 * Seeds a librarian and a member user on first start so the app is usable out of the box.
 * Idempotent: it only inserts rows whose username is missing, so the runner can fire on
 * every boot in dev without producing duplicates.
 */
@Component
public class SecuritySeed implements ApplicationRunner {

	private final DSLContext dsl;

	private final PasswordEncoder passwordEncoder;

	public SecuritySeed(DSLContext dsl, PasswordEncoder passwordEncoder) {
		this.dsl = dsl;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(ApplicationArguments args) {
		upsertUser("librarian", "librarian", Role.LIBRARIAN);
		upsertUser("alice", "alice", Role.MEMBER);
	}

	private void upsertUser(String username, String rawPassword, Role role) {
		boolean exists = dsl.fetchExists(dsl.selectFrom(APP_USER).where(APP_USER.USERNAME.eq(username)));
		if (exists) {
			return;
		}
		dsl.insertInto(APP_USER)
			.set(APP_USER.USERNAME, username)
			.set(APP_USER.PASSWORD_HASH, passwordEncoder.encode(rawPassword))
			.set(APP_USER.ROLE, role.name())
			.execute();
	}

}
