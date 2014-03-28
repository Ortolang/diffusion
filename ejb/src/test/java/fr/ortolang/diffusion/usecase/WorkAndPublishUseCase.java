package fr.ortolang.diffusion.usecase;

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
