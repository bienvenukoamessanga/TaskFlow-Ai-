package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.example.model.SubTask
import com.example.model.TaskItem
import com.example.model.TaskPriority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class TaskBreakdownResult(
    val subtasks: List<SubTask>,
    val tips: String,
    val suggestedPriority: TaskPriority? = null,
    val suggestedDurationMinutes: Int? = null
)

data class MagicTaskResult(
    val title: String,
    val description: String,
    val category: String,
    val priority: TaskPriority,
    val estimatedMinutes: Int,
    val subtasks: List<SubTask>,
    val tips: String
)

data class DailyPlanResult(
    val summary: String,
    val focusOrder: List<String>,
    val motivationalTip: String,
    val recommendedPomodoros: Int
)

class GeminiApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }
    }

    private suspend fun callGemini(prompt: String, jsonMode: Boolean = true): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Veuillez configurer votre clé GEMINI_API_KEY dans le panneau Secrets pour activer l'intelligence artificielle.")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val requestJson = JSONObject()
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()
        val partObj = JSONObject()
        partObj.put("text", prompt)
        partsArray.put(partObj)
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        requestJson.put("contents", contentsArray)

        if (jsonMode) {
            val configObj = JSONObject()
            configObj.put("responseMimeType", "application/json")
            requestJson.put("generationConfig", configObj)
        }

        val requestBody = requestJson.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseBodyString = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val errorMsg = try {
                val errObj = JSONObject(responseBodyString)
                errObj.optJSONObject("error")?.optString("message") ?: "Erreur HTTP ${response.code}"
            } catch (_: Exception) {
                "Erreur HTTP ${response.code}: $responseBodyString"
            }
            throw RuntimeException(errorMsg)
        }

        val json = JSONObject(responseBodyString)
        val candidates = json.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            throw RuntimeException("Aucune réponse générée par Gemini.")
        }

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        if (parts == null || parts.length() == 0) {
            throw RuntimeException("Contenu vide reçu de Gemini.")
        }

        parts.getJSONObject(0).optString("text", "")
    }

    suspend fun breakdownTask(title: String, description: String): Result<TaskBreakdownResult> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Tu es l'assistant d'organisation TaskFlow Ai.
                Découpe cette tâche en sous-tâches concrètes, actionnables et rapides.
                Tâche: "$title"
                Détails: "$description"

                Réponds au format JSON strict:
                {
                  "subtasks": ["Sous-tâche 1", "Sous-tâche 2", "Sous-tâche 3", "Sous-tâche 4"],
                  "tips": "Conseil méthodologique percutant pour réussir cette tâche sans procrastiner",
                  "priority": "LOW" ou "MEDIUM" ou "HIGH" ou "URGENT",
                  "durationMinutes": 45
                }
            """.trimIndent()

            val rawJson = callGemini(prompt, jsonMode = true)
            val parsed = JSONObject(rawJson)
            val subtasksJson = parsed.optJSONArray("subtasks") ?: JSONArray()
            val subtaskList = mutableListOf<SubTask>()
            for (i in 0 until subtasksJson.length()) {
                val subTitle = subtasksJson.getString(i).trim()
                if (subTitle.isNotBlank()) {
                    subtaskList.add(SubTask(title = subTitle, isDone = false))
                }
            }

            val tips = parsed.optString("tips", "Divisez vos efforts en blocs de concentration.")
            val priorityStr = parsed.optString("priority", "MEDIUM")
            val priority = try { TaskPriority.valueOf(priorityStr) } catch (_: Exception) { TaskPriority.MEDIUM }
            val duration = parsed.optInt("durationMinutes", 30)

            Result.success(
                TaskBreakdownResult(
                    subtasks = subtaskList,
                    tips = tips,
                    suggestedPriority = priority,
                    suggestedDurationMinutes = duration
                )
            )
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Breakdown failed", e)
            Result.failure(e)
        }
    }

    suspend fun magicCreateTask(userPrompt: String): Result<MagicTaskResult> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Tu es TaskFlow Ai. L'utilisateur veut planifier une action en langage naturel:
                "$userPrompt"

                Extrais les informations et déduis un plan structuré en JSON strict:
                {
                  "title": "Titre clair et concis (max 50 caractères)",
                  "description": "Brève description utile",
                  "category": "Travail" ou "Personnel" ou "Études" ou "Santé" ou "Projets" ou "Créatif" ou "Général",
                  "priority": "LOW" ou "MEDIUM" ou "HIGH" ou "URGENT",
                  "estimatedMinutes": 30,
                  "subtasks": ["Étape 1", "Étape 2", "Étape 3"],
                  "tips": "Conseil d'efficacité pour cette tâche"
                }
            """.trimIndent()

            val rawJson = callGemini(prompt, jsonMode = true)
            val parsed = JSONObject(rawJson)
            val title = parsed.optString("title", userPrompt.take(40))
            val description = parsed.optString("description", "")
            val category = parsed.optString("category", "Général")
            val priorityStr = parsed.optString("priority", "MEDIUM")
            val priority = try { TaskPriority.valueOf(priorityStr) } catch (_: Exception) { TaskPriority.MEDIUM }
            val estimatedMinutes = parsed.optInt("estimatedMinutes", 30)
            val tips = parsed.optString("tips", "Fixez un premier pas simple pour démarrer.")

            val subtasksJson = parsed.optJSONArray("subtasks") ?: JSONArray()
            val subtaskList = mutableListOf<SubTask>()
            for (i in 0 until subtasksJson.length()) {
                val subTitle = subtasksJson.getString(i).trim()
                if (subTitle.isNotBlank()) {
                    subtaskList.add(SubTask(title = subTitle, isDone = false))
                }
            }

            Result.success(
                MagicTaskResult(
                    title = title,
                    description = description,
                    category = category,
                    priority = priority,
                    estimatedMinutes = estimatedMinutes,
                    subtasks = subtaskList,
                    tips = tips
                )
            )
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "MagicCreate failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateDailyFlowPlan(pendingTasks: List<TaskItem>): Result<DailyPlanResult> = withContext(Dispatchers.IO) {
        try {
            if (pendingTasks.isEmpty()) {
                return@withContext Result.success(
                    DailyPlanResult(
                        summary = "Aucune tâche en attente ! Vous êtes à jour dans votre flux.",
                        focusOrder = emptyList(),
                        motivationalTip = "Profitez de ce temps libre ou planifiez de nouveaux objectifs inspirants.",
                        recommendedPomodoros = 0
                    )
                )
            }

            val tasksDesc = pendingTasks.take(15).joinToString("\n") {
                "- [${it.priority.displayName}] ${it.title} (Catégorie: ${it.category}, ~${it.estimatedMinutes}min)"
            }

            val prompt = """
                Tu es le Copilote IA de TaskFlow Ai.
                Analyse la liste des tâches suivantes à faire par l'utilisateur:
                $tasksDesc

                Génère un plan d'action optimal pour aujourd'hui en JSON strict:
                {
                  "summary": "Résumé stratégique de la journée (max 3 phrases)",
                  "focusOrder": ["Nom de la 1ère tâche à attaquer", "Nom de la 2ème tâche", "Nom de la 3ème tâche"],
                  "motivationalTip": "Un conseil percutant pour maximiser le focus et l'énergie",
                  "recommendedPomodoros": 4
                }
            """.trimIndent()

            val rawJson = callGemini(prompt, jsonMode = true)
            val parsed = JSONObject(rawJson)
            val summary = parsed.optString("summary", "Concentrez-vous sur vos priorités élevées en premier.")
            val motivationalTip = parsed.optString("motivationalTip", "Éliminez les distractions pendant les 45 premières minutes.")
            val pomodoros = parsed.optInt("recommendedPomodoros", 4)

            val orderJson = parsed.optJSONArray("focusOrder") ?: JSONArray()
            val orderList = mutableListOf<String>()
            for (i in 0 until orderJson.length()) {
                val item = orderJson.getString(i).trim()
                if (item.isNotBlank()) orderList.add(item)
            }

            Result.success(
                DailyPlanResult(
                    summary = summary,
                    focusOrder = orderList,
                    motivationalTip = motivationalTip,
                    recommendedPomodoros = pomodoros
                )
            )
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Daily plan failed", e)
            Result.failure(e)
        }
    }
}
