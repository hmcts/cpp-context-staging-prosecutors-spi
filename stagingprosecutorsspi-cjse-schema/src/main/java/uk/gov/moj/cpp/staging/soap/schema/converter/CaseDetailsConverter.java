package uk.gov.moj.cpp.staging.soap.schema.converter;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import uk.gov.dca.xmlschemas.libra.PoliceCaseStructure;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CaseDetails;

public class CaseDetailsConverter implements Converter<PoliceCaseStructure, CaseDetails> {

    @Override
    public CaseDetails convert(final PoliceCaseStructure source) {

        return new CaseDetails.Builder().
                withPtiurn(source.getPTIURN()).
                withOriginatingOrganisation(source.getOriginatingOrganisation()).
                withCaseInitiationCode(!isEmpty(source.getCaseInitiation()) ? source.getCaseInitiation() : source.getInitiationCode()).
                withSummonsCode(source.getSummonsCode()).
                withInformant(source.getInformant()).
                withCpSorganisation(source.getCPSorganisation()).
                withCaseMarker(source.getCaseMarker()).
                withInitialHearing(new HearingConverter().convert(source)).
                withVehicleOperatorLicenceNumber(source.getVehicleOperatorLicenceNumber()).build();
    }

}

