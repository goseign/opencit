/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.mtwilson.as.rest.v2.model;

import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.repository.FilterCriteria;
import com.intel.mtwilson.jaxrs2.DefaultFilterCriteria;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

/**
 *
 * @author ssbangal
 */
public class HostAikCertificateFilterCriteria extends DefaultFilterCriteria implements FilterCriteria<HostAikCertificate> {
    
    @PathParam("host_id")
    public UUID hostUuid;
    @QueryParam("id")
    public String aikSha256;
    
}
