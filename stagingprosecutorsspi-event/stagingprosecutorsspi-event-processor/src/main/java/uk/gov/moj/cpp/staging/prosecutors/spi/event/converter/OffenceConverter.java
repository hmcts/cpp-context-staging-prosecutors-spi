package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.AlcoholRelatedOffence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.VehicleRelatedOffence;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.utils.Citation;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceOffense;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceVictim;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


public class OffenceConverter implements Converter<PoliceOffense, Offence> {

    private static final String DELIMITER_FOR_WELSH = "\\|\\|";
    private static final Integer UNKNOWN = 9;
    private static final Integer NOT_RECORDED_NOT_KNOWN = 0;

    @Override
    public Offence convert(final PoliceOffense source) {
        return Offence.offence()
                .withOffenceId(UUID.randomUUID())
                .withChargeDate(source.getBaseOffenceDetails().getChargeDate())
                .withOffenceCode(source.getBaseOffenceDetails().getOffenceCode())
                .withOffenceCommittedDate(source.getBaseOffenceDetails().getOffenceDateStartDate())
                .withOffenceCommittedEndDate(source.getBaseOffenceDetails().getOffenceEndDate())
                .withOffenceDateCode(source.getBaseOffenceDetails().getOffenceDateCode())
                .withOffenceLocation(source.getBaseOffenceDetails().getLocationOfOffence())
                .withOffenceSequenceNumber(source.getBaseOffenceDetails().getOffenceSequenceNumber())
                .withOffenceWording(getOffenseWordingForEnglishOrWelsh(source.getBaseOffenceDetails().getOffenceWording(), 0))
                .withOffenceWordingWelsh(getOffenseWordingForEnglishOrWelsh(source.getBaseOffenceDetails().getOffenceWording(), 1))
                .withStatementOfFacts(getStatementOfFactsForEnglishOrWelsh(source.getProsecutionFacts(), 0))
                .withStatementOfFactsWelsh(getStatementOfFactsForEnglishOrWelsh(source.getProsecutionFacts(), 1))
                .withVehicleRelatedOffence(getVehicleRelatedOffence(source))
                .withOtherPartyVictim(buildOtherPartyVictim(source.getOtherPartyVictim()))
                .withAlcoholRelatedOffence(getAlcoholRelatedOffence(source))
                .withArrestDate(source.getBaseOffenceDetails().getArrestDate())
                .build();
    }

    private List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.PoliceVictim> buildOtherPartyVictim(final List<PoliceVictim> otherPartyVictim) {
        return otherPartyVictim.stream()
                .map(victim ->
                        uk.gov.moj.cpp.prosecution.casefile.json.schemas.PoliceVictim.policeVictim()
                                .withBirthDate(victim.getBirthDate())
                                .withGender(victim.getGender())
                                .withObservedEthnicity(UNKNOWN.equals(victim.getObservedEthnicity()) ? NOT_RECORDED_NOT_KNOWN : victim.getObservedEthnicity()).withSelfDefinedEthnicity(victim.getSelfDefinedEthnicity())
                                .withPersonalInformation(buildPersonalInformation(victim))
                                .build()).collect(Collectors.toList());


    }

    private PersonalInformation buildPersonalInformation(final PoliceVictim victim) {

        return PersonalInformation.personalInformation()
                .withObservedEthnicity(UNKNOWN.equals(victim.getObservedEthnicity()) ? NOT_RECORDED_NOT_KNOWN : victim.getObservedEthnicity())
                .withFirstName(victim.getPersonGivenName1())
                .withLastName(victim.getPersonFamilyName())
                .withTitle(victim.getPersonTitle())
                .withAddress(victim.getAddress() != null ? new AddressConverter().convert(victim.getAddress()) : null)
                .withContactDetails(buildContactDetails(victim))
                .build();
    }

    private ContactDetails buildContactDetails(final PoliceVictim victim) {
        return ContactDetails.contactDetails().
                withHome(victim.getTelephoneNumberHome())
                .withMobile(victim.getTelephoneNumberMobile())
                .withPrimaryEmail(victim.getEmailAddress1())
                .withSecondaryEmail(victim.getEmailAddress2())
                .build();
    }

    private AlcoholRelatedOffence getAlcoholRelatedOffence(final PoliceOffense source) {
        if (null == source.getBaseOffenceDetails().getAlcoholLevelAmount() || null == source.getBaseOffenceDetails().getAlcoholLevelMethod()) {
            return null;
        }

        return AlcoholRelatedOffence.alcoholRelatedOffence()
                .withAlcoholLevelAmount(source.getBaseOffenceDetails().getAlcoholLevelAmount())
                .withAlcoholLevelMethod(source.getBaseOffenceDetails().getAlcoholLevelMethod())
                .build();
    }

    private VehicleRelatedOffence getVehicleRelatedOffence(final PoliceOffense source) {
        if (null == source.getBaseOffenceDetails().getVehicleCode() || null == source.getBaseOffenceDetails().getVehicleRegistrationMark()) {
            return null;
        }

        return VehicleRelatedOffence.vehicleRelatedOffence()
                .withVehicleCode(source.getBaseOffenceDetails().getVehicleCode())
                .withVehicleRegistrationMark(source.getBaseOffenceDetails().getVehicleRegistrationMark())
                .build();
    }

    private String getStatementOfFactsForEnglishOrWelsh(final String prosecutionFacts, int index) {
        if (null == prosecutionFacts) {
            return null;
        }

        final String[] statementOfFactsForEnglishAndWelsh = prosecutionFacts.split(DELIMITER_FOR_WELSH);
        if (index <= statementOfFactsForEnglishAndWelsh.length - 1) {
            return statementOfFactsForEnglishAndWelsh[index].trim();
        } else {
            return null;
        }
    }

    private String getOffenseWordingForEnglishOrWelsh(final String offenseWording, final int index) {
        if (null == offenseWording) {
            return null;
        }

        final String[] offenseWordingForEnglishAndWelsh = offenseWording.split(DELIMITER_FOR_WELSH);
        if (index <= offenseWordingForEnglishAndWelsh.length - 1) {
            return removeAllCitationOccurrences(offenseWordingForEnglishAndWelsh[index].trim());
        } else {
            return null;
        }
    }

    private String removeAllCitationOccurrences(final String text) {

        String text1 = text;
        for (final Citation citation : Citation.values()) {
            text1 = removeCitationTillEndOfLine(citation, text1);
        }

        return removeRemainingCitationBetweenTwoSets(text1);
    }

    private String removeCitationTillEndOfLine(final Citation citation, final String text) {
        final int len = text.indexOf(citation.getValue());

        if (len > 0) {
            return text.substring(0, len);
        }
        return text;
    }

    private String removeRemainingCitationBetweenTwoSets(final String text) {
        int tokenCount = text.split("--").length - 1;
        String removedCitationText = text;
        for (int i = 0; i < tokenCount; i++) {
            removedCitationText = removeCitationBetweenTwoSets(removedCitationText);
            tokenCount = removedCitationText.split("--").length - 1;
        }

        return removedCitationText;
    }

    private String removeCitationBetweenTwoSets(final String text) {
        final int firstToken = text.indexOf("--");
        int correspondingToken = 0;

        if (firstToken > 0) {
            correspondingToken = text.indexOf("--", firstToken + 2);
        }

        if (correspondingToken > 0) {
            final StringBuilder sb = new StringBuilder();
            sb.append(text.substring(0, firstToken));
            sb.append(text.substring(correspondingToken + 2));
            return sb.toString();
        }

        return text;
    }
}

