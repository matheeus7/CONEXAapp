package br.conexa.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** CT-086 (RNF06): os módulos só se enxergam pelas portas; o núcleo de cada um é privado. */
@AnalyzeClasses(packages = "br.conexa", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

    private static ArchRule nucleoPrivado(String componente) {
        return noClasses().that().resideOutsideOfPackage("br.conexa." + componente + "..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "br.conexa." + componente + ".nucleo..", "br.conexa." + componente + ".adaptador..")
                .because("o componente " + componente + " só pode ser usado pela sua porta");
    }

    @ArchTest
    static final ArchRule autenticacaoPrivada = nucleoPrivado("autenticacao");

    @ArchTest
    static final ArchRule areasPrivada = nucleoPrivado("areas");

    @ArchTest
    static final ArchRule conexoesPrivada = nucleoPrivado("conexoes");

    @ArchTest
    static final ArchRule tarefasPrivada = nucleoPrivado("tarefas");

    @ArchTest
    static final ArchRule apresentacaoSoUsaPortas = noClasses().that().resideInAPackage("br.conexa.web..")
            .should().dependOnClassesThat().resideInAnyPackage("..nucleo..", "..adaptador..");

    @ArchTest
    static final ArchRule portasNaoDependemDoNucleo = noClasses().that().resideInAPackage("..porta..")
            .should().dependOnClassesThat().resideInAnyPackage("..nucleo..", "..adaptador..", "..web..");

    @ArchTest
    static final ArchRule nucleoNaoDependeDaWeb = noClasses().that().resideInAPackage("..nucleo..")
            .should().dependOnClassesThat().resideInAPackage("br.conexa.web..");

    @ArchTest
    static final ArchRule semCiclosEntrePortas = classes().that().resideInAPackage("br.conexa.autenticacao.porta..")
            .should().onlyDependOnClassesThat().resideOutsideOfPackage("br.conexa.areas..");
}
