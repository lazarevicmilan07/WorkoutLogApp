package com.workoutlog.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.itextpdf.text.BaseColor
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.Font
import com.itextpdf.text.FontFactory
import com.itextpdf.text.PageSize
import com.itextpdf.text.Paragraph
import com.itextpdf.text.Phrase
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import com.workoutlog.domain.model.MonthlyReport
import com.workoutlog.domain.model.YearlyReport
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

object ExportUtil {

    fun exportMonthlyToExcel(context: Context, report: MonthlyReport): Uri? {
        val yearMonth = YearMonth.of(report.year, report.month)
        val fileName = "WorkoutLog_${yearMonth.year}_${String.format("%02d", yearMonth.monthValue)}.xlsx"

        return createExcelFile(context, fileName) { workbook, outputStream ->
            val sheet = workbook.createSheet("Monthly Report")

            val headerStyle = workbook.createCellStyle().apply {
                fillForegroundColor = IndexedColors.ROYAL_BLUE.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                val font = workbook.createFont().apply {
                    color = IndexedColors.WHITE.index
                    bold = true
                }
                setFont(font)
            }

            // Title
            var rowIndex = 0
            val titleRow = sheet.createRow(rowIndex++)
            titleRow.createCell(0).setCellValue(
                "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${yearMonth.year} - Workout Report"
            )

            rowIndex++ // blank row

            // Summary
            val summaryHeader = sheet.createRow(rowIndex++)
            summaryHeader.createCell(0).apply { setCellValue("Metric"); cellStyle = headerStyle }
            summaryHeader.createCell(1).apply { setCellValue("Value"); cellStyle = headerStyle }

            listOf(
                "Total Workouts" to "${report.totalWorkouts}",
                "Rest Days" to "${report.totalRestDays}",
                "Total Duration (min)" to "${report.totalDuration}",
                "Total Calories" to "${report.totalCalories}"
            ).forEach { (label, value) ->
                val row = sheet.createRow(rowIndex++)
                row.createCell(0).setCellValue(label)
                row.createCell(1).setCellValue(value)
            }

            rowIndex++ // blank row

            // Workout type distribution
            val distHeader = sheet.createRow(rowIndex++)
            distHeader.createCell(0).apply { setCellValue("Workout Type"); cellStyle = headerStyle }
            distHeader.createCell(1).apply { setCellValue("Count"); cellStyle = headerStyle }
            distHeader.createCell(2).apply { setCellValue("Percentage"); cellStyle = headerStyle }

            val total = report.workoutTypeCounts.sumOf { it.count }.toFloat()
            report.workoutTypeCounts.forEach { tc ->
                val row = sheet.createRow(rowIndex++)
                row.createCell(0).setCellValue(tc.workoutType.name)
                row.createCell(1).setCellValue(tc.count.toDouble())
                row.createCell(2).setCellValue("${((tc.count / total) * 100).toInt()}%")
            }

            rowIndex++

            // Daily breakdown
            val dailyHeader = sheet.createRow(rowIndex++)
            dailyHeader.createCell(0).apply { setCellValue("Day"); cellStyle = headerStyle }
            dailyHeader.createCell(1).apply { setCellValue("Workouts"); cellStyle = headerStyle }

            report.dailyCounts.forEach { dc ->
                val row = sheet.createRow(rowIndex++)
                row.createCell(0).setCellValue("Day ${dc.day}")
                row.createCell(1).setCellValue(dc.count.toDouble())
            }

            // Auto-size columns
            for (i in 0..2) sheet.setColumnWidth(i, 5000)

            workbook.write(outputStream)
        }
    }

