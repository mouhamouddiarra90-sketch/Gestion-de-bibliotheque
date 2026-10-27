import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class EmpruntService {
    public static final int MAX_EMPRUNTS = 3;

    /** Emprunts dont le livre n'a pas encore été retourné (date_retour NULL). */
    public ArrayList<Emprunt> listerEnCours() throws SQLException {
        ArrayList<Emprunt> emprunts = new ArrayList<>();
        String sql = "SELECT e.id, e.date_emprunt, "
                + "l.id AS livre_id, l.titre, l.auteur, l.disponible, "
                + "a.id AS adherent_id, a.nom, a.telephone "
                + "FROM emprunts e "
                + "JOIN livres l ON l.id = e.livre_id "
                + "JOIN adherents a ON a.id = e.adherent_id "
                + "WHERE e.date_retour IS NULL "
                + "ORDER BY e.date_emprunt, e.id";
        try (Connection conn = ConnexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Livre livre = new Livre(
                        rs.getInt("livre_id"),
                        rs.getString("titre"),
                        rs.getString("auteur"),
                        rs.getBoolean("disponible"));
                Adherent adherent = new Adherent(
                        rs.getInt("adherent_id"),
                        rs.getString("nom"),
                        rs.getString("telephone"));
                emprunts.add(new Emprunt(
                        rs.getInt("id"),
                        livre,
                        adherent,
                        rs.getDate("date_emprunt").toLocalDate(),
                        null));
            }
        }
        return emprunts;
    }

    /**
     * Emprunt dans une transaction : l'insertion dans "emprunts" et la mise à jour
     * de "livres" réussissent ensemble ou pas du tout.
     * Les vérifications se font avant toute écriture : en cas de message d'erreur,
     * le commit ne change donc rien dans la base.
     */
    public String emprunter(int idLivre, int idAdherent) throws SQLException {
        try (Connection conn = ConnexionBD.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String message = traiterEmprunt(conn, idLivre, idAdherent);
                conn.commit();
                return message;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public String retourner(int idLivre) throws SQLException {
        try (Connection conn = ConnexionBD.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String message = traiterRetour(conn, idLivre);
                conn.commit();
                return message;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private String traiterEmprunt(Connection conn, int idLivre, int idAdherent)
            throws SQLException {
        // 1. Le livre existe-t-il ? (FOR UPDATE verrouille la ligne pendant la transaction)
        String titre;
        boolean disponible;
        String sqlLivre = "SELECT titre, disponible FROM livres WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sqlLivre)) {
            ps.setInt(1, idLivre);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return "Erreur : livre introuvable.";
                }
                titre = rs.getString("titre");
                disponible = rs.getBoolean("disponible");
            }
        }

        // 2. L'adhérent existe-t-il ?
        String nom;
        String sqlAdherent = "SELECT nom FROM adherents WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlAdherent)) {
            ps.setInt(1, idAdherent);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return "Erreur : adhérent introuvable.";
                }
                nom = rs.getString("nom");
            }
        }

        // 3. Règles métier
        if (!disponible) {
            return "Erreur : ce livre est déjà emprunté.";
        }
        if (compterEmpruntsEnCours(conn, idAdherent) >= MAX_EMPRUNTS) {
            return "Erreur : " + nom + " a atteint la limite de " + MAX_EMPRUNTS + " livres.";
        }

        // 4. Enregistrement
        String sqlInsert =
                "INSERT INTO emprunts (livre_id, adherent_id, date_emprunt) VALUES (?, ?, CURDATE())";
        try (PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
            ps.setInt(1, idLivre);
            ps.setInt(2, idAdherent);
            ps.executeUpdate();
        }
        try (PreparedStatement ps =
                     conn.prepareStatement("UPDATE livres SET disponible = FALSE WHERE id = ?")) {
            ps.setInt(1, idLivre);
            ps.executeUpdate();
        }

        return "Succès : \"" + titre + "\" emprunté par " + nom + ".";
    }

    private String traiterRetour(Connection conn, int idLivre) throws SQLException {
        // 1. Le livre existe-t-il ?
        String titre;
        try (PreparedStatement ps =
                     conn.prepareStatement("SELECT titre FROM livres WHERE id = ? FOR UPDATE")) {
            ps.setInt(1, idLivre);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return "Erreur : livre introuvable.";
                }
                titre = rs.getString("titre");
            }
        }

        // 2. Y a-t-il un emprunt en cours pour ce livre ? (date_retour IS NULL)
        int idEmprunt;
        String nom;
        String sqlEmprunt =
                "SELECT e.id, a.nom FROM emprunts e "
                + "JOIN adherents a ON a.id = e.adherent_id "
                + "WHERE e.livre_id = ? AND e.date_retour IS NULL FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sqlEmprunt)) {
            ps.setInt(1, idLivre);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return "Erreur : ce livre n'est pas emprunté.";
                }
                idEmprunt = rs.getInt("id");
                nom = rs.getString("nom");
            }
        }

        // 3. Enregistrement du retour
        try (PreparedStatement ps =
                     conn.prepareStatement("UPDATE emprunts SET date_retour = CURDATE() WHERE id = ?")) {
            ps.setInt(1, idEmprunt);
            ps.executeUpdate();
        }
        try (PreparedStatement ps =
                     conn.prepareStatement("UPDATE livres SET disponible = TRUE WHERE id = ?")) {
            ps.setInt(1, idLivre);
            ps.executeUpdate();
        }

        return "Succès : \"" + titre + "\" retourné par " + nom + ".";
    }

    private int compterEmpruntsEnCours(Connection conn, int idAdherent) throws SQLException {
        String sql = "SELECT COUNT(*) FROM emprunts WHERE adherent_id = ? AND date_retour IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idAdherent);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
