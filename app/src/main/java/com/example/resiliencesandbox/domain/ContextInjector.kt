package com.example.resiliencesandbox.domain

import com.example.resiliencesandbox.data.CharacterRepository
import com.example.resiliencesandbox.data.local.dao.InventoryDao
import com.example.resiliencesandbox.data.local.dao.LocationDao
import com.example.resiliencesandbox.data.local.dao.NpcDao
import com.example.resiliencesandbox.data.local.entity.CharacterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContextInjector(
    private val repository: CharacterRepository,
    private val locationDao: LocationDao,
    private val npcDao: NpcDao,
    private val inventoryDao: InventoryDao
) {

    suspend fun buildOmniscientPrompt(
        userQuery: String, 
        crisisReason: String? = null,
        isWakeUp: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        // 1. Récupération de l'état actuel ou d'un état par défaut
        val character = repository.getCharacterState() ?: CharacterEntity(
            id = 1,
            argent = 0,
            energie = 90,
            postureActuelle = "Inconnue",
            gameTimeMinutes = 0L,
            physical = 85, social = 0, intellect = 0, survival = 60, finance = 0, willpower = 0, creativity = 0,
            peur = 0, colere = 0, tristesse = 0, joie = 0, calme = 0, fatigue = 0, toxicite = 0
        )

        // 2. Récupération des 2 derniers évènements (ils arrivent triés du plus récent au plus ancien via le DAO)
        // On les inverse (reversed) pour les avoir du plus ancien au plus récent chronologiquement.
        val recentLogs = repository.getRecentEventLogs(2).reversed()

        // 3. Formatage de la mémoire récente en texte
        val logsHistoriques = if (recentLogs.isEmpty()) {
            "Aucun souvenir récent."
        } else {
            recentLogs.joinToString(separator = "\n") { log ->
                "- ${log.descriptionText}"
            }
        }

        // 4. Extraction de l'environnement (Mondes, PNJ, Objets)
        val currentLocation = locationDao.getAllLocations().lastOrNull()
        val allNpcs = npcDao.getAllNpcs()
        val npcsString = if (allNpcs.isEmpty()) "Aucun" else allNpcs.joinToString { it.name }
        val allInventory = inventoryDao.getAllInventory()
        val inventoryString = if (allInventory.isEmpty()) "Vide" else allInventory.joinToString { "${it.name} (${it.condition}) x${it.quantity}" }

        // 4.5 Formatage du temps
        val days = arrayOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")
        val totalMinutes = character.gameTimeMinutes + (8 * 60)
        val currentDay = days[(totalMinutes / (24 * 60)).toInt() % 7]
        val hours = (totalMinutes / 60) % 24
        val minutes = totalMinutes % 60
        val timeString = String.format("%s, %02d:%02d", currentDay, hours, minutes)

        // 5. Injection et formatage dans le template strict demandé
        """
        <start_of_turn>user
        [TEMPS ET HORLOGE]
        Heure locale in-game : $timeString
        
        [SYSTÈME - RÔLE NARRATIF]
        Tu es le Maître de Jeu d'une simulation de vie textuelle RÉALISTE et CONTEMPORAINE. Pas de science-fiction, pas d'apocalypse, pas de magie.
        Le contexte de départ est défini UNIQUEMENT par la première action du joueur.
        Règle : Ton ton est neutre, terre-à-terre et factuel. La difficulté du jeu provient de la galère du quotidien (manque d'argent, fatigue, problèmes matériels, relations sociales complexes). Décris l'environnement moderne de manière concise. Laisse le joueur libre de son action SANS jamais poser de questions bateau comme "Que fais-tu ?".
        Règle Narrative Absolue : Ne redécris JAMAIS le lieu actuel si le joueur n'a pas changé de zone. Concentre-toi strictement sur l'action immédiate et le dialogue.

        [SYSTÈME - RÔLE TECHNIQUE]
        Tu DOIS obligatoirement terminer ta réponse par un bloc JSON valide encadré par <DATA> et </DATA>.
        Règle PNJ : Ne remplis la liste 'nouveaux_pnj' que si le personnage a une réelle importance pour la suite. Ne liste pas les figurants ou la foule.
        Règle Objet : Génère une description courte (1 phrase) factuelle et précise pour chaque nouvel objet.
        Règle Métabolique : Si le joueur consomme des aliments avariés, de l'alcool, ou s'épuise sans s'hydrater, génère une valeur positive de 'toxicite'. S'il a une nutrition saine et un vrai repos, génère une valeur négative pour purger son organisme.

        EXEMPLE DE RÉPONSE EXIGÉE DE TA PART :
        Tu es dans ta cuisine. Une épaisse fumée s'échappe de ton Airfryer en mode Max crisp, ton repas est complètement cramé. Tu as très faim, tu es fatigué de ta journée, et il te reste 12 euros sur ton compte en banque. Que fais-tu ?
        <DATA>{"peur": 2, "fatigue": 15, "toxicite": 2, "lieu": "appartement", "nouveaux_pnj": [], "nouveaux_objets": [{"nom": "Clé anglaise", "etat": "Rouillée", "description": "Lourde et tachée d'huile, mais encore solide."}]}</DATA>

        [ÉTAT INTERNE ET STATISTIQUES]
        Posture: ${character.postureActuelle} | Argent: ${character.argent}€ | Énergie: ${character.energie}/100
        Émotions dominantes: Peur(${character.peur}), Colère(${character.colere}), Tristesse(${character.tristesse}), Joie(${character.joie}), Calme(${character.calme}), Fatigue(${character.fatigue}), Toxicité(${character.toxicite})

        [ENVIRONNEMENT ET INVENTAIRE (BDD)]
        Lieu actuel : ${currentLocation?.name ?: "Inconnu"}
        Personnages connus ici : $npcsString
        Inventaire : $inventoryString

        [MÉMOIRE RÉCENTE DES ÉVÈNEMENTS (BDD)]
        $logsHistoriques

        ${
            when {
                crisisReason != null -> "[DIRECTIVE D'URGENCE]\nLe joueur a atteint un point de rupture : $crisisReason. Ignore l'action du joueur. Raconte son effondrement physique ou mental immédiat et ses conséquences dramatiques, puis impose un réveil. Dans ton bloc JSON, réajuste obligatoirement ses jauges pour refléter ce repos forcé (ex: baisse la fatigue) ou ce choc."
                isWakeUp -> "[DIRECTIVE DE RÉVEIL]\nLe joueur revient après une longue période d'auto-pilote. Voici ce qu'il s'est passé en son absence:\n$logsHistoriques\nRaconte de manière brutale comment il reprend conscience de son environnement. Ignore son action."
                else -> ""
            }
        }

        ACTION DU JOUEUR : "$userQuery"

        [DIRECTIVE ABSOLUE] Tu es un moteur de jeu textuel. Interdiction formelle de poser des questions à la fin de tes narrations. Tu DOIS obligatoirement terminer ta réponse par le bloc technique exact suivant, SANS utiliser de formatage Markdown ou de blocs de code autour : <DATA>{"peur":0,"fatigue":0,"toxicite":0,"lieu":"nom_du_lieu","nouveaux_pnj":[],"nouveaux_objets":[]}</DATA>
        [DIRECTIVE FORMAT] : NE GÉNÈRE AUCUN TEXTE APRÈS LA BALISE </DATA>. LA BALISE DOIT ÊTRE LE TOUT DERNIER ÉLÉMENT DE TA RÉPONSE.
        <end_of_turn>
        <start_of_turn>model
        """.trimIndent()
    }
}
