'use strict';

// L'interface est servie par Spring Boot : même adresse que l'API, donc pas de CORS.
const API = '/api';
const app = document.getElementById('app');

/* ==========================================================
   Utilitaires
   ========================================================== */

// Crée un élément HTML. Les textes passent par createTextNode : pas d'injection HTML possible.
function el(tag, props = {}, ...children) {
    const node = document.createElement(tag);
    for (const [cle, valeur] of Object.entries(props)) {
        if (cle === 'class') node.className = valeur;
        else if (cle.startsWith('on')) node.addEventListener(cle.slice(2), valeur);
        else if (valeur === true) node.setAttribute(cle, '');
        else if (valeur !== false && valeur != null) node.setAttribute(cle, valeur);
    }
    for (const enfant of children.flat()) {
        node.append(enfant instanceof Node ? enfant : document.createTextNode(enfant ?? ''));
    }
    return node;
}

// Icônes (contenu fixe, jamais de données saisies)
const ICONES = {
    tableau: '<rect x="3" y="3" width="7" height="9" rx="1.5"/><rect x="14" y="3" width="7" height="5" rx="1.5"/><rect x="14" y="12" width="7" height="9" rx="1.5"/><rect x="3" y="16" width="7" height="5" rx="1.5"/>',
    livre: '<path d="M4 19.5V5a2 2 0 0 1 2-2h14v14H6a2 2 0 0 0-2 2.5Z"/><path d="M8 7h8M8 11h5"/>',
    adherents: '<circle cx="9" cy="8" r="3.5"/><path d="M2.5 20c0-3.6 2.9-6 6.5-6s6.5 2.4 6.5 6"/><path d="M16 4.7a3.5 3.5 0 0 1 0 6.6M18 14.4c2.2.8 3.5 2.7 3.5 5.6"/>',
    emprunts: '<path d="M4 8h14l-3-3M20 16H6l3 3"/>',
    check: '<path d="M5 12.5l4.5 4.5L19 7.5"/>',
    retour: '<path d="M9 14l-4-4 4-4"/><path d="M5 10h9a5 5 0 0 1 0 10h-3"/>'
};

function icone(nom) {
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('viewBox', '0 0 24 24');
    svg.setAttribute('class', 'icone');
    svg.setAttribute('aria-hidden', 'true');
    svg.innerHTML = ICONES[nom];
    return svg;
}

function formatDate(iso) {
    if (!iso) return '';
    const [annee, mois, jour] = iso.split('-').map(Number);
    return new Date(annee, mois - 1, jour).toLocaleDateString('fr-FR');
}

function toast(message, type = 'ok') {
    const boite = el('div', { class: type === 'erreur' ? 'toast erreur' : 'toast' }, message);
    document.getElementById('toasts').append(boite);
    setTimeout(() => boite.remove(), type === 'erreur' ? 6000 : 3500);
}

function blocErreur(message) {
    return el('div', { class: 'error-box', role: 'alert' }, message);
}

function vide(message) {
    return el('div', { class: 'table-card' }, el('p', { class: 'empty' }, message));
}

// colonnes : ['ID', 'Titre', ..., 'Actions'] ; lignes : tableaux de cellules (texte ou éléments)
function tableau(colonnes, lignes) {
    return el('div', { class: 'table-card' },
        el('table', {},
            el('thead', {}, el('tr', {}, colonnes.map(c => el('th', {}, c)))),
            el('tbody', {}, lignes.map(ligne =>
                el('tr', {}, ligne.map((cellule, i) =>
                    el('td', { 'data-label': colonnes[i],
                               class: colonnes[i] === 'Actions' ? 'actions' : '' }, cellule)))))));
}

/* ==========================================================
   Appels à l'API
   ========================================================== */

function messageErreur(donnees, statut) {
    // Erreurs de validation (400) : liste des messages des champs
    if (donnees && Array.isArray(donnees.errors) && donnees.errors.length > 0) {
        return donnees.errors.map(e => e.defaultMessage).join(' ');
    }
    if (donnees && donnees.message) return donnees.message;
    return 'Erreur ' + statut + '.';
}

