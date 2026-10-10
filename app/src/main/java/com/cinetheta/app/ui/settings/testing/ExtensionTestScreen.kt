package com.cinetheta.app.ui.settings.testing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cinetheta.domain.provider.Provider
import com.cinetheta.app.di.ProviderModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TestStatus {
    IDLE, RUNNING, PASSED, FAILED
}

data class ProviderTestState(
    val provider: Provider,
    val status: TestStatus = TestStatus.IDLE,
    val logs: List<TestingUtils.Logger.Message> = emptyList(),
    val error: Throwable? = null
)

class ExtensionTestViewModel : ViewModel() {
    private val _testStates = MutableStateFlow<List<ProviderTestState>>(emptyList())
    val testStates: StateFlow<List<ProviderTestState>> = _testStates.asStateFlow()

    private val _isRunningAll = MutableStateFlow(false)
    val isRunningAll: StateFlow<Boolean> = _isRunningAll.asStateFlow()

    init {
        val providers = ProviderModule.repository.enabledProviders()
        _testStates.value = providers.map { ProviderTestState(it) }
    }

    fun runAllTests() {
        if (_isRunningAll.value) return
        _isRunningAll.value = true

        val currentStates = _testStates.value.map { it.copy(status = TestStatus.RUNNING, logs = emptyList(), error = null) }
        _testStates.value = currentStates

        viewModelScope.launch(Dispatchers.IO) {
            val providersToTest = currentStates.map { it.provider }
            TestingUtils.getDeferredProviderTests(this, providersToTest) { provider, result ->
                val newStatus = if (result.success) TestStatus.PASSED else TestStatus.FAILED
                
                _testStates.value = _testStates.value.map { state ->
                    if (state.provider.id == provider.id) {
                        state.copy(
                            status = newStatus,
                            logs = result.log,
                            error = result.exception
                        )
                    } else {
                        state
                    }
                }
                
                val allDone = _testStates.value.all { it.status == TestStatus.PASSED || it.status == TestStatus.FAILED }
                if (allDone) {
                    _isRunningAll.value = false
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionTestScreen(
    onBackClick: () -> Unit,
    viewModel: ExtensionTestViewModel = viewModel()
) {
    val testStates by viewModel.testStates.collectAsState()
    val isRunningAll by viewModel.isRunningAll.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Extensions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.runAllTests() }) {
                if (isRunningAll) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                } else {
                    Text("Run Tests", modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "This tool tests the search and detail loading capabilities of enabled extensions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(testStates) { state ->
                ProviderTestCard(state = state)
            }
        }
    }
}

@Composable
fun ProviderTestCard(state: ProviderTestState) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.provider.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                when (state.status) {
                    TestStatus.IDLE -> Icon(Icons.Default.HourglassEmpty, contentDescription = "Idle", tint = Color.Gray)
                    TestStatus.RUNNING -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    TestStatus.PASSED -> Icon(Icons.Default.CheckCircle, contentDescription = "Passed", tint = Color(0xFF00C853))
                    TestStatus.FAILED -> Icon(Icons.Default.Error, contentDescription = "Failed", tint = Color.Red)
                }
            }

            if (expanded && state.logs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                
                Column {
                    state.logs.forEach { log ->
                        val color = when (log.level) {
                            TestingUtils.Logger.LogLevel.Normal -> MaterialTheme.colorScheme.onSurface
                            TestingUtils.Logger.LogLevel.Warning -> Color(0xFFFFA000)
                            TestingUtils.Logger.LogLevel.Error -> Color.Red
                        }
                        Text(
                            text = log.toString(),
                            color = color,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    if (state.error != null) {
                        Text(
                            text = "Exception: ${state.error.message}",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
