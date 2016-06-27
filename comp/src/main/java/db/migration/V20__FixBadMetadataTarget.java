package db.migration;

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
 * Copyright (C) 2013 - 2016 Ortolang Team
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import fr.ortolang.diffusion.core.entity.MetadataElement;

public class V20__FixBadMetadataTarget implements JdbcMigration {

    private static final Logger LOGGER = Logger.getLogger(V20__FixBadMetadataTarget.class.getName());

    @Override
    public void migrate(Connection connection) throws SQLException {
        try {
            PreparedStatement col_stmt = connection.prepareStatement("SELECT * FROM collection;");
            try {
                ResultSet col_rs = col_stmt.executeQuery();
                while ( col_rs.next() ) {
                    String col_id = col_rs.getString("id");
                    String col_mdc = col_rs.getString("metadatascontent");
                    String col_key = null;
                    PreparedStatement col_key_stmt = connection.prepareStatement("SELECT re.key FROM registryentry re WHERE re.identifier = '/core/collection/" + col_id + "'");
                    try {
                        ResultSet col_key_rs = col_key_stmt.executeQuery();
                        if ( col_key_rs.next() ) {
                            col_key = col_key_rs.getString("key");
                        } else {
                            System.out.println("orphean collection found with id: " + col_id);
                        }
                    } finally {
                        col_key_stmt.close();
                    }
                    if ( col_key != null  && col_mdc != null && col_mdc.length() > 0 ) {
                        for ( String md : Arrays.asList(col_mdc.split("\n")) ) {
                            MetadataElement mde = MetadataElement.deserialize(md);
                            fixMetadataTarget(connection, col_key, mde.getKey());
                        }
                        
                    } 
                }
            } finally {
                col_stmt.close();
            }
            
            PreparedStatement obj_stmt = connection.prepareStatement("SELECT * FROM dataobject;");
            try {
                ResultSet obj_rs = obj_stmt.executeQuery();
                while ( obj_rs.next() ) {
                    String obj_id = obj_rs.getString("id");
                    String obj_mdc = obj_rs.getString("metadatascontent");
                    String obj_key = null;
                    PreparedStatement obj_key_stmt = connection.prepareStatement("SELECT re.key FROM registryentry re WHERE re.identifier = '/core/object/" + obj_id + "'");
                    try {
                        ResultSet obj_key_rs = obj_key_stmt.executeQuery();
                        if ( obj_key_rs.next() ) {
                            obj_key = obj_key_rs.getString("key");
                        } else {
                            System.out.println("orphean dataobject found with id: " + obj_id);
                        }
                    } finally {
                        obj_key_stmt.close();
                    }
                    if ( obj_key != null && obj_mdc != null && obj_mdc.length() > 0 ) {
                        for ( String md : Arrays.asList(obj_mdc.split("\n")) ) {
                            MetadataElement mde = MetadataElement.deserialize(md);
                            fixMetadataTarget(connection, obj_key, mde.getKey());
                        }
                        
                    }
                }
            } finally {
                col_stmt.close();
            }
        } catch ( SQLException e ) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }
    }
    
    private boolean fixMetadataTarget(Connection connection, String target, String mdkey) throws SQLException {
        boolean result = true; 
        String md_id = null;
        PreparedStatement md_id_stmt = connection.prepareStatement("SELECT re.identifier FROM registryentry re WHERE re.key = '" + mdkey + "'");
        try {
            ResultSet md_id_rs = md_id_stmt.executeQuery();
            if ( md_id_rs.next() ) {
                md_id = md_id_rs.getString("identifier");
            } else {
                System.out.println("unable to find registry entry with key: " + mdkey);
                result = false;
            }
        } finally {
            md_id_stmt.close();
        }
        if ( md_id != null ) {
            PreparedStatement md_stmt = connection.prepareStatement("SELECT md.target FROM metadataobject md WHERE md.id = '" + md_id.substring(md_id.lastIndexOf("/")+1, md_id.length()) + "'");
            try {
                ResultSet md_rs = md_stmt.executeQuery();
                if ( md_rs.next() ) {
                    String md_target = md_rs.getString("target");
                    if ( !md_target.equals(target) ) {
                        System.out.println("FOUND MISTAKE in metadata with key " + mdkey + ": excepted target is " + target + " but found " + md_target + " -> FIXING");
                        PreparedStatement update_stmt = connection.prepareStatement("UPDATE metadataobject SET target = '" + target + "' WHERE id = '" + md_id.substring(md_id.lastIndexOf("/")+1, md_id.length()) + "'");
                        try {
                            update_stmt.execute();
                        } finally {
                            update_stmt.close();
                        }
                    } 
                } else {
                    System.out.println("unable to find metadata for id: " + md_id.substring(md_id.lastIndexOf("/")+1, md_id.length()));
                    result = false;
                }
            } finally {
                md_stmt.close();
            }
        }
        return result;
    }
    
}
