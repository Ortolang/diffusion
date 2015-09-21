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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import net.handle.hdllib.AdminRecord;
import net.handle.hdllib.Common;
import net.handle.hdllib.Encoder;
import net.handle.hdllib.HandleValue;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.store.handle.entity.Handle;

@Local(HandleStoreService.class)
@Stateless(name=HandleStoreService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class HandleStoreServiceBean implements HandleStoreService {

	private static final Logger LOGGER = Logger.getLogger(HandleStoreServiceBean.class.getName());
	private static byte[] admin = null;
	
	private static final String[] OBJECT_TYPE_LIST = new String[] { };
    private static final String[] OBJECT_PERMISSIONS_LIST = new String[] { };
    
    @PersistenceContext(unitName = "ortolangPU")
	private EntityManager em;

	private byte[] getAdminValue() {
		if ( admin == null ) {
			String adminHandle = "0.NA/" + OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.HANDLE_PREFIX);
			admin = Encoder.encodeAdminRecord(new AdminRecord(adminHandle.getBytes(), 300, true, true, true, true, true, true, true, true, true, true, true, true));
		}
		return admin;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void recordHandle(String handle, String key, String url) throws HandleStoreServiceException {
		String name = handle.toUpperCase(Locale.ENGLISH);
		LOGGER.log(Level.FINE, "recording handle : " + handle);
		try {
			List<Handle> oldHandles = listHandleValues(name);
			for ( Handle old : oldHandles ) {
				LOGGER.log(Level.FINE, "deleting previously recorded handle for this name: " + old);
				em.remove(old);
				em.flush();
			}
		} catch (HandleNotFoundException e) {
			//
		}
		int timestamp = (int) (System.currentTimeMillis() / 1000);
		Handle adminValue = new Handle(name.getBytes(), 100, key, Common.ADMIN_TYPE, getAdminValue(), HandleValue.TTL_TYPE_RELATIVE, 86400, timestamp, null, true, true, true, false);
		Handle urlValue = new Handle(name.getBytes(), 1, key, Common.STD_TYPE_URL, url.getBytes(), HandleValue.TTL_TYPE_RELATIVE, 86400, timestamp, null, true, true, true, false);
		em.persist(adminValue);
		em.persist(urlValue);
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<Handle> listHandleValues(String handle) throws HandleStoreServiceException, HandleNotFoundException {
		String name = handle.toUpperCase(Locale.ENGLISH);
		List<Handle> handles = null;
		TypedQuery<Handle> query = em.createNamedQuery("findHandleByName", Handle.class).setParameter("name", name.getBytes()); 
		handles = query.getResultList();
		if ( handles == null || handles.size() == 0 ) {
			throw new HandleNotFoundException("no values found with handle [" + handle + "]");
		}
		return handles;
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> findHandlesForKey(String key) throws HandleStoreServiceException, HandleNotFoundException {
		List<String> names = new ArrayList<String> ();
		TypedQuery<byte[]> query = em.createNamedQuery("findHandleNameForKey", byte[].class).setParameter("key", key);
		List<byte[]> bnames = query.getResultList(); 
		if ( bnames == null || bnames.size() == 0 ) {
			throw new HandleNotFoundException("no name found for key [" + key + "]");
		}
		try {
			for (byte[] bname : bnames) {
				names.add(new String(bname, "UTF-8"));
			}
		} catch ( UnsupportedEncodingException e ) {
			throw new HandleStoreServiceException("unable to decode name", e);
		}
		return names;
	}
	
	//Service methods
    
    @Override
    public String getServiceName() {
        return HandleStoreService.SERVICE_NAME;
    }
    
    @Override
    public Map<String, String> getServiceInfos() {
        //TODO provide infos about active connections, config, ports, etc...
        return Collections.emptyMap();
    }

    @Override
    public String[] getObjectTypeList() {
        return OBJECT_TYPE_LIST;
    }

    @Override
    public String[] getObjectPermissionsList(String type) throws OrtolangException {
        return OBJECT_PERMISSIONS_LIST;
    }

    @Override
    public OrtolangObject findObject(String key) throws OrtolangException {
        throw new OrtolangException("this service does not managed any object");
    }

    @Override
    public OrtolangObjectSize getSize(String key) throws OrtolangException {
        throw new OrtolangException("this service does not managed any object");
    }
	
}
