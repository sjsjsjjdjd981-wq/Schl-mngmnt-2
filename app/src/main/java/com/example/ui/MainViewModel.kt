package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CoachingRepository
import com.example.model.CoachingClass
import com.example.model.PreviousYearArchive
import com.example.model.Student
import com.example.model.TargetDate
import com.example.util.PdfGenerator
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CoachingRepository.create(application)
    private val auth by lazy { Firebase.auth }

    // Authorized Teacher Account (sjsjsjjdjd981@gmail.com)
    val defaultAuthorizedEmail = "sjsjsjjdjd981@gmail.com"

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isAuthorized = MutableStateFlow(true)
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    // Using teacher's UID as setupId ensures data isolation
    val setupId: String
        get() = _currentUser.value?.uid ?: "default_setup"

    // Target Dates, Classes, Students
    private val _targetDates = MutableStateFlow<List<TargetDate>>(emptyList())
    val targetDates: StateFlow<List<TargetDate>> = _targetDates.asStateFlow()

    private val _classes = MutableStateFlow<List<CoachingClass>>(emptyList())
    val classes: StateFlow<List<CoachingClass>> = _classes.asStateFlow()

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    private val _archives = MutableStateFlow<List<PreviousYearArchive>>(emptyList())
    val archives: StateFlow<List<PreviousYearArchive>> = _archives.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // Fee Management UI state
    val selectedFeeClass = MutableStateFlow<CoachingClass?>(null)
    val selectedFeeSection = MutableStateFlow<String?>(null)
    val selectedDueLevel = MutableStateFlow(0) // Default to Level 0

    init {
        try {
            _currentUser.value = auth.currentUser
            auth.addAuthStateListener { firebaseAuth ->
                try {
                    val user = firebaseAuth.currentUser
                    _currentUser.value = user
                    if (user != null) {
                        _isAuthorized.value = true
                        loadUserData(user.uid)
                    } else {
                        _targetDates.value = emptyList()
                        _classes.value = emptyList()
                        _students.value = emptyList()
                        _archives.value = emptyList()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainViewModel", "Error in auth listener callback", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Failed to initialize auth listener", e)
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun loadUserData(uid: String) {
        viewModelScope.launch {
            repository.observeTargetDates(uid)
                .catch { e -> _message.value = e.localizedMessage }
                .collect { _targetDates.value = it }
        }
        viewModelScope.launch {
            repository.observeClasses(uid)
                .catch { e -> _message.value = e.localizedMessage }
                .collect { classList ->
                    _classes.value = classList
                    if (selectedFeeClass.value == null && classList.isNotEmpty()) {
                        selectedFeeClass.value = classList.first()
                        selectedFeeSection.value = classList.first().sections.firstOrNull()
                    }
                }
        }
        viewModelScope.launch {
            repository.observeStudents(uid)
                .catch { e -> _message.value = e.localizedMessage }
                .collect { _students.value = it }
        }
        viewModelScope.launch {
            repository.observePreviousYearArchives(uid)
                .catch { e -> _message.value = e.localizedMessage }
                .collect { _archives.value = it }
        }
    }

    // Refresh Action (Fee Management Refresh Button)
    fun refreshData() {
        _isLoading.value = true
        val uid = _currentUser.value?.uid
        if (uid != null) {
            loadUserData(uid)
        }
        _isLoading.value = false
        _message.value = "Data refreshed successfully"
    }

    // TARGET DATES
    fun addTargetDate(dateStr: String, label: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val nextOrder = (_targetDates.value.maxOfOrNull { it.orderIndex } ?: 0L) + 1L
                repository.addTargetDate(setupId, dateStr, label, nextOrder)
                _message.value = "Target date added: $label"
            } catch (e: Exception) {
                _message.value = "Failed to add date: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteTargetDate(targetDateId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.deleteTargetDate(setupId, targetDateId)
                _message.value = "Target date removed"
            } catch (e: Exception) {
                _message.value = "Error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // CLASSES
    fun addClass(
        classNumber: Int,
        className: String,
        fullYearFee: Double,
        sections: List<String>,
        targetFees: Map<String, Double>
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.addClass(setupId, classNumber, className, fullYearFee, sections, targetFees)
                _message.value = "Added $className successfully"
            } catch (e: Exception) {
                _message.value = "Failed to add class: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateClass(
        classId: String,
        fullYearFee: Double,
        targetFees: Map<String, Double>
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.updateClass(setupId, classId, fullYearFee, targetFees)
                _message.value = "Class fees updated successfully"
            } catch (e: Exception) {
                _message.value = "Update failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteClass(classId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.deleteClass(setupId, classId)
                _message.value = "Class deleted"
            } catch (e: Exception) {
                _message.value = "Error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // STUDENTS
    fun addStudent(
        name: String,
        fatherName: String,
        contactNumber: String,
        selectedClass: CoachingClass,
        selectedSection: String?,
        initialDeposit: Double
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val student = Student(
                    name = name.trim(),
                    fatherName = fatherName.trim(),
                    mainContactNumber = contactNumber.trim(),
                    `class` = selectedClass.className,
                    classId = selectedClass.id,
                    section = selectedSection?.ifBlank { null },
                    feeStatus = initialDeposit,
                    dueDateLevel = 0, // Level 0: first-time student with no First Due Date
                    currentDueDate = null,
                    firstDueDate = null
                )
                repository.addStudent(setupId, student)
                _message.value = "Student registered successfully!"
            } catch (e: Exception) {
                _message.value = "Failed to register student: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // UPDATE FEE STATUS (Additive: New = Old + Deposit)
    fun updateStudentFeeStatus(studentId: String, depositAmount: Double, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.updateStudentFeeStatus(studentId, depositAmount)
                _message.value = "Fee deposit of ₹${String.format(Locale.getDefault(), "%,.0f", depositAmount)} updated!"
                onComplete()
            } catch (e: Exception) {
                _message.value = "Failed to update fee: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // UPDATE DUE DATE & LEVEL
    fun updateStudentDueDate(studentId: String, newLevel: Int, newDueDate: String, isFirstTime: Boolean) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.updateStudentDueDate(studentId, newLevel, newDueDate, isFirstTime)
                _message.value = "Due Date updated to $newDueDate (Level $newLevel)"
            } catch (e: Exception) {
                _message.value = "Error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // CHANGE COACHING YEAR (Auto-Archive + Purge Students)
    fun changeCoachingYear(context: Context, sessionLabel: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val currentStudents = _students.value
                val currentClasses = _classes.value

                // 1. Auto-generate final year PDF report snapshot
                val reportFile = PdfGenerator.generateYearEndPdf(context, currentStudents, currentClasses, sessionLabel)

                // 2. Prepare JSON summary for archive
                val classFeeMap = currentClasses.associate { it.className to it.fullYearFee }
                val defaulters = currentStudents.filter { s ->
                    val fee = classFeeMap[s.`class`] ?: 0.0
                    fee > 0 && s.feeStatus < fee
                }
                val totalPending = defaulters.sumOf { s -> (classFeeMap[s.`class`] ?: 0.0) - s.feeStatus }

                val archive = PreviousYearArchive(
                    userId = setupId,
                    sessionLabel = sessionLabel,
                    totalStudents = defaulters.size,
                    totalPendingAmount = totalPending,
                    reportDataJson = "${reportFile.name}|${defaulters.size}"
                )

                // 3. Save to previous_year_pdfs
                repository.savePreviousYearArchive(archive)

                // 4. Purge students collection
                repository.purgeAllStudents(setupId)

                _message.value = "Session $sessionLabel concluded! Students reset for new year."
                onComplete()
            } catch (e: Exception) {
                _message.value = "Failed to change coaching year: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deletePreviousYearArchive(archiveId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.deletePreviousYearArchive(archiveId)
                _message.value = "Archived record deleted"
            } catch (e: Exception) {
                _message.value = "Error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // SIGN OUT
    fun signOut() {
        auth.signOut()
    }
}
