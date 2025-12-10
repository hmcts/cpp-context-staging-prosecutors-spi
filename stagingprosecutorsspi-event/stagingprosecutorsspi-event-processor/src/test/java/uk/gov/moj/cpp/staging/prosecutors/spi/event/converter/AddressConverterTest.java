package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceIndividualDefendant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AddressConverterTest {

    @Mock
    private PoliceIndividualDefendant policeIndividualDefendant;

    @InjectMocks
    private AddressConverter addressConverter;

    //When Paon is less than 10 chars and saon is populated
    @Test
    public void convertWhenPaonIsLessThan10CharsAndSoanIsPopulated() {
        final uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address mockAddress = getAddressWithPaonLessThan10CharsAndSoanPopulated();
        when(policeIndividualDefendant.getAddress()).thenReturn(mockAddress);

        Address address = addressConverter.convert(policeIndividualDefendant.getAddress());
        assertNotNull(address);
        assertThat(address.getAddress1(), is("Hoover Building")); //addressLine1 = saon
        assertThat(address.getAddress2(), is("682 Green Lane")); //addressLine2 = first 10 character of paon  + street description
        assertThat(address.getAddress3(), is("Ilford")); // addressLine3 = Locality/Town/PostTown/Administrative Area
        assertThat(address.getAddress4(), is("Essex")); // addressLine4 = Locality/Town/PostTown/Administrative Area
        assertThat(address.getAddress5(), is(nullValue()));
        assertThat(address.getPostcode(), is(mockAddress.getPostcode()));
    }

    //When Paon is less than 10 chars and saon is empty
    @Test
    public void convertWhenPaonIsLessThan10CharsAndSoanNotPopulated() {
        final uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address mockAddress = getAddressWhenOnlyPoanIsPopulatedWithLessThan10Chars();
        when(policeIndividualDefendant.getAddress()).thenReturn(mockAddress);

        Address address = addressConverter.convert(policeIndividualDefendant.getAddress());
        assertNotNull(address);
        assertThat(address.getAddress1(), is("682 Green Lane")); //addressLine1 = first 10 character of paon  + street description
        assertThat(address.getAddress2(), is("East Ham")); // addressLine2 = Locality
        assertThat(address.getAddress3(), is("Ilford")); // addressLine3 = Town
        assertThat(address.getAddress4(), is("Essex")); //addressLine4 = Administrative Area (As postTown not populated)
        assertThat(address.getAddress5(), is(nullValue()));
    }

    //When Paon is greater than 10 chars and saon is populated
    @Test
    public void convertWhenPaonIsGreaterThan10CharsAndSoanIsPopulated() {
        final uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address mockAddress = getAddressWhenPaonIsGreaterThan10CharsAndSaonIsPopulated();
        when(policeIndividualDefendant.getAddress()).thenReturn(mockAddress);

        Address address = addressConverter.convert(policeIndividualDefendant.getAddress());
        assertNotNull(address);
        assertThat(address.getAddress1(), is("Hoover Building"));
        assertThat(address.getAddress2(), is("682 Essex Electrical Supplies")); //addressLine1 = paon full text
        assertThat(address.getAddress3(), is("Green Lane")); // addressLine2 = street description
        assertThat(address.getAddress4(), is("Ilford")); //addressLine3 = Town
        assertThat(address.getAddress5(), is("Essex")); //addressLine4 = Administrative Area (As postTown not populated)
    }

    //When Paon is greater than 10 chars and saon is empty
    @Test
    public void convertWhenPaonIsGreaterThan10CharsAndSoanNotPopulated() {
        final uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address mockAddress = getAddressWhenPaonIsGreaterThan10CharsAndSaonNotPopulated();
        when(policeIndividualDefendant.getAddress()).thenReturn(mockAddress);

        Address address = addressConverter.convert(policeIndividualDefendant.getAddress());
        assertNotNull(address);
        assertThat(address.getAddress1(), is("682 Essex Electrical Supplies")); //addressLine1 = paon full text
        assertThat(address.getAddress2(), is("Green Lane")); // addressLine2 = street description
        assertThat(address.getAddress3(), is("Ilford")); //addressLine3 = Town
        assertThat(address.getAddress4(), is("Essex")); //addressLine4 = Administrative Area
        assertThat(address.getAddress5(), is(nullValue()));
    }


    //When Paon is less than 10 characters And SAON/Locality/Town/PostTown/AdminstrativeArea all populated
    @Test
    public void convertWhenPaonIsLessThan10CharsAndAllLocalityPopulated() {
        final uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address mockAddress = getAddressWhenPaonLessThan10CharsAndAllLocalityPopulated();
        when(policeIndividualDefendant.getAddress()).thenReturn(mockAddress);

        Address address = addressConverter.convert(policeIndividualDefendant.getAddress());
        assertNotNull(address);
        assertThat(address.getAddress1(), is("Hoover Building")); //addressLine1 = saon
        assertThat(address.getAddress2(), is("682 Green Lane")); //addressLine2 = first 10 character of paon  + street description
        assertThat(address.getAddress3(), is("East Ham")); //addressLine3 = Locality
        assertThat(address.getAddress4(), is("Ilford")); //addressLine4 = Town
        assertThat(address.getAddress5(), is("Greater London")); //addressLine3 = PostTown
        //no more line Administrative Area will be ignored.
    }

    //When Paon is less than 10 characters And SAON/Street Description/Locality has more than 35 char text
    @Test
    public void convertWhenPaonIsLessThan10CharsAndLocalityStreetDescGT35Chars() {
        final uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address mockAddress = getAddressWhenPaonLessThan10CharsAndLocalityStreetDescGT35Chars();
        when(policeIndividualDefendant.getAddress()).thenReturn(mockAddress);

        Address address = addressConverter.convert(policeIndividualDefendant.getAddress());
        assertNotNull(address);
        assertThat(address.getAddress1(), is("Hoover Building")); //addressLine1 = saon
        assertThat(address.getAddress2(), is("682 Green Lane, Near Kingsbury cir+")); //addressLine2 = first 10 character of paon  + street description //+ appended for 32nd char
        assertThat(address.getAddress3(), is("East Ham, Northern Eastern Greater+")); //addressLine3 = Locality (East Ham, Northern Eastern Greater London concatenated)
        assertThat(address.getAddress4(), is("Ilford")); //addressLine4 = Town
        assertThat(address.getAddress5(), is("Greater London")); //addressLine3 = PostTown
        //no more line Administrative Area will be ignored.
    }

    //When Paon is less than 10 characters And SAON/Street Description/Locality has exact equal to 35 char text
    @Test
    public void convertWhenPaonIsLessThan10CharsAndLocalityStreetDescExactEqualTo35Chars() {
        final uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address mockAddress = getAddressWhenPaonLessThan10CharsAndLocalityStreetDescEQT35Chars();
        when(policeIndividualDefendant.getAddress()).thenReturn(mockAddress);

        Address address = addressConverter.convert(policeIndividualDefendant.getAddress());
        assertNotNull(address);
        assertThat(address.getAddress1(), is("Hoover Building")); //addressLine1 = saon
        assertThat(address.getAddress2(), is("682 Green Lane, Near Kingsbury cir+")); //addressLine2 = first 10 character of paon  + street description //+ appended for 32nd char
        assertThat(address.getAddress3(), is("East Ham, North East Greater London")); //addressLine3 = Locality
        assertThat(address.getAddress4(), is("Ilford")); //addressLine4 = Town
        assertThat(address.getAddress5(), is("Greater London")); //addressLine3 = PostTown
        //no more line Administrative Area will be ignored.
    }

    private uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address getAddressWithPaonLessThan10CharsAndSoanPopulated() {
        return uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address.address()
                .withPaon("682")
                .withSaon("Hoover Building")
                .withStreetDescription("Green Lane")
                .withTown("Ilford")
                .withAdministrativeArea("Essex")
                .withPostcode("SW1 2HH")
                .build();
    }

    private uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address getAddressWhenPaonIsGreaterThan10CharsAndSaonNotPopulated() {
        return uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address.address()
                .withPaon("682 Essex Electrical Supplies")
                .withStreetDescription("Green Lane")
                .withTown("Ilford")
                .withAdministrativeArea("Essex")
                .withPostcode("IG3 9RX")
                .build();
    }

    private uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address getAddressWhenPaonIsGreaterThan10CharsAndSaonIsPopulated() {
        return uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address.address()
                .withPaon("682 Essex Electrical Supplies")
                .withSaon("Hoover Building")
                .withStreetDescription("Green Lane")
                .withTown("Ilford")
                .withAdministrativeArea("Essex")
                .withPostcode("IG3 9RX")
                .build();
    }

    private uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address getAddressWhenPaonLessThan10CharsAndLocalityStreetDescGT35Chars() {
        return uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address.address()
                .withPaon("682")
                .withSaon("Hoover Building")
                .withStreetDescription("Green Lane, Near Kingsbury circle")
                .withLocality("East Ham, Northern Eastern Greater London")
                .withTown("Ilford")
                .withPostTown("Greater London")
                .withAdministrativeArea("Essex")
                .withPostcode("IG3 9RX")
                .build();
    }

    private uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address getAddressWhenPaonLessThan10CharsAndLocalityStreetDescEQT35Chars() {
        return uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address.address()
                .withPaon("682")
                .withSaon("Hoover Building")
                .withStreetDescription("Green Lane, Near Kingsbury circle")
                .withLocality("East Ham, North East Greater London")
                .withTown("Ilford")
                .withPostTown("Greater London")
                .withAdministrativeArea("Essex")
                .withPostcode("IG3 9RX")
                .build();
    }

    private uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address getAddressWhenPaonLessThan10CharsAndAllLocalityPopulated() {
        return uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address.address()
                .withPaon("682")
                .withSaon("Hoover Building")
                .withStreetDescription("Green Lane")
                .withLocality("East Ham")
                .withTown("Ilford")
                .withPostTown("Greater London")
                .withAdministrativeArea("Essex")
                .withPostcode("IG3 9RX")
                .build();
    }

    private uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address getAddressWhenOnlyPoanIsPopulatedWithLessThan10Chars() {
        return uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address.address()
                .withPaon("682       ")
                .withStreetDescription("Green Lane")
                .withLocality("East Ham")
                .withTown("Ilford")
                .withAdministrativeArea("Essex")
                .withPostcode("IG3 9RX")
                .build();
    }

    private uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address getMockAddressWhenOnlySoanPopulated() {
        return uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address.address()
                .withSaon("Hanger lane roundabout")
                .withPostcode("SW1 2HH")
                .build();
    }

}


