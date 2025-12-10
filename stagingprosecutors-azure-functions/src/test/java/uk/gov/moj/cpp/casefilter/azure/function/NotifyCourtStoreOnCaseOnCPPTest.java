package uk.gov.moj.cpp.casefilter.azure.function;

import com.microsoft.azure.functions.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.gov.moj.cpp.casefilter.azure.exception.CourtStoreException;
import uk.gov.moj.cpp.casefilter.azure.service.NotifyCourtStoreService;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class NotifyCourtStoreOnCaseOnCPPTest {

    private static final String CASE_URN = "ABCD1234";
    private NotifyCourtStoreOnCaseOnCPP notifyCourtStoreOnCaseOnCPP;
    private ExecutionContext context;
    private NotifyCourtStoreService notifyCourtStoreService;
    private Logger logger;

    @BeforeEach
    public void setup() {
        notifyCourtStoreOnCaseOnCPP = new NotifyCourtStoreOnCaseOnCPP();
        notifyCourtStoreService = mock(NotifyCourtStoreService.class);
        notifyCourtStoreOnCaseOnCPP.setNotifyCourtStoreService(notifyCourtStoreService);
        context = mock(ExecutionContext.class);
        logger = mock(Logger.class);
        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    public void shouldVerifyNotifyCourtStoreOnCaseOnCpp() throws Exception {
        doNothing().when(notifyCourtStoreService).relayCase(Mockito.anyString(), Mockito.any(Logger.class));
        notifyCourtStoreOnCaseOnCPP.notifyCourtStoreOnCaseOnCpp(CASE_URN, context);
        verify(notifyCourtStoreService).relayCase(anyString(), isA(Logger.class));
    }

    @Test
    public void shouldVerifyNotifyCourtStoreOnCaseOnCppFailure() throws Exception {
        doThrow(new CourtStoreException()).when(notifyCourtStoreService).relayCase(Mockito.anyString(), Mockito.any(Logger.class));
        assertThrows(CourtStoreException.class, () -> notifyCourtStoreOnCaseOnCPP.notifyCourtStoreOnCaseOnCpp(CASE_URN, context));
    }

}
