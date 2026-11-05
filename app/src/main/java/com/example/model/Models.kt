package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class TargetDate(
    @DocumentId val id: String = "",
    val date: String = "",
    val label: String = "",
    val orderIndex: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CoachingClass(
    @DocumentId val id: String = "",
    val classNumber: Int = 0,
    val className: String = "",
    val fullYearFee: Double = 0.0,
    val sections: List<String> = emptyList(),
    val targetFees: Map<String, Double> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Student(
    @DocumentId val id: String = "",
    val setupId: String = "",
    val userId: String = "",
    val name: String = "",
    val fatherName: String = "",
    val mainContactNumber: String = "",
    val `class`: String = "",
    val classId: String = "",
    val section: String? = null,
    val feeStatus: Double = 0.0,
    val dueDateLevel: Int = 0, // 0 = first-time student with no First Due Date, 1..5 = due date levels
    val currentDueDate: String? = null,
    val firstDueDate: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PreviousYearArchive(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sessionLabel: String = "",
    val totalStudents: Int = 0,
    val totalPendingAmount: Double = 0.0,
    val reportDataJson: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
