package uk.gov.moj.cpp.staging.prosecutorapi.query.api.accesscontrol;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class QuerySubmissionTest extends BaseDroolsAccessControlTest {

    private static final String STAGING_PROSECUTORS_QUERY_SUBMISSION = "stagingprosecutorsspi.query.cpp-message";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public QuerySubmissionTest() {
        super("QUERY_API_SESSION");
    }

    @Test
    public void shouldAllowAuthorisedUserToQuerySubmission() {
        final Action action = createActionFor(STAGING_PROSECUTORS_QUERY_SUBMISSION);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, RuleConstants.getSystemUsersAndCJSEGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToQuerySubmission() {
        final Action action = createActionFor(STAGING_PROSECUTORS_QUERY_SUBMISSION);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }



    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return Collections.singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

}