async function api(chemin, options = {}) {
    let reponse;
    try {
        reponse = await fetch(API + chemin, {
            headers: { 'Content-Type': 'application/json' },
            ...options
        });
    } catch (e) {
        throw new Error("Impossible de joindre l'API. Vérifiez que le serveur Spring Boot est démarré.");
    }
    if (reponse.status === 204) return null;

    let donnees = null;
    try { donnees = await reponse.json(); } catch (e) { /* réponse sans JSON */ }

    if (!reponse.ok) throw new Error(messageErreur(donnees, reponse.status));
    return donnees;
}

/* ==========================================================
   Boîtes de dialogue : confirmation et formulaire générique
   ========================================================== */

function confirmer(message) {
    const dlg = document.getElementById('dialog-confirm');
    document.getElementById('confirm-message').textContent = message;
    dlg.returnValue = '';
    return new Promise(resolve => {
        dlg.addEventListener('close', () => resolve(dlg.returnValue === 'ok'), { once: true });
        dlg.showModal();
    });
}

const dlgForm = document.getElementById('dialog-form');
const formGen = document.getElementById('form-generique');
const formErreur = document.getElementById('form-erreur');
let champsCourants = [];
let enregistrerCourant = async () => {};
let rafraichir = async () => {};   // redéfini par la page affichée

// champs : [{ nom, libelle, max, obligatoire }] ; enregistrer(donnees) appelle l'API
function ouvrirFormulaire({ titre, champs, valeurs = {}, enregistrer }) {
    document.getElementById('form-titre').textContent = titre;
    champsCourants = champs;
    enregistrerCourant = enregistrer;
    document.getElementById('form-champs').replaceChildren(...champs.flatMap(c => [
        el('label', { for: 'champ-' + c.nom }, c.libelle),
        el('input', { id: 'champ-' + c.nom, name: c.nom, type: 'text', maxlength: c.max,
                      required: !!c.obligatoire, autocomplete: 'off', value: valeurs[c.nom] ?? '' })
    ]));
    formErreur.hidden = true;
    dlgForm.showModal();
    formGen.elements[champs[0].nom].focus();
}

document.getElementById('form-annuler').addEventListener('click', () => dlgForm.close());

formGen.addEventListener('submit', async (e) => {
    e.preventDefault();
    const bouton = formGen.querySelector('button[type="submit"]');
    bouton.disabled = true;
    formErreur.hidden = true;

    const donnees = {};
    champsCourants.forEach(c => { donnees[c.nom] = formGen.elements[c.nom].value.trim(); });

    try {
        await enregistrerCourant(donnees);
        dlgForm.close();
    } catch (err) {
        formErreur.textContent = err.message;   // affiché dans la boîte (les messages passent dessous)
        formErreur.hidden = false;
    } finally {
        bouton.disabled = false;
    }
});

/* ==========================================================
   Page : tableau de bord
   ========================================================== */

