-- Base de données de la bibliothèque
CREATE DATABASE IF NOT EXISTS bibliotheque
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE bibliotheque;

-- Réinitialisation (attention : efface les données existantes)
DROP TABLE IF EXISTS emprunts;
DROP TABLE IF EXISTS livres;
DROP TABLE IF EXISTS adherents;

CREATE TABLE livres (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    titre       VARCHAR(150) NOT NULL,
    auteur      VARCHAR(100) NOT NULL,
    disponible  BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE adherents (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    nom        VARCHAR(100) NOT NULL,
    telephone  VARCHAR(20)
) ENGINE=InnoDB;

CREATE TABLE emprunts (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    livre_id      INT NOT NULL,
    adherent_id   INT NOT NULL,
    date_emprunt  DATE NOT NULL,
    date_retour   DATE NULL,
    CONSTRAINT fk_emprunt_livre
        FOREIGN KEY (livre_id) REFERENCES livres(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_emprunt_adherent
        FOREIGN KEY (adherent_id) REFERENCES adherents(id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;

-- Données de test
INSERT INTO livres (titre, auteur, disponible) VALUES
    ('Java pour débutants', 'Marie Dupont', TRUE),
    ('Soundjata ou l''épopée mandingue', 'Djibril Tamsir Niane', FALSE),
    ('Une si longue lettre', 'Mariama Bâ', TRUE),
    ('Base de données et SQL', 'Pierre Martin', FALSE),
    ('Les Bouts de bois de Dieu', 'Sembène Ousmane', TRUE);

INSERT INTO adherents (nom, telephone) VALUES
    ('Aminata Diallo', '76000001'),
    ('Ibrahim Keita', '66000002'),
    ('Fatoumata Coulibaly', '79000003');

INSERT INTO emprunts (livre_id, adherent_id, date_emprunt, date_retour) VALUES
    (2, 1, CURDATE() - INTERVAL 3 DAY, NULL),
    (1, 2, CURDATE() - INTERVAL 10 DAY, CURDATE() - INTERVAL 5 DAY),
    (4, 3, CURDATE() - INTERVAL 1 DAY, NULL);
