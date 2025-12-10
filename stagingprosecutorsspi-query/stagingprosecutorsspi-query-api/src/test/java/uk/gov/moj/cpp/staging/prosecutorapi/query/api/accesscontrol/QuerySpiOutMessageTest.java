package uk.gov.moj.cpp.staging.prosecutorapi.query.api.accesscontrol;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class QuerySpiOutMessageTest extends BaseDroolsAccessControlTest {

    private static final String STAGING_PROSECUTORS_QUERY_SPI_MESSAGE = "stagingprosecutorsspi.query.spi-out-message";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;


    public QuerySpiOutMessageTest() {
        super("QUERY_API_SESSION");
    }

    @Test
    public void shouldAllowAuthorisedUserToQuerySpiOutMessage() {
        final Action action = createActionFor(STAGING_PROSECUTORS_QUERY_SPI_MESSAGE);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, RuleConstants.getSystemUsers()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToQuerySubmission() {
        final Action action = createActionFor(STAGING_PROSECUTORS_QUERY_SPI_MESSAGE);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}
