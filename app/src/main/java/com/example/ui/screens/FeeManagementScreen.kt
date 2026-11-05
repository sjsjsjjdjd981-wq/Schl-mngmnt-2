package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CoachingClass
import com.example.model.Student
import com.example.model.TargetDate
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeManagementScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onStudentClick: (student: Student) -> Unit
) {
    val context = LocalContext.current
    val classes by viewModel.classes.collectAsState()
    val targetDates by viewModel.targetDates.collectAsState()
    val students by viewModel.students.collectAsState()

    val selectedClass by viewModel.selectedFeeClass.collectAsState()
    val selectedSection by viewModel.selectedFeeSection.collectAsState()
    val selectedLevel by viewModel.selectedDueLevel.collectAsState()

    var classDropdownExpanded by remember { mutableStateOf(false) }
    var studentForDueUpdate by remember { mutableStateOf<Student?>(null) }

    // REAL-CALENDAR TARGET DATE EVALUATION
    // Active Target Date is the latest common target date reached on real calendar
    val activeTargetDate = remember(targetDates) {
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        targetDates.filter { it.date <= todayIso }
            .maxByOrNull { it.date }
            ?: targetDates.minByOrNull { it.date }
    }

    // Active Target Fee for the selected class
    val activeTargetFee = remember(selectedClass, activeTargetDate) {
        if (selectedClass != null && activeTargetDate != null) {
            selectedClass!!.targetFees[activeTargetDate.id] ?: 0.0
        } else 0.0
    }

    // Out of Target Date Students Filtering:
    // 1. Matches selected class
    // 2. Matches selected section (if chosen)
    // 3. Fee Status < Active Target Fee
    // 4. Matches selected Due Date Level (0..5)
    val outOfTargetStudents = remember(students, selectedClass, selectedSection, activeTargetFee, selectedLevel) {
        if (selectedClass == null) emptyList()
        else {
            students.filter { s ->
                val matchesClass = s.`class` == selectedClass!!.className || s.classId == selectedClass!!.id
                val matchesSection = selectedSection == null || s.section == selectedSection
                val isDeficit = s.feeStatus < activeTargetFee
                val matchesLevel = s.dueDateLevel == selectedLevel

                matchesClass && matchesSection && isDeficit && matchesLevel
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fee Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    // REFRESH BUTTON (TOP HEADER)
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Data",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. SELECT CLASS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Select Class:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))

                        ExposedDropdownMenuBox(
                            expanded = classDropdownExpanded,
                            onExpandedChange = { classDropdownExpanded = !classDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedClass?.className ?: "Select a Class",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = classDropdownExpanded,
                                onDismissRequest = { classDropdownExpanded = false }
                            ) {
                                classes.forEach { cls ->
                                    DropdownMenuItem(
                                        text = { Text(cls.className) },
                                        onClick = {
                                            viewModel.selectedFeeClass.value = cls
                                            viewModel.selectedFeeSection.value = cls.sections.firstOrNull()
                                            classDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // 2. SECTIONS AS CLICKABLE BUTTONS (IF MULTIPLE SECTIONS CONFIGURED)
                        if (selectedClass != null && selectedClass!!.sections.size >= 2) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Select Section:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                selectedClass!!.sections.forEach { sec ->
                                    val isSelected = selectedSection == sec
                                    Button(
                                        onClick = { viewModel.selectedFeeSection.value = sec },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text(sec, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(10.dp))

                        // 3. CURRENT TARGET DATE & TARGET FEE (TOP-RIGHT ALIGNED INFO)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "Current Target Date: ${activeTargetDate?.label ?: "None Configured"}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Target Fee: ₹${String.format(Locale.getDefault(), "%,.0f", activeTargetFee)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. OUT OF TARGET DATE STUDENT LIST & LEVEL FILTER BAR (0..5)
            item {
                Column {
                    Text(
                        text = "Out of Target Date Student List",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // HORIZONTAL LEVEL FILTER BAR: "0  1  2  3  4  5"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (0..5).forEach { level ->
                            val isSelected = selectedLevel == level
                            val isRedLevel = level == 5
                            Button(
                                onClick = { viewModel.selectedDueLevel.value = level },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        isSelected && isRedLevel -> Color(0xFFD32F2F)
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isRedLevel -> Color(0xFFFFEBEE)
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    contentColor = when {
                                        isSelected -> Color.White
                                        isRedLevel -> Color(0xFFD32F2F)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                            ) {
                                Text(
                                    text = if (level == 0) "Level 0 (1st-Time)" else "Level $level",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // 5. STUDENT CARDS IN OUT OF TARGET DATE LIST
            if (outOfTargetStudents.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = if (activeTargetFee == 0.0) "No target fee configured for this date."
                            else "No Level $selectedLevel students under target deficit in this class/section.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(outOfTargetStudents, key = { it.id }) { s ->
                    val deficit = activeTargetFee - s.feeStatus

                    FeeDefaulterCard(
                        student = s,
                        deficitAmount = deficit,
                        onClick = { onStudentClick(s) },
                        onUpdateDueDate = { studentForDueUpdate = s }
                    )
                }
            }
        }
    }

    // DIALOG: ASSIGN / UPDATE DUE DATE & LEVEL
    if (studentForDueUpdate != null) {
        val s = studentForDueUpdate!!
        val isFirstTime = s.dueDateLevel == 0
        var pickedDate by remember { mutableStateOf(s.currentDueDate ?: "") }
        var chosenLevel by remember { mutableStateOf(if (isFirstTime) 1 else (s.dueDateLevel + 1).coerceAtMost(5)) }

        AlertDialog(
            onDismissRequest = { studentForDueUpdate = null },
            title = { Text(if (isFirstTime) "Assign First Due Date" else "Update Due Date") },
            text = {
                Column {
                    Text("Student: ${s.name} (${s.`class`})", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Selected Due Date:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val sel = Calendar.getInstance()
                                    sel.set(y, m, d)
                                    pickedDate = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(sel.time)
                                },
                                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (pickedDate.isBlank()) "Pick Calendar Due Date" else pickedDate)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Select Due Date Level:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..5).forEach { lvl ->
                            Button(
                                onClick = { chosenLevel = lvl },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (chosenLevel == lvl) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (chosenLevel == lvl) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("Lvl $lvl")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pickedDate.isNotBlank()) {
                            viewModel.updateStudentDueDate(s.id, chosenLevel, pickedDate, isFirstTime)
                            studentForDueUpdate = null
                        }
                    }
                ) {
                    Text("Save Due Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentForDueUpdate = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun FeeDefaulterCard(
    student: Student,
    deficitAmount: Double,
    onClick: () -> Unit,
    onUpdateDueDate: () -> Unit
) {
    val hasAssignedOrUpdatedDueDate = student.dueDateLevel > 0 || !student.firstDueDate.isNullOrBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Name & Section with Due Date Updated Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = student.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Father: ${student.fatherName}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // DUE DATE UPDATED ICON (Shown if assigned or updated, omitted for Level 0 first-time)
                    if (hasAssignedOrUpdatedDueDate) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Update,
                                    contentDescription = "Due Date Assigned/Updated",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (student.dueDateLevel == 5) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFEBEE)
                        ) {
                            Text(
                                text = "RED LIST",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFD32F2F),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Class and Section
            Row {
                Text(
                    text = "Class: ${student.`class`}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!student.section.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Section: ${student.section}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Deposited Fee Status
            Text(
                text = "Fee Status: ₹${String.format(Locale.getDefault(), "%,.0f", student.feeStatus)}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // REQUIRED FEE IN BOLD RED NEGATIVE FORMAT
            Text(
                text = "The fee required to complete this target fee of this target date:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "−₹${String.format(Locale.getDefault(), "%,.0f", deficitAmount)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD32F2F) // RED TEXT
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action: Assign / Update Due Date Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onUpdateDueDate,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (hasAssignedOrUpdatedDueDate) "Update Due Date" else "Assign First Due Date", fontSize = 12.sp)
                }
            }
        }
    }
}
