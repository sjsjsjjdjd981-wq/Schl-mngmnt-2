package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Student
import com.example.ui.MainViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchStudentScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onStudentClick: (student: Student) -> Unit
) {
    val allStudents by viewModel.students.collectAsState()
    val classes by viewModel.classes.collectAsState()

    var nameFilter by remember { mutableStateOf("") }
    var fatherFilter by remember { mutableStateOf("") }
    var selectedClassFilter by remember { mutableStateOf<String?>(null) }
    var selectedSectionFilter by remember { mutableStateOf<String?>(null) }
    var feeStatusFilter by remember { mutableStateOf("") }

    var isSearchExecuted by remember { mutableStateOf(false) }
    var classDropdownExpanded by remember { mutableStateOf(false) }
    var sectionDropdownExpanded by remember { mutableStateOf(false) }

    // Dynamic available sections based on selected class filter
    val availableSections = remember(selectedClassFilter, classes) {
        classes.firstOrNull { it.className == selectedClassFilter }?.sections ?: emptyList()
    }

    // Filter computation
    val filteredStudents = remember(allStudents, isSearchExecuted, nameFilter, fatherFilter, selectedClassFilter, selectedSectionFilter, feeStatusFilter) {
        if (!isSearchExecuted) {
            allStudents
        } else {
            allStudents.filter { s ->
                val matchesName = nameFilter.isBlank() || s.name.contains(nameFilter.trim(), ignoreCase = true)
                val matchesFather = fatherFilter.isBlank() || s.fatherName.contains(fatherFilter.trim(), ignoreCase = true)
                val matchesClass = selectedClassFilter == null || s.`class` == selectedClassFilter
                val matchesSection = selectedSectionFilter == null || s.section == selectedSectionFilter
                val matchesFee = feeStatusFilter.isBlank() || s.feeStatus >= (feeStatusFilter.toDoubleOrNull() ?: 0.0)

                matchesName && matchesFather && matchesClass && matchesSection && matchesFee
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Student", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
            // FILTER INPUT CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Filter Student Directory",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Search by any one field or a combination (Contact numbers excluded)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 1. Name Filter
                        OutlinedTextField(
                            value = nameFilter,
                            onValueChange = { nameFilter = it },
                            label = { Text("Student Name") },
                            placeholder = { Text("e.g. Rahul") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2. Father's Name Filter
                        OutlinedTextField(
                            value = fatherFilter,
                            onValueChange = { fatherFilter = it },
                            label = { Text("Father's Name") },
                            placeholder = { Text("e.g. Rajesh") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 3. Class Filter
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExposedDropdownMenuBox(
                                expanded = classDropdownExpanded,
                                onExpandedChange = { classDropdownExpanded = !classDropdownExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = selectedClassFilter ?: "All Classes",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Class") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = classDropdownExpanded,
                                    onDismissRequest = { classDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Classes") },
                                        onClick = {
                                            selectedClassFilter = null
                                            selectedSectionFilter = null
                                            classDropdownExpanded = false
                                        }
                                    )
                                    classes.forEach { cls ->
                                        DropdownMenuItem(
                                            text = { Text(cls.className) },
                                            onClick = {
                                                selectedClassFilter = cls.className
                                                selectedSectionFilter = null
                                                classDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // 4. Section Filter (if available)
                            if (availableSections.isNotEmpty()) {
                                ExposedDropdownMenuBox(
                                    expanded = sectionDropdownExpanded,
                                    onExpandedChange = { sectionDropdownExpanded = !sectionDropdownExpanded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = selectedSectionFilter ?: "All Sec",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Section") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionDropdownExpanded) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = sectionDropdownExpanded,
                                        onDismissRequest = { sectionDropdownExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("All Sections") },
                                            onClick = {
                                                selectedSectionFilter = null
                                                sectionDropdownExpanded = false
                                            }
                                        )
                                        availableSections.forEach { sec ->
                                            DropdownMenuItem(
                                                text = { Text(sec) },
                                                onClick = {
                                                    selectedSectionFilter = sec
                                                    sectionDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons: Search & Clear
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { isSearchExecuted = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Search")
                            }

                            OutlinedButton(
                                onClick = {
                                    nameFilter = ""
                                    fatherFilter = ""
                                    selectedClassFilter = null
                                    selectedSectionFilter = null
                                    feeStatusFilter = ""
                                    isSearchExecuted = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset")
                            }
                        }
                    }
                }
            }

            // RESULTS HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Students Found (${filteredStudents.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap student to open Profile",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // RESULTS LIST
            if (filteredStudents.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "No matching student records found.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(filteredStudents, key = { it.id }) { s ->
                    SearchResultCard(
                        student = s,
                        onClick = { onStudentClick(s) }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultCard(
    student: Student,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Father: ${student.fatherName}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${student.`class`}${if (!student.section.isNullOrBlank()) " (${student.section})" else ""}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Deposited: ₹${String.format(Locale.getDefault(), "%,.0f", student.feeStatus)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View Profile",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
