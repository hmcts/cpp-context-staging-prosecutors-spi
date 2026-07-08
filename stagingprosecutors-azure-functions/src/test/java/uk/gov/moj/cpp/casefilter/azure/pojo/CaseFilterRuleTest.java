package uk.gov.moj.cpp.casefilter.azure.pojo;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.moj.cpp.casefilter.azure.pojo.SpiCase.SpiCaseBuilder.aSpiCase;

import java.util.Optional;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

public class CaseFilterRuleTest {

    private static final String MATCHING_COURT_CENTRE = "B45MH";
    private static final String INITIATION_CODE = "C";

    @Test
    public void shouldFallBackToExactEqualsWhenEitherDigitPrefixedProsecutorCodeShorterThanThreeChars() {
        assertProsecutorMatch("2", "2", true);
        assertProsecutorMatch("20", "20", true);
        assertProsecutorMatch("2", "3", false);
        assertProsecutorMatch("20", "21", false);
        assertProsecutorMatch("2", "203", false);
        assertProsecutorMatch("20", "20345", false);
        assertProsecutorMatch("203", "20", false);
        assertProsecutorMatch("203", "2", false);
    }

    @Test
    public void shouldKeepSubstringSemanticsForDigitPrefixedProsecutorCodesLengthThreeOrMore() {
        assertProsecutorMatch("203", "203", true);
        assertProsecutorMatch("203", "903", true);
        assertProsecutorMatch("20399", "203", true);
        assertProsecutorMatch("203", "2031234", true);
        assertProsecutorMatch("213", "203", false);
        assertProsecutorMatch("523", "203", false);
    }

    @Test
    public void shouldUseExactEqualsForAlphaPrefixedSpiProsecutorCodesOfAnyLength() {
        assertProsecutorMatch("A", "A", true);
        assertProsecutorMatch("A", "B", false);
        assertProsecutorMatch("AB", "AB", true);
        assertProsecutorMatch("A45AA00", "A45AA00", true);
        assertProsecutorMatch("A45AA00", "A45AA99", false);
    }

    @Test
    public void shouldNotMatchMixedDigitAndAlphaPrefixedProsecutorCodes() {
        assertProsecutorMatch("203", "A03", false);
        assertProsecutorMatch("2", "A5X", false);
        assertProsecutorMatch("A03", "203", false);
    }

    @Test
    public void shouldMatchEmptyProsecutorCodesViaEqualsWithoutThrowing() {
        assertProsecutorMatch("", "", true);
        assertProsecutorMatch("", "203", false);
    }

    @Test
    public void shouldFallBackToExactEqualsInMatchOUCODEWhenEitherCodeShorterThanThreeChars() {
        final Logger logger = mock(Logger.class);
        assertThat(ruleWithProsecutor("203").matchOUCODE("2", logger), is(false));
        assertThat(ruleWithProsecutor("203").matchOUCODE("20", logger), is(false));
        assertThat(ruleWithProsecutor("20").matchOUCODE("20", logger), is(true));
        assertThat(ruleWithProsecutor("05").matchOUCODE("056CH00", logger), is(false));
        assertThat(ruleWithProsecutor("05").matchOUCODE("05", logger), is(true));
        assertThat(ruleWithProsecutor("").matchOUCODE("ABC", logger), is(false));
    }

    @Test
    public void shouldKeepSubstringSemanticsInMatchOUCODEForLengthThreeOrMore() {
        final Logger logger = mock(Logger.class);
        assertThat(ruleWithProsecutor("903").matchOUCODE("203", logger), is(true));
        assertThat(ruleWithProsecutor("903").matchOUCODE("213", logger), is(false));
        assertThat(ruleWithProsecutor("203").matchOUCODE("103", logger), is(true));
        assertThat(ruleWithProsecutor("203").matchOUCODE("523", logger), is(false));
        assertThat(ruleWithProsecutor("ABCD").matchOUCODE("XBCZ", logger), is(true));
    }

    @Test
    public void shouldReturnFalseAndLogWhenMatchOUCODEGivenBlankOucode() {
        final Logger logger = mock(Logger.class);
        final CaseFilterRule rule = ruleWithProsecutor("203");
        assertThat(rule.matchOUCODE(null, logger), is(false));
        assertThat(rule.matchOUCODE("", logger), is(false));
        assertThat(rule.matchOUCODE("   ", logger), is(false));
        verify(logger, times(3)).info("CaseFilterRule: oucode is blank");
    }

    private void assertProsecutorMatch(final String spiProsecutorCode, final String ruleProsecutorCode, final boolean expected) {
        final SpiCase spiCase = aSpiCase()
                .withCourtCentreOUCode(MATCHING_COURT_CENTRE)
                .withProsecutorOUCode(spiProsecutorCode)
                .withCaseInitiationCode(INITIATION_CODE)
                .withUrn("ABCDEF22")
                .withSummonsCode(Optional.empty())
                .build();
        assertThat("spi=[" + spiProsecutorCode + "] rule=[" + ruleProsecutorCode + "]",
                ruleWithProsecutor(ruleProsecutorCode).match(spiCase), is(expected));
    }

    private CaseFilterRule ruleWithProsecutor(final String prosecutorOUCode) {
        return new CaseFilterRule(MATCHING_COURT_CENTRE, null, prosecutorOUCode, INITIATION_CODE, "", null, null, null, "Yes");
    }
}
