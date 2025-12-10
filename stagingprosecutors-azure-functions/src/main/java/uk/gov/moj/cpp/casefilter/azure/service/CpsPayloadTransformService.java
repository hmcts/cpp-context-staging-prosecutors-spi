package uk.gov.moj.cpp.casefilter.azure.service;

import uk.gov.moj.cpp.casefilter.azure.utils.DateTimeProvider;

import javax.json.*;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.moj.cpp.casefilter.azure.utils.ExceptionProvider.generateMissingFieldException;
import static uk.gov.moj.cpp.casefilter.azure.utils.FileUtil.getPathValue;

import uk.gov.moj.cpp.casefilter.azure.utils.DateTimeProvider;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

public class CpsPayloadTransformService {

    public static final String CASES = "cases";
    public static final String ADDITIONAL_DEFENDANT_SUBJECT = "additionalDefendantSubject";
    private final DateTimeProvider dateTimeProvider = new DateTimeProvider();

    public JsonObject transform(final JsonObject sourcePayload) {
        return createObjectBuilder()
                .add("notificationDate", dateTimeProvider.getUTCZonedDateTimeString())
                .add("notificationType", getNotificationType(sourcePayload))
                .add("materialNotification", getMaterialNotificationObject(sourcePayload))
                .build();
    }

    private String getNotificationType(JsonObject sourcePayload) {
        String notificationType = "defence-disclosure";
        if (sourcePayload.get("notificationType") != null) {
            notificationType = sourcePayload.getString("notificationType");
        }
        return notificationType;
    }

    private JsonObjectBuilder getMaterialNotificationObject(final JsonObject sourcePayload) {
        final JsonObjectBuilder objectBuilder = createObjectBuilder();
        // throwing exception for missing mandatory fields
        objectBuilder
                .add("materialId", getPathValue(sourcePayload, "subjectDetails.material").orElseThrow(generateMissingFieldException("subjectDetails.material")))
                .add("materialType", getPathValue(sourcePayload, "subjectDetails.materialType").orElseThrow(generateMissingFieldException("subjectDetails.materialType")))
                .add("materialContentType", getPathValue(sourcePayload, "subjectDetails.materialContentType").orElseThrow(generateMissingFieldException("subjectDetails.materialContentType")))
                .add("materialName", getPathValue(sourcePayload, "subjectDetails.materialName").orElseThrow(generateMissingFieldException("subjectDetails.materialName")))
                .add("fileName", getPathValue(sourcePayload, "subjectDetails.fileName").orElseThrow(generateMissingFieldException("subjectDetails.fileName")));

        final Optional<JsonObjectBuilder> courtApplicationSubject = getCourtApplicationSubject(sourcePayload);
        final Optional<JsonObjectBuilder> prosecutionCaseSubject = getProsecutionCaseSubject(sourcePayload);

        courtApplicationSubject.ifPresent(jsonObjectBuilder -> objectBuilder.add("courtApplicationSubject", jsonObjectBuilder));
        prosecutionCaseSubject.ifPresent(jsonObjectBuilder -> objectBuilder.add("prosecutionCaseSubject", jsonObjectBuilder));

        return objectBuilder;
    }

    private Optional<JsonObjectBuilder> getCourtApplicationSubject(final JsonObject sourcePayload) {
        final Optional<String> fieldValue = getPathValue(sourcePayload, "subjectDetails.courtApplicationSubject.courtApplicationId");
        return fieldValue.map(s -> createObjectBuilder().add("courtApplicationId", s));
    }

    private Optional<JsonObjectBuilder> getProsecutionCaseSubject(final JsonObject sourcePayload) {
        final JsonObjectBuilder objectBuilder = createObjectBuilder();
        final Optional<JsonArrayBuilder> defendantSubject = getDefendantSubject(sourcePayload);
        final JsonArrayBuilder cases = Json.createArrayBuilder();
        if (Objects.nonNull(sourcePayload.get(CASES)) && !sourcePayload.getJsonArray(CASES).isEmpty()) {
            JsonArray casesFromNotification = sourcePayload.getJsonArray(CASES);
            IntStream.range(0, casesFromNotification.size()).mapToObj(caseCounter -> casesFromNotification.getJsonObject(caseCounter)).forEach(caseFromNotification ->
                    buildDefendanSubjectCaseseFromNotification(cases, caseFromNotification)
            );
        } else {
            final Optional<String> caseUrn = getPathValue(sourcePayload, "subjectDetails.prosecutionCaseSubject.caseUrn");
            final Optional<String> prosecutingAuthority = getPathValue(sourcePayload, "subjectDetails.prosecutionCaseSubject.prosecutingAuthority");
            final JsonObjectBuilder caseBuilder = createObjectBuilder();
            caseUrn.ifPresent(s -> caseBuilder.add("caseURN", s));
            prosecutingAuthority.ifPresent(s -> caseBuilder.add("prosecutingAuthority", s));
            final JsonObject caseJsonObject = caseBuilder.build();
            if (!caseJsonObject.keySet().isEmpty()) {
                cases.add(caseJsonObject);
            }
            if (!caseUrn.isPresent() && !prosecutingAuthority.isPresent() && !defendantSubject.isPresent()) {
                return empty();
            }
        }
        final JsonArray caseArray = cases.build();
        if (!caseArray.isEmpty()) {
            objectBuilder.add("case", caseArray);
        }

        defendantSubject.ifPresent(jsonArrayBuilder -> objectBuilder.add("defendantSubject", jsonArrayBuilder));
        return of(objectBuilder);
    }

