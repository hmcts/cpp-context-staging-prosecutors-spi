package uk.gov.moj.cpp.casefilter.azure.function;

import static com.microsoft.azure.functions.HttpMethod.POST;
import static com.microsoft.azure.functions.HttpStatus.OK;
import static com.microsoft.azure.functions.annotation.AuthorizationLevel.FUNCTION;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringBetween;

import uk.gov.moj.cpp.casefilter.azure.pojo.CJSEMetaData;
import uk.gov.moj.cpp.casefilter.azure.pojo.CJSEMetaDataResponse;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Logger;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

@SuppressWarnings({"squid:S1312", "squid:S3776", "squid:S134", "squid:S1166", "squid:S2221"})
public class CJSEMetaDataFunction {


    private static final String START_TAG = "&amp;gt;";
    private static final String END_TAG = "&amp;lt;/";

    private static final String PTI_URN = "PTIURN";
    private static final String CASE_INITIATION = "CaseInitiation";
    private static final String INITIATION_CODE = "InitiationCode";
    private static final String COURT_HEARING_LOCATION = "CourtHearingLocation";
    private static final String PROSECUTOR_REFERENCE = "OriginatingOrganisation";
    private static final String DATE_OF_HEARING = "DateOfHearing";
    private static final String TIME_OF_HEARING = "TimeOfHearing";
    private static final String SUMMONS_CODE = "SummonsCode";
    private static final String ROUTE_DATA_RESP = "RouteDataResp";


    @FunctionName("CJSEMetaDataFunction")
    @SuppressWarnings({"squid:S2629", "squid:S3457"})
    public HttpResponseMessage cjseMetaData(
            @HttpTrigger(name = "req", methods = {POST}, authLevel = FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        final Logger logger = context.getLogger();
        logger.info("received Byte Message");

        final String body = request.getBody().orElse(null);

        if(nonNull(body) && body.contains(PTI_URN)){
            logger.info("Message body with urn : " + getPropertyValue(body, PTI_URN));
        }else {
            logger.info("empty body");
        }

        final CJSEMetaData cjseMetaData = parseStreamToGetCJSEMetaData(request.getBody(), logger);

        final CJSEMetaDataResponse cjseMetaDataResponse = new CJSEMetaDataResponse(cjseMetaData);
        return request.createResponseBuilder(OK).body(cjseMetaDataResponse).build();
    }

    private CJSEMetaData parseStreamToGetCJSEMetaData(final Optional<String> body, final Logger logger) {
        CJSEMetaData cjseMetaData = null;
        if (body.isPresent()) {
            try {

                if (body.get().contains(ROUTE_DATA_RESP)) {
                    return new CJSEMetaData(false, true);
                }
                final String message = body.get();
                final String caseURN = getPropertyValue(message, PTI_URN);
                final String caseInitiationCode = deriveCaseInitiationCode(message);
                final String summons = getPropertyValue(message, SUMMONS_CODE);
                final String courtCenterOUCode = getPropertyValue(message, COURT_HEARING_LOCATION);
                final String prosecutorOUCode = getPropertyValue(message, PROSECUTOR_REFERENCE);
                final String timeOfHearing = getPropertyValue(message, TIME_OF_HEARING);
                final String dateOfHearing = getPropertyValue(message, DATE_OF_HEARING);

                cjseMetaData = buildCjseMetaData(logger, caseURN, caseInitiationCode, prosecutorOUCode, courtCenterOUCode, timeOfHearing, dateOfHearing, summons);

            } catch (Exception e) {
                logger.log(SEVERE, "Failed to find tags", e);
            }
        }

        if (cjseMetaData == null) {
            cjseMetaData = new CJSEMetaData(true, null);
        }
        return cjseMetaData;
    }

    private String getPropertyValue(final String message, final String tagName) {
        final String startTag = buildStartTagFor(tagName);
        return substringBetween(message, startTag, END_TAG);
    }

    private String deriveCaseInitiationCode(final String body) {
        return Optional.ofNullable(getPropertyValue(body, CASE_INITIATION)).orElse(getPropertyValue(body, INITIATION_CODE));
    }


    private String buildStartTagFor(final String name) {
        return format("%s%s", name, START_TAG);
    }

    private CJSEMetaData buildCjseMetaData(final Logger logger, final String caseURN, final String caseInitiationCode,
                                           final String prosecutorOUCode, final String courtCenterOUCode, final String timeOfHearing,
                                           final String dateOfHearing, final String summons) {

        if (validateFields(caseURN, caseInitiationCode, prosecutorOUCode, courtCenterOUCode, timeOfHearing, dateOfHearing)) {
            return new CJSEMetaData(caseURN, caseInitiationCode, prosecutorOUCode, courtCenterOUCode, timeOfHearing, dateOfHearing, summons);
        } else {
            final String logMessage = format("Case filtered out due to MDI validation failure. caseURN=%s, caseInitiationCode=%s,  prosecutorOUCode=%s, courtCenterOUCode=%s, timeOfHearing=%s, dateOfHearing=%s, summons=%s",
                    caseURN, caseInitiationCode, prosecutorOUCode, courtCenterOUCode, timeOfHearing, dateOfHearing, summons);
            logger.log(WARNING, logMessage);
            return new CJSEMetaData(true, null);
        }

    }

    private boolean validateFields(final String caseURN, final String caseInitiationCode, final String prosecutorOUCode, final String courtCenterOUCode, final String timeOfHearing, final String dateOfHearing) {
        final List<Supplier<Boolean>> conditions =
                asList(() -> isNotBlank(caseURN),
                        () -> isNotBlank(caseInitiationCode),
                        () -> isNotBlank(prosecutorOUCode),
                        () -> isNotBlank(courtCenterOUCode),
                        () -> isNotBlank(timeOfHearing),
                        () -> isNotBlank(dateOfHearing));

        return conditions.stream().allMatch(Supplier::get);
    }
}
