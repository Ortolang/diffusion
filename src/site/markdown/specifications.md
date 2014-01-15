

Le **centre de diffusion** est basé sur une **architecture orientée composants** ce qui permet de découpler les différentes 
problématiques et d'assurer une testabilité maximum.

Plusieurs **composants** sont ainsi definis, chacun de ces composants offrant des **services** spécifiques.

Les différents composants définis sont : 

  - Un composant de **stockage des objets numériques** de la plateforme
  - Un composant de **stockage des méta données** de ce objets
  - Un composant de **gestion de permissions** d'accès aux objets et aux méta données
  - Un composant **d'import/export** des objets et de leurs méta données
  - Un composant **d'identification pérenne** des objets
  

## Composant 'stockage des objets numériques'

Le compostant de stockage des objets numériques doit répondre à plusieurs exigences fonctionnelles : 

  - Pouvoir stocker et récupérer des **objets numériques** 
  - Pouvoir stocker et récupérer des **collections d'objets**
  - Optimiser le support de stockage en **évitant les doublons** d'objets
  - Accéder aux objets à partir d'un **identifiant unique**
  - Supporter des **milliards d'objets**
  - Garantir **l'accès transactionnel** aux objets stockés
  - Minimiser la surcharge de traitement afin de proposer des **performances** en lecture/écriture proches de celles du support 
  physique sous jacent
  
Afin de répondre à ces exigences, ce composant proposera 3 services : 

  - Un service de **génération d'identifiant** unique basé sur le contenu des objets (empreinte numérique)
  - Un service de stockage des **objets numériques** (flux d'octet)
  - Un service de stockage des **collections d'objets** (arborescence)

La **génération d'identifiant unique** basée sur le contenu des objets permettra **d'éviter les doublons** d'objets. Pour cela, 
on utilisera un **algorithme de hashage** qui permettra de réaliser une **empreinte numérique** de l'objet.
Ce type d'algorithme est assez fiable mais ne permet pas de **garantir l'unicité** de l'empreinte pour tous les objets, on 
parle alors de **collision**. En cas de collision, deux objets différents vont générer la même empreinte. Il faudra donc 
impérativement que le service de stockage soit en mesure de **détecter ces collisions**. Pour autant, certains algorithme comme
SHA-1 n'ont pas de collision connue à ce jour même si la théorie montre qu'il en existe. 

Les **objets numériques binaires** sont la **plus petite entité stockable**. Ils sont constitués uniquement d'un **flux binaire**. Ainsi
deux flux binaires identiques auront forcément la même empreinte et donc le même identifiant. La détection de collision et doublons 
devient donc **simple et rapide**.  

Les **collections d'objets** sont des **structures** plus complexes qui permettent de **regrouper des objets numériques** 
afin de leur donner un sens. Ce regroupement pourra contenir des **données sémantiques** supplémentaires comme le nom d'un 
objet. Cette information sémantique permettra de donner à un même objet numérique **un sens différent** en fonction de la 
collection dans laquelle il se trouve. Les collections seront également **identifiées** à l'aide d'une **empreinte** calculée
sur la **représentation textuelle** (serialisation) de la structure. Par exemple, pour stocker un fichier il faudra donc 
stocker un objet binaire ainsi qu'une collection à un élément (le nom de ce fichier et l'identifiant de son flux binaire). 
L'ajout d'autres informations sémantiques (comme la date de modification par exemple)  dans la structure de la collection 
aura pour effet de rendre unique une collection par rapport à cette information sémantique ce qui n'est pas forcément 
souhaitable. Il est donc primordial de bien **définir les données sémantiques** à inclure dans cette structure afin de ne pas 
**créer des doublons inutilement**.

## Composant 'stockage des méta données'

  