async function pageTableauDeBord() {
    const [livres, adherents, emprunts] = await Promise.all([
        api('/livres'), api('/adherents'), api('/emprunts')
    ]);
    const disponibles = livres.filter(l => l.disponible).length;
    const empruntes = livres.length - disponibles;
    const enCours = emprunts.filter(e => !e.dateRetour).length;
    const retournes = emprunts.length - enCours;
    const taux = livres.length ? Math.round(disponibles * 100 / livres.length) : 0;
    const derniers = [...emprunts].sort((a, b) => b.id - a.id).slice(0, 5);

    const kpi = (valeur, libelle, nomIcone, couleur, lien) =>
        el('a', { class: 'kpi', href: lien },
            el('span', { class: 'kpi-icone ' + couleur }, icone(nomIcone)),
            el('span', {},
                el('span', { class: 'kpi-valeur' }, String(valeur)),
                el('span', { class: 'kpi-libelle' }, libelle)));

    const donut = el('div', { class: 'donut', role: 'img',
        'aria-label': taux + ' % des livres sont disponibles' },
        el('span', { class: 'donut-valeur' }, taux + ' %'));
    donut.style.setProperty('--pct', taux);

    const date = new Date().toLocaleDateString('fr-FR',
        { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });

    let listeRecente;
    if (derniers.length === 0) {
        listeRecente = el('p', { class: 'empty' }, 'Aucun emprunt enregistré.');
    } else {
        listeRecente = tableau(['Livre', 'Adhérent', 'Date', 'Statut'], derniers.map(e => [
            e.livre.titre,
            e.adherent.nom,
            formatDate(e.dateEmprunt),
            el('span', { class: e.dateRetour ? 'badge ok' : 'badge warn' },
                e.dateRetour ? 'Retourné' : 'En cours')
        ]));
    }

    app.replaceChildren(
        el('section', { class: 'hero' },
            el('h1', {}, 'Tableau de bord'),
            el('p', {}, date),
            el('div', { class: 'hero-actions' },
                el('a', { class: 'btn light', href: '#/emprunts' }, 'Enregistrer un emprunt'),
                el('a', { class: 'btn ghost', href: '#/livres' }, 'Gérer les livres'))),
        el('div', { class: 'kpis' },
            kpi(livres.length, 'Livres', 'livre', 'c1', '#/livres'),
            kpi(disponibles, 'Livres disponibles', 'check', 'c2', '#/livres'),
            kpi(adherents.length, 'Adhérents', 'adherents', 'c3', '#/adherents'),
            kpi(enCours, 'Emprunts en cours', 'emprunts', 'c4', '#/emprunts'),
            kpi(retournes, 'Retours enregistrés', 'retour', 'c5', '#/emprunts')),
        el('div', { class: 'dashboard-grid' },
            el('section', { class: 'panel' },
                el('h2', {}, 'Disponibilité des livres'),
                el('div', { class: 'donut-zone' }, donut,
                    el('div', { class: 'legende' },
                        el('span', {}, disponibles + ' disponible(s)'),
                        el('span', { class: 'pris' }, empruntes + ' emprunté(s)')))),
            el('section', {},
                el('h2', {}, 'Derniers emprunts'),
                listeRecente))
    );
}

/* ==========================================================
   Page : livres
   ========================================================== */

function formulaireLivre(livre) {
    ouvrirFormulaire({
        titre: livre ? 'Modifier le livre' : 'Ajouter un livre',
        champs: [
            { nom: 'titre', libelle: 'Titre', max: 150, obligatoire: true },
            { nom: 'auteur', libelle: 'Auteur', max: 100, obligatoire: true }
        ],
        valeurs: livre || {},
        enregistrer: async (donnees) => {
            const corps = JSON.stringify(donnees);
            if (livre) {
                await api('/livres/' + livre.id, { method: 'PUT', body: corps });
                toast('Livre modifié.');
            } else {
                await api('/livres', { method: 'POST', body: corps });
                toast('Livre ajouté.');
            }
            await rafraichir();
        }
    });
}

async function supprimerLivre(livre) {
    if (!await confirmer('Supprimer « ' + livre.titre + ' » ? Cette action est définitive.')) return;
    try {
        await api('/livres/' + livre.id, { method: 'DELETE' });
        toast('Livre supprimé.');
        await rafraichir();
    } catch (err) {
        toast(err.message, 'erreur');
    }
}

function tableauLivres(livres) {
    if (livres.length === 0) return vide('Aucun livre trouvé.');
    return tableau(['ID', 'Titre', 'Auteur', 'Statut', 'Actions'], livres.map(livre => [
        String(livre.id),
        livre.titre,
        livre.auteur,
        el('span', { class: livre.disponible ? 'badge ok' : 'badge warn' },
            livre.disponible ? 'Disponible' : 'Emprunté'),
        [el('button', { class: 'btn small', type: 'button',
                        onclick: () => formulaireLivre(livre) }, 'Modifier'),
         el('button', { class: 'btn small danger-outline', type: 'button',
                        onclick: () => supprimerLivre(livre) }, 'Supprimer')]
    ]));
}

async function pageLivres() {
    const champRecherche = el('input', {
        type: 'search', id: 'recherche', 'aria-label': 'Rechercher un livre',
        placeholder: 'Rechercher un titre ou un auteur…'
    });
    const caseDispo = el('input', { type: 'checkbox', id: 'filtre-dispo' });
    const zoneResultats = el('div');

    async function charger() {
        const params = new URLSearchParams();
        if (champRecherche.value.trim()) params.set('recherche', champRecherche.value.trim());
        if (caseDispo.checked) params.set('disponible', 'true');
        const texte = params.toString();
        try {
            zoneResultats.replaceChildren(tableauLivres(await api('/livres' + (texte ? '?' + texte : ''))));
        } catch (err) {
            zoneResultats.replaceChildren(blocErreur(err.message));
        }
    }
    rafraichir = charger;

    let minuteur;
    champRecherche.addEventListener('input', () => {
        clearTimeout(minuteur);
        minuteur = setTimeout(charger, 300);
    });
    caseDispo.addEventListener('change', charger);

    app.replaceChildren(
        el('div', { class: 'page-head' },
            el('h1', {}, 'Livres'),
            el('button', { class: 'btn primary', type: 'button',
                           onclick: () => formulaireLivre(null) }, 'Ajouter un livre')),
        el('div', { class: 'toolbar' },
            champRecherche,
            el('label', { class: 'checkbox', for: 'filtre-dispo' }, caseDispo, 'Disponibles seulement')),
        zoneResultats
    );
    await charger();
}

