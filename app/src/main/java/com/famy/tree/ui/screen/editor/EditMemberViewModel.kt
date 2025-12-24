package com.famy.tree.ui.screen.editor

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.famy.tree.domain.model.FamilyMember
import com.famy.tree.domain.model.Gender
import com.famy.tree.domain.repository.FamilyMemberRepository
import com.famy.tree.domain.repository.FamilyTreeRepository
import com.famy.tree.domain.usecase.CreateMemberUseCase
import com.famy.tree.domain.usecase.UpdateMemberUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

data class EditMemberFormState(
    val firstName: String = "",
    val lastName: String = "",
    val maidenName: String = "",
    val nickname: String = "",
    val gender: Gender = Gender.UNKNOWN,
    val birthDate: Long? = null,
    val birthPlace: String = "",
    val deathDate: Long? = null,
    val deathPlace: String = "",
    val isLiving: Boolean = true,
    val biography: String = "",
    val occupation: String = "",
    val education: String = "",
    val religion: String = "",
    val nationality: String = "",
    val notes: String = "",
    val photoPath: String? = null,
    val customFields: MutableMap<String, String> = mutableMapOf()
)

data class EditMemberUiState(
    val isEditing: Boolean = false,
    val form: EditMemberFormState = EditMemberFormState(),
    val originalMember: FamilyMember? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val hasChanges: Boolean = false,
    val error: String? = null,
    val validationErrors: Map<String, String> = emptyMap()
)

