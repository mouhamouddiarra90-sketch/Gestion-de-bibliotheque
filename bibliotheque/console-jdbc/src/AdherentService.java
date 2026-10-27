import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class AdherentService {
    private static final String SELECT_ADHERENTS = "SELECT id, nom, telephone FROM adherents";

    public Adherent ajouter(String nom, String telephone) throws SQLException {
        String sql = "INSERT INTO adherents (nom, telephone) VALUES (?, ?)";
        try (Connection conn = ConnexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nom);
            ps.setString(2, telephone);
            ps.executeUpdate();

            try (ResultSet cles = ps.getGeneratedKeys()) {
                cles.next();
                return new Adherent(cles.getInt(1), nom, telephone);
            }
        }
    }

    public ArrayList<Adherent> lister() throws SQLException {
        return chercher(SELECT_ADHERENTS + " ORDER BY id");
    }

    /** Recherche par mot-clé dans le nom ou le téléphone. */
    public ArrayList<Adherent> rechercher(String motCle) throws SQLException {
        String motif = "%" + motCle + "%";
        return chercher(SELECT_ADHERENTS + " WHERE nom LIKE ? OR telephone LIKE ? ORDER BY id",
                motif, motif);
    }

    /** Retourne null si l'adhérent n'existe pas. */
    public Adherent trouverParId(int id) throws SQLException {
        ArrayList<Adherent> resultat = chercher(SELECT_ADHERENTS + " WHERE id = ?", String.valueOf(id));
        return resultat.isEmpty() ? null : resultat.get(0);
    }

    /** Un champ laissé vide conserve son ancienne valeur. */
    public String modifier(int id, String nouveauNom, String nouveauTelephone) throws SQLException {
        Adherent adherent = trouverParId(id);
        if (adherent == null) {
            return "Erreur : adhérent introuvable.";
        }
        if (nouveauNom.isEmpty() && nouveauTelephone.isEmpty()) {
            return "Aucune modification : les deux champs sont restés vides.";
        }
        String nom = nouveauNom.isEmpty() ? adherent.getNom() : nouveauNom;
        String telephone = nouveauTelephone.isEmpty() ? adherent.getTelephone() : nouveauTelephone;

        String sql = "UPDATE adherents SET nom = ?, telephone = ? WHERE id = ?";
        try (Connection conn = ConnexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nom);
            ps.setString(2, telephone);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
        return "Adhérent modifié : " + new Adherent(id, nom, telephone);
    }

    /** Refusé si l'adhérent a un emprunt en cours ou un historique d'emprunts. */
    public String supprimer(int id) throws SQLException {
        Adherent adherent = trouverParId(id);
        if (adherent == null) {
            return "Erreur : adhérent introuvable.";
        }
        if (compter("SELECT COUNT(*) FROM emprunts WHERE adherent_id = ? AND date_retour IS NULL", id) > 0) {
            return "Erreur : impossible de supprimer un adhérent ayant des emprunts en cours.";
        }
        if (compter("SELECT COUNT(*) FROM emprunts WHERE adherent_id = ?", id) > 0) {
            return "Erreur : cet adhérent a un historique d'emprunts et ne peut pas être supprimé.";
        }
        try (Connection conn = ConnexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM adherents WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        return "Adhérent supprimé : \"" + adherent.getNom() + "\".";
    }

    // ---------- Méthodes privées ----------

    private int compter(String sql, int id) throws SQLException {
        try (Connection conn = ConnexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private ArrayList<Adherent> chercher(String sql, String... parametres) throws SQLException {
        ArrayList<Adherent> adherents = new ArrayList<>();
        try (Connection conn = ConnexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < parametres.length; i++) {
                ps.setString(i + 1, parametres[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    adherents.add(new Adherent(
                            rs.getInt("id"),
                            rs.getString("nom"),
                            rs.getString("telephone")));
                }
            }
        }
        return adherents;
    }
}
