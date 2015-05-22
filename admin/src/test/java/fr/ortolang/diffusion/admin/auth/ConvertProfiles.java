package fr.ortolang.diffusion.admin.auth;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

public class ConvertProfiles {

	private static final Logger LOGGER = Logger.getLogger(ConvertProfiles.class.getName());
	
	@Test
	public void importUsers() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
		try {
			List<JsonPays> pays = Arrays.asList(mapper.readValue(new File("/media/space/jerome/Data/SLDR/pays.json"), JsonPays[].class));
			List<JsonProducer> producers = Arrays.asList(mapper.readValue(new File("/media/space/jerome/Data/SLDR/producers.json"), JsonProducer[].class));
			List<JsonProfile> profiles = Arrays.asList(mapper.readValue(new File("/media/space/jerome/Data/SLDR/profiles.json"), JsonProfile[].class));
			for (JsonProfile profile : profiles) {
				try {
					int org_id = Integer.parseInt(profile.pro_organisme);
					LOGGER.log(Level.INFO, "organisme is a number, switching with ref name.");
					for ( JsonProducer producer : producers ) {
						if ( Integer.parseInt(producer.prod_id) == org_id ) {
							LOGGER.log(Level.INFO, "org_id replace with producer name : " + producer.prod_nom);
							profile.pro_organisme = producer.prod_nom;
							break;
						}
					}
				} catch ( NumberFormatException e ) {
					LOGGER.log(Level.INFO, "organisme is not a number, nothing to change.");
					profile.pro_organisme = profile.pro_organisme.replaceAll("<br/>", " ");
					profile.pro_organisme = profile.pro_organisme.replaceAll("<br />", " ");
					profile.pro_organisme = profile.pro_organisme.replaceAll("<br>", " ");
					profile.pro_organisme = profile.pro_organisme.replaceAll("<BR/>", " ");
					profile.pro_organisme = profile.pro_organisme.replaceAll("<br", " ");
					profile.pro_organisme = profile.pro_organisme.replaceAll(">", " ");
				}
				profile.pro_domaine_recherche = profile.pro_domaine_recherche.replaceAll("<br/>", " ");
				profile.pro_domaine_recherche = profile.pro_domaine_recherche.replaceAll("<br />", " ");
				profile.pro_domaine_recherche = profile.pro_domaine_recherche.replaceAll("<br>", " ");
				profile.pro_domaine_recherche = profile.pro_domaine_recherche.replaceAll("<BR/>", " ");
				profile.pro_domaine_recherche = profile.pro_domaine_recherche.replaceAll("<br", " ");
				profile.pro_domaine_recherche = profile.pro_domaine_recherche.replaceAll(">", " ");
				profile.pro_adresse = profile.pro_adresse.replaceAll("<br/>", " ");
				profile.pro_adresse = profile.pro_adresse.replaceAll("<br />", " ");
				profile.pro_adresse = profile.pro_adresse.replaceAll("<BR/>", " ");
				profile.pro_adresse = profile.pro_adresse.replaceAll("<br>", " ");
				profile.pro_adresse = profile.pro_adresse.replaceAll("<br", " ");
				profile.pro_adresse = profile.pro_adresse.replaceAll(">", " ");
				if ( profile.pro_id_idref.equals("0") ) {
					profile.pro_id_idref = "";
				}
				if ( profile.pro_adresse.equals("0") ) {
					profile.pro_adresse = "";
				}
				if ( profile.pro_organisme_url.equals("0") ) {
					profile.pro_organisme_url = "";
				}
				try {
					if ( profile.pro_pays_id.length() > 0 ) {
						int pays_id = Integer.parseInt(profile.pro_pays_id);
						for ( JsonPays pay : pays ) {
							if ( Integer.parseInt(pay.pays_id) == pays_id ) {
								LOGGER.log(Level.INFO, "setting pays_nom: " + pay.pays_nom);
								profile.pro_pays_nom = pay.pays_nom;
								break;
							}
						}
					}
				} catch ( NumberFormatException e ) {
					LOGGER.log(Level.INFO, "unable to parse code pays");
				}
			}
			mapper.writerWithDefaultPrettyPrinter().writeValue(new File("/media/space/jerome/Data/SLDR/profiles.new.json"), profiles);
		} catch ( Exception e) {
			e.printStackTrace();
		}
	}
	
	static class JsonProfile {
		public String pro_id = "";
		public String pro_login = "";
		public String pro_genre = "";
		public String pro_titre = "";
		public String pro_firstname = "";
		public String pro_lastname = "";
		public String pro_emailt = "";
		public String pro_email = "";
		public String pro_organisme = "";
		public String pro_metier = "";
		public String pro_domaine_recherche = "";
		public String pro_adresse = "";
		public String pro_cp = "";
		public String pro_ville = "";
		public String pro_pays_id = "";
		public String pro_pays_nom = "";
		public String pro_organisme_url = "";
		public String pro_telephonet = "";
		public String pro_telephone = "";
		public String pro_telecopie = "";
		public String pro_langue = "";
		public String pro_id_orcid = "";
		public String pro_id_viaf = "";
		public String pro_id_idref = "";
		public String pro_id_linkedin = "";

		public JsonProfile() {
		}
	}
	
	static class JsonProducer {
		public String prod_id = "";
		public String prod_nom = "";
		public String prod_sigle = "";
		public String prod_ville = "";
		public String prod_pays = "";
		public String prod_url = "";
		public String prod_path = "";
		public String prod_active = "";
		public String prod_corpus = "";
		public String prod_ressource = "";
		public String prod_outil = "";
		
		public JsonProducer() {
		}
	}
	
	static class JsonPays {
		public String pays_id = "";
		public String pays_code = "";
		public String pays_nom = "";
		
		public JsonPays() {
		}
	}

}
