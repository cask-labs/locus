package com.locus.core.domain

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.Test

class DomainArchitectureTest {

    private val importedClasses = ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("com.locus.core.domain")

    @Test
    fun `domain classes should not depend on android sdk`() {
        noClasses()
            .that().resideInAPackage("com.locus.core.domain..")
            .should().dependOnClassesThat().resideInAPackage("android..")
            .check(importedClasses)
    }

    @Test
    fun `use cases should be named ending with UseCase`() {
        classes()
            .that().implement(UseCase::class.java)
            .should().haveSimpleNameEndingWith("UseCase")
            .check(importedClasses)
    }
}
