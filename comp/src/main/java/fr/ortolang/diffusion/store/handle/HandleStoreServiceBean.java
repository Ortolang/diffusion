package fr.ortolang.diffusion.store.handle;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.security.PrivateKey;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.RolesAllowed;
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
@RolesAllowed({ "system", "user" })
@Lock(LockType.READ)
public class HandleStoreServiceBean implements HandleStoreService {

	private static Logger logger = Logger.getLogger(HandleStoreServiceBean.class.getName());

	private PrivateKey privkey = null;
	private HandleResolver resolver;
	private PublicKeyAuthenticationInfo auth;
	private AdminRecord admin;

	public HandleStoreServiceBean() {
		logger.log(Level.INFO, "Instanciating service");
	}

	@PostConstruct
	public void init() {
		logger.log(Level.INFO, "Initializing service");
		try {
			URL keyFileURL = HandleStoreServiceBean.class.getClassLoader().getResource(OrtolangConfig.getInstance().getProperty("handle.key"));
			logger.log(Level.FINE, "using private key file : " + keyFileURL.getPath());
			File f = new File(keyFileURL.getPath());
			FileInputStream fs = new FileInputStream(f);
			byte[] key = new byte[(int) f.length()];
			int n = 0;
			while (n < key.length) {
				key[n++] = (byte) fs.read();
			}
			fs.read(key);
			fs.close();

			logger.log(Level.FINE, "decrypting key");
			byte[] secKey = OrtolangConfig.getInstance().getProperty("handle.key.passphrase").getBytes();
			key = Util.decrypt(key, secKey);
			privkey = Util.getPrivateKeyFromBytes(key, 0);

			logger.log(Level.FINE, "building handle resolver");
			resolver = new HandleResolver();

			auth = new PublicKeyAuthenticationInfo(OrtolangConfig.getInstance().getProperty("handle.admin").getBytes(), Integer.valueOf(
					OrtolangConfig.getInstance().getProperty("handle.index")).intValue(), privkey);
			admin = new AdminRecord(OrtolangConfig.getInstance().getProperty("handle.admin").getBytes(), 300, true, true, true, true, true, true, true, true, true, true, true,
					true);
			logger.log(Level.FINE, "auth info: " + auth.toString());
			logger.log(Level.FINE, "admin record: " + admin.toString());

		} catch (Throwable t) {
			logger.log(Level.SEVERE, "Unable to initialize Handle Store : " + t, t);
		}
	}

	@PreDestroy
	public void shutdown() {
		logger.log(Level.INFO, "Shuting down service");

	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String create(String suffix, String... values) throws HandleStoreServiceException {
		int timestamp = (int) (System.currentTimeMillis() / 1000);
		String name = OrtolangConfig.getInstance().getProperty("handle.prefix") + "/" + suffix;
		logger.log(Level.FINE, "complete handle name : " + name);
		HandleValue[] val = new HandleValue[values.length + 1];
		val[0] = new HandleValue(100, "HS_ADMIN".getBytes(), Encoder.encodeAdminRecord(admin), HandleValue.TTL_TYPE_RELATIVE, 86400, timestamp, null, true, true, true, false);
		logger.log(Level.FINE, "handle value created for admin : " + val[0]);
		for (int i = 1; i <= values.length; i++) {
			val[i] = new HandleValue(i, Common.STD_TYPE_URL, values[i - 1].getBytes(), HandleValue.TTL_TYPE_RELATIVE, 86400, timestamp, null, true, true, true, false);
			logger.log(Level.FINE, "handle value created for index " + i + " : " + val[i]);
		}
		try {
			logger.log(Level.FINE, "building create request");
			CreateHandleRequest req = new CreateHandleRequest(name.getBytes(), val, auth);
			logger.log(Level.FINE, "processing request...");
			AbstractResponse response = resolver.processRequest(req);
			logger.log(Level.FINE, "response received");
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
		logger.log(Level.FINE, "complete handle name : " + name);
		try {
			logger.log(Level.FINE, "building resolution request");
			ResolutionRequest req = new ResolutionRequest(name.getBytes(), null, null, null);
			logger.log(Level.FINE, "processing request...");
			AbstractResponse response = resolver.processRequest(req);
			logger.log(Level.FINE, "response received");
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
		logger.log(Level.FINE, "complete handle name : " + name);
		try {
			logger.log(Level.FINE, "building resolution request");
			ResolutionRequest req = new ResolutionRequest(name.getBytes(), null, null, null);
			logger.log(Level.FINE, "processing request...");
			AbstractResponse response = resolver.processRequest(req);
			logger.log(Level.FINE, "response received");
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
		logger.log(Level.FINE, "complete handle name : " + name);
		try {
			logger.log(Level.FINE, "building delete request");
			DeleteHandleRequest req = new DeleteHandleRequest(name.getBytes(), auth);
			logger.log(Level.FINE, "processing request...");
			AbstractResponse response = resolver.processRequest(req);
			logger.log(Level.FINE, "response received");
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
