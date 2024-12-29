package eu.ciechanowiec.sling.rocket.llm;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Optional;

@ToString
@Slf4j
@SuppressWarnings("ClassWithTooManyFields")
@Getter(AccessLevel.PACKAGE)
class LLMConfigObfuscated {

    private final String llmAPIurl;
    @ToString.Exclude
    private final String llmAPIBearerToken;
    private final String llmModel;
    @Getter(AccessLevel.NONE)
    private final int llmMaxTokens;
    @Getter(AccessLevel.NONE)
    private final int llmMaxCompletionTokens;
    private final float llmTemperature;
    private final float llmFrequencyPenalty;
    private final float llmTopP;

    LLMConfigObfuscated(LLMConfig llmConfig) {
        this.llmAPIurl = llmConfig.llm_api_url();
        this.llmAPIBearerToken = llmConfig.llm_api_bearer_token();
        this.llmModel = llmConfig.llm_model();
        this.llmMaxTokens = llmConfig.llm_max__tokens();
        this.llmMaxCompletionTokens = llmConfig.llm_max__completion__tokens();
        this.llmTemperature = llmConfig.llm_temperature();
        this.llmFrequencyPenalty = llmConfig.llm_frequency__penalty();
        this.llmTopP = llmConfig.llm_top__p();
        log.info("Initialized {}", this);
    }

    Optional<Integer> llmMaxTokens() {
        return llmMaxTokens == NumberUtils.INTEGER_ZERO ? Optional.empty() : Optional.of(llmMaxTokens);
    }

    Optional<Integer> llmMaxCompletionTokens() {
        return llmMaxCompletionTokens == NumberUtils.INTEGER_ZERO ?
                Optional.empty() : Optional.of(llmMaxCompletionTokens);
    }
}
