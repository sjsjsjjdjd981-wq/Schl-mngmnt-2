package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.CoachingClass
import com.example.model.PreviousYearArchive
import com.example.model.Student
import com.example.model.TargetDate
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

import com.google.firebase.FirebaseApp

class CoachingRepository(private val db: FirebaseFirestore) {

    companion object {
        fun create(context: Context): CoachingRepository {
            try {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    FirebaseApp.initializeApp(context)
                }
            } catch (e: Exception) {
                Log.e("CoachingRepository", "FirebaseApp init check failed", e)
            }

            val firestore = try {
                val dbIdRes = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
                val dbId = if (dbIdRes != 0) context.getString(dbIdRes) else ""
                if (dbId.isNotBlank()) {
                    FirebaseFirestore.getInstance(dbId)
                } else {
                    FirebaseFirestore.getInstance()
                }
            } catch (e: Exception) {
                Log.w("CoachingRepository", "Custom DB init failed, fallback to default", e)
                FirebaseFirestore.getInstance()
            }
            return CoachingRepository(firestore)
        }
    }

    constructor(context: Context) : this(create(context).db)

    private val auth = Firebase.auth

    fun currentUserId(): String? = auth.currentUser?.uid

    fun currentUserEmail(): String? = auth.currentUser?.email

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // TARGET DATES
    fun observeTargetDates(setupId: String): Flow<List<TargetDate>> {
        val ref = db.collection("coaching_setup")
            .document(setupId)
            .collection("target_dates")
            .orderBy("orderIndex")

        return ref.snapshots().map { snapshot ->
            snapshot.toObjects(TargetDate::class.java)
        }
    }

    suspend fun addTargetDate(setupId: String, date: String, label: String, orderIndex: Long) {
        val ref = db.collection("coaching_setup")
            .document(setupId)
            .collection("target_dates")

        val payload = hashMapOf(
            "date" to date,
            "label" to label,
            "orderIndex" to orderIndex,
            "createdAt" to FieldValue.serverTimestamp()
        )
        ref.add(payload).await()
    }

    suspend fun deleteTargetDate(setupId: String, targetDateId: String) {
        db.collection("coaching_setup")
            .document(setupId)
            .collection("target_dates")
            .document(targetDateId)
            .delete()
            .await()
    }

    // CLASSES
    fun observeClasses(setupId: String): Flow<List<CoachingClass>> {
        val ref = db.collection("coaching_setup")
            .document(setupId)
            .collection("classes")
            .orderBy("classNumber")

        return ref.snapshots().map { snapshot ->
            snapshot.toObjects(CoachingClass::class.java)
        }
    }

    suspend fun addClass(
        setupId: String,
        classNumber: Int,
        className: String,
        fullYearFee: Double,
        sections: List<String>,
        targetFees: Map<String, Double>
    ) {
        val ref = db.collection("coaching_setup")
            .document(setupId)
            .collection("classes")

        val payload = hashMapOf(
            "classNumber" to classNumber,
            "className" to className,
            "fullYearFee" to fullYearFee,
            "sections" to sections,
            "targetFees" to targetFees,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        ref.add(payload).await()
    }

    suspend fun updateClass(
        setupId: String,
        classId: String,
        fullYearFee: Double,
        targetFees: Map<String, Double>
    ) {
        val ref = db.collection("coaching_setup")
            .document(setupId)
            .collection("classes")
            .document(classId)

        ref.update(
            mapOf(
                "fullYearFee" to fullYearFee,
                "targetFees" to targetFees,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun deleteClass(setupId: String, classId: String) {
        db.collection("coaching_setup")
            .document(setupId)
            .collection("classes")
            .document(classId)
            .delete()
            .await()
    }

    // STUDENTS
    fun observeStudents(setupId: String): Flow<List<Student>> {
        val ref = db.collection("students")
            .whereEqualTo("setupId", setupId)

        return ref.snapshots().map { snapshot ->
            snapshot.toObjects(Student::class.java)
        }
    }

    suspend fun addStudent(setupId: String, student: Student) {
        val uid = requireUserId()
        val ref = db.collection("students")

        val payload = hashMapOf(
            "setupId" to setupId,
            "userId" to uid,
            "name" to student.name,
            "fatherName" to student.fatherName,
            "mainContactNumber" to student.mainContactNumber,
            "class" to student.`class`,
            "classId" to student.classId,
            "section" to student.section,
            "feeStatus" to student.feeStatus,
            "dueDateLevel" to student.dueDateLevel,
            "currentDueDate" to student.currentDueDate,
            "firstDueDate" to student.firstDueDate,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        ref.add(payload).await()
    }

    suspend fun updateStudentFeeStatus(studentId: String, newlyDepositedAmount: Double) {
        val docRef = db.collection("students").document(studentId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val currentFee = (snapshot.get("feeStatus") as? Number)?.toDouble() ?: 0.0
            val newFee = currentFee + newlyDepositedAmount
            transaction.update(
                docRef,
                mapOf(
                    "feeStatus" to newFee,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
    }

    suspend fun updateStudentDueDate(
        studentId: String,
        newLevel: Int,
        newDueDate: String,
        isFirstTime: Boolean = false
    ) {
        val docRef = db.collection("students").document(studentId)
        val updates = mutableMapOf<String, Any>(
            "dueDateLevel" to newLevel,
            "currentDueDate" to newDueDate,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isFirstTime) {
            updates["firstDueDate"] = newDueDate
        }
        docRef.update(updates).await()
    }

    // PURGE ALL STUDENTS FOR NEW COACHING YEAR
    suspend fun purgeAllStudents(setupId: String) {
        val query = db.collection("students")
            .whereEqualTo("setupId", setupId)
            .get()
            .await()

        val batch = db.batch()
        for (doc in query.documents) {
            batch.delete(doc.reference)
        }
        batch.commit().await()
    }

    // PREVIOUS YEAR PDF ARCHIVES
    suspend fun savePreviousYearArchive(archive: PreviousYearArchive) {
        val uid = requireUserId()
        val ref = db.collection("previous_year_pdfs")
        val payload = hashMapOf(
            "userId" to uid,
            "sessionLabel" to archive.sessionLabel,
            "totalStudents" to archive.totalStudents,
            "totalPendingAmount" to archive.totalPendingAmount,
            "reportDataJson" to archive.reportDataJson,
            "createdAt" to FieldValue.serverTimestamp()
        )
        ref.add(payload).await()
    }

    fun observePreviousYearArchives(userId: String): Flow<List<PreviousYearArchive>> {
        val ref = db.collection("previous_year_pdfs")
            .whereEqualTo("userId", userId)

        return ref.snapshots().map { snapshot ->
            snapshot.toObjects(PreviousYearArchive::class.java)
        }
    }

    suspend fun deletePreviousYearArchive(archiveId: String) {
        db.collection("previous_year_pdfs")
            .document(archiveId)
            .delete()
            .await()
    }
}
