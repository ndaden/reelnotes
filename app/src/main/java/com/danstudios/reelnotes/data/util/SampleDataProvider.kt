package com.danstudios.reelnotes.data.util

import com.danstudios.reelnotes.domain.model.*

object SampleDataProvider {

    fun getSampleNotes(): List<ReelNote> {
        val now = System.currentTimeMillis()

        val pastaRecipe = ReelNote(
            id = 1L,
            reelUrl = "https://www.instagram.com/reel/DDh2O36IEyL/",
            shortcode = "DDh2O36IEyL",
            author = "@pastalover_fr",
            title = "Pâtes crémeuses à l'ail et parmesan (15 min)",
            category = NoteCategory.RECIPE,
            summary = "Une recette ultra crémeuse et réconfortante prête en 15 minutes chrono.",
            rawCaption = "Recette express : pâtes à l'ail et parmesan. Parfait pour les soirs de flemme !",
            markdownContent = """
                # Pâtes crémeuses à l'ail et parmesan (15 min)
                
                **Auteur :** @pastalover_fr  
                **Catégorie :** 🍳 Recette  
                **Lien Reel :** [Voir sur Instagram](https://www.instagram.com/reel/DDh2O36IEyL/)
                
                ### Résumé
                Une recette ultra crémeuse et réconfortante prête en 15 minutes chrono.
                
                ⏱️ Préparation : 5 min • 🔥 Cuisson : 10 min • 👥 Portions : 2 personnes
                
                ## Ingrédients
                - [ ] **250 g** Tagliatelles ou spaghettis
                - [ ] **3 gousses** Ail finement émincé
                - [ ] **150 ml** Crème liquide entière
                - [ ] **60 g** Parmesan fraîchement râpé
                - [ ] **2 c. à soupe** Huile d'olive extra vierge
                - [ ] **1 pincée** Sel et poivre du moulin
                - [ ] **1 poignée** Basilic ou persil frais
                
                ## Étapes / Préparation
                1. Porter une grande casserole d'eau salée à ébullition et cuire les pâtes al dente.
                2. Dans une sauteuse, faire revenir doucement l'ail dans l'huile d'olive sans le faire brunir (1-2 min).
                3. Ajouter la crème liquide et laisser frémir 2 minutes à feu moyen.
                4. Incorporer le parmesan hors du feu en fouettant pour créer une émulsion soyeuse.
                5. Égoutter les pâtes en réservant une louche d'eau de cuisson, puis les mélanger à la sauce. Servir immédiatement !
                
                ## Astuces du chef
                - 💡 Conservez toujours un peu d'eau de cuisson des pâtes : l'amidon lie parfaitement la sauce !
                - 💡 Utilisez du vrai Parmigiano Reggiano râpé au dernier moment pour une texture sans grumeaux.
                
                ### Tags
                #pasta #recettefacile #recetteexpress #comfortfood #faitmaison
            """.trimIndent(),
            structuredData = StructuredNoteData(
                summary = "Une recette ultra crémeuse et réconfortante prête en 15 minutes chrono.",
                prepTime = "5 min",
                cookTime = "10 min",
                servings = "2 personnes",
                ingredients = listOf(
                    IngredientItem(name = "Tagliatelles ou spaghettis", amount = "250", unit = "g"),
                    IngredientItem(name = "Ail finement émincé", amount = "3", unit = "gousses"),
                    IngredientItem(name = "Crème liquide entière", amount = "150", unit = "ml"),
                    IngredientItem(name = "Parmesan fraîchement râpé", amount = "60", unit = "g"),
                    IngredientItem(name = "Huile d'olive extra vierge", amount = "2", unit = "c. à soupe"),
                    IngredientItem(name = "Sel et poivre du moulin", amount = "1", unit = "pincée"),
                    IngredientItem(name = "Basilic ou persil frais", amount = "1", unit = "poignée")
                ),
                steps = listOf(
                    StepItem(1, "Porter une grande casserole d'eau salée à ébullition et cuire les pâtes al dente."),
                    StepItem(2, "Dans une sauteuse, faire revenir doucement l'ail dans l'huile d'olive sans le faire brunir (1-2 min)."),
                    StepItem(3, "Ajouter la crème liquide et laisser frémir 2 minutes à feu moyen."),
                    StepItem(4, "Incorporer le parmesan hors du feu en fouettant pour créer une émulsion soyeuse."),
                    StepItem(5, "Égoutter les pâtes en réservant une louche d'eau de cuisson, puis les mélanger à la sauce. Servir immédiatement !")
                ),
                tips = listOf(
                    "Conservez toujours un peu d'eau de cuisson des pâtes : l'amidon lie parfaitement la sauce !",
                    "Utilisez du vrai Parmigiano Reggiano râpé au dernier moment pour une texture sans grumeaux."
                ),
                keyTakeaways = listOf("Plat express en 15 minutes", "Sauce liée à l'eau de cuisson")
            ),
            thumbnailUrl = null,
            tags = listOf("pasta", "recettefacile", "recetteexpress", "comfortfood", "faitmaison"),
            isFavorite = true,
            createdAt = now - 86400000L,
            updatedAt = now - 86400000L
        )

        val workoutNote = ReelNote(
            id = 2L,
            reelUrl = "https://www.instagram.com/reel/C7x9Y12Zabc/",
            shortcode = "C7x9Y12Zabc",
            author = "@fitness_coach_sam",
            title = "Routine Express Haut du Corps au Poids du Corps",
            category = NoteCategory.WORKOUT,
            summary = "Circuit 15 minutes complet pour pectoraux, triceps et dos sans aucun matériel.",
            rawCaption = "Entraînement complet sans matériel pour le haut du corps. 4 tours, 45s de repos.",
            markdownContent = """
                # Routine Express Haut du Corps au Poids du Corps
                
                **Auteur :** @fitness_coach_sam  
                **Catégorie :** 💪 Sport & Fitness  
                **Lien Reel :** [Voir sur Instagram](https://www.instagram.com/reel/C7x9Y12Zabc/)
                
                ### Résumé
                Circuit 15 minutes complet pour pectoraux, triceps et dos sans aucun matériel.
                
                ## Exercices du Circuit
                1. Pompes classiques : 4 séries de 12 répétitions (pectoraux et triceps)
                2. Dips sur chaise ou canapé : 3 séries de 15 répétitions (triceps)
                3. Gainage militaire / commando : 3 séries de 45 secondes (abdos et gainage)
                4. Pompes pike / piquées : 3 séries de 10 répétitions (épaules)
                
                ## Points clés
                - 45 secondes de repos entre chaque tour
                - Effectuer 4 tours complets
                - Veiller à contracter les abdos et fessiers pendant les pompes
                
                ### Tags
                #fitness #workout #musculation #homeworkout #sansmateriel
            """.trimIndent(),
            structuredData = StructuredNoteData(
                summary = "Circuit 15 minutes complet pour pectoraux, triceps et dos sans aucun matériel.",
                steps = listOf(
                    StepItem(1, "Pompes classiques : 4 séries de 12 répétitions (pectoraux et triceps)"),
                    StepItem(2, "Dips sur chaise ou canapé : 3 séries de 15 répétitions (triceps)"),
                    StepItem(3, "Gainage militaire / commando : 3 séries de 45 secondes (abdos et gainage)"),
                    StepItem(4, "Pompes pike / piquées : 3 séries de 10 répétitions (épaules)")
                ),
                keyTakeaways = listOf(
                    "45 secondes de repos entre chaque tour",
                    "Effectuer 4 tours complets pour une intensité optimale",
                    "Maintenir un alignement parfait du corps"
                ),
                tips = listOf("Si les pompes sont trop dures, posez les genoux au sol.")
            ),
            thumbnailUrl = null,
            tags = listOf("fitness", "workout", "musculation", "homeworkout", "sansmateriel"),
            isFavorite = false,
            createdAt = now - 43200000L,
            updatedAt = now - 43200000L
        )

        val techNote = ReelNote(
            id = 3L,
            reelUrl = "https://www.instagram.com/reel/C9a1B23CdEf/",
            shortcode = "C9a1B23CdEf",
            author = "@tech_astuces",
            title = "3 Réglages Cachés pour Rendre son Smartphone 2x Plus Rapide",
            category = NoteCategory.TIPS_INFO,
            summary = "Trois astuces simples dans les paramètres Android pour supprimer les ralentissements.",
            rawCaption = "Astuces Android indispensables à activer d'urgence !",
            markdownContent = """
                # 3 Réglages Cachés pour Rendre son Smartphone 2x Plus Rapide
                
                **Auteur :** @tech_astuces  
                **Catégorie :** 💡 Astuce & Info  
                **Lien Reel :** [Voir sur Instagram](https://www.instagram.com/reel/C9a1B23CdEf/)
                
                ### Résumé
                Trois astuces simples dans les paramètres Android pour supprimer les ralentissements.
                
                ## Points clés
                - Réduire les échelles d'animation à 0.5x dans les Options pour développeurs (navigation instantanée)
                - Désactiver la recherche WiFi et Bluetooth en arrière-plan dans Localisation (gain de batterie et fluidité)
                - Nettoyer le cache des applications sociales (Instagram, TikTok) qui accumulent plusieurs gigaoctets
                
                ## Étapes
                1. Paramètres > À propos du téléphone > Taper 7 fois sur Numéro de build pour activer le mode développeur.
                2. Dans Système > Options développeur, modifier Échelle d'animation des fenêtres / transitions à 0.5x.
                3. Dans Paramètres > Localisation > Recherche Wi-Fi / Bluetooth, décocher les deux options.
                
                ### Tags
                #android #astucetech #smartphone #optimisation #productivite
            """.trimIndent(),
            structuredData = StructuredNoteData(
                summary = "Trois astuces simples dans les paramètres Android pour supprimer les ralentissements.",
                steps = listOf(
                    StepItem(1, "Paramètres > À propos du téléphone > Taper 7 fois sur Numéro de build."),
                    StepItem(2, "Dans Système > Options développeur, passer les échelles d'animation à 0.5x."),
                    StepItem(3, "Dans Localisation > Recherche, désactiver la recherche permanente Wi-Fi et Bluetooth.")
                ),
                keyTakeaways = listOf(
                    "Échelle d'animation à 0.5x rend l'interface 2 fois plus réactive",
                    "Désactivation du scan Bluetooth/Wi-Fi économise 10% de batterie quotidienne",
                    "Nettoyer régulièrement le cache des réseaux sociaux"
                )
            ),
            thumbnailUrl = null,
            tags = listOf("android", "astucetech", "smartphone", "optimisation", "productivite"),
            isFavorite = true,
            createdAt = now - 18000000L,
            updatedAt = now - 18000000L
        )

        return listOf(pastaRecipe, workoutNote, techNote)
    }
}
