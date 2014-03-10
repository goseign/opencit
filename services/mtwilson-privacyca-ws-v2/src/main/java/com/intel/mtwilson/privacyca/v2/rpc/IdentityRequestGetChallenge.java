/*
 * Copyright (C) 2014 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.privacyca.v2.rpc;

import com.intel.dcsg.cpg.x509.X509Util;
import com.intel.mtwilson.My;
import com.intel.mtwilson.launcher.ws.ext.RPC;
import gov.niarl.his.privacyca.TpmIdentityProof;
import gov.niarl.his.privacyca.TpmIdentityRequest;
import gov.niarl.his.privacyca.TpmKeyParams;
import gov.niarl.his.privacyca.TpmPubKey;
import gov.niarl.his.privacyca.TpmSymmetricKey;
import gov.niarl.his.privacyca.TpmUtils;
import gov.niarl.his.privacyca.TpmUtils.TpmUnsignedConversionException;
import java.io.File;
import java.util.List;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author jbuhacoff
 */
@RPC("aik_request_get_challenge")
public class IdentityRequestGetChallenge implements Callable<byte[]> {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IdentityRequestGetChallenge.class);

    private byte[] identityRequest;
    private byte[] endorsementCertificate;

    public void setIdentityRequest(byte[] identityRequest) {
        this.identityRequest = identityRequest;
    }

    public void setEndorsementCertificate(byte[] endorsementCertificate) {
        this.endorsementCertificate = endorsementCertificate;
    }

    public byte[] getIdentityRequest() {
        return identityRequest;
    }

    public byte[] getEndorsementCertificate() {
        return endorsementCertificate;
    }

    private Map<Principal, RSAPublicKey> getEndorsementCertificates() throws IOException, CertificateException {
	 Map<Principal, RSAPublicKey> endorsementCerts = new HashMap<Principal, RSAPublicKey>();
     File ekCacertsPemFile = My.configuration().getPrivacyCaEndorsementCacertsFile();
     try(FileInputStream in = new FileInputStream(ekCacertsPemFile)) {
         String ekCacertsPem = IOUtils.toString(in); // throws IOException
         List<X509Certificate> ekCacerts = X509Util.decodePemCertificates(ekCacertsPem); // throws CertificateException
         for(X509Certificate ekCacert : ekCacerts) {
             endorsementCerts.put((Principal)ekCacert.getSubjectDN(), (RSAPublicKey)ekCacert.getPublicKey());
         }
     }
        return endorsementCerts;
    }
    
    // TODO:   fix the implementation... apparently  HisPrivacyCAWebService2Impl  was using member variables to store info between two requests steps, but we are stateless so need to address that somehow.
    @Override
    public byte[] call() throws Exception {
	 RSAPrivateKey caPrivKey = TpmUtils.privKeyFromP12(My.configuration().getPrivacyCaIdentityP12().getAbsolutePath(), My.configuration().getPrivacyCaIdentityPassword());
	 X509Certificate caPubCert = TpmUtils.certFromP12(My.configuration().getPrivacyCaIdentityP12().getAbsolutePath(), My.configuration().getPrivacyCaIdentityPassword());
	 
     // load the trusted ek cacerts
        Map<Principal, RSAPublicKey> endorsementCerts = getEndorsementCertificates();
        
			//decrypt identityRequest and endorsementCertificate
			TpmIdentityRequest idReq = new TpmIdentityRequest(identityRequest);
            TpmIdentityProof idProof = idReq.decrypt(caPrivKey); // TODO   how to transfer this to the id request submit response?
			TpmIdentityRequest tempEC = new TpmIdentityRequest(endorsementCertificate);
			X509Certificate ekCert = TpmUtils.certFromBytes(tempEC.decryptRaw(caPrivKey));
            log.debug("Validating endorsement certificate");
            ekCert.verify(endorsementCerts.get(ekCert.getIssuerDN())); // throws SignatureException
			//check out the endorsement certificate
			//if the cert is good, issue challenge
            byte[] identityRequestChallenge = TpmUtils.createRandomBytes(32);

            //check the rest of the identity proof
			if(!idProof.checkValidity((RSAPublicKey)caPubCert.getPublicKey())){
                log.error("TPM IDPROOF failed validit check");
                throw new RuntimeException("Invalid identity request");
			}
            
            // save the challenge and idproof for use in identity request submit response if the client successfully answers the challenge
            // the filename is the challenge (in hex) and the content is the idproof
            File datadir = new File(My.filesystem().getBootstrapFilesystem().getVarPath() + File.separator + "privacyca-aik-requests"); // TODO:  put this in a privacyca configuration class
            if( !datadir.exists() ) { datadir.mkdirs(); }
            String filename = TpmUtils.byteArrayToHexString(identityRequest); //Hex.encodeHexString(identityRequestChallenge)
            try(FileOutputStream out = new FileOutputStream(datadir.toPath().resolve(filename).toFile())) {
                IOUtils.write(idProof.toByteArray(), out);
            }
            String optionsFilename = filename + ".opt";
            try(FileOutputStream out = new FileOutputStream(datadir.toPath().resolve(optionsFilename).toFile())) {
                // and save the 3 trousers mode options into a second file because they are not included 
                Util.TpmIdentityProofOptions options = new Util.TpmIdentityProofOptions();
                options.TrousersModeIV = idProof.getIVmode();
                options.TrousersModeSymkeyEncscheme = idProof.getSymkeyEncscheme();
                options.TrousersModeBlankOeap = idProof.getOeapMode();
                String hexOptions = Util.encodeTpmIdentityProofOptionsToHex(options);
                IOUtils.write(hexOptions, out);
            }
            // also save the ekcert for the identity request submit response 
            String ekcertFilename = filename + ".ekcert";
            try(FileOutputStream out = new FileOutputStream(datadir.toPath().resolve(ekcertFilename).toFile())) {
                IOUtils.write(ekCert.getEncoded(), out);
            }
            
			//encrypt the challenge and return
			log.debug("Phase 1 details:");
			log.debug(" AIK blob: " + TpmUtils.byteArrayToHexString(idProof.getAik().toByteArray()));
			log.debug(" challenge: " + TpmUtils.byteArrayToHexString(identityRequestChallenge));
			byte[] toReturn = createReturn(idProof.getAik(), (RSAPublicKey)ekCert.getPublicKey(), identityRequestChallenge);
			log.debug(" toReturn: " + TpmUtils.byteArrayToHexString(toReturn));
			return toReturn;        
    }
    
	private static byte[] createReturn(TpmPubKey aik, RSAPublicKey pubEk, byte[] challengeRaw) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, TpmUnsignedConversionException, IOException{
		byte [] key = TpmUtils.createRandomBytes(16);
		byte [] iv = TpmUtils.createRandomBytes(16);
		byte [] encryptedBlob = TpmUtils.concat(iv, TpmUtils.TCGSymEncrypt(challengeRaw, key, iv));
		byte [] credSize = TpmUtils.intToByteArray(encryptedBlob.length);

		TpmSymmetricKey symKey = new TpmSymmetricKey();
		symKey.setKeyBlob(key);
		symKey.setAlgorithmId(TpmKeyParams.TPM_ALG_AES);
		symKey.setEncScheme(TpmKeyParams.TPM_ES_SYM_CBC_PKCS5PAD);
		TpmKeyParams keyParms = new TpmKeyParams();
		keyParms.setAlgorithmId(TpmKeyParams.TPM_ALG_AES);
		keyParms.setEncScheme(TpmKeyParams.TPM_ES_NONE);
		keyParms.setSigScheme((short)0);
		keyParms.setSubParams(null);
		keyParms.setTrouSerSmode(true);

		byte [] asymBlob = TpmUtils.TCGAsymEncrypt(TpmUtils.concat(symKey.toByteArray(), TpmUtils.sha1hash(aik.toByteArray())), pubEk);
		byte [] symBlob = TpmUtils.concat(TpmUtils.concat(credSize, keyParms.toByteArray()), encryptedBlob);
		return TpmUtils.concat(asymBlob, symBlob);
	}
    
}
