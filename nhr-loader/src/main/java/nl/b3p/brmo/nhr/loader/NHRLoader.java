/*
 * Copyright (C) 2022 B3Partners B.V.
 *
 * SPDX-License-Identifier: MIT
 *
 */

package nl.b3p.brmo.nhr.loader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.ws.BindingProvider;
import nl.b3p.brmo.loader.BrmoFramework;
import nl.kvk.schemas.schemas.hrip.dataservice._2015._02.Dataservice;
import nl.kvk.schemas.schemas.hrip.dataservice._2015._02.InschrijvingRequestType;
import nl.kvk.schemas.schemas.hrip.dataservice._2015._02.InschrijvingResponseType;
import nl.kvk.schemas.schemas.hrip.dataservice._2015._02.MeldingType;
import nl.kvk.schemas.schemas.hrip.dataservice._2015._02.ObjectFactory;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;

public class NHRLoader {
    // JAXB context used to serialize NHR responses for the BRMO.
    private static JAXBContext jaxbContext;
    private static ObjectFactory factory = new ObjectFactory();

    /**
     * Send a single NHR request.
     */
    public static void sendSingleRequest(Dataservice dataservice, BrmoFramework brmoFramework, String kvkNummer, String rsin) throws Exception {
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(InschrijvingResponseType.class);
        }

        InschrijvingRequestType request = new InschrijvingRequestType();
        String description;
        if (kvkNummer != null) {
            description = String.format("KVK nummer %s", kvkNummer);
            request.setKvkNummer(kvkNummer);
        } else if (rsin != null) {
            description = String.format("KVK RSIN %s", rsin);
            request.setRsin(rsin);
        } else {
            throw new IllegalArgumentException("KVK nummer en RSIN zijn beiden null");
        }

        // Generate the UUID manually, as CXF generates urn:uuid:{uuid}, which is explicitly disallowed.
        BindingProvider bindingProvider = (BindingProvider)dataservice;
        AddressingProperties properties = (AddressingProperties)bindingProvider.getRequestContext().get(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES);
        AttributedURIType msgid = new AttributedURIType();
        msgid.setValue("uuid:" + UUID.randomUUID().toString());
        properties.setMessageID(msgid);

        // Send the actual request.
        InschrijvingResponseType response = dataservice.ophalenInschrijving(request);

        List<MeldingType> errors = response.getMeldingen().getFout();
        if (!errors.isEmpty()) {
            HashMap<String, String> errorMap = new HashMap<>();
            for (MeldingType melding : errors) {
                errorMap.put(melding.getCode(), melding.getOmschrijving());
            }

            throw new NHRException(errorMap);
        }

        // Serialize the XML into the format expected by BRMO.
        JAXBElement<InschrijvingResponseType> wrappedResponse = factory.createOphalenInschrijvingResponse(response);
        Marshaller marshaller = jaxbContext.createMarshaller();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        marshaller.marshal(wrappedResponse, outputStream);

        brmoFramework.loadFromStream(BrmoFramework.BR_NHR, new ByteArrayInputStream(outputStream.toByteArray()), String.format("NHR %s %s", LocalDateTime.now(), description), (Long) null);
    }
}