    private void buildDefendanSubjectCaseseFromNotification(final JsonArrayBuilder cases, final JsonObject caseFromNotification) {
        final JsonObjectBuilder caseBuilder = createObjectBuilder();
        final JsonValue caseUrnFromNotification = caseFromNotification.get("caseUrn");
        if (Objects.nonNull(caseUrnFromNotification)) {
            caseBuilder.add("caseURN", caseUrnFromNotification);
        }
        final JsonValue caseProsecutingAuthorityFromNotification = caseFromNotification.get("prosecutingAuthority");
        if (Objects.nonNull(caseProsecutingAuthorityFromNotification)) {
            caseBuilder.add("prosecutingAuthority", caseProsecutingAuthorityFromNotification);
        }
        final JsonObject caseJsonObject = caseBuilder.build();
        if (!caseJsonObject.keySet().isEmpty()) {
            cases.add(caseJsonObject);
        }
    }

    private Optional<JsonArrayBuilder> getDefendantSubject(final JsonObject sourcePayload) {
        return getDefendantAndOrganisation(sourcePayload);
    }

    private Optional<JsonArrayBuilder> getDefendantAndOrganisation(final JsonObject sourcePayload) {
        final JsonArrayBuilder defendantSubjectArrayBuilder = createArrayBuilder();

        if (Objects.nonNull(sourcePayload.get(ADDITIONAL_DEFENDANT_SUBJECT)) && !sourcePayload.getJsonArray(ADDITIONAL_DEFENDANT_SUBJECT).isEmpty()) {
            JsonArray additionalDefendantSubjects = sourcePayload.getJsonArray(ADDITIONAL_DEFENDANT_SUBJECT);
            IntStream.range(0, additionalDefendantSubjects.size()).mapToObj(caseCounter -> additionalDefendantSubjects.getJsonObject(caseCounter)).forEach(additionalDefendantSubject ->
                    defendantSubjectArrayBuilder.add(mapAdditionalDefendantSubject(additionalDefendantSubject))
            );
        }else {
            final JsonObjectBuilder objectBuilder = createObjectBuilder();
            final Optional<String> cpsDefendantId = getPathValue(sourcePayload, "subjectDetails.prosecutionCaseSubject.defendantSubject.cpsDefendantId");
            final Optional<String> asn = getPathValue(sourcePayload, "subjectDetails.prosecutionCaseSubject.defendantSubject.asn");
            final Optional<JsonObjectBuilder> personDefendantDetails = getPersonDefendantDetails(sourcePayload);
            final Optional<JsonObjectBuilder> organisationDefendantDetails = getOrganisationDefendantDetails(sourcePayload);

            if (asn.isPresent()) {
                objectBuilder.add("asn", asn.get());
            } else {
                cpsDefendantId.ifPresent(s -> objectBuilder.add("cpsDefendantId", s));
            }

            personDefendantDetails.ifPresent(s -> objectBuilder.add("cpsPersonDefendantDetails", s));
            organisationDefendantDetails.ifPresent(s -> objectBuilder.add("cpsOrganisationDefendantDetails", s));

            if (!cpsDefendantId.isPresent() && !asn.isPresent() && !personDefendantDetails.isPresent() && !organisationDefendantDetails.isPresent()) {
                return empty();
            }
            defendantSubjectArrayBuilder.add(objectBuilder);
        }
        return of(defendantSubjectArrayBuilder);
    }