@HiltViewModel
class EditMemberViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memberRepository: FamilyMemberRepository,
    private val treeRepository: FamilyTreeRepository,
    private val createMemberUseCase: CreateMemberUseCase,
    private val updateMemberUseCase: UpdateMemberUseCase
) : ViewModel() {

    private val treeId: Long = savedStateHandle.get<Long>("treeId") ?: 0L
    private val memberId: Long? = savedStateHandle.get<Long>("memberId")?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(EditMemberUiState(isEditing = memberId != null))
    val uiState: StateFlow<EditMemberUiState> = _uiState.asStateFlow()

    init {
        loadMember()
    }

    private fun loadMember() {
        viewModelScope.launch {
            if (memberId != null) {
                val member = memberRepository.getMember(memberId)
                if (member != null) {
                    _uiState.update {
                        it.copy(
                            form = EditMemberFormState(
                                firstName = member.firstName,
                                lastName = member.lastName ?: "",
                                maidenName = member.maidenName ?: "",
                                nickname = member.nickname ?: "",
                                gender = member.gender,
                                birthDate = member.birthDate,
                                birthPlace = member.birthPlace ?: "",
                                deathDate = member.deathDate,
                                deathPlace = member.deathPlace ?: "",
                                isLiving = member.isLiving,
                                biography = member.biography ?: "",
                                occupation = member.occupation ?: "",
                                education = member.education ?: "",
                                religion = member.religion ?: "",
                                nationality = member.nationality ?: "",
                                notes = member.notes ?: "",
                                photoPath = member.photoPath,
                                customFields = member.customFields.toMutableMap()
                            ),
                            originalMember = member,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(error = "Member not found", isLoading = false) }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateFirstName(value: String) {
        _uiState.update {
            it.copy(
                form = it.form.copy(firstName = value),
                hasChanges = true,
                validationErrors = it.validationErrors - "firstName"
            )
        }
    }

    fun updateLastName(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(lastName = value), hasChanges = true)
        }
    }

    fun updateMaidenName(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(maidenName = value), hasChanges = true)
        }
    }

    fun updateNickname(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(nickname = value), hasChanges = true)
        }
    }

    fun updateGender(value: Gender) {
        _uiState.update {
            it.copy(form = it.form.copy(gender = value), hasChanges = true)
        }
    }

    fun updateBirthDate(value: Long?) {
        _uiState.update {
            val newForm = it.form.copy(birthDate = value)
            val errors = it.validationErrors.toMutableMap()

            if (value != null && value > System.currentTimeMillis()) {
                errors["birthDate"] = "Birth date cannot be in the future"
            } else {
                errors.remove("birthDate")
            }

            if (value != null && newForm.deathDate != null && value > newForm.deathDate) {
                errors["birthDate"] = "Birth date cannot be after death date"
            }

            it.copy(form = newForm, hasChanges = true, validationErrors = errors)
        }
    }

    fun updateBirthPlace(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(birthPlace = value), hasChanges = true)
        }
    }

    fun updateDeathDate(value: Long?) {
        _uiState.update {
            val newForm = it.form.copy(deathDate = value, isLiving = value == null)
            val errors = it.validationErrors.toMutableMap()

            if (value != null && newForm.birthDate != null && value < newForm.birthDate) {
                errors["deathDate"] = "Death date cannot be before birth date"
            } else {
                errors.remove("deathDate")
            }

            it.copy(form = newForm, hasChanges = true, validationErrors = errors)
        }
    }

    fun updateDeathPlace(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(deathPlace = value), hasChanges = true)
        }
    }

    fun updateIsLiving(value: Boolean) {
        _uiState.update {
            val newForm = if (value) {
                it.form.copy(isLiving = true, deathDate = null, deathPlace = "")
            } else {
                it.form.copy(isLiving = false)
            }
            it.copy(form = newForm, hasChanges = true)
        }
    }

    fun updateBiography(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(biography = value), hasChanges = true)
        }
    }

    fun updateOccupation(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(occupation = value), hasChanges = true)
        }
    }

    fun updateEducation(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(education = value), hasChanges = true)
        }
    }

    fun updateReligion(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(religion = value), hasChanges = true)
        }
    }

    fun updateNationality(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(nationality = value), hasChanges = true)
        }
    }

    fun updateNotes(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(notes = value), hasChanges = true)
        }
    }

    fun updatePhoto(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val photoDir = File(context.filesDir, "photos")
                if (!photoDir.exists()) photoDir.mkdirs()

                val tempId = memberId ?: System.currentTimeMillis()
                val photoFile = File(photoDir, "${tempId}_${UUID.randomUUID()}.jpg")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(photoFile).use { output ->
                        input.copyTo(output)
                    }
                }

                _uiState.update {
                    it.copy(
                        form = it.form.copy(photoPath = photoFile.absolutePath),
                        hasChanges = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to save photo: ${e.message}") }
            }
        }
    }

    fun addCustomField(key: String, value: String) {
        _uiState.update {
            val fields = it.form.customFields.toMutableMap()
            fields[key] = value
            it.copy(
                form = it.form.copy(customFields = fields),
                hasChanges = true
            )
        }
    }

    fun removeCustomField(key: String) {
        _uiState.update {
            val fields = it.form.customFields.toMutableMap()
            fields.remove(key)
            it.copy(
                form = it.form.copy(customFields = fields),
                hasChanges = true
            )
        }
    }

    private fun validate(): Boolean {
        val errors = mutableMapOf<String, String>()
        val form = _uiState.value.form

        if (form.firstName.isBlank()) {
            errors["firstName"] = "First name is required"
        }

        if (form.birthDate != null && form.birthDate > System.currentTimeMillis()) {
            errors["birthDate"] = "Birth date cannot be in the future"
        }

        if (form.birthDate != null && form.deathDate != null && form.deathDate < form.birthDate) {
            errors["deathDate"] = "Death date cannot be before birth date"
        }

        _uiState.update { it.copy(validationErrors = errors) }
        return errors.isEmpty()
    }

    fun save(onSaved: (Long) -> Unit) {
        if (!validate()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            try {
                val form = _uiState.value.form

                if (memberId != null) {
                    val originalMember = _uiState.value.originalMember!!
                    val updatedMember = originalMember.copy(
                        firstName = form.firstName.trim(),
                        lastName = form.lastName.trim().takeIf { it.isNotEmpty() },
                        maidenName = form.maidenName.trim().takeIf { it.isNotEmpty() },
                        nickname = form.nickname.trim().takeIf { it.isNotEmpty() },
                        gender = form.gender,
                        birthDate = form.birthDate,
                        birthPlace = form.birthPlace.trim().takeIf { it.isNotEmpty() },
                        deathDate = form.deathDate,
                        deathPlace = form.deathPlace.trim().takeIf { it.isNotEmpty() },
                        isLiving = form.isLiving,
                        biography = form.biography.trim().takeIf { it.isNotEmpty() },
                        occupation = form.occupation.trim().takeIf { it.isNotEmpty() },
                        education = form.education.trim().takeIf { it.isNotEmpty() },
                        religion = form.religion.trim().takeIf { it.isNotEmpty() },
                        nationality = form.nationality.trim().takeIf { it.isNotEmpty() },
                        notes = form.notes.trim().takeIf { it.isNotEmpty() },
                        photoPath = form.photoPath,
                        customFields = form.customFields.filterValues { it.isNotBlank() },
                        updatedAt = System.currentTimeMillis()
                    )
                    updateMemberUseCase(updatedMember)
                    onSaved(memberId)
                } else {
                    val memberCount = memberRepository.observeMemberCount(treeId).first()
                    val setAsRoot = memberCount == 0

                    val newMember = createMemberUseCase(
                        treeId = treeId,
                        firstName = form.firstName.trim(),
                        lastName = form.lastName.trim().takeIf { it.isNotEmpty() },
                        gender = form.gender,
                        birthDate = form.birthDate,
                        birthPlace = form.birthPlace.trim().takeIf { it.isNotEmpty() },
                        isLiving = form.isLiving,
                        setAsRoot = setAsRoot
                    )

                    val fullMember = newMember.copy(
                        maidenName = form.maidenName.trim().takeIf { it.isNotEmpty() },
                        nickname = form.nickname.trim().takeIf { it.isNotEmpty() },
                        deathDate = form.deathDate,
                        deathPlace = form.deathPlace.trim().takeIf { it.isNotEmpty() },
                        biography = form.biography.trim().takeIf { it.isNotEmpty() },
                        occupation = form.occupation.trim().takeIf { it.isNotEmpty() },
                        education = form.education.trim().takeIf { it.isNotEmpty() },
                        religion = form.religion.trim().takeIf { it.isNotEmpty() },
                        nationality = form.nationality.trim().takeIf { it.isNotEmpty() },
                        notes = form.notes.trim().takeIf { it.isNotEmpty() },
                        photoPath = form.photoPath,
                        customFields = form.customFields.filterValues { it.isNotBlank() }
                    )

                    updateMemberUseCase(fullMember)
                    onSaved(newMember.id)
                }

                _uiState.update { it.copy(isSaving = false, hasChanges = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to save member", isSaving = false)
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
