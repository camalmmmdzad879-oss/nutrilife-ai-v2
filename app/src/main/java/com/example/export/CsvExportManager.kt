package com.example.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.local.DiaryEntryEntity
import com.example.data.local.ExerciseLogEntity
import com.example.data.local.WaterLogEntity
import com.example.data.local.WeightLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExportManager {

    suspend fun exportNutritionCsv(
        context: Context,
        diary: List<DiaryEntryEntity>,
        water: List<WaterLogEntity>,
        weight: List<WeightLogEntity>,
        exercise: List<ExerciseLogEntity>
    ): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val file = File(exportDir, "nutrilife_report_$stamp.csv")

        val sb = StringBuilder()
        sb.appendLine("RECORD_TYPE,DATE,CATEGORY_OR_TYPE,NAME_OR_NOTE,QUANTITY,UNIT,CALORIES_KCAL,PROTEIN_G,CARBS_G,FAT_G,FIBER_G,SOURCE")

        diary.forEach { d ->
            sb.appendLine(
                listOf(
                    "DIARY",
                    d.dateIso,
                    csvEscape(d.mealType),
                    csvEscape(d.foodName),
                    String.format(Locale.US, "%.1f", d.servingGrams),
                    "g",
                    String.format(Locale.US, "%.0f", d.calories),
                    String.format(Locale.US, "%.1f", d.proteinG),
                    String.format(Locale.US, "%.1f", d.carbsG),
                    String.format(Locale.US, "%.1f", d.fatG),
                    String.format(Locale.US, "%.1f", d.fiberG),
                    csvEscape(d.sourceType)
                ).joinToString(",")
            )
        }
        water.forEach { w ->
            sb.appendLine("WATER,${w.dateIso},HYDRATION,Water,${w.amountMl},ml,0,0,0,0,0,USER_ENTERED")
        }
        weight.forEach { wt ->
            sb.appendLine("WEIGHT,${wt.dateIso},BODY_WEIGHT,${csvEscape(wt.note)},${String.format(Locale.US, "%.1f", wt.weightKg)},kg,0,0,0,0,0,USER_ENTERED")
        }
        exercise.forEach { ex ->
            sb.appendLine("EXERCISE,${ex.dateIso},${csvEscape(ex.activityType)},${csvEscape(ex.note)},${ex.durationMinutes},min,${ex.estimatedCaloriesBurned},0,0,0,0,ESTIMATED")
        }

        file.writeText(sb.toString(), Charsets.UTF_8)
        file
    }

    fun shareCsvFile(context: Context, file: File, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    private fun csvEscape(raw: String): String {
        val sanitized = raw.replace("\"", "\"\"")
        return "\"$sanitized\""
    }
}
