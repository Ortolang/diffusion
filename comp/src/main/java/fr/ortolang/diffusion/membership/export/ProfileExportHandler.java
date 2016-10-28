package fr.ortolang.diffusion.membership.export;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObjectExportHandler;
import fr.ortolang.diffusion.dump.XmlDumpAttributes;
import fr.ortolang.diffusion.dump.XmlDumpHelper;
import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.membership.entity.ProfileData;
import fr.ortolang.diffusion.membership.entity.ProfileKey;

public class ProfileExportHandler implements OrtolangObjectExportHandler {
    
    private Profile profile; 
    
    public ProfileExportHandler(Profile profile) {
        this.profile = profile;
   }
    
    @Override
    public void dumpObject(XMLStreamWriter writer) throws OrtolangException {
        try {
            XmlDumpAttributes attrs = new XmlDumpAttributes();
            attrs.put("id", profile.getId());
            attrs.put("given-name", profile.getGivenName());
            attrs.put("family-name", profile.getFamilyName());
            attrs.put("secret", profile.getSecret());
            attrs.put("status", profile.getStatus().name());
            attrs.put("email-verified", Boolean.toString(profile.isEmailVerified()));
            attrs.put("email", profile.getEmail());
            attrs.put("email-hash", profile.getEmailHash());
            attrs.put("email-visibility", profile.getEmailVisibility().name());
            attrs.put("complete", Boolean.toString(profile.isComplete()));
            attrs.put("status", profile.getStatus().name());
            attrs.put("referential-id", profile.getReferentialId());
            attrs.put("friends", profile.getFriends());
            XmlDumpHelper.startElement("membership", "profile", attrs, writer);
            
            attrs = new XmlDumpAttributes();
            XmlDumpHelper.startElement("profile", "groups", attrs, writer);
            for ( String group : profile.getGroups() ) {
                attrs = new XmlDumpAttributes();
                attrs.put("key", group);
                XmlDumpHelper.outputEmptyElement("profile", "group", attrs, writer);
            }
            XmlDumpHelper.endElement(writer);
            
            attrs = new XmlDumpAttributes();
            XmlDumpHelper.startElement("profile", "keys", attrs, writer);
            for ( ProfileKey key : profile.getKeys() ) {
                attrs = new XmlDumpAttributes();
                attrs.put("key", key.getKey());
                attrs.put("password", key.getPassword());
                XmlDumpHelper.outputEmptyElement("profile", "key", attrs, writer);
            }
            XmlDumpHelper.endElement(writer);
            
            attrs = new XmlDumpAttributes();
            XmlDumpHelper.startElement("profile", "infos", attrs, writer);
            for ( Entry<String, ProfileData> info : profile.getInfos().entrySet() ) {
                attrs = new XmlDumpAttributes();
                attrs.put("key", info.getKey());
                attrs.put("name", info.getValue().getName());
                attrs.put("source", info.getValue().getSource());
                attrs.put("value", info.getValue().getValue());
                attrs.put("visibility", info.getValue().getVisibility().name());
                XmlDumpHelper.outputEmptyElement("profile", "info", attrs, writer);
            }
            XmlDumpHelper.endElement(writer);
            
            XmlDumpHelper.endElement(writer);
        } catch ( XMLStreamException e ) {
            throw new OrtolangException("error during dumping group", e);
        }
    }

    @Override
    public Set<String> getObjectDependencies() throws OrtolangException {
        Set<String> deps = new HashSet<String> ();
        deps.add(profile.getFriends());
        for ( String group : profile.getGroups() ) {
            deps.add(group);
        }
        return deps;
    }

    @Override
    public Set<String> getObjectBinaryStreams() throws OrtolangException {
        return Collections.emptySet();
    }

}
