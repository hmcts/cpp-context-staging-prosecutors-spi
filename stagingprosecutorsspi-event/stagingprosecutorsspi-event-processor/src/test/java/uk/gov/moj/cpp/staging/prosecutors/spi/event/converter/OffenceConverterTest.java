package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Gender;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.BaseOffense;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceOffense;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceVictim;

import java.time.LocalDate;
import java.util.Arrays;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OffenceConverterTest {

    private static final String WELSH_OFFENCE_WORDING = "Ar 16/09/2018 yn RHUALLT, SIR DDINBYCH bu i chi yrru cerbyd modur, " +
            "sef AUDI BF16 XOC, ar ffordd ddeuol, sef yr A55, ar gyflymder o fwy na 70 milltir yr awr ";

    //LEGISLATION OR STATEMENT OF FACT CITATIONS
    private static final String ENGLISH_OFFENCE_WORDING = "On 16/09/2018 at RHUALLT, DENBIGHSHIRE drove a motor vehicle, namely an AUDI BF16 XOC," +
            " on a dual carriageway road, namely the A55, at a speed exceeding 70 miles per hour ";

    private static final String Offence_WORDING_WITH_LEGISLATION_CITATION = "On 16/09/2018 at RHUALLT, DENBIGHSHIRE drove a motor vehicle, namely an AUDI BF16 XOC," +
            " on a dual carriageway road, namely the A55, at a speed exceeding 70 miles per hour -- LEGISLATION: 'Contrary to article 2(c) of the" +
            " 70 Miles Per Hour, 60 Miles Per Hour and 50 Miles Per Hour (Temporary Speed Limit) Order 1977, section 89(1) of the Road Traffic Regulation Act 1984 " +
            "and Schedule 2 to the Road Traffic Offenders Act 1988.' -- CCCJS CODE: 'SV77010' -- ACPO: '' -- PNLD CODE: 'H874' || Ar 16/09/2018 yn RHUALLT, " +
            "SIR DDINBYCH bu i chi yrru cerbyd modur, sef AUDI BF16 XOC, ar ffordd ddeuol, sef yr A55, ar gyflymder o fwy na 70 milltir yr awr -- LEGISLATION: " +
            "'Yn groes i erthygl 2(c) Gorchymyn 70 Milltir yr Awr, 60 Milltir yr Awr a 50 Milltir yr Awr (Cyfyngiad Cyflymder Dros Dro) 1977," +
            " adran 89(1) Deddf Rheoleiddio Traffig Ffyrdd 1984 ac Atodlen 2 Deddf Troseddwyr Traffig Ffyrdd 1988.' -- CCCJS CODE: 'SV77010' -- ACPO: ''" +
            " -- PNLD CODE: 'H874'";

    //OTHER CITATION - 3 CITATIONS IN TEXT
    private static final String OFFENCE_WORDING_TEXT_WITH_THREE_OTHER_CITATION = "On 16/09/2018 at RHUALLT, DENBIGHSHIRE drove a motor vehicle, namely an AUDI BF16 XOC," +
            " on a dual carriageway road, namely the A55, at a speed exceeding 70 miles per hour -- CCCJS CODE: 'SV77010' -- ACPO: '' -- PNLD CODE: 'H874' || Ar 16/09/2018 yn RHUALLT, " +
            "SIR DDINBYCH bu i chi yrru cerbyd modur, sef AUDI BF16 XOC, ar ffordd ddeuol, sef yr A55, ar gyflymder o fwy na 70 milltir yr awr -- LEGISLATION: " +
            "'Yn groes i erthygl 2(c) Gorchymyn 70 Milltir yr Awr, 60 Milltir yr Awr a 50 Milltir yr Awr (Cyfyngiad Cyflymder Dros Dro) 1977," +
            " adran 89(1) Deddf Rheoleiddio Traffig Ffyrdd 1984 ac Atodlen 2 Deddf Troseddwyr Traffig Ffyrdd 1988.' -- CCCJS CODE: 'SV77010' -- ACPO: ''" +
            " -- PNLD CODE: 'H874'";

    private static final String ENGLISH_OFFENCE_WORDING_REMOVED_THREE_OTHER_CITATION = "On 16/09/2018 at RHUALLT, DENBIGHSHIRE drove a motor vehicle, namely an AUDI BF16 XOC, " +
            "on a dual carriageway road, namely the A55, at a speed exceeding 70 miles per hour  ACPO: '' -- PNLD CODE: 'H874'";

    //OTHER CITATION - 4 CITATIONS IN TEXT
    private static final String OFFENCE_WORDING_WITH_FOUREQUAL_Citation = "On 16/09/2018 at RHUALLT, DENBIGHSHIRE drove a motor vehicle, namely an AUDI BF16 XOC," +
            " on a dual carriageway road, namely the A55, at a speed exceeding 70 miles per hour -- CCCJS CODE: 'SV77010' -- ACPO: '' -- PNLD CODE: 'H874' --SVVCODE: 'trial' || Ar 16/09/2018 yn RHUALLT, " +
            "SIR DDINBYCH bu i chi yrru cerbyd modur, sef AUDI BF16 XOC, ar ffordd ddeuol, sef yr A55, ar gyflymder o fwy na 70 milltir yr awr -- LEGISLATION: " +
            "'Yn groes i erthygl 2(c) Gorchymyn 70 Milltir yr Awr, 60 Milltir yr Awr a 50 Milltir yr Awr (Cyfyngiad Cyflymder Dros Dro) 1977," +
            " adran 89(1) Deddf Rheoleiddio Traffig Ffyrdd 1984 ac Atodlen 2 Deddf Troseddwyr Traffig Ffyrdd 1988.' -- CCCJS CODE: 'SV77010' -- ACPO: ''" +
            " -- PNLD CODE: 'H874' --SVVCODE: 'trial'";

    private static final String ENGLISH_OFFENCE_WORDING_REMOVED_FOUR_OTHER_CITATION = "On 16/09/2018 at RHUALLT, DENBIGHSHIRE drove a motor vehicle, namely an AUDI BF16 XOC, " +
            "on a dual carriageway road, namely the A55, at a speed exceeding 70 miles per hour  ACPO: '' SVVCODE: 'trial'";

    //OTHER CITATION - SINGLE CITATIONS IN TEXT
    private static final String OFFENCE_WORDING_WITH_SINGLE_Citation = "On 16/09/2018 at RHUALLT, DENBIGHSHIRE drove a motor vehicle, namely an AUDI BF16 XOC," +
            " on a dual carriageway road, namely the A55, at a speed exceeding 70 miles per hour -- PNLD CODE: 'H874'|| Ar 16/09/2018 yn RHUALLT, " +
            "SIR DDINBYCH bu i chi yrru cerbyd modur, sef AUDI BF16 XOC, ar ffordd ddeuol, sef yr A55, ar gyflymder o fwy na 70 milltir yr awr -- LEGISLATION: " +
            "'Yn groes i erthygl 2(c) Gorchymyn 70 Milltir yr Awr, 60 Milltir yr Awr a 50 Milltir yr Awr (Cyfyngiad Cyflymder Dros Dro) 1977," +
            " adran 89(1) Deddf Rheoleiddio Traffig Ffyrdd 1984 ac Atodlen 2 Deddf Troseddwyr Traffig Ffyrdd 1988.' -- CCCJS CODE: 'SV77010' -- ACPO: ''" +
            " -- PNLD CODE: 'H874' --SVVCODE: 'trial'";

    private static final String ENGLISH_OFFENCE_WORDING_REMOVED_SINGLE_OTHER_CITATION = "On 16/09/2018 at RHUALLT, DENBIGHSHIRE drove a motor vehicle, namely an AUDI BF16 XOC, " +
            "on a dual carriageway road, namely the A55, at a speed exceeding 70 miles per hour -- PNLD CODE: 'H874'";

    private static final String ENGLISH_PROSECUTION_FACTS = "English prosecution facts";
    private static final String WELSH_PROSECUTION_FACTS = "Welsh proscution facts";
    private static final String ENGLISH_WELSH_PROSECUTION_FACTS = "English prosecution facts || Welsh proscution facts";
    private static final String ENGLISH_PROSECUTION_FACTS_WITH_DELIMITER = "English prosecution facts ||";

    @Test
    public void shouldHaveCorrectFieldsAfterConvertingPoliceOffenseToOffence() {
        PoliceOffense policeOffense = getMockPoliceOffence(Offence_WORDING_WITH_LEGISLATION_CITATION, ENGLISH_WELSH_PROSECUTION_FACTS);
        Offence offence = new OffenceConverter().convert(policeOffense);

        assertThat(offence.getStatementOfFacts(), is(ENGLISH_PROSECUTION_FACTS));
        assertThat(offence.getStatementOfFactsWelsh(), is(WELSH_PROSECUTION_FACTS));
        assertThat(offence.getChargeDate(), is(policeOffense.getBaseOffenceDetails().getChargeDate()));
        assertThat(offence.getOffenceCommittedDate(), is(policeOffense.getBaseOffenceDetails().getOffenceDateStartDate()));
        assertThat(offence.getOffenceCommittedEndDate(), is(policeOffense.getBaseOffenceDetails().getOffenceEndDate()));
        assertThat(offence.getOffenceDateCode(), is(policeOffense.getBaseOffenceDetails().getOffenceDateCode()));
        assertThat(offence.getOffenceLocation(), is(policeOffense.getBaseOffenceDetails().getLocationOfOffence()));
        assertThat(offence.getOffenceSequenceNumber(), is(policeOffense.getBaseOffenceDetails().getOffenceSequenceNumber()));
        assertThat(offence.getOffenceWording(), is(ENGLISH_OFFENCE_WORDING));
        assertThat(offence.getOtherPartyVictim().get(0).getGender(), is(policeOffense.getOtherPartyVictim().get(0).getGender()));
        assertThat(offence.getOtherPartyVictim().get(0).getObservedEthnicity(), is(0));
        assertThat(offence.getOtherPartyVictim().get(0).getSelfDefinedEthnicity(), is(policeOffense.getOtherPartyVictim().get(0).getSelfDefinedEthnicity()));
        assertThat(offence.getOtherPartyVictim().get(0).getPersonalInformation().getFirstName(), is(policeOffense.getOtherPartyVictim().get(0).getPersonGivenName1()));
        assertThat(offence.getOtherPartyVictim().get(0).getPersonalInformation().getLastName(), is(policeOffense.getOtherPartyVictim().get(0).getPersonFamilyName()));
        assertThat(offence.getAlcoholRelatedOffence().getAlcoholLevelAmount(), is(policeOffense.getBaseOffenceDetails().getAlcoholLevelAmount()));
        assertThat(offence.getAlcoholRelatedOffence().getAlcoholLevelMethod(), is(policeOffense.getBaseOffenceDetails().getAlcoholLevelMethod()));
        assertThat(offence.getVehicleRelatedOffence().getVehicleCode(), is(policeOffense.getBaseOffenceDetails().getVehicleCode()));
        assertThat(offence.getVehicleRelatedOffence().getVehicleRegistrationMark(), is(policeOffense.getBaseOffenceDetails().getVehicleRegistrationMark()));


        assertThat(offence.getOffenceWordingWelsh(), is(WELSH_OFFENCE_WORDING));

    }

    @Test
    public void shouldHaveCorrectStatementOfFactsForEnglishOnly() {
        PoliceOffense policeOffense = getMockPoliceOffence(Offence_WORDING_WITH_LEGISLATION_CITATION, ENGLISH_PROSECUTION_FACTS);
        Offence offence = new OffenceConverter().convert(policeOffense);
        assertThat(offence.getStatementOfFacts(), is(ENGLISH_PROSECUTION_FACTS));
        assertThat(offence.getStatementOfFactsWelsh(), is(nullValue()));
    }

    @Test
    public void shouldHaveCorrectStatementOfFactsForEnglishOnlyWithDelimiter() {
        PoliceOffense policeOffense = getMockPoliceOffence(Offence_WORDING_WITH_LEGISLATION_CITATION, ENGLISH_PROSECUTION_FACTS_WITH_DELIMITER);
        Offence offence = new OffenceConverter().convert(policeOffense);
        assertThat(offence.getStatementOfFacts(), is(ENGLISH_PROSECUTION_FACTS));
        assertThat(offence.getStatementOfFactsWelsh(), is(nullValue()));
    }

    @Test
    public void shouldHaveCorrectStatementOfFactsWelshWhenSupplied() {
        PoliceOffense policeOffense = getMockPoliceOffence(Offence_WORDING_WITH_LEGISLATION_CITATION, ENGLISH_WELSH_PROSECUTION_FACTS);
        Offence offence = new OffenceConverter().convert(policeOffense);
        assertThat(offence.getStatementOfFacts(), is(ENGLISH_PROSECUTION_FACTS));
        assertThat(offence.getStatementOfFactsWelsh(), is(WELSH_PROSECUTION_FACTS));
    }

    @Test
    public void shouldRemoveOtherCitiationsFromOffenceWithThreeCitations() {
        PoliceOffense policeOffense = getMockPoliceOffence(OFFENCE_WORDING_TEXT_WITH_THREE_OTHER_CITATION, ENGLISH_PROSECUTION_FACTS);
        Offence offence = new OffenceConverter().convert(policeOffense);

        assertThat(offence.getStatementOfFacts(), is(policeOffense.getProsecutionFacts()));
        assertThat(offence.getChargeDate(), is(policeOffense.getBaseOffenceDetails().getChargeDate()));
        assertThat(offence.getOffenceCommittedDate(), is(policeOffense.getBaseOffenceDetails().getOffenceDateStartDate()));
        assertThat(offence.getOffenceCommittedEndDate(), is(policeOffense.getBaseOffenceDetails().getOffenceEndDate()));
        assertThat(offence.getOffenceDateCode(), is(policeOffense.getBaseOffenceDetails().getOffenceDateCode()));
        assertThat(offence.getOffenceLocation(), is(policeOffense.getBaseOffenceDetails().getLocationOfOffence()));
        assertThat(offence.getOffenceSequenceNumber(), is(policeOffense.getBaseOffenceDetails().getOffenceSequenceNumber()));
        assertThat(offence.getOffenceWording(), is(ENGLISH_OFFENCE_WORDING_REMOVED_THREE_OTHER_CITATION));
        assertThat(offence.getOffenceWordingWelsh(), is(WELSH_OFFENCE_WORDING));

    }

    @Test
    public void shouldRemoveOtherCitiationsFromOffenceWithFourCitations() {
        PoliceOffense policeOffense = getMockPoliceOffence(OFFENCE_WORDING_WITH_FOUREQUAL_Citation, ENGLISH_PROSECUTION_FACTS);
        Offence offence = new OffenceConverter().convert(policeOffense);

        assertThat(offence.getStatementOfFacts(), is(policeOffense.getProsecutionFacts()));
        assertThat(offence.getChargeDate(), is(policeOffense.getBaseOffenceDetails().getChargeDate()));
        assertThat(offence.getOffenceCommittedDate(), is(policeOffense.getBaseOffenceDetails().getOffenceDateStartDate()));
        assertThat(offence.getOffenceCommittedEndDate(), is(policeOffense.getBaseOffenceDetails().getOffenceEndDate()));
        assertThat(offence.getOffenceDateCode(), is(policeOffense.getBaseOffenceDetails().getOffenceDateCode()));
        assertThat(offence.getOffenceLocation(), is(policeOffense.getBaseOffenceDetails().getLocationOfOffence()));
        assertThat(offence.getOffenceSequenceNumber(), is(policeOffense.getBaseOffenceDetails().getOffenceSequenceNumber()));
        assertThat(offence.getOffenceWording(), is(ENGLISH_OFFENCE_WORDING_REMOVED_FOUR_OTHER_CITATION));
        assertThat(offence.getOffenceWordingWelsh(), is(WELSH_OFFENCE_WORDING));

    }

    @Test
    public void shouldRemoveOtherCitiationsFromOffenceWithSingleCitations() {
        PoliceOffense policeOffense = getMockPoliceOffence(OFFENCE_WORDING_WITH_SINGLE_Citation, ENGLISH_PROSECUTION_FACTS);
        Offence offence = new OffenceConverter().convert(policeOffense);

        assertThat(offence.getStatementOfFacts(), is(policeOffense.getProsecutionFacts()));
        assertThat(offence.getChargeDate(), is(policeOffense.getBaseOffenceDetails().getChargeDate()));
        assertThat(offence.getOffenceCommittedDate(), is(policeOffense.getBaseOffenceDetails().getOffenceDateStartDate()));
        assertThat(offence.getOffenceCommittedEndDate(), is(policeOffense.getBaseOffenceDetails().getOffenceEndDate()));
        assertThat(offence.getOffenceDateCode(), is(policeOffense.getBaseOffenceDetails().getOffenceDateCode()));
        assertThat(offence.getOffenceLocation(), is(policeOffense.getBaseOffenceDetails().getLocationOfOffence()));
        assertThat(offence.getOffenceSequenceNumber(), is(policeOffense.getBaseOffenceDetails().getOffenceSequenceNumber()));
        assertThat(offence.getOffenceWording(), is(ENGLISH_OFFENCE_WORDING_REMOVED_SINGLE_OTHER_CITATION));
        assertThat(offence.getOffenceWordingWelsh(), is(WELSH_OFFENCE_WORDING));

    }

    private PoliceOffense getMockPoliceOffence(final String OffenceWording, final String prosecutionFacts) {
        return PoliceOffense.policeOffense()
                .withProsecutionFacts(prosecutionFacts)
                .withOtherPartyVictim(Arrays.asList(PoliceVictim.policeVictim()
                        .withGender(2)
                        .withPersonGivenName1("FirstName")
                        .withPersonGivenName2("LastName")
                        .withObservedEthnicity(9)
                        .withSelfDefinedEthnicity("W1")
                        .withBirthDate(LocalDate.parse("1998-10-10"))
                        .withTelephoneNumberBusiness("01234567890")
                        .build()))
                .withBaseOffenceDetails(
                        BaseOffense.baseOffense()
                                .withChargeDate(LocalDate.of(1, 2, 15))
                                .withOffenceDateStartDate(LocalDate.of(1, 1, 1))
                                .withOffenceEndDate(LocalDate.of(1, 2, 2))
                                .withOffenceDateCode(1234)
                                .withLocationOfOffence("Location")
                                .withOffenceSequenceNumber(1234)
                                .withOffenceWording(OffenceWording)
                                .withAlcoholLevelAmount(10)
                                .withAlcoholLevelMethod("Breaath")
                                .withVehicleCode("34")
                                .withVehicleRegistrationMark("LN19UUB")
                                .build())


                .build();
    }
}
