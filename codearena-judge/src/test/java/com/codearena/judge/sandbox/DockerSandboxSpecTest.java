package com.codearena.judge.sandbox;

import com.codearena.judge.sandbox.DockerSandbox.LanguageSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guards the language matrix: every supported language must resolve to a spec
 * with a sandbox image and a run command, compiled languages must carry a compile
 * command, interpreted ones must not, and unsupported languages must be rejected.
 */
class DockerSandboxSpecTest {

    @ParameterizedTest
    @ValueSource(strings = {"JAVA", "CPP", "C", "PYTHON", "JAVASCRIPT", "GO", "RUST", "KOTLIN"})
    void everySupportedLanguageHasImageAndRunCommand(String language) {
        LanguageSpec spec = DockerSandbox.getLanguageSpec(language);
        assertThat(spec.image()).isNotBlank();
        assertThat(spec.runCmd()).isNotEmpty();
        assertThat(spec.sourceFile()).isNotBlank();
    }

    @Test
    void languageLookupIsCaseInsensitive() {
        assertThat(DockerSandbox.getLanguageSpec("python").image())
                .isEqualTo(DockerSandbox.getLanguageSpec("PYTHON").image());
    }

    @Test
    void compiledLanguagesHaveACompileStep() {
        for (String lang : new String[]{"JAVA", "CPP", "C", "GO", "RUST", "KOTLIN"}) {
            assertThat(DockerSandbox.getLanguageSpec(lang).compileCmd())
                    .as("%s should compile", lang)
                    .isNotNull();
        }
    }

    @Test
    void interpretedLanguagesHaveNoCompileStep() {
        assertThat(DockerSandbox.getLanguageSpec("PYTHON").compileCmd()).isNull();
        assertThat(DockerSandbox.getLanguageSpec("JAVASCRIPT").compileCmd()).isNull();
    }

    @Test
    void cFamilyUsesGccImage() {
        assertThat(DockerSandbox.getLanguageSpec("CPP").image()).isEqualTo("gcc:13");
        assertThat(DockerSandbox.getLanguageSpec("C").image()).isEqualTo("gcc:13");
    }

    @Test
    void javaUsesTemurinAndKotlinUsesItsOwnImage() {
        // Regression guard: openjdk:21-slim doesn't exist; Java must use Temurin.
        assertThat(DockerSandbox.getLanguageSpec("JAVA").image()).isEqualTo("eclipse-temurin:21-jdk-alpine");
        // Kotlin needs the custom kotlinc image, not a stock JDK.
        assertThat(DockerSandbox.getLanguageSpec("KOTLIN").image()).isEqualTo("codearena-kotlin:21");
    }

    @Test
    void unsupportedLanguageIsRejected() {
        assertThatThrownBy(() -> DockerSandbox.getLanguageSpec("COBOL"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
