

Le **centre de diffusion** est basé sur une **architecture orientée composants**. Plusieurs **composants** sont ainsi definis, 
chacun de ces composants offrant des **services** spécifiques.

Les différents composants définis sont : 

  - Un composant de **stockage des objets numériques** de la plateforme
  - Un composant de **stockage des méta données** de ce objets
  - Un composant de **contrôle d'accès** aux objets et aux méta données
  - Un composant **d'import/export** des objets et de leurs méta données
  - Un composant **d'identification pérenne** des objets
  

## Composant 'stockage des objets numériques'

Le compostant de stockage des objets numériques doit répondre à plusieurs contraintes : 

  - Pouvoir stocker et restituer des **objets numériques**, ces objets pouvant eux même contenir plusieurs éléments binaires. 
  - Optimiser le support de stockage en **évitant les doublons** de données binaires au maximum.
  - Accéder aux objets à partir d'un **identifiant unique**
  - Supporter des **milliards d'objets**
  - Garantir **l'accès transactionnel** aux objets stockés
  - Minimiser la surcharge de traitement afin de proposer des **performances** en lecture/écriture proches de celles du support 
  physique sous jacent
  
Afin de répondre à ces exigences, ce composant sera découpé en plusieurs services : 

  - Un service de **génération d'empreinte numérique** basé sur le contenu des objets
  - Un service de stockage des **objets binaires**
  - Un service de stockage des **conteneurs d'objets binaires**

La **génération d'empreinte numérique** basée sur le contenu des objets permettra **d'éviter les doublons** d'objets. Pour cela, 
on utilisera un **algorithme de hashage** qui permettra d'identifier de manière unique une donnée binaire.
Ce type d'algorithme est assez fiable mais ne permet pas de **garantir l'unicité** de l'empreinte pour tous les objets, on 
parle alors de **collision**. En cas de collision, deux objets différents vont générer la même empreinte. Il faudra donc 
impérativement que le service de stockage soit en mesure de **détecter ces collisions**. Pour autant, certains algorithmes comme
SHA-1 n'ont pas de collision connue à ce jour même si la théorie montre qu'il en existe. 

Les **objets binaires** sont la **plus petite entité stockable**. Ils sont constitués uniquement d'un **flux binaire**. Ainsi
deux flux binaires identiques auront forcément la **même empreinte** et donc le même identifiant. La détection de collision et doublons 
devient donc **simple et rapide**. Pour autant ce type d'objet **ne peut pas suffire** à stocker des fichiers utilisateurs car une
partie de l'information (nom de fichier notamment) serait perdue. C'est donc un **service interne** permettant uniquement de 
**garantir un stockage optimal sans doublons** avec un possibilité de **détecter une corruption** des données. 

Les **conteneurs d'objets binaires** sont des **structures** plus complexes qui permettent **d'associer les objets binaires** 
avec des données sémantiques (nom de fichier, chemin dans une arborescance, etc...)  mais également de les **organiser** dans des 
collections. Les conteneurs d'objets binaires seront identifiés à l'aide d'un **identifiant unique de type UUID**. 

L'utilisation de conteneurs et d'objets binaires permet d'optimiser le stockage brute des données en évitant des doublons 
tout en conservant la possibilité d'associer des données sémantiques différentes (comme un nom de fichier) au même contenu 
binaire avec un identifiant propre.
  

## Composant 'stockage des méta données'

  