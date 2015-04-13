package fr.ortolang.diffusion.store.handle;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.security.PrivateKey;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import net.handle.hdllib.AbstractMessage;
import net.handle.hdllib.AbstractResponse;
import net.handle.hdllib.AdminRecord;
import net.handle.hdllib.Common;
import net.handle.hdllib.CreateHandleRequest;
import net.handle.hdllib.DeleteHandleRequest;
import net.handle.hdllib.Encoder;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleResolver;
import net.handle.hdllib.HandleValue;
import net.handle.hdllib.PublicKeyAuthenticationInfo;
import net.handle.hdllib.ResolutionRequest;
import net.handle.hdllib.ResolutionResponse;
import net.handle.hdllib.Util;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangConfig;

@Local(HandleStoreService.class)
@Singleton(name = HandleStoreService.SERVICE_NAME)
@SecurityDomain("ortolang")
@Lock(LockType.READ)
@PermitAll
public class HandleStoreServiceBean implements HandleStoreService {

	private static final Logger LOGGER = Logger.getLogger(HandleStoreServiceBean.class.getName());

	private PrivateKey privkey = null;
	private HandleResolver resolver;
	private PublicKeyAuthenticationInfo auth;
	private AdminRecord admin;

	public HandleStoreServiceBean() {
		LOGGER.log(Level.INFO, "Instantiating service");
	}

	@PostConstruct
	public void init() {
		LOGGER.log(Level.INFO, "Initializing service");
		try {
			URL keyFileURL = HandleStoreServiceBean.class.getClassLoader().getResource(OrtolangConfig.getInstance().getProperty("handle.key"));
			LOGGER.log(Level.FINE, "using private key file : " + keyFileURL.getPath());
			File f = new File(keyFileURL.getPath());
			FileInputStream fs = new FileInputStream(f);
			byte[] key = new byte[(int) f.length()];
			int n = 0;
			while (n < key.length) {
				key[n++] = (byte) fs.read();
			}
			fs.read(key);
			fs.close();

			LOGGER.log(Level.FINE, "decrypting key");
			byte[] secKey = OrtolangConfig.getInstance().getProperty("handle.key.passphrase").getBytes();
			key = Util.decrypt(key, secKey);
			privkey = Util.getPrivateKeyFromBytes(key, 0);

			LOGGER.log(Level.FINE, "building handle resolver");
			resolver = new HandleResolver();

			auth = new PublicKeyAuthenticationInfo(OrtolangConfig.getInstance().getProperty("handle.admin").getBytes(), Integer.parseInt(
					OrtolangConfig.getInstance().getProperty("handle.index")), privkey);
			admin = new AdminRecord(OrtolangConfig.getInstance().getProperty("handle.admin").getBytes(), 300, true, true, true, true, true, true, true, true, true, true, true,
					true);
			LOGGER.log(Level.FINE, "auth info: " + auth.toString());
			LOGGER.log(Level.FINE, "admin record: " + admin.toString());

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Unable to initialize Handle Store : " + e.getMessage(), e);
		}
	}

