package fr.ortolang.diffusion.usecase;

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

public class WorkAndPublishUseCase {
	
	/**
	 * Le scénario mets en oeuvre 5 utilisateurs : 
	 * 	- User1 est producteur 
	 *  - User2 est producteur 
	 *  - User3 est authentifié
	 *  - Admin1 est modérateur
	 *  - Admin2 est modéateur
	 *  - Guest n'est pas authentifié
	 * 
	 * Guest consulte la page de visualisation des objets de la plateforme, 
	 * 	- 'corpus 14' n'existe pas
	 * 
	 * User1 se connecte, 
	 * 	- il crée un projet nommé 'corpus 14' de type 'corpus'
	 *  - dans ce projet, il crée un dossier 'data1'
	 *  - il ajoute deux fichiers dans ce dossier data1 : audio1.wav et trans1.trs
	 *  - il ajoute l'utilisateur User2 aux membres du projet
	 *  - une notification est alors envoyée à cet utilisateur pour l'informer qu'il a été ajouté en tant que membre du projet 'corpus 14'
	 *  
	 * User2 se connecte, 
	 *  - il peut voir le projet 'corpus 14' dans la liste de ses projets
	 *  - il ajoute un fichier dans le dossier data1 : trans2.trs
	 *  - une notification est envoyée aux membres du projet 'corpus 14' informant qu'un fichier a été déposé
	 * 
	 * User1 se connecte, 
	 * 	- il demande la publication du projet
	 *  - les éléments du projet sont verrouillés
	 *  - les membres du projet et les modérateurs sont notifiés qu'une demande de publication a été formulée pour 'corpus 14'
	 *  
	 * Admin1 se connecte,
	 * 	- il peut voir la demande de publication dans la liste de ses tâches en cours,
	 *  - il peut consulter les contenus du projet 'corpus 14' qui ont été soumis pour la publication
	 *  - il refuse la publication et rédige un message motivant son refus
	 *  - les membres du projet et les modérateurs sont notifiés que la demande a été traitée et qu'elle a été refusée
	 *  - les éléments du projet sont dévérouillés
	 *  
	 * User2 se connecte, 
	 * 	- il met à jour le fichier trans2.trs suite aux recommandations du modérateur
	 *  - une notification est envoyée aux membres du projet 'corpus 14' informant qu'un fichier a été modifié
	 *  
	 * User1 se connecte, 
	 *  - il consulte le fichier trans2.trs
	 *  - il demande la publication du projet
	 *  - les éléments du projet sont verrouillés
	 *  - les membres du projet et les modérateurs sont notifiés qu'une demande de publication a été formulée pour 'corpus 14'
	 *  
	 * Admin2 se connecte, 
	 *  - il peut voir la demande de publication dans la liste de ses tâches en cours,
	 *  - il peut consulter les contenus du projet 'corpus 14' qui ont été soumis pour la publication
	 *  - il accepte la publication
	 *  - les membres du projet et les modérateurs sont notifiés que la demande a été traitée et qu'elle a été acceptée
	 *  - les éléments du projet sont dupliqués (nouvelle version)
	 *  - la collection racine du projet est ajoutée aux versions du projet
	 *  - les éléments soumis à publication sont rendus visibles sur la plateforme publique
	 *  
	 * Guest consulte la page de visualisation des objets de la plateforme 
	 *  - 'corpus 14' version 1 existe et contient une sous collection data1 contenant 3 fichiers.
	 *  - il télécharge une version complète du corpus (format zip)
	 *  
	 * Guest décide de faire une recherche plein texte sur un mot contenu dans trans2.trs
	 *  - les résultats de recherche contiennent un lien vers le fichier trans2.trs
	 *  - il télécharge directement le fichier trans2.trs
	 *  - il décide de créer un lien vers ce fichier
	 *  	- un projet éphémère (session) est créé pour lui
	 *  	- un lien est créé dans le projet éphémère vers le fichier trans2.trs 
	 */

}
