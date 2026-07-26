package ai.unified.process.demo.book.library.core.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

/**
 * Principal returned by {@link AppUserDetailsService}. Carries the {@code app_user} row's
 * id and role so callers can scope queries by the logged-in user without touching the
 * security context directly.
 */
public class AppUserDetails extends User {

	private final long appUserId;

	private final Role role;

	public AppUserDetails(long appUserId, String username, String passwordHash, Role role) {
		super(username, passwordHash, List.of(new SimpleGrantedAuthority(role.authority())));
		this.appUserId = appUserId;
		this.role = role;
	}

	public long appUserId() {
		return appUserId;
	}

	public Role role() {
		return role;
	}

}
