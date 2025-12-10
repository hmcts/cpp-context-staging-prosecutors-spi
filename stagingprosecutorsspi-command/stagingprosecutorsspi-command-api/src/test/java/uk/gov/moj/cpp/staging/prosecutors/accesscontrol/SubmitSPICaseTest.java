package uk.gov.moj.cpp.staging.prosecutors.accesscontrol;

import static org.mockito.BDDMockito.given;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class SubmitSPICaseTest extends BaseDroolsAccessControlTest {

    public static final String STAGINGPROSECUTORSSPI_COMMAND_SPI_RECEIVE_PROSECUTION_CASE = "stagingprosecutorsspi.command.spi.receive-prosecution-case";
    public static final String STAGINGPROSECUTORSSPI_COMMAND_SPI_FILTER_PROSECUTION_CASE = "stagingprosecutorsspi.command.spi.filter-prosecution-case";
    private static final String HMCTS_CJS_SJP_PROSECUTION = "hmcts.cjs.receive-spi-message";
    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public SubmitSPICaseTest() {
        super("COMMAND_API_SESSION");
    }

    @Test
    public void shouldAllowAuthorisedUserToSubmitSjpProsecution() {
        final Action action = createActionFor(HMCTS_CJS_SJP_PROSECUTION);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, RuleConstants.getCJSEGroups())).willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToSubmitSjpProsecution() {
        final Action action = createActionFor(HMCTS_CJS_SJP_PROSECUTION);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, RuleConstants.getCJSEGroups())).willReturn(false);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToReceiveProsecutionCase() {
        final Action action = createActionFor(STAGINGPROSECUTORSSPI_COMMAND_SPI_RECEIVE_PROSECUTION_CASE);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToFilterProsecutionCase() {
        final Action action = createActionFor(STAGINGPROSECUTORSSPI_COMMAND_SPI_FILTER_PROSECUTION_CASE);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, RuleConstants.getCJSEGroups())).willReturn(false);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return Collections.singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}
