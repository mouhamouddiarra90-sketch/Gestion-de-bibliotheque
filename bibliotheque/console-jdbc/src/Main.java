import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    // Longueurs maximales (identiques aux colonnes de la base)
    private static final int MAX_TITRE = 150;
    private static final int MAX_AUTEUR = 100;
    private static final int MAX_NOM = 100;
    private static final int MAX_TELEPHONE = 20;

    private static Scanner scanner = new Scanner(System.in);
    private static LivreService livreService = new LivreService();
    private static AdherentService adherentService = new AdherentService();
    private static EmpruntService empruntService = new EmpruntService();

    public static void main(String[] args) {
        // Test de connexion au démarrage
        try (Connection conn = ConnexionBD.getConnection()) {
            System.out.println("Connexion à la base réussie.");
            System.out.println();
        } catch (SQLException e) {
            System.out.println("Impossible de se connecter à la base : " + e.getMessage());
            System.out.println("Vérifiez : MySQL/MariaDB démarré, base \"bibliotheque\" importée,");
            System.out.println("identifiants (DB_USER, DB_PASSWORD) et pilote lib/mariadb-java-client-2.7.6.jar.");
            return;
        }

        int choix;
        do {
            afficherMenu();
            choix = lireEntier("Votre choix : ");
            System.out.println();

            try {
                switch (choix) {
                    case 1: ajouterLivre(); break;
                    case 2: modifierLivre(); break;
                    case 3: supprimerLivre(); break;
                    case 4: afficherLivres(livreService.lister(), "Aucun livre enregistré."); break;
                    case 5: afficherLivres(livreService.listerDisponibles(),
                            "Aucun livre disponible."); break;
                    case 6: rechercherLivre(); break;
                    case 7: ajouterAdherent(); break;
                    case 8: modifierAdherent(); break;
                    case 9: supprimerAdherent(); break;
                    case 10: afficherAdherents(adherentService.lister(),
                            "Aucun adhérent enregistré."); break;
                    case 11: rechercherAdherent(); break;
                    case 12: emprunterLivre(); break;
                    case 13: retournerLivre(); break;
                    case 14: afficherEmpruntsEnCours(); break;
                    case 0: System.out.println("Au revoir !"); break;
                    default: System.out.println("Choix invalide : entrez un nombre du menu.");
                }
            } catch (SQLException e) {
                afficherErreurSQL(e);
            }
            System.out.println();
        } while (choix != 0);
    }

    private static void afficherMenu() {
        System.out.println("============ BIBLIOTHÈQUE ============");
        System.out.println(" LIVRES");
        System.out.println("   1. Ajouter un livre");
        System.out.println("   2. Modifier un livre");
        System.out.println("   3. Supprimer un livre");
        System.out.println("   4. Afficher tous les livres");
        System.out.println("   5. Afficher les livres disponibles");
        System.out.println("   6. Rechercher un livre");
        System.out.println(" ADHÉRENTS");
        System.out.println("   7. Ajouter un adhérent");
        System.out.println("   8. Modifier un adhérent");
        System.out.println("   9. Supprimer un adhérent");
        System.out.println("  10. Afficher les adhérents");
        System.out.println("  11. Rechercher un adhérent");
        System.out.println(" EMPRUNTS");
        System.out.println("  12. Emprunter un livre");
        System.out.println("  13. Retourner un livre");
        System.out.println("  14. Afficher les emprunts en cours");
        System.out.println("   0. Quitter");
        System.out.println("======================================");
    }

    // ---------- Livres ----------

    private static void ajouterLivre() throws SQLException {
        String titre = lireTexte("Titre : ", MAX_TITRE, true);
        String auteur = lireTexte("Auteur : ", MAX_AUTEUR, true);
        System.out.println("Livre ajouté : " + livreService.ajouter(titre, auteur));
    }

    private static void modifierLivre() throws SQLException {
        int id = lireEntier("ID du livre à modifier : ");
        Livre livre = livreService.trouverParId(id);
        if (livre == null) {
            System.out.println("Erreur : livre introuvable.");
            return;
        }
        System.out.println("Livre actuel : " + livre);
        String titre = lireTexte("Nouveau titre (Entrée = garder l'ancien) : ", MAX_TITRE, false);
        String auteur = lireTexte("Nouvel auteur (Entrée = garder l'ancien) : ", MAX_AUTEUR, false);
        System.out.println(livreService.modifier(id, titre, auteur));
    }

    private static void supprimerLivre() throws SQLException {
        int id = lireEntier("ID du livre à supprimer : ");
        Livre livre = livreService.trouverParId(id);
        if (livre == null) {
            System.out.println("Erreur : livre introuvable.");
            return;
        }
        System.out.println(livre);
        if (!confirmer("Confirmer la suppression ? (o/n) : ")) {
            System.out.println("Suppression annulée.");
            return;
        }
        System.out.println(livreService.supprimer(id));
    }

    private static void rechercherLivre() throws SQLException {
        String motCle = lireTexte("Mot-clé (titre ou auteur) : ", MAX_TITRE, true);
        afficherLivres(livreService.rechercher(motCle), "Aucun livre trouvé pour \"" + motCle + "\".");
    }

    private static void afficherLivres(ArrayList<Livre> livres, String messageVide) {
        if (livres.isEmpty()) {
            System.out.println(messageVide);
            return;
        }
        System.out.println("--- " + livres.size() + " livre(s) ---");
        for (Livre l : livres) {
            System.out.println(l);
        }
    }

    // ---------- Adhérents ----------

    private static void ajouterAdherent() throws SQLException {
        String nom = lireTexte("Nom complet : ", MAX_NOM, true);
        String tel = lireTexte("Téléphone (facultatif) : ", MAX_TELEPHONE, false);
        System.out.println("Adhérent ajouté : " + adherentService.ajouter(nom, tel));
    }

    private static void modifierAdherent() throws SQLException {
        int id = lireEntier("ID de l'adhérent à modifier : ");
        Adherent adherent = adherentService.trouverParId(id);
        if (adherent == null) {
            System.out.println("Erreur : adhérent introuvable.");
            return;
        }
        System.out.println("Adhérent actuel : " + adherent);
        String nom = lireTexte("Nouveau nom (Entrée = garder l'ancien) : ", MAX_NOM, false);
        String tel = lireTexte("Nouveau téléphone (Entrée = garder l'ancien) : ", MAX_TELEPHONE, false);
        System.out.println(adherentService.modifier(id, nom, tel));
    }

    private static void supprimerAdherent() throws SQLException {
        int id = lireEntier("ID de l'adhérent à supprimer : ");
        Adherent adherent = adherentService.trouverParId(id);
        if (adherent == null) {
            System.out.println("Erreur : adhérent introuvable.");
            return;
        }
        System.out.println(adherent);
        if (!confirmer("Confirmer la suppression ? (o/n) : ")) {
            System.out.println("Suppression annulée.");
            return;
        }
        System.out.println(adherentService.supprimer(id));
    }

    private static void rechercherAdherent() throws SQLException {
        String motCle = lireTexte("Mot-clé (nom ou téléphone) : ", MAX_NOM, true);
        afficherAdherents(adherentService.rechercher(motCle),
                "Aucun adhérent trouvé pour \"" + motCle + "\".");
    }

    private static void afficherAdherents(ArrayList<Adherent> adherents, String messageVide) {
        if (adherents.isEmpty()) {
            System.out.println(messageVide);
            return;
        }
        System.out.println("--- " + adherents.size() + " adhérent(s) ---");
        for (Adherent a : adherents) {
            System.out.println(a);
        }
    }

    // ---------- Emprunts ----------

    private static void emprunterLivre() throws SQLException {
        int idLivre = lireEntier("ID du livre : ");
        int idAdherent = lireEntier("ID de l'adhérent : ");
        System.out.println(empruntService.emprunter(idLivre, idAdherent));
    }

    private static void retournerLivre() throws SQLException {
        int idLivre = lireEntier("ID du livre à retourner : ");
        System.out.println(empruntService.retourner(idLivre));
    }

    private static void afficherEmpruntsEnCours() throws SQLException {
        ArrayList<Emprunt> emprunts = empruntService.listerEnCours();
        if (emprunts.isEmpty()) {
            System.out.println("Aucun emprunt en cours.");
            return;
        }
        System.out.println("--- " + emprunts.size() + " emprunt(s) en cours ---");
        for (Emprunt e : emprunts) {
            System.out.println(e);
        }
    }

    // ---------- Gestion des erreurs ----------

    private static void afficherErreurSQL(SQLException e) {
        String etat = e.getSQLState();
        if (etat != null && etat.startsWith("08")) {
            System.out.println("Erreur : connexion à la base perdue ou impossible. "
                    + "Vérifiez que MySQL/MariaDB est démarré.");
        } else {
            System.out.println("Erreur base de données : " + e.getMessage());
        }
    }

    // ---------- Saisie ----------

    /** Lit une ligne ; si l'entrée est fermée (Ctrl+D / fin de fichier), quitte proprement. */
    private static String lireLigne() {
        if (!scanner.hasNextLine()) {
            System.out.println();
            System.out.println("Fin de la saisie. Au revoir !");
            System.exit(0);
        }
        return scanner.nextLine().trim();
    }

    /** Redemande la saisie tant qu'elle est vide (si obligatoire) ou trop longue. */
    private static String lireTexte(String message, int maxLongueur, boolean obligatoire) {
        while (true) {
            System.out.print(message);
            String saisie = lireLigne();
            if (obligatoire && saisie.isEmpty()) {
                System.out.println("Ce champ est obligatoire.");
            } else if (saisie.length() > maxLongueur) {
                System.out.println("Maximum " + maxLongueur + " caractères (vous en avez saisi "
                        + saisie.length() + ").");
            } else {
                return saisie;
            }
        }
    }

    private static int lireEntier(String message) {
        while (true) {
            System.out.print(message);
            String saisie = lireLigne();
            try {
                return Integer.parseInt(saisie);
            } catch (NumberFormatException e) {
                System.out.println("Veuillez entrer un nombre entier valide.");
            }
        }
    }

    private static boolean confirmer(String message) {
        System.out.print(message);
        String reponse = lireLigne().toLowerCase();
        return reponse.equals("o") || reponse.equals("oui");
    }
}