	@PreDestroy
	public void shutdown() {
		LOGGER.log(Level.INFO, "Shutting down service");

	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String create(String suffix, String... values) throws HandleStoreServiceException {
		int timestamp = (int) (System.currentTimeMillis() / 1000);
		String name = OrtolangConfig.getInstance().getProperty("handle.prefix") + "/" + suffix;
		LOGGER.log(Level.FINE, "complete handle name : " + name);
		HandleValue[] val = new HandleValue[values.length + 1];
		val[0] = new HandleValue(100, "HS_ADMIN".getBytes(), Encoder.encodeAdminRecord(admin), HandleValue.TTL_TYPE_RELATIVE, 86400, timestamp, null, true, true, true, false);
		LOGGER.log(Level.FINE, "handle value created for admin : " + val[0]);
		for (int i = 1; i <= values.length; i++) {
			val[i] = new HandleValue(i, Common.STD_TYPE_URL, values[i - 1].getBytes(), HandleValue.TTL_TYPE_RELATIVE, 86400, timestamp, null, true, true, true, false);
			LOGGER.log(Level.FINE, "handle value created for index " + i + " : " + val[i]);
		}
		try {
			LOGGER.log(Level.FINE, "building create request");
			CreateHandleRequest req = new CreateHandleRequest(name.getBytes(), val, auth);
			LOGGER.log(Level.FINE, "processing request...");
			AbstractResponse response = resolver.processRequest(req);
			LOGGER.log(Level.FINE, "response received");
			if (response.responseCode != AbstractMessage.RC_SUCCESS) {
				throw new HandleStoreServiceException("handle server response error code: " + response.responseCode);
			}
			return "hdl:" + name;
		} catch (HandleException e) {
			throw new HandleStoreServiceException("error during creating handle", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public boolean exists(String suffix) throws HandleStoreServiceException {
		String name = OrtolangConfig.getInstance().getProperty("handle.prefix") + "/" + suffix;
		LOGGER.log(Level.FINE, "complete handle name : " + name);
		try {
			LOGGER.log(Level.FINE, "building resolution request");
			ResolutionRequest req = new ResolutionRequest(name.getBytes(), null, null, null);
			LOGGER.log(Level.FINE, "processing request...");
			AbstractResponse response = resolver.processRequest(req);
			LOGGER.log(Level.FINE, "response received");
			if (response.responseCode == AbstractMessage.RC_HANDLE_NOT_FOUND) {
				return false;
			}
			if (response.responseCode != AbstractMessage.RC_SUCCESS) {
				throw new HandleStoreServiceException("handle server response error code: " + response.responseCode);
			}
			return true;
		} catch (HandleException e) {
			throw new HandleStoreServiceException("error during creating handle", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String read(String suffix) throws HandleStoreServiceException {
		String name = OrtolangConfig.getInstance().getProperty("handle.prefix") + "/" + suffix;
		LOGGER.log(Level.FINE, "complete handle name : " + name);
		try {
			LOGGER.log(Level.FINE, "building resolution request");
			ResolutionRequest req = new ResolutionRequest(name.getBytes(), null, null, null);
			LOGGER.log(Level.FINE, "processing request...");
			AbstractResponse response = resolver.processRequest(req);
			LOGGER.log(Level.FINE, "response received");
			if (response.responseCode != AbstractMessage.RC_SUCCESS) {
				throw new HandleStoreServiceException("handle server response error code: " + response.responseCode);
			}
			if (response instanceof ResolutionResponse) {
				HandleValue[] values = ((ResolutionResponse) response).getHandleValues();
				for (int i = 0; values != null && i < values.length; i++) {
					if (values[i] != null && values[i].hasType(Common.STD_TYPE_URL)) {
						return values[i].getDataAsString();
					}
				}
			}
			return null;
		} catch (HandleException e) {
			throw new HandleStoreServiceException("error during creating handle", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void delete(String suffix) throws HandleStoreServiceException {
		String name = OrtolangConfig.getInstance().getProperty("handle.prefix") + "/" + suffix;
		LOGGER.log(Level.FINE, "complete handle name : " + name);
		try {
			LOGGER.log(Level.FINE, "building delete request");
			DeleteHandleRequest req = new DeleteHandleRequest(name.getBytes(), auth);
			LOGGER.log(Level.FINE, "processing request...");
			AbstractResponse response = resolver.processRequest(req);
			LOGGER.log(Level.FINE, "response received");
			if (response.responseCode != AbstractMessage.RC_SUCCESS) {
				throw new HandleStoreServiceException("handle server response error code: " + response.responseCode);
			}
		} catch (HandleException e) {
			throw new HandleStoreServiceException("error during deleting handle", e);
		}
	}

	public void setTraceEnabled() {
		resolver.traceMessages = true;
	}

}
