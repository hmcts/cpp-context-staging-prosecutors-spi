package uk.gov.moj.cpp.staging.soap.schema.converter;

import static uk.gov.moj.cpp.staging.soap.schema.converter.ObservedEthnicity.getObservedEthnicity;
import uk.gov.dca.xmlschemas.libra.PoliceParentGuardianStructure;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceParentGuardian;

public class PoliceParentGuardianConverter implements Converter<PoliceParentGuardianStructure, PoliceParentGuardian> {

    @Override
    public PoliceParentGuardian convert(final PoliceParentGuardianStructure policeParentGuardianStructure) {

        return new PoliceParentGuardian.Builder().
                withAddress(new AddressConverter().convert(policeParentGuardianStructure.getAddress())).
                withBasePersonDetails(new BasePersonDetailConverter().convert(policeParentGuardianStructure.getBasePersonDetails())).
                withSelfDefinedEthnicity(policeParentGuardianStructure.getPersonEthnicity() == null ? null : policeParentGuardianStructure.getPersonEthnicity().getSelfDefinedEthnicity()).
                withObservedEthnicity(getObservedEthnicity(policeParentGuardianStructure.getPersonEthnicity()))
                .build();
    }


}
