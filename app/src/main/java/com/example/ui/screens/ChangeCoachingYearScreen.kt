package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PreviousYearArchive
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeCoachingYearScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val archives by viewModel.archives.collectAsState()
    val students by viewModel.students.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }
    var archiveToDelete by remember { mutableStateOf<PreviousYearArchive?>(null) }
    var deleteConfirmInput by remember { mutableStateOf("") }

    val defaultSession = remember {
        val y = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date()).toIntOrNull() ?: 2026
        "Session $y-${y + 1}"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change the Coaching Year", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // WARNING & INITIATE RESET CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE65100).copy(alpha = 0.08f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Annual Session Transition",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "This action prepares your coaching center for the new academic batch:\n" +
                                    "• Auto-generates and archives the final year-end PDF report in 'Previous Year PDFs'.\n" +
                                    "• Clears all student records from the active student list.\n" +
                                    "• All your configured classes, full-year fees, sections, target dates, and target fees remain 100% preserved and ready for new admissions.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                        ) {
                            Icon(imageVector = Icons.Default.History, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Change Coaching Year (Reset Students)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ARCHIVED PREVIOUS YEARS SECTION
            item {
                Text(
                    text = "Previous Year Archived PDF Reports (${archives.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (archives.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "No previous session archives found. When you change the coaching year, reports are automatically archived here.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            } else {
                items(archives, key = { it.id }) { arc ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = arc.sessionLabel,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Defaulters: ${arc.totalStudents} | Pending: ₹${String.format(Locale.getDefault(), "%,.0f", arc.totalPendingAmount)}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Archived in Firestore collection 'previous_year_pdfs'",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // DELETE SAFEGUARD ACTION
                            IconButton(
                                onClick = {
                                    archiveToDelete = arc
                                    deleteConfirmInput = ""
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG 1: RESET CONFIRMATION FLOW (CHECKBOXES + TYPE "Confirm")
    if (showResetDialog) {
        var check1 by remember { mutableStateOf(false) }
        var check2 by remember { mutableStateOf(false) }
        var confirmTextInput by remember { mutableStateOf("") }
        var sessionNameInput by remember { mutableStateOf(defaultSession) }

        val canProceed = check1 && check2 && confirmTextInput.trim().equals("Confirm", ignoreCase = false)

        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Confirm Coaching Year Reset", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "This will clear ${students.size} active student records and auto-save their final PDF report.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Session Label:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedTextField(
                        value = sessionNameInput,
                        onValueChange = { sessionNameInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Checkbox 1
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = check1, onCheckedChange = { check1 = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("I understand active students will be cleared for the new year.", fontSize = 12.sp)
                    }

                    // Checkbox 2
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = check2, onCheckedChange = { check2 = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Auto-generate & preserve final PDF report in archive.", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Type \"Confirm\" below to enable reset:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = confirmTextInput,
                        onValueChange = { confirmTextInput = it },
                        placeholder = { Text("Confirm") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (canProceed) {
                            viewModel.changeCoachingYear(context, sessionNameInput) {
                                showResetDialog = false
                                Toast.makeText(context, "New coaching year started!", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = canProceed,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                ) {
                    Text("Reset & Start New Year")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    // DIALOG 2: ARCHIVE DELETION SAFEGUARD (TYPE "Delete")
    if (archiveToDelete != null) {
        val canDelete = deleteConfirmInput.trim().equals("Delete", ignoreCase = false)

        AlertDialog(
            onDismissRequest = { archiveToDelete = null },
            title = { Text("Delete Archived Report") },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to permanently delete '${archiveToDelete?.sessionLabel}' archive?",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("To confirm deletion, please type \"Delete\" below:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = deleteConfirmInput,
                        onValueChange = { deleteConfirmInput = it },
                        placeholder = { Text("Delete") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (canDelete) {
                            archiveToDelete?.let { viewModel.deletePreviousYearArchive(it.id) }
                            archiveToDelete = null
                        }
                    },
                    enabled = canDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { archiveToDelete = null }) { Text("Cancel") }
            }
        )
    }
}
