package ai.unified.process.demo.book.library.core.ui.layout;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.sidenav.SideNavVariant;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.spring.security.AuthenticationContext;
import ai.unified.process.demo.book.library.core.ui.LoginView;
import java.util.Locale;
import java.util.Objects;
import org.springframework.security.core.userdetails.UserDetails;

@AnonymousAllowed
@Layout
public class MainLayout extends AppLayout implements AfterNavigationObserver {

	private final H2 viewTitle = new H2();

	private final transient AuthenticationContext authContext;

	public MainLayout(AuthenticationContext authContext) {
		this.authContext = authContext;
		// DRAWER as primary section makes the navy rail run the full height of the
		// window, with the navbar sitting beside it above the content.
		setPrimarySection(Section.DRAWER);
		addHeaderContent();
		addDrawerContent();
	}

	@Override
	public void afterNavigation(AfterNavigationEvent event) {
		viewTitle.setText(getCurrentPageTitle());
	}

	private void addHeaderContent() {
		var toggle = new DrawerToggle();
		toggle.setAriaLabel("Menu toggle");
		toggle.addClassNames("app-drawer-toggle");

		viewTitle.addClassNames("app-view-title");

		var navbar = new HorizontalLayout(toggle, viewTitle, createUserActions());
		navbar.setWidthFull();
		navbar.setAlignItems(FlexComponent.Alignment.CENTER);
		navbar.expand(viewTitle);
		navbar.addClassNames("app-navbar");

		addToNavbar(navbar);
	}

	private HorizontalLayout createUserActions() {
		var layout = new HorizontalLayout();
		layout.setAlignItems(FlexComponent.Alignment.CENTER);
		layout.addClassNames("app-user-actions");

		authContext.getAuthenticatedUser(UserDetails.class).ifPresentOrElse(user -> {
			var avatar = new Avatar(user.getUsername());
			avatar.addThemeVariants(AvatarVariant.AURA_FILLED);

			var menu = new MenuBar();
			menu.addThemeVariants(MenuBarVariant.TERTIARY);
			menu.addClassNames("app-user-menu");
			menu.addItem(avatar).getSubMenu().addItem("Log out", _ -> authContext.logout());

			layout.add(menu);
		}, () -> {
			var loginLink = new RouterLink("Login", LoginView.class);
			loginLink.addClassNames("app-login-link");
			layout.add(loginLink);
		});
		return layout;
	}

	private void addDrawerContent() {
		var scroller = new Scroller(createNavigation());
		addToDrawer(createBrand(), scroller, createDrawerFooter());
	}

	private Header createBrand() {
		var mark = new Div(VaadinIcon.BOOK.create());
		mark.addClassNames("app-brand-mark");

		var name = new Span("Book Library");
		name.addClassNames("app-brand-name");

		var brand = new Header(mark, name);
		brand.addClassNames("app-brand");
		return brand;
	}

	/**
	 * Builds the navigation from the routes annotated with {@code @Menu}, so the drawer
	 * grows automatically as views are added rather than needing to be edited by hand.
	 */
	private SideNav createNavigation() {
		var nav = new SideNav();
		// FILLED gives the current item the solid accent pill of the reference design.
		nav.addThemeVariants(SideNavVariant.AURA_FILLED);
		for (MenuEntry entry : MenuConfiguration.getMenuEntries()) {
			var item = new SideNavItem(entry.title(), entry.path(), iconFor(entry));
			nav.addItem(item);
		}
		return nav;
	}

	private Icon iconFor(MenuEntry entry) {
		return entry.icon() == null || entry.icon().isBlank() ? VaadinIcon.CIRCLE_THIN.create()
				: new Icon(entry.icon());
	}

	private Footer createDrawerFooter() {
		var footer = new Footer();
		footer.addClassNames("app-drawer-footer");

		authContext.getAuthenticatedUser(UserDetails.class).ifPresent(user -> {
			var label = new Span("Signed in as");
			label.addClassNames("app-drawer-footer-label");

			var name = new Span(user.getUsername());
			name.addClassNames("app-drawer-footer-name");

			var roles = user.getAuthorities()
				.stream()
				.map(authority -> Objects.requireNonNullElse(authority.getAuthority(), ""))
				.filter(authority -> !authority.isBlank())
				.map(MainLayout::displayRole)
				.toList();
			var role = new Span(String.join(", ", roles));
			role.addClassNames("app-drawer-footer-role");

			footer.add(label, name, role);
		});
		return footer;
	}

	private static String displayRole(String authority) {
		var role = authority.replaceFirst("^ROLE_", "").toLowerCase(Locale.ROOT);
		return role.isEmpty() ? role : Character.toUpperCase(role.charAt(0)) + role.substring(1);
	}

	private String getCurrentPageTitle() {
		if (getContent() instanceof HasDynamicTitle hasDynamicTitle) {
			return hasDynamicTitle.getPageTitle() == null ? "" : hasDynamicTitle.getPageTitle();
		}
		else if (getContent().getClass().getAnnotation(PageTitle.class) != null) {
			return getContent().getClass().getAnnotation(PageTitle.class).value();
		}
		else {
			return MenuConfiguration.getPageHeader(getContent()).orElse("");
		}
	}

}
