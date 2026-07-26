package ai.unified.process.demo.book.library;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

class ArchitectureTest {

	// Packages

	public static final String PACKAGE_ROOT = "ai.unified.process.demo.book.library";

	public static final String UI_PACKAGE = "..ui..";

	// Security configuration is the one place outside ..ui.. that may import com.vaadin..
	// (VaadinSecurityConfigurer, AuthenticationContext, LoginForm).
	public static final String SECURITY_PACKAGE = "..security..";

	// Layers

	private static final String UI_LAYER = "UI";

	// Modules

	private static final String CORE_MODULE = "..core..";

	private static final String GREETING_MODULE = "..greeting..";

	private final JavaClasses classes = new ClassFileImporter().importPackages(PACKAGE_ROOT);

	@Test
	void layered_architecture_check() {
		layeredArchitecture().consideringAllDependencies()

			.layer(UI_LAYER)
			.definedBy(UI_PACKAGE)

			.whereLayer(UI_LAYER)
			.mayNotBeAccessedByAnyLayer()

			.check(classes);
	}

	@Test
	void module_check_core_may_not_be_accessed_by_any_other_module() {
		noClasses().that()
			.resideInAPackage(CORE_MODULE)
			.should()
			.accessClassesThat()
			.resideInAnyPackage(GREETING_MODULE)
			.check(classes);
	}

	@Test
	void verify_that_only_the_ui_layer_is_using_vaadin() {
		noClasses().that()
			.resideOutsideOfPackages(UI_PACKAGE, SECURITY_PACKAGE)
			.should()
			.accessClassesThat()
			.resideInAnyPackage("com.vaadin..")
			.check(classes);
	}

}
