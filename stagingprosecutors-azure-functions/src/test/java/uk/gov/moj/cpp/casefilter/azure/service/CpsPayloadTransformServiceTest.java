package uk.gov.moj.cpp.casefilter.azure.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.casefilter.azure.exception.MissingFieldException;
import uk.gov.moj.cpp.casefilter.azure.utils.DateTimeProvider;

import javax.json.JsonObject;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.casefilter.azure.utils.FileUtil.getPathValue;
import static uk.gov.moj.cpp.casefilter.azure.utils.FileUtils.getPayload;

@ExtendWith(MockitoExtension.class)
public class CpsPayloadTransformServiceTest {

    private static final String NOTIFICATION_DATE_TIME_STR = "2001-12-17T09:30:47Z";

    @Mock
    private DateTimeProvider dateTimeProvider;

    private final CpsPayloadTransformService target = new CpsPayloadTransformService();

    @BeforeEach
    public void setUp() {
        setField(target, "dateTimeProvider", dateTimeProvider);
        when(dateTimeProvider.getUTCZonedDateTimeString()).thenReturn(NOTIFICATION_DATE_TIME_STR);
    }

    @Test
    public void shouldTransformSuccessfully() {
        final JsonObject inputObject = getJsonObject("NotificationInputPayload.json");
        final JsonObject transformedObject = target.transform(inputObject);

        assertThat(getPathValue(transformedObject, "$.notificationDate").get(), is(NOTIFICATION_DATE_TIME_STR));
        assertThat(getPathValue(transformedObject, "$.notificationType").get(), is("defence-disclosure"));

        assertThat(getPathValue(transformedObject, "$.materialNotification.materialId"), is(getPathValue(inputObject, "subjectDetails.material")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.materialType"), is(getPathValue(inputObject, "subjectDetails.materialType")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.materialContentType"), is(getPathValue(inputObject, "subjectDetails.materialContentType")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.materialName"), is(getPathValue(inputObject, "subjectDetails.materialName")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.fileName"), is(getPathValue(inputObject, "subjectDetails.fileName")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.courtApplicationSubject.courtApplicationId"), is(getPathValue(inputObject, "subjectDetails.courtApplicationSubject.courtApplicationId")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.case[0].caseURN"), is(getPathValue(inputObject, "subjectDetails.prosecutionCaseSubject.caseUrn")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.case[0].prosecutingAuthority"), is(getPathValue(inputObject, "subjectDetails.prosecutionCaseSubject.prosecutingAuthority")));

        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[0].asn"), is(getPathValue(inputObject, "subjectDetails.prosecutionCaseSubject.defendantSubject.asn")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[0].cpsPersonDefendantDetails.title"), is(getPathValue(inputObject, "subjectDetails.prosecutionCaseSubject.defendantSubject.cpsPersonDefendantDetails.title")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[0].cpsPersonDefendantDetails.forename"), is(getPathValue(inputObject, "subjectDetails.prosecutionCaseSubject.defendantSubject.cpsPersonDefendantDetails.forename")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[0].cpsPersonDefendantDetails.forename2"), is(getPathValue(inputObject, "subjectDetails.prosecutionCaseSubject.defendantSubject.cpsPersonDefendantDetails.forename2")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[0].cpsPersonDefendantDetails.forename3"), is(getPathValue(inputObject, "subjectDetails.prosecutionCaseSubject.defendantSubject.cpsPersonDefendantDetails.forename3")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[0].cpsPersonDefendantDetails.surname"), is(getPathValue(inputObject, "subjectDetails.prosecutionCaseSubject.defendantSubject.cpsPersonDefendantDetails.surname")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[0].cpsPersonDefendantDetails.dateOfBirth"), is(getPathValue(inputObject, "subjectDetails.prosecutionCaseSubject.defendantSubject.cpsPersonDefendantDetails.dateOfBirth")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[0].cpsOrganisationDefendantDetails.organisationName"), is(getPathValue(inputObject, "subjectDetails.prosecutionCaseSubject.defendantSubject.cpsOrganisationDefendantDetails.organisationName")));
    }
    @Test
    public void shouldTransformSuccessfullyForNows() {
        final JsonObject inputObject = getJsonObject("NotificationInputPayloadNows.json");
        final JsonObject transformedObject = target.transform(inputObject);

        assertThat(getPathValue(transformedObject, "$.notificationDate").get(), is(NOTIFICATION_DATE_TIME_STR));
        assertThat(getPathValue(transformedObject, "$.notificationType").get(), is("defence-disclosure"));

        assertThat(getPathValue(transformedObject, "$.materialNotification.materialId"), is(getPathValue(inputObject, "subjectDetails.material")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.materialType"), is(getPathValue(inputObject, "subjectDetails.materialType")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.materialContentType"), is(getPathValue(inputObject, "subjectDetails.materialContentType")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.materialName"), is(getPathValue(inputObject, "subjectDetails.materialName")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.fileName"), is(getPathValue(inputObject, "subjectDetails.fileName")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.courtApplicationSubject.courtApplicationId"), is(getPathValue(inputObject, "subjectDetails.courtApplicationSubject.courtApplicationId")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.case[0].caseURN"), is(getPathValue(inputObject, "cases[0].caseUrn")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.case[0].prosecutingAuthority"), is(getPathValue(inputObject, "cases[0].prosecutingAuthority")));

        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[0].asn"), is(getPathValue(inputObject, "subjectDetails.prosecutionCaseSubject.defendantSubject.asn")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[0].cpsPersonDefendantDetails.title"), is(getPathValue(inputObject, "subjectDetails.prosecutionCaseSubject.defendantSubject.cpsPersonDefendantDetails.title")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[0].cpsPersonDefendantDetails.forename"), is(getPathValue(inputObject, "subjectDetails.prosecutionCaseSubject.defendantSubject.cpsPersonDefendantDetails.forename")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[0].cpsPersonDefendantDetails.forename2"), is(getPathValue(inputObject, "subjectDetails.prosecutionCaseSubject.defendantSubject.cpsPersonDefendantDetails.forename2")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[0].cpsPersonDefendantDetails.forename3"), is(getPathValue(inputObject, "subjectDetails.prosecutionCaseSubject.defendantSubject.cpsPersonDefendantDetails.forename3")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[0].cpsPersonDefendantDetails.surname"), is(getPathValue(inputObject, "subjectDetails.prosecutionCaseSubject.defendantSubject.cpsPersonDefendantDetails.surname")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[0].cpsPersonDefendantDetails.dateOfBirth"), is(getPathValue(inputObject, "subjectDetails.prosecutionCaseSubject.defendantSubject.cpsPersonDefendantDetails.dateOfBirth")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[0].cpsOrganisationDefendantDetails.organisationName"), is(getPathValue(inputObject, "subjectDetails.prosecutionCaseSubject.defendantSubject.cpsOrganisationDefendantDetails.organisationName")));
    }
    @Test
    public void shouldTransformSuccessfullyForPTPH() {
        final JsonObject inputObject = getJsonObject("NotificationInputPayloadForPTPHFinalised.json");
        final JsonObject transformedObject = target.transform(inputObject);

        assertThat(getPathValue(transformedObject, "$.notificationDate").get(), is(NOTIFICATION_DATE_TIME_STR));
        assertThat(getPathValue(transformedObject, "$.notificationType").get(), is("defence-disclosure"));

        assertThat(getPathValue(transformedObject, "$.materialNotification.materialId"), is(getPathValue(inputObject, "subjectDetails.material")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.materialType"), is(getPathValue(inputObject, "subjectDetails.materialType")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.materialContentType"), is(getPathValue(inputObject, "subjectDetails.materialContentType")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.materialName"), is(getPathValue(inputObject, "subjectDetails.materialName")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.fileName"), is(getPathValue(inputObject, "subjectDetails.fileName")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.courtApplicationSubject.courtApplicationId"), is(getPathValue(inputObject, "subjectDetails.courtApplicationSubject.courtApplicationId")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.case[0].caseURN"), is(getPathValue(inputObject, "cases[0].caseUrn")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.case[0].prosecutingAuthority"), is(getPathValue(inputObject, "cases[0].prosecutingAuthority")));

        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[0].asn"), is(getPathValue(inputObject, "additionalDefendantSubject[0].asn")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[0].cpsPersonDefendantDetails.title"), is(getPathValue(inputObject, "additionalDefendantSubject[0].cpsPersonDefendantDetails.title")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[0].cpsPersonDefendantDetails.forename"), is(getPathValue(inputObject, "additionalDefendantSubject[0].cpsPersonDefendantDetails.forename")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[0].cpsPersonDefendantDetails.forename2"), is(getPathValue(inputObject, "additionalDefendantSubject[0].cpsPersonDefendantDetails.forename2")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[0].cpsPersonDefendantDetails.forename3"), is(getPathValue(inputObject, "additionalDefendantSubject[0].cpsPersonDefendantDetails.forename3")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[0].cpsPersonDefendantDetails.surname"), is(getPathValue(inputObject, "additionalDefendantSubject[0].cpsPersonDefendantDetails.surname")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[0].cpsPersonDefendantDetails.dateOfBirth"), is(getPathValue(inputObject, "additionalDefendantSubject[0].cpsPersonDefendantDetails.dateOfBirth")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[0].cpsOrganisationDefendantDetails.organisationName"), is(getPathValue(inputObject, "additionalDefendantSubject[0].cpsOrganisationDefendantDetails.organisationName")));

        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[1].asn"), is(getPathValue(inputObject, "additionalDefendantSubject[1].asn")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[1].cpsPersonDefendantDetails.title"), is(getPathValue(inputObject, "additionalDefendantSubject[1].cpsPersonDefendantDetails.title")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[1].cpsPersonDefendantDetails.forename"), is(getPathValue(inputObject, "additionalDefendantSubject[1].cpsPersonDefendantDetails.forename")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[1].cpsPersonDefendantDetails.forename2"), is(getPathValue(inputObject, "additionalDefendantSubject[1].cpsPersonDefendantDetails.forename2")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[1].cpsPersonDefendantDetails.forename3"), is(getPathValue(inputObject, "additionalDefendantSubject[1].cpsPersonDefendantDetails.forename3")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[1].cpsPersonDefendantDetails.surname"), is(getPathValue(inputObject, "additionalDefendantSubject[1].cpsPersonDefendantDetails.surname")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[1].cpsPersonDefendantDetails.dateOfBirth"), is(getPathValue(inputObject, "additionalDefendantSubject[1].cpsPersonDefendantDetails.dateOfBirth")));
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[1].cpsOrganisationDefendantDetails.organisationName"), is(getPathValue(inputObject, "additionalDefendantSubject[1].cpsOrganisationDefendantDetails.organisationName")));

    }
    @Test
    public void shouldTransformCourtOrdersSuccessfully() {
        final JsonObject inputObject = getJsonObject("NotificationInputCourtOrderPayload.json");
        final JsonObject transformedObject = target.transform(inputObject);
        assertThat(getPathValue(transformedObject, "$.notificationType").get(), is("court-now-created"));
    }

    @Test
    public void shouldTransformSuccessfullyWithCpsDefendantId() {
        final JsonObject inputObject = getJsonObject("NotificationInputCpsDefendantIdPayload.json");
        final JsonObject transformedObject = target.transform(inputObject);
        assertThat(getPathValue(transformedObject, "$.materialNotification.prosecutionCaseSubject.defendantSubject[0].cpsDefendantId"), is(getPathValue(inputObject, "subjectDetails.prosecutionCaseSubject.defendantSubject.cpsDefendantId")));
    }

    @Test
    public void shouldFailWhenRequiredMaterialFieldsMissing() {
        final JsonObject inputObject = getJsonObject("NotificationFailureInputPayload.json");
        assertThrows(MissingFieldException.class, () -> target.transform(inputObject));
    }

    private JsonObject getJsonObject(final String fileName) {
        final String payload = getPayload(fileName);
        return new StringToJsonObjectConverter().convert(payload);
    }
}