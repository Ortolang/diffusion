package db.migration;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import fr.ortolang.diffusion.membership.MembershipService;

public class V21__AddNewGroupsPermissions implements JdbcMigration {

    private static final Logger LOGGER = Logger.getLogger(V21__AddNewGroupsPermissions.class.getName());

    @Override
    public void migrate(Connection connection) throws SQLException {
        try {
            PreparedStatement rule_stmt = connection.prepareStatement("SELECT * FROM authorisationpolicy;");
            try {
                ResultSet rule_rs = rule_stmt.executeQuery();
                while (rule_rs.next()) {
                    String rule_id = rule_rs.getString("id");
                    String rule_content = rule_rs.getString("rulescontent");
                    Properties rules = loadRules(rule_content);
                    if ( rules.containsKey(MembershipService.MODERATORS_GROUP_KEY) ) {
                        LOGGER.log(Level.FINE, "found rule with moderators permissions, adding publisher abd reviewers policy...");
                        if ( rules.getProperty(MembershipService.MODERATORS_GROUP_KEY).contains("download") ) {
                            rules.setProperty(MembershipService.PUBLISHERS_GROUP_KEY, "read,update,download");
                            rules.setProperty(MembershipService.REVIEWERS_GROUP_KEY, "read,download");
                        } else {
                            rules.setProperty(MembershipService.PUBLISHERS_GROUP_KEY, "read,update");
                            rules.setProperty(MembershipService.REVIEWERS_GROUP_KEY, "read");
                        }
                        PreparedStatement update_rules_stmt = connection.prepareStatement("UPDATE authorisationpolicy SET rulescontent = '" + saveRules(rules) + "' WHERE id = '" + rule_id + "';");
                        try {
                            update_rules_stmt.executeUpdate();
                        } finally {
                            update_rules_stmt.close();
                        }
                    }
                    
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "unable to parse rules" + e.getMessage(), e);
            } finally {
                rule_stmt.close();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }
    }

    private Properties loadRules(String rulesContent) throws IOException {
        Properties rules = new Properties();
        if (rulesContent.length() > 0) {
            rules.load(new StringReader(rulesContent));
        }
        return rules;
    }

    private String saveRules(Properties rules) throws IOException {
        StringWriter output = new StringWriter();
        rules.store(output, null);
        return output.toString();
    }
}