/* ==========================================================
   Page : adhérents (ajout, modification, suppression, recherche)
   ========================================================== */

function formulaireAdherent(adherent) {
    ouvrirFormulaire({
        titre: adherent ? "Modifier l'adhérent" : 'Ajouter un adhérent',
        champs: [
            { nom: 'nom', libelle: 'Nom complet', max: 100, obligatoire: true },
            { nom: 'telephone', libelle: 'Téléphone (facultatif)', max: 20, obligatoire: false }
        ],
        valeurs: adherent || {},
        enregistrer: async (donnees) => {
            const corps = JSON.stringify({ nom: donnees.nom, telephone: donnees.telephone || null });
            if (adherent) {
                await api('/adherents/' + adherent.id, { method: 'PUT', body: corps });
                toast('Adhérent modifié.');
            } else {
                await api('/adherents', { method: 'POST', body: corps });
                toast('Adhérent ajouté.');
            }
            await rafraichir();
        }
    });
}

async function supprimerAdherent(adherent) {
    if (!await confirmer('Supprimer « ' + adherent.nom + ' » ? Cette action est définitive.')) return;
    try {
        await api('/adherents/' + adherent.id, { method: 'DELETE' });
        toast('Adhérent supprimé.');
        await rafraichir();
    } catch (err) {
        toast(err.message, 'erreur');
    }
}

function tableauAdherents(adherents) {
    if (adherents.length === 0) return vide('Aucun adhérent trouvé.');
    return tableau(['ID', 'Nom', 'Téléphone', 'Actions'], adherents.map(a => [
        String(a.id),
        a.nom,
        a.telephone || '—',
        [el('button', { class: 'btn small', type: 'button',
                        onclick: () => formulaireAdherent(a) }, 'Modifier'),
         el('button', { class: 'btn small danger-outline', type: 'button',
                        onclick: () => supprimerAdherent(a) }, 'Supprimer')]
    ]));
}

async function pageAdherents() {
    const champRecherche = el('input', {
        type: 'search', id: 'recherche', 'aria-label': 'Rechercher un adhérent',
        placeholder: 'Rechercher un nom ou un téléphone…'
    });
    const zoneResultats = el('div');

    async function charger() {
        const texte = champRecherche.value.trim();
        try {
            const adherents = await api('/adherents' +
                (texte ? '?recherche=' + encodeURIComponent(texte) : ''));
            zoneResultats.replaceChildren(tableauAdherents(adherents));
        } catch (err) {
            zoneResultats.replaceChildren(blocErreur(err.message));
        }
    }
    rafraichir = charger;

    let minuteur;
    champRecherche.addEventListener('input', () => {
        clearTimeout(minuteur);
        minuteur = setTimeout(charger, 300);
    });

    app.replaceChildren(
        el('div', { class: 'page-head' },
            el('h1', {}, 'Adhérents'),
            el('button', { class: 'btn primary', type: 'button',
                           onclick: () => formulaireAdherent(null) }, 'Ajouter un adhérent')),
        el('div', { class: 'toolbar' }, champRecherche),
        zoneResultats
    );
    await charger();
}

/* ==========================================================
   Page : emprunts et retours
   ========================================================== */

