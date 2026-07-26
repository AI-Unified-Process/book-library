package ai.unified.process.demo.book.library.core.security;

public enum Role {

	MEMBER, LIBRARIAN;

	public String authority() {
		return "ROLE_" + name();
	}

}
