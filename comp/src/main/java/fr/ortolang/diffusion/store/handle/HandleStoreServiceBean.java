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
import java.util.HashMap;
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
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import net.handle.hdllib.AdminRecord;
import net.handle.hdllib.Common;
import net.handle.hdllib.Encoder;
import net.handle.hdllib.HandleValue;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.store.handle.entity.Handle;

@Local(HandleStoreService.class)
@Stateless(name=HandleStoreService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class HandleStoreServiceBean implements HandleStoreService {

    private static final Logger LOGGER = Logger.getLogger(HandleStoreServiceBean.class.getName());
    private static byte[] admin = null;

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
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Handle readHandle(String handle) throws HandleStoreServiceException, HandleNotFoundException {
		String name = handle.toUpperCase(Locale.ENGLISH);
        LOGGER.log(Level.FINE, "reading handle : " + handle);
        try {
	        Handle hdl = em.createNamedQuery("findHandleByName", Handle.class).setParameter("name", name.getBytes()).getSingleResult();
	        return hdl;
        } catch (NoResultException e) {
        	throw new HandleNotFoundException("unable to find a handle with name: " + name);
        }
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
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void dropHandle(String handle) throws HandleStoreServiceException {
        String name = handle.toUpperCase(Locale.ENGLISH);
        LOGGER.log(Level.FINE, "dropping handle : " + handle);
        try {
            List<Handle> oldHandles = listHandleValues(name);
            for ( Handle old : oldHandles ) {
                LOGGER.log(Level.FINE, "deleting handle for this name: " + old);
                em.remove(old);
                em.flush();
            }
        } catch (HandleNotFoundException e) {
            //
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Handle> findHandlesByName(String filter) throws HandleStoreServiceException {
        String name = "%" + filter.toUpperCase(Locale.ENGLISH) + "%";
        TypedQuery<Handle> query = em.createNamedQuery("searchHandleByName", Handle.class).setParameter("name", name.getBytes());
        return query.getResultList();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Handle> findHandlesByValue(String filter) throws HandleStoreServiceException {
        String value = "%" + filter + "%";
        TypedQuery<Handle> query = em.createNamedQuery("searchHandleByValue", Handle.class).setParameter("value", value.getBytes());
        return query.getResultList();
    }
    
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Handle> listHandleValues(String handle) throws HandleStoreServiceException, HandleNotFoundException {
        String name = handle.toUpperCase(Locale.ENGLISH);
        List<Handle> handles;
        TypedQuery<Handle> query = em.createNamedQuery("findHandleByName", Handle.class).setParameter("name", name.getBytes());
        handles = query.getResultList();
        if ( handles == null || handles.isEmpty() ) {
            throw new HandleNotFoundException("no values found with handle [" + handle + "]");
        }
        return handles;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> listHandlesForKey(String key) throws HandleStoreServiceException {
        List<String> names = new ArrayList<String> ();
        TypedQuery<byte[]> query = em.createNamedQuery("findHandleNameForKey", byte[].class).setParameter("key", key);
        List<byte[]> bnames = query.getResultList();
        try {
            for (byte[] bname : bnames) {
                names.add(new String(bname, "UTF-8"));
            }
        } catch ( UnsupportedEncodingException e ) {
            throw new HandleStoreServiceException("unable to decode name", e);
        }
        return names;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Handle> listHandlesValuesForKey(String key) throws HandleStoreServiceException {
        TypedQuery<Handle> query = em.createNamedQuery("findHandleForKey", Handle.class).setParameter("key", key);
        return query.getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public long countHandles() throws HandleStoreServiceException {
        TypedQuery<Long> query = em.createNamedQuery("countHandles", Long.class);
        return query.getSingleResult();
    }

    //Service methods

    @Override
    public String getServiceName() {
        return HandleStoreService.SERVICE_NAME;
    }

    @Override
    public Map<String, String> getServiceInfos() {
        Map<String, String> infos = new HashMap<String, String> ();
        try {
            infos.put(HandleStoreService.INFO_TOTAL_SIZE, Long.toString(countHandles()));
        } catch ( Exception e ) {
            //
        }
        return infos;
    }

}
