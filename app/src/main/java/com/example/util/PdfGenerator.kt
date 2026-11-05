package com.example.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.model.CoachingClass
import com.example.model.Student
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    data class DefaulterStudent(
        val student: Student,
        val classFee: Double,
        val pendingDue: Double
    )

    fun generateYearEndPdf(
        context: Context,
        students: List<Student>,
        classes: List<CoachingClass>,
        sessionLabel: String
    ): File {
        // Map classId to fullYearFee
        val classFeeMap = classes.associate { it.className to it.fullYearFee }
        val classIdFeeMap = classes.associate { it.id to it.fullYearFee }

        // Filter students where feeStatus < class full-year fee
        val defaulters = students.mapNotNull { student ->
            val fee = classFeeMap[student.`class`] ?: classIdFeeMap[student.classId] ?: 0.0
            if (fee > 0 && student.feeStatus < fee) {
                DefaulterStudent(student, fee, fee - student.feeStatus)
            } else null
        }.sortedWith(compareBy({ it.student.`class` }, { it.student.section ?: "" }, { it.student.name }))

        val document = PdfDocument()
        val pageWidth = 595 // A4 standard width in points
        val pageHeight = 842 // A4 standard height in points

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(21, 101, 192) // Primary Blue
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subTitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val redPaint = Paint().apply {
            color = Color.rgb(211, 47, 47) // Red
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var y = 40f

        // Header
        canvas.drawText("Coaching Center Management", 40f, y, titlePaint)
        y += 18f
        canvas.drawText("Year-End Pending Fee Report — $sessionLabel", 40f, y, subTitlePaint)
        y += 14f
        val dateFormat = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
        canvas.drawText("Generated on: ${dateFormat.format(Date())}", 40f, y, subTitlePaint)
        y += 20f

        canvas.drawLine(40f, y, pageWidth - 40f, y, linePaint)
        y += 20f

        var currentGroup = ""
        var sNo = 1

        for (item in defaulters) {
            val groupKey = "${item.student.`class`}${if (!item.student.section.isNullOrBlank()) " - ${item.student.section}" else ""}"
            
            // Check if we need to print a group header
            if (groupKey != currentGroup) {
                currentGroup = groupKey
                if (y > pageHeight - 100) {
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    y = 40f
                }
                y += 10f
                val groupPaint = Paint().apply {
                    color = Color.rgb(13, 71, 161)
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("Class: $groupKey (Full-Year Fee: ₹${String.format(Locale.getDefault(), "%,.0f", item.classFee)})", 40f, y, groupPaint)
                y += 16f

                // Draw Table Header
                canvas.drawRect(40f, y - 12f, pageWidth - 40f, y + 6f, Paint().apply { color = Color.rgb(238, 242, 246) })
                canvas.drawText("S.No", 45f, y, headerPaint)
                canvas.drawText("Student Name", 80f, y, headerPaint)
                canvas.drawText("Father's Name", 200f, y, headerPaint)
                canvas.drawText("Contact No", 310f, y, headerPaint)
                canvas.drawText("Deposited", 400f, y, headerPaint)
                canvas.drawText("Pending Due", 480f, y, headerPaint)
                y += 16f
            }

            // Check page overflow
            if (y > pageHeight - 50) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
            }

            canvas.drawText("$sNo", 45f, y, textPaint)
            canvas.drawText(item.student.name.take(18), 80f, y, textPaint)
            canvas.drawText(item.student.fatherName.take(16), 200f, y, textPaint)
            canvas.drawText(item.student.mainContactNumber, 310f, y, textPaint)
            canvas.drawText("₹${String.format(Locale.getDefault(), "%,.0f", item.student.feeStatus)}", 400f, y, textPaint)
            canvas.drawText("−₹${String.format(Locale.getDefault(), "%,.0f", item.pendingDue)}", 480f, y, redPaint)

            y += 14f
            canvas.drawLine(40f, y - 4f, pageWidth - 40f, y - 4f, Paint().apply { color = Color.rgb(240, 240, 240) })
            sNo++
        }

        if (defaulters.isEmpty()) {
            canvas.drawText("No pending fee records found! All students have completed their class fees.", 40f, y + 20f, subTitlePaint)
        }

        document.finishPage(page)

        // Save PDF to app internal files or downloads
        val fileDir = File(context.filesDir, "reports")
        if (!fileDir.exists()) fileDir.mkdirs()
        val file = File(fileDir, "coaching_report_${System.currentTimeMillis()}.pdf")
        val outputStream = FileOutputStream(file)
        document.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        document.close()

        return file
    }

    fun getFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
