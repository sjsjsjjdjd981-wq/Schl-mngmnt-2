package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CoachingClass
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val classes by viewModel.classes.collectAsState()

    var nameInput by remember { mutableStateOf("") }
    var fatherNameInput by remember { mutableStateOf("") }
    var contactNumberInput by remember { mutableStateOf("") }
    var selectedClass by remember(classes) { mutableStateOf(classes.firstOrNull()) }
    var selectedSection by remember(selectedClass) { mutableStateOf(selectedClass?.sections?.firstOrNull()) }
    var feeStatusInput by remember { mutableStateOf("") } // default 0 if blank

    var classDropdownExpanded by remember { mutableStateOf(false) }
    var sectionDropdownExpanded by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Student", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            if (classes.isEmpty()) {
                Text(
                    text = "No coaching classes configured yet. Please configure classes in 'Setup Your Coaching' first.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            } else {
                // 1. STUDENT NAME
                Text("Student Name *", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = { Text("e.g. Rahul Sharma") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. FATHER'S NAME
                Text("Father's Name *", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = fatherNameInput,
                    onValueChange = { fatherNameInput = it },
                    placeholder = { Text("e.g. Rajesh Sharma") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. CONTACT NUMBER
                Text("Contact Number *", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = contactNumberInput,
                    onValueChange = { contactNumberInput = it.filter { c -> c.isDigit() } },
                    placeholder = { Text("10-digit mobile number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 4. CLASS SELECTION (ONLY CONFIGURED CLASSES)
                Text("Select Class *", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
                                    selectedClass = cls
                                    selectedSection = cls.sections.firstOrNull()
                                    classDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // 5. SECTION SELECTION (CONDITIONAL)
                if (selectedClass != null && selectedClass!!.sections.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Select Section", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))

                    ExposedDropdownMenuBox(
                        expanded = sectionDropdownExpanded,
                        onExpandedChange = { sectionDropdownExpanded = !sectionDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedSection ?: "Select Section",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = sectionDropdownExpanded,
                            onDismissRequest = { sectionDropdownExpanded = false }
                        ) {
                            selectedClass!!.sections.forEach { sec ->
                                DropdownMenuItem(
                                    text = { Text(sec) },
                                    onClick = {
                                        selectedSection = sec
                                        sectionDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 6. FEE STATUS (INITIAL DEPOSIT)
                Text("Initial Deposited Amount (Fee Status)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Leave blank if no payment is made at registration (defaults to ₹0)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = feeStatusInput,
                    onValueChange = { feeStatusInput = it.filter { c -> c.isDigit() } },
                    placeholder = { Text("₹0 (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(errorMsg ?: "", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(28.dp))

                // CONFIRM & REGISTER BUTTON
                Button(
                    onClick = {
                        if (nameInput.isBlank()) {
                            errorMsg = "Please enter student name."
                            return@Button
                        }
                        if (fatherNameInput.isBlank()) {
                            errorMsg = "Please enter father's name."
                            return@Button
                        }
                        if (contactNumberInput.length < 10) {
                            errorMsg = "Please enter a valid 10-digit contact number."
                            return@Button
                        }
                        val chosenClass = selectedClass
                        if (chosenClass == null) {
                            errorMsg = "Please select a class."
                            return@Button
                        }
                        val deposit = feeStatusInput.toDoubleOrNull() ?: 0.0

                        viewModel.addStudent(
                            name = nameInput,
                            fatherName = fatherNameInput,
                            contactNumber = contactNumberInput,
                            selectedClass = chosenClass,
                            selectedSection = selectedSection,
                            initialDeposit = deposit
                        )
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text("Register Student", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
