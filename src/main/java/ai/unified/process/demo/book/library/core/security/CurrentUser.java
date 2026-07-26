package ai.unified.process.demo.book.library.core.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Read-only window onto the currently authenticated user. Reads Spring's
 * {@link SecurityContextHolder} directly so it can be injected anywhere — including the
 * domain layer — without pulling in Vaadin.
 *
 * <p>
 * This bean exposes the {@code app_user} identity. Features that link a domain entity
 * (e.g. {@code member}) to an {@code app_user} should look it up via
 * {@link #requireAppUserId()}.
 */
@Component
public class CurrentUser {

	public Optional<AppUserDetails> get() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return Optional.empty();
		}
		Object principal = authentication.getPrincipal();
		return principal instanceof AppUserDetails details ? Optional.of(details) : Optional.empty();
	}

	public AppUserDetails require() {
		return get().orElseThrow(() -> new IllegalStateException("No authenticated user in security context"));
	}

	public long requireAppUserId() {
		return require().appUserId();
	}

	public boolean isLibrarian() {
		return get().map(d -> d.role() == Role.LIBRARIAN).orElse(false);
	}

}
