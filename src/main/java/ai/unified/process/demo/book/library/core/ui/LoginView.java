package ai.unified.process.demo.book.library.core.ui;

import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "login", autoLayout = false)
@PageTitle("Login")
@AnonymousAllowed
public class LoginView extends LoginOverlay implements BeforeEnterObserver {

	public LoginView() {
		setAction("login");

		var i18n = LoginI18n.createDefault();
		var header = new LoginI18n.Header();
		header.setTitle("Book Library");
		header.setDescription("Sign in to browse the catalog and borrow books.");
		i18n.setHeader(header);
		i18n.setAdditionalInformation("Demo accounts — Librarian: librarian / librarian · Member: alice / alice");
		setI18n(i18n);

		setOpened(true);
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		setError(event.getLocation().getQueryParameters().getParameters().containsKey("error"));
	}

}
