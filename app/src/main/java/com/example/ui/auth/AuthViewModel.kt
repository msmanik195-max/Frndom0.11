package com.example.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthScreen {
    SPLASH,
    LOGIN,
    REGISTER,
    WELCOME
}

data class AuthUiState(
    val currentScreen: AuthScreen = AuthScreen.SPLASH,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentUserProfile: UserProfile? = null,

    // Login Form State
    val loginIdentifier: String = "",
    val loginPassword: String = "",
    val isLoginPasswordVisible: Boolean = false,

    // Register Form State
    val regFirstName: String = "",
    val regLastName: String = "",
    val regIdentifierType: String = "email", // "email" or "phone"
    val regEmail: String = "",
    val regPhone: String = "",
    val regGender: String = "Male", // "Male", "Female", "Custom"
    val regBirthDay: Int = 1,
    val regBirthMonth: Int = 1,
    val regBirthYear: Int = 2000,
    val regPassword: String = "",
    val regConfirmPassword: String = "",
    val isRegPasswordVisible: Boolean = false,
    val isRegConfirmPasswordVisible: Boolean = false,

    // Forgot Password Dialog State
    val showForgotPasswordDialog: Boolean = false,
    val forgotPasswordEmail: String = "",
    val isResettingPassword: Boolean = false,
    val resetSuccessMessage: String? = null,
    val resetErrorMessage: String? = null
)

class AuthViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: AuthRepository = AuthRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Initialize and check persistent session
        checkExistingAuth()
    }

    private fun checkExistingAuth() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val startTime = System.currentTimeMillis()

            val userRepo = com.example.data.repository.UserRepository(getApplication())
            val currentFirebaseUser = repository.currentUser
            val lastActiveUid = userRepo.getLastActiveUserUid()

            var targetProfile: UserProfile? = null

            if (currentFirebaseUser != null) {
                val profile = repository.fetchCurrentUserProfile()
                targetProfile = if (profile != null) {
                    userRepo.enrichProfileWithVerification(profile)
                } else {
                    val fallback = userRepo.getLocalUserProfile(currentFirebaseUser.uid) ?: UserProfile(
                        uid = currentFirebaseUser.uid,
                        email = currentFirebaseUser.email ?: "",
                        fullName = currentFirebaseUser.displayName?.takeIf { it.isNotBlank() } ?: "User"
                    )
                    userRepo.enrichProfileWithVerification(fallback)
                }
            } else if (!lastActiveUid.isNullOrBlank()) {
                val localUser = userRepo.getLocalUserProfile(lastActiveUid)
                if (localUser != null) {
                    targetProfile = userRepo.enrichProfileWithVerification(localUser)
                }
            }

            if (targetProfile != null) {
                userRepo.saveLocalUserProfile(targetProfile)
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < 650L) {
                    delay(650L - elapsed)
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentUserProfile = targetProfile,
                        currentScreen = AuthScreen.WELCOME
                    )
                }
            } else {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < 650L) {
                    delay(650L - elapsed)
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentScreen = AuthScreen.LOGIN
                    )
                }
            }
        }
    }

    fun navigateTo(screen: AuthScreen) {
        val currentIdent = _uiState.value.loginIdentifier.trim()
        if (screen == AuthScreen.REGISTER && currentIdent.isNotBlank()) {
            val isEmail = currentIdent.contains("@")
            _uiState.update {
                it.copy(
                    currentScreen = screen,
                    regIdentifierType = if (isEmail) "email" else "phone",
                    regEmail = if (isEmail && it.regEmail.isBlank()) currentIdent else it.regEmail,
                    regPhone = if (!isEmail && it.regPhone.isBlank()) currentIdent else it.regPhone,
                    errorMessage = null,
                    successMessage = null
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                currentScreen = screen,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    // --- Login Form Updaters ---
    fun onLoginIdentifierChange(value: String) {
        _uiState.update { it.copy(loginIdentifier = value, errorMessage = null) }
    }

    fun onLoginPasswordChange(value: String) {
        _uiState.update { it.copy(loginPassword = value, errorMessage = null) }
    }

    fun toggleLoginPasswordVisibility() {
        _uiState.update { it.copy(isLoginPasswordVisible = !it.isLoginPasswordVisible) }
    }

    // --- Register Form Updaters ---
    fun onRegFirstNameChange(value: String) {
        _uiState.update { it.copy(regFirstName = value, errorMessage = null) }
    }

    fun onRegLastNameChange(value: String) {
        _uiState.update { it.copy(regLastName = value, errorMessage = null) }
    }

    fun onRegIdentifierTypeChange(type: String) {
        _uiState.update { it.copy(regIdentifierType = type, errorMessage = null) }
    }

    fun onRegEmailChange(value: String) {
        _uiState.update { it.copy(regEmail = value, errorMessage = null) }
    }

    fun onRegPhoneChange(value: String) {
        _uiState.update { it.copy(regPhone = value, errorMessage = null) }
    }

    fun onRegGenderChange(gender: String) {
        _uiState.update { it.copy(regGender = gender) }
    }

    fun onRegBirthDateChange(day: Int, month: Int, year: Int) {
        _uiState.update {
            it.copy(
                regBirthDay = day,
                regBirthMonth = month,
                regBirthYear = year
            )
        }
    }

    fun onRegPasswordChange(value: String) {
        _uiState.update { it.copy(regPassword = value, errorMessage = null) }
    }

    fun onRegConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(regConfirmPassword = value, errorMessage = null) }
    }

    fun toggleRegPasswordVisibility() {
        _uiState.update { it.copy(isRegPasswordVisible = !it.isRegPasswordVisible) }
    }

    fun toggleRegConfirmPasswordVisibility() {
        _uiState.update { it.copy(isRegConfirmPasswordVisible = !it.isRegConfirmPasswordVisible) }
    }

    // --- Actions ---

    fun login() {
        val state = _uiState.value
        val identifier = state.loginIdentifier.trim()
        val password = state.loginPassword

        if (identifier.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email or phone number") }
            return
        }

        if (password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter your password") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.loginUser(identifier, password)
            if (result.isSuccess) {
                val profile = result.getOrNull()
                if (profile != null) {
                    val userRepo = com.example.data.repository.UserRepository(getApplication())
                    userRepo.saveLocalUserProfile(profile)
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentUserProfile = profile,
                        currentScreen = AuthScreen.WELCOME,
                        loginPassword = ""
                    )
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Login failed. Please verify your credentials."
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = errorMsg
                    )
                }
            }
        }
    }

    fun openForgotPasswordDialog(prefillEmail: String = _uiState.value.loginIdentifier) {
        val email = if (prefillEmail.contains("@")) prefillEmail.trim() else ""
        _uiState.update {
            it.copy(
                showForgotPasswordDialog = true,
                forgotPasswordEmail = email,
                resetSuccessMessage = null,
                resetErrorMessage = null
            )
        }
    }

    fun closeForgotPasswordDialog() {
        _uiState.update {
            it.copy(
                showForgotPasswordDialog = false,
                resetSuccessMessage = null,
                resetErrorMessage = null
            )
        }
    }

    fun onForgotPasswordEmailChange(email: String) {
        _uiState.update { it.copy(forgotPasswordEmail = email, resetErrorMessage = null) }
    }

    fun sendPasswordResetEmail(email: String = _uiState.value.forgotPasswordEmail) {
        val targetEmail = email.trim()
        if (targetEmail.isBlank() || !targetEmail.contains("@")) {
            _uiState.update { it.copy(resetErrorMessage = "Please enter a valid email address.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isResettingPassword = true, resetErrorMessage = null, resetSuccessMessage = null) }
            val result = repository.sendPasswordResetEmail(targetEmail)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isResettingPassword = false,
                        resetSuccessMessage = "Password reset link sent! Check your inbox (or spam folder).",
                        resetErrorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isResettingPassword = false,
                        resetErrorMessage = result.exceptionOrNull()?.message ?: "Failed to send reset link."
                    )
                }
            }
        }
    }

    fun quickAdminLogin(email: String = _uiState.value.loginIdentifier) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, showForgotPasswordDialog = false) }
            val cleanEmail = email.ifBlank { "marufma143g@gmail.com" }
            val result = repository.quickDeveloperLogin(cleanEmail)
            if (result.isSuccess) {
                val profile = result.getOrNull()
                if (profile != null) {
                    val userRepo = com.example.data.repository.UserRepository(getApplication())
                    userRepo.saveLocalUserProfile(profile)
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentUserProfile = profile,
                        currentScreen = AuthScreen.WELCOME,
                        loginPassword = ""
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Login failed."
                    )
                }
            }
        }
    }

    fun register() {
        val state = _uiState.value
        val firstName = state.regFirstName.trim()
        val lastName = state.regLastName.trim()
        val identifierType = state.regIdentifierType
        val email = state.regEmail.trim()
        val phone = state.regPhone.trim()
        val gender = state.regGender
        val day = state.regBirthDay
        val month = state.regBirthMonth
        val year = state.regBirthYear
        val password = state.regPassword
        val confirmPassword = state.regConfirmPassword

        // Validations
        if (firstName.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter your First Name") }
            return
        }

        if (lastName.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter your Last Name") }
            return
        }

        val identifierValue = if (identifierType == "email") email else phone

        if (identifierType == "email") {
            if (email.isEmpty()) {
                _uiState.update { it.copy(errorMessage = "Please enter your email address") }
                return
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                _uiState.update { it.copy(errorMessage = "Please enter a valid email address") }
                return
            }
        } else {
            if (phone.isEmpty()) {
                _uiState.update { it.copy(errorMessage = "Please enter your phone number") }
                return
            }
            if (phone.length < 7) {
                _uiState.update { it.copy(errorMessage = "Please enter a valid phone number") }
                return
            }
        }

        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters") }
            return
        }

        if (password != confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.registerUser(
                firstName = firstName,
                lastName = lastName,
                identifierType = identifierType,
                identifierValue = identifierValue,
                gender = gender,
                birthDay = day,
                birthMonth = month,
                birthYear = year,
                password = password
            )

            if (result.isSuccess) {
                val profile = result.getOrNull()
                if (profile != null) {
                    val userRepo = com.example.data.repository.UserRepository(getApplication())
                    userRepo.saveLocalUserProfile(profile)
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentUserProfile = profile,
                        currentScreen = AuthScreen.WELCOME,
                        regPassword = "",
                        regConfirmPassword = ""
                    )
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Registration failed. Please try again."
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = errorMsg
                    )
                }
            }
        }
    }

    fun logout() {
        repository.logout()
        val userRepo = com.example.data.repository.UserRepository(getApplication())
        userRepo.clearLastActiveUserUid()
        _uiState.update {
            it.copy(
                currentUserProfile = null,
                currentScreen = AuthScreen.LOGIN,
                errorMessage = null,
                successMessage = null
            )
        }
    }
}