    fun exportYearlyToExcel(context: Context, report: YearlyReport): Uri? {
        val fileName = "WorkoutLog_${report.year}_Yearly.xlsx"

        return createExcelFile(context, fileName) { workbook, outputStream ->
            val sheet = workbook.createSheet("Yearly Report")

            val headerStyle = workbook.createCellStyle().apply {
                fillForegroundColor = IndexedColors.ROYAL_BLUE.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                val font = workbook.createFont().apply {
                    color = IndexedColors.WHITE.index
                    bold = true
                }
                setFont(font)
            }

            var rowIndex = 0
            val titleRow = sheet.createRow(rowIndex++)
            titleRow.createCell(0).setCellValue("${report.year} - Yearly Workout Report")

            rowIndex++

            // Summary
            val summaryHeader = sheet.createRow(rowIndex++)
            summaryHeader.createCell(0).apply { setCellValue("Metric"); cellStyle = headerStyle }
            summaryHeader.createCell(1).apply { setCellValue("Value"); cellStyle = headerStyle }

            listOf(
                "Total Workouts" to "${report.totalWorkouts}",
                "Rest Days" to "${report.totalRestDays}"
            ).forEach { (label, value) ->
                val row = sheet.createRow(rowIndex++)
                row.createCell(0).setCellValue(label)
                row.createCell(1).setCellValue(value)
            }

            rowIndex++

            // Monthly counts
            val monthHeader = sheet.createRow(rowIndex++)
            monthHeader.createCell(0).apply { setCellValue("Month"); cellStyle = headerStyle }
            monthHeader.createCell(1).apply { setCellValue("Workouts"); cellStyle = headerStyle }

            report.monthlyCounts.forEach { mc ->
                val row = sheet.createRow(rowIndex++)
                row.createCell(0).setCellValue(
                    Month.of(mc.month).getDisplayName(TextStyle.FULL, Locale.getDefault())
                )
                row.createCell(1).setCellValue(mc.count.toDouble())
            }

            rowIndex++

            // Type distribution
            val distHeader = sheet.createRow(rowIndex++)
            distHeader.createCell(0).apply { setCellValue("Workout Type"); cellStyle = headerStyle }
            distHeader.createCell(1).apply { setCellValue("Count"); cellStyle = headerStyle }

            report.workoutTypeCounts.forEach { tc ->
                val row = sheet.createRow(rowIndex++)
                row.createCell(0).setCellValue(tc.workoutType.name)
                row.createCell(1).setCellValue(tc.count.toDouble())
            }

            for (i in 0..2) sheet.setColumnWidth(i, 5000)
            workbook.write(outputStream)
        }
    }

