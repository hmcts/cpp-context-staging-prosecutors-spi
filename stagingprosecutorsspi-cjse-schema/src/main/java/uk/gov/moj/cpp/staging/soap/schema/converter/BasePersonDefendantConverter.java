package uk.gov.moj.cpp.staging.soap.schema.converter;


import uk.gov.dca.xmlschemas.libra.BasePersonDefendantStructure;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.BasePersonDefendant;

public class BasePersonDefendantConverter implements Converter<BasePersonDefendantStructure, BasePersonDefendant> {

    @Override
    public BasePersonDefendant convert(final BasePersonDefendantStructure basePersonDefendantStructure) {
        return new BasePersonDefendant.Builder().
                withBailConditions(basePersonDefendantStructure.getBailConditions()).
                withPnCidentifier(basePersonDefendantStructure.getPNCidentifier()).
                withBasePersonDetails(new BasePersonDetailConverter().convert(basePersonDefendantStructure.getBasePersonDetails())).
                build();
    }

}
