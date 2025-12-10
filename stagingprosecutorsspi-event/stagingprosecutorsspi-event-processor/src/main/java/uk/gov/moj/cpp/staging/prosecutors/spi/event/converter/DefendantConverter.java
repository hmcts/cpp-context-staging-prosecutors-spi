package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter;

import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.IndividualAlias.individualAlias;

import com.google.common.base.Strings;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Language;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.BasePersonName;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceDefendant;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceOffense;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiInitialHearing;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DefendantConverter implements SpiConverter<PoliceDefendant, Defendant, SpiInitialHearing> {

    @Override
    public Defendant convert(final PoliceDefendant source, final SpiInitialHearing initialHearing) {

        if (source.getPoliceIndividualDefendant() != null) {
            return getPoliceIndividualDefendent(source, initialHearing).build();
        } else {
            return getPoliceCorporateDefendent(source, initialHearing).build();
        }
    }

    private Defendant.Builder getPoliceIndividualDefendent(final PoliceDefendant source, final SpiInitialHearing initialHearing) {
        String pncIdentifier = source.getPoliceIndividualDefendant().getPnCidentifier();
        if (Strings.isNullOrEmpty(pncIdentifier) && source.getPoliceIndividualDefendant().getPersonDefendant() != null){
            pncIdentifier = source.getPoliceIndividualDefendant().getPersonDefendant().getPnCidentifier();
        }

        final Defendant.Builder builder = Defendant.defendant()
                .withId(source.getId().toString())
                .withAsn(source.getProsecutorReference())
                .withProsecutorDefendantReference(source.getProsecutorReference())
                .withCroNumber(source.getPoliceIndividualDefendant().getCrOnumber())
                .withPncIdentifier(pncIdentifier)
                .withHearingLanguage(Language.valueOf(source.getPoliceIndividualDefendant().getHearingLanguage()))
                .withDocumentationLanguage(Language.valueOf(source.getPoliceIndividualDefendant().getDocumentationLanguage()))
                .withLanguageRequirement(source.getPoliceIndividualDefendant().getLanguageNeeds())
                .withPostingDate(getChargeDatefromFirstOffence(source.getOffence()))
                .withIndividual(new IndividualDefendantConverter().convert(source.getPoliceIndividualDefendant()))
                .withOffences(transformOffences(source.getOffence()))
                .withSpecificRequirements(source.getPoliceIndividualDefendant().getSpecialNeeds())
                .withNumPreviousConvictions(null) //Not needed we can ignore it.
                .withInitialHearing(new InitialHearingConverter().convert(initialHearing))
                .withCustodyStatus(source.getPoliceIndividualDefendant().getCustodyStatus())
                ;
        if(source.getPoliceIndividualDefendant() != null
                && source.getPoliceIndividualDefendant().getAlias() != null
                && !source.getPoliceIndividualDefendant().getAlias().isEmpty()) {
            builder.withIndividualAliases(buildIndividualAliases(source.getPoliceIndividualDefendant().getAlias()));
        }



        return builder;

    }

    private Defendant.Builder getPoliceCorporateDefendent(final PoliceDefendant source, final SpiInitialHearing initialHearing) {
        return Defendant.defendant()
                .withId(source.getId().toString())
                .withAsn(source.getProsecutorReference())
                .withOffences(transformOffences(source.getOffence()))
                .withOrganisationName(source.getPoliceCorporateDefendant().getOrganisationName())
                .withCroNumber(source.getPoliceCorporateDefendant().getCrOnumber())
                .withProsecutorDefendantReference(source.getProsecutorReference())
                .withPncIdentifier(source.getPoliceCorporateDefendant().getPnCidentifier())
                .withAliasForCorporate(source.getPoliceCorporateDefendant().getAlias())
                .withTelephoneNumberBusiness(source.getPoliceCorporateDefendant().getTelephoneNumberBusiness())
                .withEmailAddress1(source.getPoliceCorporateDefendant().getEmailAddress1())
                .withEmailAddress2(source.getPoliceCorporateDefendant().getEmailAddress2())
                .withAddress(new AddressConverter().convert(source.getPoliceCorporateDefendant().getAddress()))
                .withPostingDate(getChargeDatefromFirstOffence(source.getOffence()))
                .withInitialHearing(new InitialHearingConverter().convert(initialHearing))
                ;

    }

    private List<Offence> transformOffences(final List<PoliceOffense> policeOffenses) {
        final OffenceConverter offenceConverter = new OffenceConverter();
        return policeOffenses.stream().map(offenceConverter::convert).collect(Collectors.toList());

    }

    private LocalDate getChargeDatefromFirstOffence(final List<PoliceOffense> policeOffenses) {
        final Optional<PoliceOffense> firstOffence = policeOffenses.stream().findFirst();
        if (firstOffence.isPresent()) {
            return firstOffence.get().getBaseOffenceDetails().getChargeDate();
        }
        return null;
    }

    private List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.IndividualAlias> buildIndividualAliases(final List<BasePersonName> aliases) {
        final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.IndividualAlias> individualAliases = new ArrayList<>();
        aliases.forEach(alias ->
                    individualAliases.add(individualAlias()
                            .withTitle(alias.getPersonTitle())
                            .withFirstName(alias.getPersonGivenName1())
                            .withGivenName2(alias.getPersonGivenName2())
                            .withGivenName3(alias.getPersonGivenName3())
                            .withLastName(alias.getPersonFamilyName())
                            .build()));
        return individualAliases;
    }

}

