/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */

import com.intel.mtwilson.trustagent.model.TcbMeasurement;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

/**
 *
 * @author rksavino
 */
public class TrustagentModelTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TrustagentModelTest.class);
    
    public TrustagentModelTest() {
    }
    
    @Test
    public void TcbMeasurementJaxb() {
        try {
            InputStream in = getClass().getResourceAsStream("/measurement.xml");
            String xml = IOUtils.toString(in, Charset.forName("UTF-8"));
            log.info("XML string: {}", xml);
            
            JAXBContext context = JAXBContext.newInstance(TcbMeasurement.class);
            Unmarshaller um = context.createUnmarshaller();
            TcbMeasurement tcbMeasurement = (TcbMeasurement) um.unmarshal(new StringReader(xml));
            log.info("TcbMeasurement unmarshalled successfully.");
            
            javax.xml.bind.Marshaller m = context.createMarshaller();
            StringWriter sw = new StringWriter();
            m.marshal(tcbMeasurement, sw);
            log.info("Marshalled TcbMeasurement: {}", sw.toString());
        } catch (IOException e) {
            log.warn("IOException: {}", e); //e.getMessage());
        } catch (Exception e) {
            log.warn("Exception: {}", e); //e.getMessage());
        }
    }
}
