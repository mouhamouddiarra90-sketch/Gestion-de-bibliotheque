import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Connexion à MariaDB / MySQL (XAMPP, Termux) avec le pilote mariadb-java-client-2.7.6.jar.
 * Valeurs par défaut de XAMPP : utilisateur root, sans mot de passe.
 * Pour les changer sans modifier le code : variables d'environnement DB_URL, DB_USER, DB_PASSWORD.
 */
public class ConnexionBD {
    private static final String URL =
            lire("DB_URL", "jdbc:mariadb://localhost:3306/bibliotheque");
    private static final String UTILISATEUR = lire("DB_USER", "root");
    private static final String MOT_DE_PASSE = lire("DB_PASSWORD", "");

    static {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Pilote MariaDB introuvable : placez mariadb-java-client-2.7.6.jar "
                    + "dans le dossier lib/.");
        }
    }

    private static String lire(String nom, String valeurParDefaut) {
        String valeur = System.getenv(nom);
        return (valeur == null || valeur.isEmpty()) ? valeurParDefaut : valeur;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, UTILISATEUR, MOT_DE_PASSE);
    }
}
