/*
 * Komunumo - Open Source Community Manager
 * Copyright (C) Marcus Fihlon and the individual contributors to Komunumo.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package app.komunumo;

import app.komunumo.test.BrowserTest;
import app.komunumo.test.KaribuTest;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

class ArchitectureTest {

    private final @NotNull JavaClasses allClasses = new ClassFileImporter()
            .importPackages("app.komunumo");
    private final @NotNull JavaClasses classesWithoutTests = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("app.komunumo");
    private final @NotNull JavaClasses onlyTests = new ClassFileImporter()
            .withImportOption(new ImportOption.OnlyIncludeTests())
            .importPackages("app.komunumo");

    @Test
    void jooqClassesShouldOnlyBeAccessedByServiceLayer() {
        noClasses()
                .that()
                .resideOutsideOfPackages(
                        "app.komunumo.data.service..", // old service layer
                        "app.komunumo.domain..control..", // new service layer
                        "app.komunumo.data.db..") // jOOQ generated classes
                .should()
                .accessClassesThat()
                .resideInAnyPackage("app.komunumo.data.db..")
                .because("only the service layer and jOOQ code should access jOOQ types directly")
                .check(classesWithoutTests);
    }

    @Test
    void servicesShouldNotReturnStreams() {
        methods()
                .that().areDeclaredInClassesThat().resideInAPackage("..control..")
                .and().arePublic()
                .and().areNotDeclaredIn(Object.class)
                .should().notHaveRawReturnType(Stream.class)
                .because("returning Stream from service methods may lead to unclosed JDBC resources")
                .check(classesWithoutTests);
    }

    @Test
    void dtosShouldBeRecordsOrEnums() {
        ArchCondition<JavaClass> beRecordOrEnum = new ArchCondition<>("be a record or enum") {
            @Override
            public void check(final @NotNull JavaClass clazz, final @NotNull ConditionEvents events) {
                final var isRecord = clazz.isRecord();
                final var isEnum = clazz.isEnum();

                if (!isRecord && !isEnum) {
                    final var message = clazz.getSimpleName() + " is neither a record nor an enum";
                    events.add(SimpleConditionEvent.violated(clazz, message));
                }
            }
        };

        classes()
                .that()
                .resideInAPackage("..entity..")
                .and().haveSimpleNameEndingWith("Dto")
                .should(beRecordOrEnum)
                .because("DTOs should be implemented as Java records or enums to ensure immutability and clarity")
                .check(classesWithoutTests);
    }

    @Test
    void forbiddenDateTimeTypesShouldNotBeUsed() {
        final var forbiddenTypes = Set.of(
                Calendar.class.getName(),
                Date.class.getName(),
                LocalDate.class.getName(),
                LocalDateTime.class.getName(),
                LocalTime.class.getName()
        );
        final var forbiddenTypeList = forbiddenTypes.stream()
                .map(fqn -> fqn.substring(fqn.lastIndexOf('.') + 1))
                .sorted()
                .collect(Collectors.joining(", "));

        final var notDependOnForbiddenDateTimeTypes = new ArchCondition<JavaClass>(
                "not depend on " + forbiddenTypeList) {
            @Override
            public void check(@NotNull JavaClass clazz, @NotNull ConditionEvents events) {
                if (clazz.getFullName().equals("app.komunumo.infra.persistence.jooq.ZonedDateTimeConverter")) {
                    return; // exception for ZonedDateTimeConverter
                }
                if (clazz.getFullName().equals("app.komunumo.domain.event.boundary.CreateEventView")) {
                    return; // TODO temporary exception for DateTimePicker usage in CreateEventView
                }

                clazz.getDirectDependenciesFromSelf().forEach(dependency -> {
                    var targetName = dependency.getTargetClass().getFullName();
                    if (forbiddenTypes.contains(targetName)) {
                        events.add(SimpleConditionEvent.violated(
                                dependency,
                                "Class " + clazz.getName() + " depends on forbidden type: " + targetName
                        ));
                    }
                });
            }
        };

        classes()
                .should(notDependOnForbiddenDateTimeTypes)
                .because("Komunumo needs the timezone information!")
                .check(classesWithoutTests);
    }

    @Test
    void junit4AssertionsShouldNotBeUsed() {
        ArchRule rule = noClasses()
                .should()
                .accessClassesThat()
                .resideInAnyPackage("org.junit")
                .andShould()
                .accessClassesThat()
                .haveSimpleName("Assert")
                .because("only AssertJ should be used for assertions");
        rule.check(onlyTests);
    }

    @Test
    void junit5AssertionsShouldNotBeUsed() {
        ArchRule rule = noClasses()
                .should()
                .accessClassesThat()
                .resideInAnyPackage("org.junit.jupiter.api")
                .andShould()
                .accessClassesThat()
                .haveSimpleName("Assertions")
                .because("only AssertJ should be used for assertions");
        rule.check(onlyTests);
    }

    @Test
    void hamcrestShouldNotBeUsed() {
        ArchRule rule = noClasses()
                .should()
                .accessClassesThat()
                .resideInAnyPackage("org.hamcrest..")
                .because("Hamcrest matchers should not be used");
        rule.check(onlyTests);
    }

    @Test
    void onlyIntegrationTestShouldUseSpringBootTest() {
        ArchRule rule = noClasses()
                .that().doNotHaveSimpleName("IntegrationTest")
                .should().beAnnotatedWith(SpringBootTest.class);

        rule.check(onlyTests);
    }

    @Test
    void karibuTestsShouldHaveSuffixKT() {
        ArchRule rule = classes()
                .that()
                    .areAssignableTo(KaribuTest.class)
                .and().doNotHaveModifier(ABSTRACT)
                .should().haveSimpleNameEndingWith("KT");

        rule.check(onlyTests);
    }

    @Test
    void browserTestsShouldHaveSuffixBT() {
        ArchRule rule = classes()
                .that()
                    .areAssignableTo(BrowserTest.class)
                .and().doNotHaveModifier(ABSTRACT)
                .should().haveSimpleNameEndingWith("BT");

        rule.check(onlyTests);
    }

    @Test
    void utilityClassesShouldHavePrivateConstructorsThatThrowExceptions() {
        for (final JavaClass javaClass : allClasses) {
            if (javaClass.getSimpleName().endsWith("Util")) {
                final Class<?> clazz = javaClass.reflect();
                for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                    assertThat(Modifier.isPrivate(constructor.getModifiers()))
                            .as("Constructor %s of %s should be private", constructor, clazz.getSimpleName())
                            .isTrue();
                    assertThatThrownBy(() -> {
                        constructor.setAccessible(true);
                        constructor.newInstance();
                    })
                            .isInstanceOf(InvocationTargetException.class)
                            .extracting(Throwable::getCause)
                            .isInstanceOf(IllegalStateException.class)
                            .extracting(Throwable::getMessage, as(STRING))
                            .isEqualTo("Utility class");
                }
            }
        }
    }

    @Test
    void localeGetLanguageShouldOnlyBeUsedInLocaleUtil() {
        final var forbiddenMethod = "getLanguage";
        final var allowedClass = "app.komunumo.util.LocaleUtil";

        final var onlyLocaleUtilMayCallGetLanguage = new ArchCondition<JavaClass>(
                "only LocaleUtil may call Locale.getLanguage()") {
            @Override
            public void check(final @NotNull JavaClass clazz, final @NotNull ConditionEvents events) {
                if (clazz.getName().equals(allowedClass)) {
                    return;
                }

                clazz.getMethodCallsFromSelf().forEach(call -> {
                    final var target = call.getTarget();
                    if (target.getOwner().isEquivalentTo(Locale.class)
                            && target.getName().equals(forbiddenMethod)
                            && target.getRawParameterTypes().isEmpty()) {
                        events.add(SimpleConditionEvent.violated(
                                call,
                                "Forbidden call to Locale.getLanguage() in class " + clazz.getName() +
                                        ": use LocaleUtil.getLanguageCode(Locale) instead"));
                    }
                });
            }
        };

        classes()
                .should(onlyLocaleUtilMayCallGetLanguage)
                .because("Locale.getLanguage() should not be used directly – use LocaleUtil.getLanguageCode() instead")
                .check(allClasses);
    }

}
