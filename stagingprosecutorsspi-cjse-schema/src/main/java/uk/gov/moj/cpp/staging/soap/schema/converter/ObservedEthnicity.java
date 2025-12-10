package uk.gov.moj.cpp.staging.soap.schema.converter;

import uk.gov.dca.xmlschemas.libra.BasePersonEthnicityStructure;

public class ObservedEthnicity {

    private static final int UNKNOWN = 9;
    private static final int NOT_RECORDED_NOT_KNOWN = 0;

    private ObservedEthnicity() {
    }

    public static Integer getObservedEthnicity(final BasePersonEthnicityStructure basePersonEthnicityStructure) {

        if (basePersonEthnicityStructure != null && basePersonEthnicityStructure.getObservedEthnicity() != null ) {

            return basePersonEthnicityStructure.getObservedEthnicity() == UNKNOWN ? NOT_RECORDED_NOT_KNOWN : basePersonEthnicityStructure.getObservedEthnicity();
        }

        return null;


    }
}
