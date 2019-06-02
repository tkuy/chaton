#Programmation réseau - Antoine BALABAUD - Thierry KUY

##Les différentes parties du projet

Les .jar sont dans le répertoires "A COMPLETER".

Pour démarrer le serveur, il faut spécifier le port

    java -jar server.jar 7777
    
Pour démarrer le client, il faut spécifier le port et le login 

    java -jar client.jar 7777 login
    
Le répertoire qui contient toutes les sources : "A COMPLETER".

La RFC est celle de elearning et n'a pas été mise à jour.

La documentation java est dans le répertoire : "A COMPLETER"
    
##Les commandes

Envoyer un message en broadcast :

    Directement écrire le message
    
Envoyer un message à bob : 
    
    @bob Mon message
    
Établir une connexion avec un autre utilisateur 
   
    /target unFichier
    
##Tests utiles

Pour tester la connexion, il suffit de créer deux utilisateur du même login.

    java -jar client.jar 7777 login
    java -jar client.jar 7777 login
    
Le deuxieme client ne pourra pas se connecter



Pour tester qu'on peut se reconnecter :
    java -jar client.jar 7777 login
    ˆC
    java -jar client.jar 7777 login
    



