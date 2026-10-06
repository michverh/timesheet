package eu.openvalue.timesheet;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = ArchitectureTest.ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  static final String ROOT = "eu.openvalue.timesheet";

  private static final String DOMAIN = ROOT + ".domain..";
  private static final String APPLICATION = ROOT + ".application..";
  private static final String ADAPTER_IN = ROOT + ".adapter.in..";
  private static final String ADAPTER_OUT = ROOT + ".adapter.out..";
  private static final String CONFIGURATION = ROOT + ".configuration..";

  @ArchTest
  static final ArchRule dependenciesPointInward = layeredArchitecture()
      .consideringOnlyDependenciesInLayers()
      .withOptionalLayers(true)
      .layer("Domain").definedBy(DOMAIN)
      .layer("Application").definedBy(APPLICATION)
      .layer("AdapterIn").definedBy(ADAPTER_IN)
      .layer("AdapterOut").definedBy(ADAPTER_OUT)
      .layer("Configuration").definedBy(CONFIGURATION)
      .whereLayer("Application").mayOnlyBeAccessedByLayers("AdapterIn", "AdapterOut", "Configuration")
      .whereLayer("AdapterIn").mayOnlyBeAccessedByLayers("Configuration")
      .whereLayer("AdapterOut").mayOnlyBeAccessedByLayers("Configuration")
      .whereLayer("Configuration").mayNotBeAccessedByAnyLayer();

  @ArchTest
  static final ArchRule domainIsPureJava = classes()
      .that().resideInAPackage(DOMAIN)
      .should().onlyDependOnClassesThat().resideInAnyPackage(DOMAIN, "java..")
      .because("the domain must stay free of frameworks and infrastructure")
      .allowEmptyShould(true);

  @ArchTest
  static final ArchRule applicationIsFrameworkFree = classes()
      .that().resideInAPackage(APPLICATION)
      .should().onlyDependOnClassesThat().resideInAnyPackage(APPLICATION, DOMAIN, "java..")
      .because("the application core orchestrates the domain without framework or infrastructure concerns")
      .allowEmptyShould(true);

  @ArchTest
  static final ArchRule persistenceStaysInOutboundAdapters = noClasses()
      .that().resideOutsideOfPackage(ADAPTER_OUT)
      .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.hibernate..",
          "org.springframework.data..")
      .because("persistence is an implementation detail of the outbound adapters");

  @ArchTest
  static final ArchRule noFieldInjection = NO_CLASSES_SHOULD_USE_FIELD_INJECTION.allowEmptyShould(true);
}
