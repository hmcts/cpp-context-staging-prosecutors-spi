package uk.gov.moj.cpp.casefilter.azure.function;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.casefilter.azure.service.NotifyCourtStoreService;
import uk.gov.moj.cpp.casefilter.azure.utils.FileUtils;

import java.util.logging.Logger;

import com.microsoft.azure.functions.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

public class NotifyCourtStoreOnCaseEjectedTest {

    private NotifyCourtStoreOnCaseEjected notifyCourtStoreOnCaseEjected;
    private ExecutionContext context;
    private NotifyCourtStoreService notifyCourtStoreService;
    private Logger logger;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        notifyCourtStoreOnCaseEjected = new NotifyCourtStoreOnCaseEjected();
        notifyCourtStoreService = mock(NotifyCourtStoreService.class);
        notifyCourtStoreOnCaseEjected.setNotifyCourtStoreService(notifyCourtStoreService);
        context = mock(ExecutionContext.class);
        logger = mock(Logger.class);
        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    public void shouldVerifyNotifyCourtStoreOnCaseEject() throws Exception {

        String eventSchema = FileUtils.getPayload("NotifyCourtStoreOnCaseEjected.json");

        doNothing().when(notifyCourtStoreService).notifyCourtStore(anyString(), isA(Logger.class));
        notifyCourtStoreOnCaseEjected.notifyCourtStoreOnCaseEject(eventSchema, context);
        verify(notifyCourtStoreService).notifyCourtStore(anyString(), isA(Logger.class));
    }

}
