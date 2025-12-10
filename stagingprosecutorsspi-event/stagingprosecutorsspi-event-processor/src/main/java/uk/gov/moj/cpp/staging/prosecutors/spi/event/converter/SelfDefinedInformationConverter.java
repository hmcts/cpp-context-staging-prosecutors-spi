package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Gender;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.GenderType;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.BasePersonDefendant;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceIndividualDefendant;

import java.util.Optional;

public class SelfDefinedInformationConverter implements Converter<PoliceIndividualDefendant, SelfDefinedInformation> {


    @Override
    public SelfDefinedInformation convert(final PoliceIndividualDefendant source) {

        final BasePersonDefendant basePersonDefendant = source.getPersonDefendant();
        Gender gender = null;
        final Optional<Gender> optionalGender = GenderType.valueFor(basePersonDefendant.getBasePersonDetails().getGender());
        if (optionalGender.isPresent()) {
            gender = optionalGender.get();
        }

        return SelfDefinedInformation.selfDefinedInformation()
                .withDateOfBirth(basePersonDefendant.getBasePersonDetails().getBirthDate())
                .withGender(gender)
                .withEthnicity(source.getSelfDefinedEthnicity())
                .build();
    }
}