async function pageEmprunts() {
    const [livres, adherents, enCours] = await Promise.all([
        api('/livres?disponible=true'),
        api('/adherents'),
        api('/emprunts?enCours=true')
    ]);

    // --- Formulaire d'emprunt ---
    const choixLivre = el('select', { id: 'emprunt-livre', required: true },
        el('option', { value: '' }, livres.length ? '— Choisir un livre —' : 'Aucun livre disponible'),
        livres.map(l => el('option', { value: l.id }, l.titre + ' — ' + l.auteur)));

    const choixAdherent = el('select', { id: 'emprunt-adherent', required: true },
        el('option', { value: '' }, adherents.length ? '— Choisir un adhérent —' : 'Aucun adhérent'),
        adherents.map(a => el('option', { value: a.id }, a.nom)));

    const formEmprunt = el('form', { class: 'card form' },
        el('h2', {}, 'Enregistrer un emprunt'),
        el('label', { for: 'emprunt-livre' }, 'Livre'), choixLivre,
        el('label', { for: 'emprunt-adherent' }, 'Adhérent'), choixAdherent,
        el('button', { class: 'btn primary', type: 'submit' }, 'Emprunter'));

    formEmprunt.addEventListener('submit', async (e) => {
        e.preventDefault();
        const bouton = formEmprunt.querySelector('button[type="submit"]');
        bouton.disabled = true;
        try {
            const emprunt = await api('/emprunts', {
                method: 'POST',
                body: JSON.stringify({
                    livreId: Number(choixLivre.value),
                    adherentId: Number(choixAdherent.value)
                })
            });
            toast('« ' + emprunt.livre.titre + ' » emprunté par ' + emprunt.adherent.nom + '.');
            await pageEmprunts();
        } catch (err) {
            toast(err.message, 'erreur');
            bouton.disabled = false;
        }
    });

    // --- Formulaire de retour ---
    const choixRetour = el('select', { id: 'retour-livre', required: true },
        el('option', { value: '' }, enCours.length ? '— Choisir un emprunt —' : 'Aucun emprunt en cours'),
        enCours.map(emp => el('option', { value: emp.livre.id },
            emp.livre.titre + ' — ' + emp.adherent.nom)));

    const formRetour = el('form', { class: 'card form' },
        el('h2', {}, 'Enregistrer un retour'),
        el('label', { for: 'retour-livre' }, 'Emprunt en cours'), choixRetour,
        el('button', { class: 'btn primary', type: 'submit' }, 'Retourner'));

    formRetour.addEventListener('submit', async (e) => {
        e.preventDefault();
        const bouton = formRetour.querySelector('button[type="submit"]');
        bouton.disabled = true;
        try {
            const emprunt = await api('/retours', {
                method: 'POST',
                body: JSON.stringify({ livreId: Number(choixRetour.value) })
            });
            toast('« ' + emprunt.livre.titre + ' » retourné.');
            await pageEmprunts();
        } catch (err) {
            toast(err.message, 'erreur');
            bouton.disabled = false;
        }
    });

    app.replaceChildren(
        el('div', { class: 'page-head' }, el('h1', {}, 'Emprunts & retours')),
        el('div', { class: 'grid-2' }, formEmprunt, formRetour),
        el('h2', {}, 'Emprunts en cours'),
        enCours.length === 0
            ? vide('Aucun emprunt en cours.')
            : tableau(['ID', 'Livre', 'Adhérent', 'Emprunté le'], enCours.map(emp => [
                String(emp.id), emp.livre.titre, emp.adherent.nom, formatDate(emp.dateEmprunt)]))
    );
}

/* ==========================================================
   Navigation (une seule page, routes dans l'adresse après #)
   ========================================================== */

const routes = {
    '/': pageTableauDeBord,
    '/livres': pageLivres,
    '/adherents': pageAdherents,
    '/emprunts': pageEmprunts
};

async function naviguer() {
    const route = location.hash.replace(/^#/, '') || '/';
    const page = routes[route] || pageTableauDeBord;

    document.querySelectorAll('#nav a').forEach(lien => {
        if (lien.dataset.route === route) lien.setAttribute('aria-current', 'page');
        else lien.removeAttribute('aria-current');
    });

    app.replaceChildren(el('p', { class: 'loading' }, 'Chargement…'));
    try {
        await page();
    } catch (err) {
        app.replaceChildren(blocErreur(err.message));
    }
}

// Icônes du menu et du logo
document.querySelectorAll('#nav a').forEach(lien => lien.prepend(icone(lien.dataset.icone)));
document.getElementById('brand-logo').append(icone('livre'));

window.addEventListener('hashchange', naviguer);
naviguer();
