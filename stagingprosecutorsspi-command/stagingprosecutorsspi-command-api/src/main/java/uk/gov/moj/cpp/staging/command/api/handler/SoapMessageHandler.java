package uk.gov.moj.cpp.staging.command.api.handler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SoapMessageHandler is to log the soap messages
 */
public class SoapMessageHandler implements SOAPHandler<SOAPMessageContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoapMessageHandler.class.getName());

    public Set<QName> getHeaders() {
        return Collections.emptySet();
    }

    public boolean handleMessage(SOAPMessageContext messageContext) {
        if (LOGGER.isInfoEnabled()) {
            final Boolean outboundProperty = (Boolean) messageContext.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

            if (outboundProperty.booleanValue()) {
                LOGGER.info("Outbound message:");
            } else {
                LOGGER.info("Inbound message:");
            }
            try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                messageContext.getMessage().writeTo(byteArrayOutputStream);
            } catch (IOException | SOAPException e) {
                //ignore
                LOGGER.info("Exception occured {}", e);
            }
        }

        return true;
    }

    public boolean handleFault(SOAPMessageContext messageContext) {
        return true;
    }

    public void close(final MessageContext context) {
        // Do Nothing
    }

}