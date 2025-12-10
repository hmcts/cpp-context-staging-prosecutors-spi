package uk.gov.moj.cpp.staging.soap.schema.converter;

import uk.gov.govtalk.people.bs7666.BSaddressStructure;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.JAXBElement;

public class AddressConverter implements Converter<BSaddressStructure, Address> {

    @Override
    public Address convert(final BSaddressStructure bSaddressStructure) {
        final List<JAXBElement<? extends Serializable>> jaxbElements = bSaddressStructure.getContent();

        return new Address.Builder().
                withAdministrativeArea(getAddressField(jaxbElements, "administrativeArea")).
                withLocality(getAddressField(jaxbElements, "locality")).
                withPaon(getAddressField(jaxbElements, "paon")).
                withPostcode(getAddressField(jaxbElements, "postcode")).
                withPostTown(getAddressField(jaxbElements, "postTown")).
                withSaon(getAddressField(jaxbElements, "saon")).
                withStreetDescription(getAddressField(jaxbElements, "streetDescription")).
                withTown(getAddressField(jaxbElements, "town")).
                withUniquePropertyReferenceNumber(getAddressField(jaxbElements, "uniquePropertyReferenceNumber")).
                withUniqueStreetReferenceNumber(getAddressField(jaxbElements, "uniqueStreetReferenceNumber"))
                .build();
    }

    private static String getAddressField(final List<JAXBElement<? extends Serializable>> jaxbElements, final String fieldName) {
        final JAXBElement<? extends Serializable> jaxbElement = jaxbElements.stream()
                .filter(el -> el.getName().getLocalPart().equalsIgnoreCase(fieldName))
                .findFirst().orElse(null);
        return jaxbElement == null ? null : jaxbElement.getValue().toString();

    }
}


