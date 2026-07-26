package ai.unified.process.demo.book.library.core.security;

import ai.unified.process.demo.book.library.db.tables.records.AppUserRecord;
import org.jooq.DSLContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static ai.unified.process.demo.book.library.db.Tables.APP_USER;

@Service
public class AppUserDetailsService implements UserDetailsService {

	private final DSLContext dsl;

	public AppUserDetailsService(DSLContext dsl) {
		this.dsl = dsl;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		AppUserRecord user = dsl.selectFrom(APP_USER).where(APP_USER.USERNAME.eq(username)).fetchOne();
		if (user == null) {
			throw new UsernameNotFoundException("No user with username: " + username);
		}
		return new AppUserDetails(user.getId(), user.getUsername(), user.getPasswordHash(),
				Role.valueOf(user.getRole()));
	}

}
