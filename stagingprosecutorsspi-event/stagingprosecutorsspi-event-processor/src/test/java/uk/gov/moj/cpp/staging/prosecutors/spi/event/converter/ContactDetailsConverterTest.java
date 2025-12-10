package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.BasePersonDetail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ContactDetailsConverterTest {

    @Mock
    private BasePersonDetail basePersonDetails;

    @InjectMocks
    private ContactDetailsConverter contactDetailsConverter;

    private static final String BUSSINESS_TELEPHONE = "987xxx";
    private static final String HOME_TELEPHONE = "987xxx";
    private static final String MOBILE_NO = "987xxx";
    private static final String EMAIL1 = "email1";
    private static final String EMAIL2 = "email2";


    @Test
    public void convert() {
        when(basePersonDetails.getTelephoneNumberBusiness()).thenReturn(BUSSINESS_TELEPHONE);
        when(basePersonDetails.getTelephoneNumberHome()).thenReturn(HOME_TELEPHONE);
        when(basePersonDetails.getTelephoneNumberMobile()).thenReturn(MOBILE_NO);
        when(basePersonDetails.getEmailAddress1()).thenReturn(EMAIL1);
        when(basePersonDetails.getEmailAddress2()).thenReturn(EMAIL2);

        ContactDetails contactDetails = contactDetailsConverter.convert(basePersonDetails);
        assertNotNull(contactDetails);
        assertThat(contactDetails.getWork(), is(BUSSINESS_TELEPHONE));
        assertThat(contactDetails.getMobile(), is(MOBILE_NO));
        assertThat(contactDetails.getHome(), is(HOME_TELEPHONE));
        assertThat(contactDetails.getPrimaryEmail(), is(EMAIL1));
        assertThat(contactDetails.getSecondaryEmail(), is(EMAIL2));
    }
}

