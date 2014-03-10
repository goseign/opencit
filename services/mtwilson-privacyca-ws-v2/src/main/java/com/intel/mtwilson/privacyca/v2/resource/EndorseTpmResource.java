/*
 * Copyright (C) 2014 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.privacyca.v2.resource;

import com.intel.mtwilson.jersey.http.OtherMediaType;
import com.intel.mtwilson.launcher.ws.ext.V2;
import com.intel.mtwilson.privacyca.v2.rpc.EndorseTpm;
import java.security.cert.X509Certificate;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author jbuhacoff
 */
@V2
@Path("/privacyca/tpm-endorsement")
public class EndorseTpmResource {
    
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces({OtherMediaType.APPLICATION_PKIX_CERT, MediaType.APPLICATION_OCTET_STREAM, OtherMediaType.APPLICATION_X_PEM_FILE, MediaType.TEXT_PLAIN})
    public X509Certificate endorseTpm(byte[] ekModulus) throws Exception {
        EndorseTpm rpc = new EndorseTpm();
        rpc.setEkModulus(ekModulus);
        return rpc.call();
    }
}
