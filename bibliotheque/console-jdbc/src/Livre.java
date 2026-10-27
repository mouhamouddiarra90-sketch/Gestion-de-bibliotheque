public class Livre {
    private int id;
    private String titre;
    private String auteur;
    private boolean disponible;

    // Nouveau livre (disponible par défaut)
    public Livre(int id, String titre, String auteur) {
        this(id, titre, auteur, true);
    }

    // Livre lu depuis la base de données
    public Livre(int id, String titre, String auteur, boolean disponible) {
        this.id = id;
        this.titre = titre;
        this.auteur = auteur;
        this.disponible = disponible;
    }

    public int getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getAuteur() {
        return auteur;
    }

    public void setAuteur(String auteur) {
        this.auteur = auteur;
    }

    public boolean estDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + titre + " - " + auteur
                + " (" + (disponible ? "Disponible" : "Emprunté") + ")";
    }
}
