package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address;


public class AddressConverter implements Converter<uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address, Address> {

    private static final String SPACE = " ";
    private static final String APPEND_OPERATOR = "+";
    private static final int COMBINED_TEXT_LIMIT = 35;
    private static final int PAON_FIRST_CHARS_LIMIT = 10;

    @Override
    public Address convert(final uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address defendantAddress) {

        if(null==defendantAddress) {
            return null;
        }
        boolean isPaonGreaterThan10Chars = false;

        if(null!=defendantAddress.getPaon()){
            isPaonGreaterThan10Chars = defendantAddress.getPaon().length() > PAON_FIRST_CHARS_LIMIT;
        }
        final boolean isSaonPresent = defendantAddress.getSaon() != null;

        final String addressLine1 = getAddressLine1(defendantAddress, isPaonGreaterThan10Chars, isSaonPresent);
        final String addressLine2 = getAddressLine2(defendantAddress, isPaonGreaterThan10Chars, isSaonPresent);
        final String addressLine3 = getAddressLine3(defendantAddress, isPaonGreaterThan10Chars, isSaonPresent, addressLine2);
        final String addressLine4 = getAddressLine4(defendantAddress, addressLine2, addressLine3);
        final String addressLine5 = getAddressLine5(defendantAddress, addressLine2, addressLine3, addressLine4);

        return uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address.address()
                .withAddress1(concatTextIfNeeded(addressLine1))
                .withAddress2(concatTextIfNeeded(addressLine2))
                .withAddress3(concatTextIfNeeded(addressLine3))
                .withAddress4(concatTextIfNeeded(addressLine4))
                .withAddress5(concatTextIfNeeded(addressLine5))
                .withPostcode(defendantAddress.getPostcode())
                .build();
    }

    private String getAddressLine1(final uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address defendantAddress,
                                   boolean isPaonGreaterThan10Chars, boolean isSaonPresent) {
        String line1 = null;
        if (!isPaonGreaterThan10Chars) {
            if (isSaonPresent) {
                line1 = defendantAddress.getSaon();
            } else {
                line1 = getFirst10Characters(defendantAddress.getPaon())
                        + SPACE + defendantAddress.getStreetDescription();
            }
        } else {
            if (isSaonPresent) {
                line1 = defendantAddress.getSaon().trim();
            } else {
                line1 = defendantAddress.getPaon().trim();
            }
        }
        return line1;
    }

    private String getAddressLine2(final uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address defendantAddress,
                                   boolean isPaonGreaterThan10Chars, boolean isSaonPresent) {
        String line2 = null;
        if (!isPaonGreaterThan10Chars) {
            if (isSaonPresent) {
                line2 = getFirst10Characters(defendantAddress.getPaon()) + SPACE + defendantAddress.getStreetDescription();
            } else {
                line2 = getAvailableLocalityTownAreaBasedOnOrder(defendantAddress);
            }
        } else {
            if (isSaonPresent) {
                line2 = defendantAddress.getPaon().trim();
            } else {
                line2 = defendantAddress.getStreetDescription();
            }
        }
        return line2;
    }

    private String getAddressLine3(final uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address defendantAddress,
                                   boolean isPaonGreaterThan10Chars, boolean isSaonPresent, String addressLine2) {
        String line3 = null;

        if (isPaonGreaterThan10Chars && isSaonPresent) {
            line3 = defendantAddress.getStreetDescription();

        } else {
            line3 = checkLinesAndGetAvailableLocalityTownArea(defendantAddress, addressLine2);
        }
        return line3;
    }

    private String getAddressLine4(final uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address defendantAddress,
                                   final String addressLine2, final String addressLine3) {
        //check last addressLine was null
        if (addressLine3 == null) {
            return null;
        }

        return checkLinesAndGetAvailableLocalityTownArea(defendantAddress, addressLine2, addressLine3);
    }

    private String getAddressLine5(final uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address defendantAddress,
                                   final String addressLine2, final String addressLine3, final String addressLine4) {
        //check last addressLine was null
        if (addressLine4 == null) {
            return null;
        }

        return checkLinesAndGetAvailableLocalityTownArea(defendantAddress, addressLine2, addressLine3, addressLine4);
    }

    private String getAvailableLocalityTownAreaBasedOnOrder(final uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address defendantAddress) {
        if (defendantAddress.getLocality() != null) {
            return defendantAddress.getLocality();
        } else if (defendantAddress.getTown() != null) {
            return defendantAddress.getTown();
        } else if (defendantAddress.getPostTown() != null) {
            return defendantAddress.getPostTown();
        } else {
            return defendantAddress.getAdministrativeArea();
        }
    }