    private JsonObjectBuilder mapAdditionalDefendantSubject(final JsonObject additionalDefendantSubject) {
        final JsonObjectBuilder objectBuilder = createObjectBuilder();
        final Optional<String> cpsDefendantId = getPathValue(additionalDefendantSubject, "cpsDefendantId");
        final Optional<String> asn = getPathValue(additionalDefendantSubject, "asn");
        final Optional<JsonObjectBuilder> personDefendantDetails = mapDefendantDetails(additionalDefendantSubject);
        final Optional<JsonObjectBuilder> organisationDefendantDetails = mapOrganisationDefendantDetails(additionalDefendantSubject);

        if (asn.isPresent()) {
            objectBuilder.add("asn", asn.get());
        } else {
            cpsDefendantId.ifPresent(s -> objectBuilder.add("cpsDefendantId", s));
        }

        personDefendantDetails.ifPresent(s -> objectBuilder.add("cpsPersonDefendantDetails", s));
        organisationDefendantDetails.ifPresent(s -> objectBuilder.add("cpsOrganisationDefendantDetails", s));
        return objectBuilder;
    }

    private Optional<JsonObjectBuilder> mapDefendantDetails(final JsonObject additionalDefendantSubject) {
        final JsonObjectBuilder objectBuilder = createObjectBuilder();
        final Optional<String> title = getPathValue(additionalDefendantSubject, "cpsPersonDefendantDetails.title");
        final Optional<String> foreName = getPathValue(additionalDefendantSubject, "cpsPersonDefendantDetails.forename");
        final Optional<String> foreName2 = getPathValue(additionalDefendantSubject, "cpsPersonDefendantDetails.forename2");
        final Optional<String> foreName3 = getPathValue(additionalDefendantSubject, "cpsPersonDefendantDetails.forename3");
        final Optional<String> surname = getPathValue(additionalDefendantSubject, "cpsPersonDefendantDetails.surname");
        final Optional<String> dateOfBirth = getPathValue(additionalDefendantSubject, "cpsPersonDefendantDetails.dateOfBirth");

        // These are mandatory fields for PersonDefendantDetails
        if (foreName.isPresent() && surname.isPresent() && dateOfBirth.isPresent()) {
            objectBuilder.add("forename", foreName.get());
            objectBuilder.add("surname", surname.get());
            objectBuilder.add("dateOfBirth", dateOfBirth.get());

            title.ifPresent(s -> objectBuilder.add("title", s));
            foreName2.ifPresent(s -> objectBuilder.add("forename2", s));
            foreName3.ifPresent(s -> objectBuilder.add("forename3", s));

            return of(objectBuilder);
        }
        return empty();
    }

    private Optional<JsonObjectBuilder> getPersonDefendantDetails(final JsonObject sourcePayload) {
        final JsonObjectBuilder objectBuilder = createObjectBuilder();
        final Optional<String> title = getPathValue(sourcePayload, "subjectDetails.prosecutionCaseSubject.defendantSubject.cpsPersonDefendantDetails.title");
        final Optional<String> foreName = getPathValue(sourcePayload, "subjectDetails.prosecutionCaseSubject.defendantSubject.cpsPersonDefendantDetails.forename");
        final Optional<String> foreName2 = getPathValue(sourcePayload, "subjectDetails.prosecutionCaseSubject.defendantSubject.cpsPersonDefendantDetails.forename2");
        final Optional<String> foreName3 = getPathValue(sourcePayload, "subjectDetails.prosecutionCaseSubject.defendantSubject.cpsPersonDefendantDetails.forename3");
        final Optional<String> surname = getPathValue(sourcePayload, "subjectDetails.prosecutionCaseSubject.defendantSubject.cpsPersonDefendantDetails.surname");
        final Optional<String> dateOfBirth = getPathValue(sourcePayload, "subjectDetails.prosecutionCaseSubject.defendantSubject.cpsPersonDefendantDetails.dateOfBirth");

        // These are mandatory fields for PersonDefendantDetails
        if (foreName.isPresent() && surname.isPresent() && dateOfBirth.isPresent()) {
            objectBuilder.add("forename", foreName.get());
            objectBuilder.add("surname", surname.get());
            objectBuilder.add("dateOfBirth", dateOfBirth.get());

            title.ifPresent(s -> objectBuilder.add("title", s));
            foreName2.ifPresent(s -> objectBuilder.add("forename2", s));
            foreName3.ifPresent(s -> objectBuilder.add("forename3", s));

            return of(objectBuilder);
        }
        return empty();
    }

    private Optional<JsonObjectBuilder> getOrganisationDefendantDetails(final JsonObject sourcePayload) {
        final Optional<String> organisationName = getPathValue(sourcePayload, "subjectDetails.prosecutionCaseSubject.defendantSubject.cpsOrganisationDefendantDetails.organisationName");

        return organisationName.map(s -> createObjectBuilder().add("organisationName", s));
    }
    private Optional<JsonObjectBuilder> mapOrganisationDefendantDetails(final JsonObject additionalDefendantSubject) {
        final Optional<String> organisationName = getPathValue(additionalDefendantSubject, "cpsOrganisationDefendantDetails.organisationName");

        return organisationName.map(s -> createObjectBuilder().add("organisationName", s));
    }
}
