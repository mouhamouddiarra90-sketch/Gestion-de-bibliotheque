public class Adherent {
    private int id;
    private String nom;
    private String telephone;

    public Adherent(int id, String nom, String telephone) {
        this.id = id;
        this.nom = nom;
        this.telephone = telephone;
    }

    public int getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getTelephone() {
        return telephone;
    }

    @Override
    public String toString() {
        String tel = (telephone == null || telephone.isEmpty()) ? "-" : telephone;
        return "[" + id + "] " + nom + " - Tel : " + tel;
    }
}