    fun exportMonthlyToPdf(context: Context, report: MonthlyReport): Uri? {
        val yearMonth = YearMonth.of(report.year, report.month)
        val fileName = "WorkoutLog_${yearMonth.year}_${String.format("%02d", yearMonth.monthValue)}.pdf"

        return createPdfFile(context, fileName) { document ->
            val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f, BaseColor.DARK_GRAY)
            val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, BaseColor.WHITE)
            val bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 11f, BaseColor.DARK_GRAY)

            // Title
            document.add(Paragraph(
                "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${yearMonth.year} - Workout Report",
                titleFont
            ).apply { spacingAfter = 20f })

            // Summary table
            val summaryTable = PdfPTable(2).apply { widthPercentage = 100f }
            addHeaderCell(summaryTable, "Metric", headerFont)
            addHeaderCell(summaryTable, "Value", headerFont)
            summaryTable.addCell(PdfPCell(Phrase("Total Workouts", bodyFont)))
            summaryTable.addCell(PdfPCell(Phrase("${report.totalWorkouts}", bodyFont)))
            summaryTable.addCell(PdfPCell(Phrase("Rest Days", bodyFont)))
            summaryTable.addCell(PdfPCell(Phrase("${report.totalRestDays}", bodyFont)))
            summaryTable.addCell(PdfPCell(Phrase("Total Duration (min)", bodyFont)))
            summaryTable.addCell(PdfPCell(Phrase("${report.totalDuration}", bodyFont)))
            summaryTable.addCell(PdfPCell(Phrase("Total Calories", bodyFont)))
            summaryTable.addCell(PdfPCell(Phrase("${report.totalCalories}", bodyFont)))
            document.add(summaryTable)
            document.add(Paragraph(" "))

            // Distribution table
            if (report.workoutTypeCounts.isNotEmpty()) {
                document.add(Paragraph("Workout Distribution", titleFont.let {
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f, BaseColor.DARK_GRAY)
                }).apply { spacingAfter = 10f })

                val distTable = PdfPTable(3).apply { widthPercentage = 100f }
                addHeaderCell(distTable, "Type", headerFont)
                addHeaderCell(distTable, "Count", headerFont)
                addHeaderCell(distTable, "%", headerFont)

                val total = report.workoutTypeCounts.sumOf { it.count }.toFloat()
                report.workoutTypeCounts.forEach { tc ->
                    distTable.addCell(PdfPCell(Phrase(tc.workoutType.name, bodyFont)))
                    distTable.addCell(PdfPCell(Phrase("${tc.count}", bodyFont)))
                    distTable.addCell(PdfPCell(Phrase("${((tc.count / total) * 100).toInt()}%", bodyFont)))
                }
                document.add(distTable)
            }
        }
    }

    fun exportYearlyToPdf(context: Context, report: YearlyReport): Uri? {
        val fileName = "WorkoutLog_${report.year}_Yearly.pdf"

        return createPdfFile(context, fileName) { document ->
            val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f, BaseColor.DARK_GRAY)
            val subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f, BaseColor.DARK_GRAY)
            val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, BaseColor.WHITE)
            val bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 11f, BaseColor.DARK_GRAY)

            document.add(Paragraph("${report.year} - Yearly Workout Report", titleFont).apply { spacingAfter = 20f })

            // Summary
            val summaryTable = PdfPTable(2).apply { widthPercentage = 100f }
            addHeaderCell(summaryTable, "Metric", headerFont)
            addHeaderCell(summaryTable, "Value", headerFont)
            summaryTable.addCell(PdfPCell(Phrase("Total Workouts", bodyFont)))
            summaryTable.addCell(PdfPCell(Phrase("${report.totalWorkouts}", bodyFont)))
            summaryTable.addCell(PdfPCell(Phrase("Rest Days", bodyFont)))
            summaryTable.addCell(PdfPCell(Phrase("${report.totalRestDays}", bodyFont)))
            document.add(summaryTable)
            document.add(Paragraph(" "))

            // Monthly
            document.add(Paragraph("Monthly Breakdown", subTitleFont).apply { spacingAfter = 10f })
            val monthTable = PdfPTable(2).apply { widthPercentage = 100f }
            addHeaderCell(monthTable, "Month", headerFont)
            addHeaderCell(monthTable, "Workouts", headerFont)

            report.monthlyCounts.forEach { mc ->
                monthTable.addCell(PdfPCell(Phrase(
                    Month.of(mc.month).getDisplayName(TextStyle.FULL, Locale.getDefault()), bodyFont
                )))
                monthTable.addCell(PdfPCell(Phrase("${mc.count}", bodyFont)))
            }
            document.add(monthTable)
            document.add(Paragraph(" "))

            // Distribution
            if (report.workoutTypeCounts.isNotEmpty()) {
                document.add(Paragraph("Workout Distribution", subTitleFont).apply { spacingAfter = 10f })
                val distTable = PdfPTable(2).apply { widthPercentage = 100f }
                addHeaderCell(distTable, "Type", headerFont)
                addHeaderCell(distTable, "Count", headerFont)

                report.workoutTypeCounts.forEach { tc ->
                    distTable.addCell(PdfPCell(Phrase(tc.workoutType.name, bodyFont)))
                    distTable.addCell(PdfPCell(Phrase("${tc.count}", bodyFont)))
                }
                document.add(distTable)
            }
        }
    }

    private fun addHeaderCell(table: PdfPTable, text: String, font: Font) {
        val cell = PdfPCell(Phrase(text, font)).apply {
            backgroundColor = BaseColor(59, 130, 246) // Primary blue
            horizontalAlignment = Element.ALIGN_CENTER
            setPadding(8f)
        }
        table.addCell(cell)
    }

    private fun createExcelFile(
        context: Context,
        fileName: String,
        block: (XSSFWorkbook, OutputStream) -> Unit
    ): Uri? {
        val workbook = XSSFWorkbook()
        return saveToDownloads(context, fileName, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") { os ->
            block(workbook, os)
            workbook.close()
        }
    }

    private fun createPdfFile(
        context: Context,
        fileName: String,
        block: (Document) -> Unit
    ): Uri? {
        return saveToDownloads(context, fileName, "application/pdf") { os ->
            val document = Document(PageSize.A4)
            PdfWriter.getInstance(document, os)
            document.open()
            block(document)
            document.close()
        }
    }

    private fun saveToDownloads(
        context: Context,
        fileName: String,
        mimeType: String,
        block: (OutputStream) -> Unit
    ): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/WorkoutLog")
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { os -> block(os) }
        }
        return uri
    }
}
