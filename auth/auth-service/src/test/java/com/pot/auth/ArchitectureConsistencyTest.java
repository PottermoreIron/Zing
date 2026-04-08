package com.pot.auth;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.pot.auth", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureConsistencyTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_frameworks_or_outer_layers = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "com.fasterxml.jackson..",
                    "jakarta.validation..",
                    "..interfaces..",
                    "..infrastructure..");

    @ArchTest
    static final ArchRule application_should_not_depend_on_interfaces = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..interfaces..");

    @ArchTest
    static final ArchRule interfaces_should_not_depend_on_infrastructure_adapters = noClasses()
            .that().resideInAPackage("..interfaces..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..infrastructure.adapter..",
                    "..infrastructure.client..");
}