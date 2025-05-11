package com.example.malvoayant.NavigationLogic.Algorithm
// SafePathFinder.kt

import android.util.Log
import com.example.malvoayant.data.models.FloorPlanState
import com.example.malvoayant.NavigationLogic.Algorithm.PathFinder
import com.example.malvoayant.data.models.Point
import kotlinx.coroutines.*

class SafePathFinder {
    private var pathFinderJob: Job? = null
    private var currentFloorPlan: FloorPlanState? = null

    // À appeler quand vous chargez la carte
    fun setFloorPlan(plan: FloorPlanState) {
        currentFloorPlan = plan
        Log.d("SafePathFinder", "Carte mise à jour")
    }

    // Trouve un chemin sans bloquer l'UI
    fun requestPath(
        start: Any,
        destination: Any,
        onSuccess: (List<Point>) -> Unit,
        onError: (String) -> Unit
    ) {
        // Annule la requête précédente si elle existe
        pathFinderJob?.cancel()

        pathFinderJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Vérifier que la carte est prête
                val plan = currentFloorPlan ?: throw Exception("La carte n'est pas chargée")

                // 2. Lancer le calcul (en arrière-plan)
                val path = PathFinder().findPath(start, destination, plan)

                // 3. Renvoyer le résultat sur le thread principal
                withContext(Dispatchers.Main) {
                    onSuccess(path)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Erreur : ${e.message}")
                }
            }
        }
    }

    // À appeler quand vous quittez l'écran
    fun cleanup() {
        pathFinderJob?.cancel()
        Log.d("SafePathFinder", "Nettoyage effectué")
    }
}