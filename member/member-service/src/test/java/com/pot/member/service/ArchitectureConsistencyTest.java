package com.pot.member.service;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.pot.member.service", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureConsistencyTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_frameworks = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "com.baomidou..",
                    "com.fasterxml.jackson..",
                    "jakarta.validation..");

    @ArchTest
    static final ArchRule persistence_entities_should_be_transport_free = noClasses()
            .that().resideInAPackage("..infrastructure.persistence.entity..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.fasterxml.jackson..",
                    "jakarta.validation..",
                    "org.hibernate.validator..",
                    "com.pot.zing.framework.common.validate..");

    @ArchTest
    static final ArchRule interfaces_should_not_depend_on_persistence = noClasses()
            .that().resideInAPackage("..interfaces..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..infrastructure.persistence..",
                    "..infrastructure.persistence.entity..");
}