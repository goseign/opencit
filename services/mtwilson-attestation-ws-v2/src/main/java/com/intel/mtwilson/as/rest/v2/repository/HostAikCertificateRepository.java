/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.mtwilson.as.rest.v2.repository;

import com.intel.dcsg.cpg.crypto.Sha1Digest;
import com.intel.dcsg.cpg.io.UUID;
import com.intel.mountwilson.as.common.ASException;
import com.intel.mtwilson.My;
import com.intel.mtwilson.as.controller.TblHostsJpaController;
import com.intel.mtwilson.as.data.TblHosts;
import com.intel.mtwilson.as.rest.v2.model.HostAikCertificate;
import com.intel.mtwilson.as.rest.v2.model.HostAikCertificateCollection;
import com.intel.mtwilson.as.rest.v2.model.HostAikCertificateFilterCriteria;
import com.intel.mtwilson.as.rest.v2.model.HostAikCertificateLocator;
import com.intel.mtwilson.datatypes.ErrorCode;
import com.intel.mtwilson.jersey.resource.SimpleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ssbangal
 */
public class HostAikCertificateRepository implements SimpleRepository<HostAikCertificate, HostAikCertificateCollection, HostAikCertificateFilterCriteria, HostAikCertificateLocator> {

    Logger log = LoggerFactory.getLogger(getClass().getName());

    @Override
    public HostAikCertificateCollection search(HostAikCertificateFilterCriteria criteria) {
        HostAikCertificateCollection objCollection = new HostAikCertificateCollection();
        try {
            TblHostsJpaController jpaController = My.jpa().mwHosts();
            if (criteria.hostUuid != null) {
                TblHosts obj = jpaController.findHostByUuid(criteria.hostUuid.toString());
                if (obj != null) {
                    objCollection.getAikCertificates().add(convert(obj));
                }
            } // TODO: Need to add the AIKSha1 search criteria later when we have the capability of multiple AIKs
        } catch (ASException aex) {
            throw aex;            
        } catch (Exception ex) {
            log.error("Error during search for hosts.", ex);
            throw new ASException(ErrorCode.AS_QUERY_HOST_ERROR, ex.getClass().getSimpleName());
        }
        return objCollection;
    }

    @Override
    public HostAikCertificate retrieve(HostAikCertificateLocator locator) {
        if (locator == null || locator.hostUuid == null) { return null; }
        String id = locator.hostUuid.toString();
        
        try {
            TblHostsJpaController jpaController = My.jpa().mwHosts();
            TblHosts obj = jpaController.findHostByUuid(id);
            if (obj != null) {
                HostAikCertificate hostAik = convert(obj);
                return hostAik;
            }
        } catch (ASException aex) {
            throw aex;            
        } catch (Exception ex) {
            log.error("Error during search for hosts.", ex);
            throw new ASException(ErrorCode.AS_QUERY_HOST_ERROR, ex.getClass().getSimpleName());
        }        
        return null;
    }

    @Override
    public void store(HostAikCertificate item) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void create(HostAikCertificate item) {
        try {
            TblHostsJpaController jpaController = My.jpa().mwHosts();
            TblHosts obj = jpaController.findHostByUuid(item.getHostUuid());
            if (obj != null) {
                obj.setAIKCertificate(item.getCertificate().toString());
                Sha1Digest aikSha1 = Sha1Digest.valueOf(item.getCertificate());
                
                jpaController.edit(obj);
            }
        } catch (ASException aex) {
            throw aex;            
        } catch (Exception ex) {
            log.error("Error during aik update for the host.", ex);
            throw new ASException(ErrorCode.AS_AIK_CREATE_ERROR, ex.getClass().getSimpleName());
        }        
    }

    @Override
    public void delete(HostAikCertificateLocator locator) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(HostAikCertificateFilterCriteria criteria) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    private HostAikCertificate convert(TblHosts obj) {
        HostAikCertificate convObj = new HostAikCertificate();
        convObj.setId(UUID.valueOf(obj.getUuid_hex()));
        convObj.setCertificate(obj.getAIKCertificate().getBytes());
        convObj.setAikSha1(obj.getAikSha1());
        return convObj;
    }
    
}