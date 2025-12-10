package uk.gov.moj.cpp.staging.command.api.soap;

import uk.gov.cjse.schemas.endpoint.types.ObjectFactory;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;

@WebService(serviceName = "CJSEService", targetNamespace = "http://schemas.cjse.gov.uk/endpoint/wsdl/", portName = "Delivery", wsdlLocation = "/META-INF/wsdl/spi.wsdl", name = "CPPGatewayToSPI")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@XmlSeeAlso({
        ObjectFactory.class
})
public class CPPGatewayToSPI extends GatewayService {
}
