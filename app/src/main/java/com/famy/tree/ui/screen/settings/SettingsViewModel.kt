package com.famy.tree.ui.screen.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.famy.tree.domain.repository.FamilyMemberRepository
import com.famy.tree.domain.repository.FamilyTreeRepository
import com.famy.tree.domain.repository.LifeEventRepository
import com.famy.tree.domain.repository.MediaRepository
import com.famy.tree.domain.repository.RelationshipRepository
import com.famy.tree.domain.usecase.ExportDataUseCase
import com.famy.tree.domain.usecase.ImportDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dateFormat: DateFormatOption = DateFormatOption.MDY,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isClearing: Boolean = false,
    val showClearConfirmation: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class DateFormatOption(val pattern: String, val example: String) {
    MDY("MM/dd/yyyy", "12/31/2024"),
    DMY("dd/MM/yyyy", "31/12/2024"),
    YMD("yyyy-MM-dd", "2024-12-31"),
    FULL("MMMM d, yyyy", "December 31, 2024")
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val treeRepository: FamilyTreeRepository,
    private val memberRepository: FamilyMemberRepository,
    private val relationshipRepository: RelationshipRepository,
    private val lifeEventRepository: LifeEventRepository,
    private val mediaRepository: MediaRepository,
    private val exportDataUseCase: ExportDataUseCase,
    private val importDataUseCase: ImportDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _uiState.value = _uiState.value.copy(themeMode = mode)
    }

    fun setDateFormat(format: DateFormatOption) {
        _uiState.value = _uiState.value.copy(dateFormat = format)
    }

    fun showClearConfirmation() {
        _uiState.value = _uiState.value.copy(showClearConfirmation = true)
    }

    fun hideClearConfirmation() {
        _uiState.value = _uiState.value.copy(showClearConfirmation = false)
    }

    fun clearAllData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isClearing = true,
                showClearConfirmation = false
            )
            try {
                treeRepository.clearAllData()
                _uiState.value = _uiState.value.copy(
                    isClearing = false,
                    message = "All data cleared successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isClearing = false,
                    error = "Failed to clear data: ${e.message}"
                )
            }
        }
    }

    fun exportToJson(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            try {
                exportDataUseCase.exportToJson(context, uri)
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    message = "Data exported successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = "Export failed: ${e.message}"
                )
            }
        }
    }

    fun exportToGedcom(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            try {
                exportDataUseCase.exportToGedcom(context, uri)
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    message = "GEDCOM exported successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = "Export failed: ${e.message}"
                )
            }
        }
    }

    fun importFromJson(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true)
            try {
                importDataUseCase.importFromJson(context, uri)
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    message = "Data imported successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    error = "Import failed: ${e.message}"
                )
            }
        }
    }

    fun importFromGedcom(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true)
            try {
                importDataUseCase.importFromGedcom(context, uri)
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    message = "GEDCOM imported successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    error = "Import failed: ${e.message}"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
