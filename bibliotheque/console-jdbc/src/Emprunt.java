import java.time.LocalDate;

public class Emprunt {
    private int id;
    private Livre livre;
    private Adherent adherent;
    private LocalDate dateEmprunt;
    private LocalDate dateRetour; // null tant que le livre n'est pas retourné

    // Nouvel emprunt (date du jour)
    public Emprunt(int id, Livre livre, Adherent adherent) {
        this(id, livre, adherent, LocalDate.now(), null);
    }

    // Emprunt lu depuis la base de données
    public Emprunt(int id, Livre livre, Adherent adherent,
                   LocalDate dateEmprunt, LocalDate dateRetour) {
        this.id = id;
        this.livre = livre;
        this.adherent = adherent;
        this.dateEmprunt = dateEmprunt;
        this.dateRetour = dateRetour;
    }

    public int getId() {
        return id;
    }

    public Livre getLivre() {
        return livre;
    }

    public Adherent getAdherent() {
        return adherent;
    }

    public boolean estEnCours() {
        return dateRetour == null;
    }

    public void enregistrerRetour() {
        this.dateRetour = LocalDate.now();
    }

    @Override
    public String toString() {
        String etat = estEnCours() ? "en cours" : "retourné le " + dateRetour;
        return "[" + id + "] \"" + livre.getTitre() + "\" (livre n°" + livre.getId() + ") - "
                + adherent.getNom() + " - emprunté le " + dateEmprunt + " (" + etat + ")";
    }
}
