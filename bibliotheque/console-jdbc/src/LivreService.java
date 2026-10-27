import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class LivreService {
    private static final String SELECT_LIVRES =
            "SELECT id, titre, auteur, disponible FROM livres";

    public Livre ajouter(String titre, String auteur) throws SQLException {
        String sql = "INSERT INTO livres (titre, auteur) VALUES (?, ?)";
        try (Connection conn = ConnexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, titre);
            ps.setString(2, auteur);
            ps.executeUpdate();

            try (ResultSet cles = ps.getGeneratedKeys()) {
                cles.next();
                return new Livre(cles.getInt(1), titre, auteur);
            }
        }
    }

    public ArrayList<Livre> lister() throws SQLException {
        return chercher(SELECT_LIVRES + " ORDER BY id");
    }

    public ArrayList<Livre> listerDisponibles() throws SQLException {
        return chercher(SELECT_LIVRES + " WHERE disponible = TRUE ORDER BY id");
    }

    /** Recherche par mot-clé dans le titre ou l'auteur (sans tenir compte des majuscules). */
    public ArrayList<Livre> rechercher(String motCle) throws SQLException {
        String motif = "%" + motCle + "%";
        return chercher(SELECT_LIVRES + " WHERE titre LIKE ? OR auteur LIKE ? ORDER BY id",
                motif, motif);
    }

    /** Retourne null si le livre n'existe pas. */
    public Livre trouverParId(int id) throws SQLException {
        String sql = SELECT_LIVRES + " WHERE id = ?";
        try (Connection conn = ConnexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? construireLivre(rs) : null;
            }
        }
    }

    /** Un champ laissé vide conserve son ancienne valeur. */
    public String modifier(int id, String nouveauTitre, String nouvelAuteur) throws SQLException {
        Livre livre = trouverParId(id);
        if (livre == null) {
            return "Erreur : livre introuvable.";
        }
        if (nouveauTitre.isEmpty() && nouvelAuteur.isEmpty()) {
            return "Aucune modification : les deux champs sont restés vides.";
        }
        if (!nouveauTitre.isEmpty()) {
            livre.setTitre(nouveauTitre);
        }
        if (!nouvelAuteur.isEmpty()) {
            livre.setAuteur(nouvelAuteur);
        }

        String sql = "UPDATE livres SET titre = ?, auteur = ? WHERE id = ?";
        try (Connection conn = ConnexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, livre.getTitre());
            ps.setString(2, livre.getAuteur());
            ps.setInt(3, id);
            ps.executeUpdate();
        }
        return "Livre modifié : " + livre;
    }

    /**
     * Un livre emprunté, ou ayant déjà un historique d'emprunts, ne peut pas être
     * supprimé (clé étrangère emprunts.livre_id).
     */
    public String supprimer(int id) throws SQLException {
        Livre livre = trouverParId(id);
        if (livre == null) {
            return "Erreur : livre introuvable.";
        }
        if (!livre.estDisponible()) {
            return "Erreur : impossible de supprimer un livre actuellement emprunté.";
        }

        try (Connection conn = ConnexionBD.getConnection()) {
            try (PreparedStatement ps =
                         conn.prepareStatement("SELECT COUNT(*) FROM emprunts WHERE livre_id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        return "Erreur : ce livre a un historique d'emprunts "
                                + "et ne peut pas être supprimé.";
                    }
                }
            }
            try (PreparedStatement ps =
                         conn.prepareStatement("DELETE FROM livres WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        }
        return "Livre supprimé : \"" + livre.getTitre() + "\".";
    }

    // ---------- Méthodes privées ----------

    private ArrayList<Livre> chercher(String sql, String... parametres) throws SQLException {
        ArrayList<Livre> livres = new ArrayList<>();
        try (Connection conn = ConnexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < parametres.length; i++) {
                ps.setString(i + 1, parametres[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    livres.add(construireLivre(rs));
                }
            }
        }
        return livres;
    }

    private Livre construireLivre(ResultSet rs) throws SQLException {
        return new Livre(
                rs.getInt("id"),
                rs.getString("titre"),
                rs.getString("auteur"),
                rs.getBoolean("disponible"));
    }
}
