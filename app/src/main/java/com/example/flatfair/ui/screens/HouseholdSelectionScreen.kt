package com.example.flatfair.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flatfair.ui.viewmodel.HouseholdUiState
import com.example.flatfair.ui.viewmodel.HouseholdViewModel

@Composable
fun HouseholdSelectionScreen(
    onSelectionSuccess: () -> Unit,
    viewModel: HouseholdViewModel = hiltViewModel()
) {
    var householdName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(true) }
    
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is HouseholdUiState.Success) {
            onSelectionSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isCreating) "Create a Household" else "Join a Household",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isCreating) {
            OutlinedTextField(
                value = householdName,
                onValueChange = { householdName = it },
                label = { Text("Household Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        } else {
            OutlinedTextField(
                value = inviteCode,
                onValueChange = { inviteCode = it },
                label = { Text("Invite Code") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        if (uiState is HouseholdUiState.Error) {
            Text(
                text = (uiState as HouseholdUiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (isCreating) {
                    viewModel.createHousehold(householdName)
                } else {
                    viewModel.joinHousehold(inviteCode)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState !is HouseholdUiState.Loading && 
                     (if (isCreating) householdName.isNotBlank() else inviteCode.isNotBlank())
        ) {
            if (uiState is HouseholdUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(if (isCreating) "Create" else "Join")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { 
            isCreating = !isCreating 
            viewModel.clearError()
        }) {
            Text(if (isCreating) "Have an invite code? Join instead" else "Want to start fresh? Create instead")
        }
    }
}
