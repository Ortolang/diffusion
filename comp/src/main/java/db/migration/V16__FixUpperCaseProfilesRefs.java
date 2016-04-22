package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

public class V16__FixUpperCaseProfilesRefs implements JdbcMigration {
    
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
