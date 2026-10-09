package eu.openvalue.timesheet;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage;
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
  private static final String PERSISTENCE_ADAPTER = ROOT + ".adapter.out.persistence..";
  private static final String INBOUND_PORTS = ROOT + ".application.port.in..";
  private static final String OUTBOUND_PORTS = ROOT + ".application.port.out..";

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
  static final ArchRule inboundAdaptersOnlyUseInboundPorts = noClasses()
      .that().resideInAPackage(ADAPTER_IN)
      .should().dependOnClassesThat(resideInAPackage(APPLICATION).and(resideOutsideOfPackage(INBOUND_PORTS)))
      .because("inbound adapters drive the application through its use case interfaces only")
      .allowEmptyShould(true);

  @ArchTest
  static final ArchRule outboundAdaptersOnlyUseOutboundPorts = noClasses()
      .that().resideInAPackage(ADAPTER_OUT)
      .should().dependOnClassesThat(resideInAPackage(APPLICATION).and(resideOutsideOfPackage(OUTBOUND_PORTS)))
      .because("outbound adapters only implement the ports the application core defines");

  @ArchTest
  static final ArchRule persistenceStaysInThePersistenceAdapter = noClasses()
      .that().resideOutsideOfPackages(PERSISTENCE_ADAPTER, CONFIGURATION)
      .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.hibernate..",
          "org.springframework.data..", "org.springframework.transaction..", "org.flywaydb..")
      .because("how timesheets are stored is an implementation detail of the persistence adapter");

  @ArchTest
  static final ArchRule documentLibrariesStayInOutboundAdapters = noClasses()
      .that().resideOutsideOfPackage(ADAPTER_OUT)
      .should().dependOnClassesThat().resideInAnyPackage("org.apache.poi..", "org.apache.pdfbox..")
      .because("document formats such as spreadsheets and PDF are implementation details of the outbound adapters");

  @ArchTest
  static final ArchRule languageModelClientsStayInOutboundAdapters = noClasses()
      .that().resideOutsideOfPackage(ADAPTER_OUT)
      .should().dependOnClassesThat().resideInAnyPackage("com.anthropic..")
      .because("which language model reads documents is an implementation detail of the outbound adapters");

  @ArchTest
  static final ArchRule noFieldInjection = NO_CLASSES_SHOULD_USE_FIELD_INJECTION.allowEmptyShould(true);
}
