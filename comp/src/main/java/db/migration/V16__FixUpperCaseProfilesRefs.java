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
import java.util.Arrays;
import java.util.List;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

public class V16__FixUpperCaseProfilesRefs implements JdbcMigration {

    @Override
    public void migrate(Connection connection) throws Exception {
        PreparedStatement grp_stmt = connection.prepareStatement("UPDATE \"GROUP\" SET memberslist=lower(memberslist)");
        PreparedStatement auth_stmt = connection.prepareStatement("UPDATE authorisationpolicy SET owner=lower(owner), rulescontent=lower(rulescontent)");
        PreparedStatement pro_stmt = connection.prepareStatement("SELECT * FROM profile", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        try {
            grp_stmt.execute();
            auth_stmt.execute();
            ResultSet pro_rs = pro_stmt.executeQuery();
            while ( pro_rs.next() ) {
                String pro_id = pro_rs.getString("id");
                String groupsList = "";
                PreparedStatement pro_grp_stmt = connection.prepareStatement("SELECT * FROM \"GROUP\" g WHERE g.memberslist LIKE '%" + pro_id + "%'");
                try {
                    ResultSet pro_grp_rs = pro_grp_stmt.executeQuery();
                    while ( pro_grp_rs.next() ) {
                        String pro_grp_name = pro_grp_rs.getString("name");
                        System.out.println("Found potential group for profile (" + pro_id + "): " + pro_grp_name);
                        String memberslist = pro_grp_rs.getString("memberslist");
                        List<String> members = Arrays.asList(memberslist.split(","));
                        if ( members.contains(pro_id) ) {
                            String pro_grp_id = pro_grp_rs.getString("id");
                            PreparedStatement pro_grp_key_stmt = connection.prepareStatement("SELECT key FROM registryentry re WHERE re.identifier = '/membership/group/" + pro_grp_id + "'");
                            try {
                                ResultSet pro_grp_key_rs = pro_grp_key_stmt.executeQuery();
                                if ( pro_grp_key_rs.next() ) {
                                    if (groupsList.length() > 0) {
                                        groupsList += ("," + pro_grp_key_rs.getString("key"));
                                    } else {
                                        groupsList += pro_grp_key_rs.getString("key");
                                    }
                                } else {
                                    System.out.println("Unable to find a key for group with identifier: /membership/group/" + pro_grp_id);
                                }
                            } finally {
                                pro_grp_key_stmt.close();
                            }
                        }
                    }
                    if ( groupsList.length() > 0 ) {
                        System.out.println("New groups list built for profile (" + pro_id + "): " + groupsList);
                        pro_rs.updateString( "groupslist", groupsList);
                        pro_rs.updateRow();
                    }
                } finally {
                    pro_grp_stmt.close();
                }
                
            }
        } finally {
            grp_stmt.close();
            auth_stmt.close();
            pro_stmt.close();
        }
    }
    
}
