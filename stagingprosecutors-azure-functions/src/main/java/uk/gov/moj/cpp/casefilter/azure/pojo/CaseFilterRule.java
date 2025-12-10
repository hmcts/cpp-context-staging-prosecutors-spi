package uk.gov.moj.cpp.casefilter.azure.pojo;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.left;
import static org.apache.commons.lang3.StringUtils.right;
import static org.apache.commons.lang3.StringUtils.stripToNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings({"squid:S3864"})
public class CaseFilterRule {

    public static final int EXPECTED_NUMBER_OF_FILTER_FIELDS = 9;
    private static final String COMMA_SEPARATOR = ",";
    private static final String YES = "Yes";

    public static final String CUSTOM_DATE_FORMAT = "dd/MM/yyyy";
    private static final Pattern FIRST_CHARACTER_AS_DIGIT = Pattern.compile("^\\d.*");
    public static final int COURT_CENTRE_CODE_LENGTH = 5;
    public static final int COURTROOM_CODE_LENGTH = 2;
    private final String courtCentreOUCode;
    private final String prosecutorOUCode;
    private final String caseInitiationCode;
    private final String isLive;
    private final String summonsCode;
    private final Optional<String> urn;
    private final Optional<LocalDateTime> hearingDateTime;
    private final Optional<String> courtRoom;

    public CaseFilterRule(final String courtCentreOUCode,
                          final String courtRoom,
                          final String prosecutorOUCode,
                          final String caseInitiationCode,
                          final String summonsCode,
                          final String urn,
                          final String hearingDate,
                          final String hearingTime,
                          final String isLive) {
        this.courtCentreOUCode = courtCentreOUCode;
        this.prosecutorOUCode = prosecutorOUCode;
        this.caseInitiationCode = caseInitiationCode;
        this.summonsCode = summonsCode;
        this.urn = ofNullable(stripToNull(urn));
        this.hearingDateTime = deriveHearingDateTime(hearingDate, hearingTime);
        this.isLive = isLive;
        this.courtRoom = ofNullable(stripToNull(courtRoom));
    }

    private Optional<LocalDateTime> deriveHearingDateTime(final String hearingDate, final String hearingTime) {
        final LocalTime time = ofNullable(stripToNull(hearingTime)).map(LocalTime::parse).orElse(LocalTime.ofSecondOfDay(0));
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(CUSTOM_DATE_FORMAT);
        return ofNullable(stripToNull(hearingDate))
                .map(date -> LocalDate.parse(date, formatter))
                .map(date -> LocalDateTime.of(date, time));
    }

    public String getIsLive() {
        return isLive;
    }

    public boolean match(final SpiCase spiCase) {
        final List<Supplier<Boolean>> conditions =
                asList(
                        () -> left(spiCase.getCourtCentreOUCode(), COURT_CENTRE_CODE_LENGTH).equals(this.courtCentreOUCode),
                        () -> this.courtRoom.map(a -> a.equals(right(spiCase.getCourtCentreOUCode(), COURTROOM_CODE_LENGTH))).orElse(true),
                        () -> prosecutorMatches(spiCase),
                        () -> spiCase.getCaseInitiationCode().equals(this.caseInitiationCode),
                        () -> spiCase.getSummonsCode().map(a -> a.equals(this.summonsCode)).orElse(true),
                        () -> urn.map(spiCase.getUrn()::equals).orElse(true),
                        () -> this.hearingDateTime.map(a -> spiCase.getHearingDateTime().isAfter(a) || spiCase.getHearingDateTime().isEqual(a))
                                .orElse(true));

        return conditions.stream().allMatch(Supplier::get);
    }

    private boolean prosecutorMatches(final SpiCase spiCase) {
        if (FIRST_CHARACTER_AS_DIGIT.matcher(spiCase.getProsecutorOUCode()).matches()) {
            return FIRST_CHARACTER_AS_DIGIT.matcher(this.prosecutorOUCode).matches() &&
                    spiCase.getProsecutorOUCode().substring(1, 3)
                            .equals(this.prosecutorOUCode.substring(1, 3));
        } else {
            return spiCase.getProsecutorOUCode().equals(this.prosecutorOUCode);
        }
    }

    public boolean matchOUCODE(final String oucode, final Logger logger) {
        if (isNotBlank(oucode)) {
            return oucode.substring(1, 3).equals(this.prosecutorOUCode.substring(1, 3));
        }
        logger.info("CaseFilterRule: oucode is blank");
        return false;
    }

    public static boolean anyRuleMatches(final InputStream inputStream, final SpiCase spiCase, final Logger logger) {
        final List<CaseFilterRule> filters = createFilters(inputStream);
        return filters.stream()
                .filter(andLogMatchedValues(f -> f.match(spiCase), logger))
                .peek(f -> logger.info(() -> format("Matched case filter rule values, CourtCentreCodeOUCode: %s, ProsecutorOUCode: %s, " +
                                "CaseInitiationCode: %s, URN: %s, SummonsCode: %s, DateOfHearing: %s, CourtRoom: %s, IsLive: %s",
                        f.courtCentreOUCode, f.prosecutorOUCode, f.caseInitiationCode, f.urn.orElse(EMPTY),
                        f.summonsCode, f.hearingDateTime.map(LocalDateTime::toString).orElse(EMPTY),
                        f.courtRoom.orElse(EMPTY), f.isLive)))
                .collect(Collectors.collectingAndThen(Collectors.toList(),
                        list -> !list.isEmpty()));
    }

    private static <T> Predicate<T> andLogMatchedValues(final Predicate<T> predicate, final Logger logger) {
        return value -> {
            if (predicate.test(value)) {
                logger.info("CaseFilterRule matched");
                return true;
            } else {
                return false;
            }
        };
    }

    public static List<CaseFilterRule> createFilters(final InputStream inputStream) {
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, UTF_8));
        return bufferedReader
                .lines()
                .skip(1)
                .filter(StringUtils::isNotBlank)
                .map(a -> a.split(COMMA_SEPARATOR))
                .filter(columns -> columns.length >= EXPECTED_NUMBER_OF_FILTER_FIELDS)
                .map(columns -> new CaseFilterRule(columns[0],
                        columns[1], columns[2], columns[3],
                        columns[4], columns[5], columns[6], columns[7], columns[8]))
                .filter(a -> YES.equals(a.getIsLive()))
                .collect(Collectors.toList());
    }

}
