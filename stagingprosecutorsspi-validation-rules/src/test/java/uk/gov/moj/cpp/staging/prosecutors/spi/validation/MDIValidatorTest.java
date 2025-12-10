package uk.gov.moj.cpp.staging.prosecutors.spi.validation;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import uk.gov.cjse.schemas.endpoint.types.ExecMode;
import uk.gov.cjse.schemas.endpoint.types.SubmitRequest;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules.DestinationIdValidationRule;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules.ExecModeValidationRule;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules.MDIValidationRule;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules.RequestIdValidationRule;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules.SourceIdValidationRule;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.enterprise.inject.Instance;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MDIValidatorTest {

    private final static String SOURCE_ID = "Z00CJSE";
    private final static String BOO_LIBRA_AS_DESTINATION_ID = "B00LIBRA";
    private final static String CPP_AS_DESTINATION_ID = "C00CommonPlatform";
    private static final String REQUEST_ID = randomUUID().toString();

    private static final int INVALID_SOURCE_ID_CODE = 304;
    private static final String INVALID_SOURCE_ID_TEXT = "WrongSource";
    private static final int INVALID_DEST_ID_CODE = 305;
    private static final String INVALID_DEST_ID_TEXT = "WrongDestination";
    private static final int INVALID_REQUEST_ID_CODE = 306;
    private static final String INVALID_REQUEST_ID_TEXT = "InvalidRequestID";
    private static final int INVALID_EXEC_MODE_CODE = 307;
    private static final String INVALID_EXEC_MODE_TEXT = "ModeError";

    @InjectMocks
    private MDIValidator mdiValidator;

    @Mock
    private Instance<MDIValidationRule> validatorRules;

    @Spy
    private SourceIdValidationRule sourceIdValidationRule = new SourceIdValidationRule(SOURCE_ID);

    @Spy
    private DestinationIdValidationRule destinationIdValidationRule = new DestinationIdValidationRule();

    @Spy
    private RequestIdValidationRule requestIdValidator;

    @Spy
    private ExecModeValidationRule execModeValidationRule;


    private SubmitRequest submitRequest;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        submitRequest = new SubmitRequest();
        submitRequest.setRequestID(REQUEST_ID);
        submitRequest.setSourceID(SOURCE_ID);
        submitRequest.getDestinationID().add(0, BOO_LIBRA_AS_DESTINATION_ID);
        submitRequest.setExecMode(ExecMode.ASYNCH);

        List<MDIValidationRule> validationRules =
                Arrays.asList(sourceIdValidationRule, requestIdValidator, destinationIdValidationRule, execModeValidationRule);

        when(validatorRules.spliterator()).thenReturn(validationRules.spliterator());
        FieldUtils.writeField(destinationIdValidationRule, "cppSystemId", BOO_LIBRA_AS_DESTINATION_ID, true);
        FieldUtils.writeField(destinationIdValidationRule, "cppAsCJSEDestination", CPP_AS_DESTINATION_ID, true);
    }

    @Test
    public void shouldNotErrorWithValidParameters() {
        Optional<ValidationError> mdiValidationError = mdiValidator.validate(submitRequest);
        assertThat(mdiValidationError.isPresent(), is(false));
    }

    @Test
    public void shouldReturnAppropriateErrorWhenSourceIdInvalid() {
        submitRequest.setSourceID("INVALID_SOURCE_ID");
        Optional<ValidationError> mdiValidationError = mdiValidator.validate(submitRequest);
        assertThat(mdiValidationError.isPresent(), is(true));
        assertThat(mdiValidationError.get().getText(), is(INVALID_SOURCE_ID_TEXT));
        assertThat(mdiValidationError.get().getCode(), is(INVALID_SOURCE_ID_CODE));
    }

    @Test
    public void shouldReturnAppropriateErrorWhenDestinationIdInvalid() {
        submitRequest.getDestinationID().set(0, "INVALID_DESTINATION_ID");
        Optional<ValidationError> mdiValidationError = mdiValidator.validate(submitRequest);
        assertThat(mdiValidationError.isPresent(), is(true));
        assertThat(mdiValidationError.get().getText(), is(INVALID_DEST_ID_TEXT));
        assertThat(mdiValidationError.get().getCode(), is(INVALID_DEST_ID_CODE));
    }

    @Test
    public void shouldPassWhenDestinationIdisValid() {
        submitRequest.getDestinationID().set(0, CPP_AS_DESTINATION_ID);
        Optional<ValidationError> mdiValidationError = mdiValidator.validate(submitRequest);
        assertThat(mdiValidationError.isPresent(), is(false));
    }

    @Test
    public void shouldReturnAppropriateErrorWhenRequestIdInvalid() {
        submitRequest.setRequestID("INVALID REQUEST ID");
        Optional<ValidationError> mdiValidationError = mdiValidator.validate(submitRequest);
        assertThat(mdiValidationError.isPresent(), is(true));
        assertThat(mdiValidationError.get().getText(), is(INVALID_REQUEST_ID_TEXT));
        assertThat(mdiValidationError.get().getCode(), is(INVALID_REQUEST_ID_CODE));
    }

    @Test
    public void shouldReturnAppropriateErrorWhenExecModeInvalid() {
        submitRequest.setExecMode(ExecMode.SYNCH);
        Optional<ValidationError> mdiValidationError = mdiValidator.validate(submitRequest);
        assertThat(mdiValidationError.isPresent(), is(true));
        assertThat(mdiValidationError.get().getText(), is(INVALID_EXEC_MODE_TEXT));
        assertThat(mdiValidationError.get().getCode(), is(INVALID_EXEC_MODE_CODE));
    }

}