    private String checkLinesAndGetAvailableLocalityTownArea(final uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address defendantAddress, final String previousAddressLine2) {
        if (defendantAddress.getLocality() != null && !defendantAddress.getLocality().equalsIgnoreCase(previousAddressLine2)) {
            return defendantAddress.getLocality();
        } else if (defendantAddress.getTown() != null && !defendantAddress.getTown().equalsIgnoreCase(previousAddressLine2)) {
            return defendantAddress.getTown();
        } else if (defendantAddress.getPostTown() != null && !defendantAddress.getPostTown().equalsIgnoreCase(previousAddressLine2)) {
            return defendantAddress.getPostTown();
        } else if (defendantAddress.getAdministrativeArea() != null && !defendantAddress.getAdministrativeArea().equalsIgnoreCase(previousAddressLine2)) {
            return defendantAddress.getAdministrativeArea();
        }

        return null;
    }

    private String checkLinesAndGetAvailableLocalityTownArea(final uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address defendantAddress,
                                                             final String previousAddressLine2,
                                                             final String previousAddressLine3) {
        if (defendantAddress.getLocality() != null && isNotAlreadyPopulated(defendantAddress.getLocality(), previousAddressLine2, previousAddressLine3)) {
            return defendantAddress.getLocality();
        } else if (defendantAddress.getTown() != null && isNotAlreadyPopulated(defendantAddress.getTown(), previousAddressLine2, previousAddressLine3)) {
            return defendantAddress.getTown();
        } else if (defendantAddress.getPostTown() != null && isNotAlreadyPopulated(defendantAddress.getPostTown(), previousAddressLine2, previousAddressLine3)) {
            return defendantAddress.getPostTown();
        } else if (defendantAddress.getAdministrativeArea() != null && isNotAlreadyPopulated(defendantAddress.getAdministrativeArea(), previousAddressLine2, previousAddressLine3)) {
            return defendantAddress.getAdministrativeArea();
        }

        return null;
    }

    private String checkLinesAndGetAvailableLocalityTownArea(final uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address defendantAddress,
                                                             final String previousAddressLine2,
                                                             final String previousAddressLine3,
                                                             final String previousAddressLine4) {
        if (defendantAddress.getLocality() != null && isNotAlreadyPopulated(defendantAddress.getLocality(), previousAddressLine2, previousAddressLine3, previousAddressLine4)
                ) {
            return defendantAddress.getLocality();
        } else if (defendantAddress.getTown() != null && isNotAlreadyPopulated(defendantAddress.getTown(), previousAddressLine2, previousAddressLine3, previousAddressLine4)) {
            return defendantAddress.getTown();
        } else if (defendantAddress.getPostTown() != null && isNotAlreadyPopulated(defendantAddress.getPostTown(), previousAddressLine2, previousAddressLine3, previousAddressLine4)) {
            return defendantAddress.getPostTown();
        } else if (defendantAddress.getAdministrativeArea() != null && isNotAlreadyPopulated(defendantAddress.getAdministrativeArea(), previousAddressLine2, previousAddressLine3, previousAddressLine4)) {
            return defendantAddress.getAdministrativeArea();
        }

        return null;
    }

    private boolean isNotAlreadyPopulated(String value, String addressLine2, String addressLine3) {
        return !addressLine2.equalsIgnoreCase(value) && !addressLine3.equalsIgnoreCase(value);
    }

    private boolean isNotAlreadyPopulated(String value, String addressLine2, String addressLine3, String addressLine4) {
        return !addressLine2.equalsIgnoreCase(value) && !addressLine3.equalsIgnoreCase(value) && !addressLine4.equalsIgnoreCase(value);
    }

    private String getFirst10Characters(final String paon) {
        final int len = paon.length() < PAON_FIRST_CHARS_LIMIT ? paon.length() : PAON_FIRST_CHARS_LIMIT - 1;
        return paon.substring(0, len).trim();
    }

    private String concatTextIfNeeded(final String addressLine) {
        if (addressLine == null) {
            return null;
        }

        String line = addressLine;
        if (addressLine.length() > COMBINED_TEXT_LIMIT) {
            line = addressLine.substring(0, COMBINED_TEXT_LIMIT - 1) + APPEND_OPERATOR;
        }
        return line;
    }
}

