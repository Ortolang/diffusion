

Le **centre de diffusion** est basé sur une **architecture orientée composants** afin de découpler les différentes problématiques liées
à ce logiciel et de pouvoir assurer une testabilité maximum du logiciel.

Plusieurs **composants** sont ainsi definis, chacun de ces composants offrant des **services** spécifiques.

Les différents composants définis sont : 

  - Un composant de **stockage des objets numériques** de la plateforme
  - Un composant de **stockage des méta données** de ce objets
  - Un composant de **gestion de permissions** d'accès aux objets et aux méta données
  - Un composant **d'import/export** des objets et de leurs méta données
  - Un composant **d'identification pérenne**
  - Un composant de **gestion des requêtes** sur les méta données
  

## Composant de stockage des objets numériques

Le compostant de stockage des objets numériques doit répondre à plusieurs exigences fonctionnelles : 

  - Pouvoir stocker et récupérer des objets numériques 
  - Pouvoir stocker et récupérer des collections d'objets
  - Optimiser le support de stockage en évitant les doublons d'objets
  - Accéder aux objets à partir d'un identifiant unique
  - Supporter des milliards d'objets
  - Garantir l'accès transactionnel aux objets stockés
  - Minimiser la surcharge de traitement afin de proposer des performances en lecture/écriture proches de celles du support physique sous jacent
  
Afin de répondre à ces exigences, ce composant proposera 3 services : 

  - Un service de **génération d'identifiant** unique basé sur le contenu des objets (empreinte numérique)
  - Un service de stockage des **objets numériques** (flux d'octet)
  - Un service de stockage des **collections d'objets** (arborescence)

Les objets numériques binaires sont la plus petite entité stockable et sont identifiés par une empreinte calculée directement à partir 
du contenu de cet objet (sha-1 par exemple). Ainsi, un objet ayant le même contenu ne pourra pas être stocké deux fois puisqu'il aura la même 
empreinte. L'utilisation d'une empreinte comme clé de contenu implique de gérer (à minima de détecter) les collisions possibles. C'est à dire
lorsque deux objets de contenus différents vont générer la même empreinte.

Les collections d'objets devront également être identifié par une empreinte calculée à partir de la représentation binaire ou 
textuelle (serialisation) de cette structure afin de garantir la non duplication de ces collections également. Pour autant, les 
informations stockées dans cette structure pourront référencer le même objet binaire sous jacent mais avec des informations (non de 
fichier) différentes ce qui donnera une empreinte différente également. Pour l'instant les informations liées aux collections et
devant être prise en compte dans la génération de l'empreinte de cette collection sont le nom du fichier.

Ainsi, toute utilisation du stockage des objets numérique même pour un simple fichier, génèra au minimum un objet binaire et une collection d'un
seul élément : l'empreinte du fichier binaire et le nom de fichier original.

  