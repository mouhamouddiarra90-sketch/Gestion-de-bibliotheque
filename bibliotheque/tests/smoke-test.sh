#!/usr/bin/env bash
# Test de bout en bout de l'API : requête HTTP -> Spring Boot -> JPA -> MySQL.
# Prérequis : MySQL démarré, API lancée (mvn spring-boot:run).
# Usage : bash tests/smoke-test.sh [URL]        (défaut : http://localhost:8080)
# Fonctionne avec Git Bash sous Windows, Linux et macOS (curl requis).
# Le test crée 1 adhérent et 1 livre de test (préfixe "Test") qui restent dans la base.

BASE="${1:-http://localhost:8080}"
S="$(date +%s)"
LIVRE_TITRE="LivreTest$S"
LIVRE_TITRE2="LivreTest${S}Modifie"
OK=0
KO=0
TMP="$(mktemp)"
trap 'rm -f "$TMP"' EXIT

# appel METHODE CHEMIN [CORPS_JSON]  ->  variables STATUT et CORPS
appel() {
    if [ -n "$3" ]; then
        STATUT=$(curl -s -o "$TMP" -w "%{http_code}" -X "$1" "$BASE$2" \
                 -H "Content-Type: application/json" -d "$3")
    else
        STATUT=$(curl -s -o "$TMP" -w "%{http_code}" -X "$1" "$BASE$2")
    fi
    CORPS="$(cat "$TMP")"
}

# verifier "description" CODE_ATTENDU ["texte attendu dans la réponse"]
verifier() {
    if [ "$STATUT" = "$2" ] && { [ -z "$3" ] || [[ "$CORPS" == *"$3"* ]]; }; then
        echo "OK     $1"
        OK=$((OK + 1))
    else
        echo "ECHEC  $1  (code reçu : $STATUT, attendu : $2)"
        echo "       réponse : ${CORPS:0:200}"
        KO=$((KO + 1))
    fi
}

extraire_id() { echo "$CORPS" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2; }

echo "Test de l'API sur $BASE"
appel GET /api/livres
if [ "$STATUT" = "000" ]; then
    echo "API injoignable : lancez d'abord MySQL puis 'mvn spring-boot:run'."
    exit 2
fi
verifier "L'API répond et lit la base" 200
echo

# 1. Ajout d'un adhérent
appel POST /api/adherents '{"nom":"Adherent Test '$S'","telephone":"70000000"}'
verifier "Ajout d'un adhérent" 201 '"nom":"Adherent Test'
ID_ADH=$(extraire_id)

# 2. Ajout d'un livre
appel POST /api/livres '{"titre":"'$LIVRE_TITRE'","auteur":"Auteur Test"}'
verifier "Ajout d'un livre (disponible par défaut)" 201 '"disponible":true'
ID_LIVRE=$(extraire_id)

# 3. Recherche
appel GET "/api/livres?recherche=$LIVRE_TITRE"
verifier "Recherche d'un livre" 200 "$LIVRE_TITRE"

# 4. Modification
appel PUT /api/livres/$ID_LIVRE '{"titre":"'$LIVRE_TITRE2'","auteur":"Auteur Test"}'
verifier "Modification d'un livre" 200 "$LIVRE_TITRE2"

appel GET "/api/livres?disponible=true&recherche=$LIVRE_TITRE2"
verifier "Livre présent dans les livres disponibles" 200 "$LIVRE_TITRE2"

# 5. Emprunt
appel POST /api/emprunts '{"livreId":'$ID_LIVRE',"adherentId":'$ID_ADH'}'
verifier "Emprunt d'un livre" 201 '"dateRetour":null'

appel GET /api/livres/$ID_LIVRE
verifier "Le livre est devenu indisponible" 200 '"disponible":false'

appel POST /api/emprunts '{"livreId":'$ID_LIVRE',"adherentId":'$ID_ADH'}'
verifier "Refus : livre déjà emprunté" 409

appel DELETE /api/livres/$ID_LIVRE
verifier "Refus : suppression d'un livre emprunté" 409

appel GET "/api/emprunts?enCours=true"
verifier "Liste des emprunts en cours" 200 "$LIVRE_TITRE2"

# 6. Retour
appel POST /api/retours '{"livreId":'$ID_LIVRE'}'
verifier "Retour d'un livre (date de retour enregistrée)" 200 '"dateRetour":"'

appel GET /api/livres/$ID_LIVRE
verifier "Le livre est de nouveau disponible" 200 '"disponible":true'

appel POST /api/retours '{"livreId":'$ID_LIVRE'}'
verifier "Refus : retour d'un livre non emprunté" 409

appel DELETE /api/livres/$ID_LIVRE
verifier "Refus : suppression d'un livre avec historique" 409

# 7. Suppression (livre sans historique)
appel POST /api/livres '{"titre":"Suppr'$S'","auteur":"Auteur Test"}'
ID_SUPPR=$(extraire_id)
appel DELETE /api/livres/$ID_SUPPR
verifier "Suppression d'un livre sans historique" 204
appel GET /api/livres/$ID_SUPPR
verifier "Le livre supprimé n'existe plus" 404

# 8. Adhérents : recherche, modification, suppression
appel GET "/api/adherents?recherche=Adherent%20Test%20$S"
verifier "Recherche d'un adhérent" 200 "Adherent Test $S"

appel PUT /api/adherents/$ID_ADH '{"nom":"Adherent Modifie '$S'","telephone":"71111111"}'
verifier "Modification d'un adhérent" 200 "Adherent Modifie $S"

appel DELETE /api/adherents/$ID_ADH
verifier "Refus : suppression d'un adhérent avec historique" 409

appel POST /api/adherents '{"nom":"AdhSuppr'$S'"}'
ID_ADH2=$(extraire_id)
appel DELETE /api/adherents/$ID_ADH2
verifier "Suppression d'un adhérent sans historique" 204
appel GET /api/adherents/$ID_ADH2
verifier "L'adhérent supprimé n'existe plus" 404

appel POST /api/adherents '{"nom":"   "}'
verifier "Refus : adhérent sans nom" 400

# 9. Validation
appel POST /api/livres '{"titre":"","auteur":""}'
verifier "Refus : livre sans titre ni auteur" 400

echo
echo "Résultat : $OK réussi(s), $KO échec(s)."
[ "$KO" -eq 0 ] && echo "TOUS LES TESTS PASSENT" || echo "DES TESTS ONT ECHOUE"
exit $KO
