package eu.ciechanowiec.sling.rocket.llm;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static org.junit.jupiter.api.Assertions.*;

class LLMConfigObfuscatedTest {

    @Test
    void testOptionals() {
        LLMConfigObfuscated zeroConfig = new LLMConfigObfuscated(config(0, 0));
        LLMConfigObfuscated nonZeroConfig = new LLMConfigObfuscated(config(100, 10));
        assertAll(
            () -> assertTrue(zeroConfig.llmMaxTokens().isEmpty()),
            () -> assertTrue(zeroConfig.llmMaxCompletionTokens().isEmpty()),
            () -> assertEquals(100, nonZeroConfig.llmMaxTokens().orElseThrow()),
            () -> assertEquals(10, nonZeroConfig.llmMaxCompletionTokens().orElseThrow())
        );
    }

    @SuppressWarnings({"OverlyComplexAnonymousInnerClass", "MethodLength"})
    private LLMConfig config(int llmMaxTokens, int llmMaxCompletionTokens) {
        return new LLMConfig() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return LLMConfig.class;
            }

            @Override
            public String llm_api_url() {
                return "";
            }

            @Override
            public String llm_api_bearer_token() {
                return "";
            }

            @Override
            public String llm_model() {
                return "";
            }

            @Override
            public int llm_max__tokens() {
                return llmMaxTokens;
            }

            @Override
            public int llm_max__completion__tokens() {
                return llmMaxCompletionTokens;
            }

            @Override
            public float llm_frequency__penalty() {
                return 0;
            }

            @Override
            public float llm_temperature() {
                return 0;
            }

            @Override
            public float llm_top__p() {
                return 0;
            }

            @Override
            public int llm_context$_$window_size() {
                return 0;
            }

            @Override
            public String jcr_home() {
                return "";
            }
        };
    }
}
