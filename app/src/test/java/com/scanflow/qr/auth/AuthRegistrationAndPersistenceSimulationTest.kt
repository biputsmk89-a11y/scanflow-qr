package com.scanflow.qr.auth

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import com.scanflow.qr.core.datastore.PreferencesManager
import com.scanflow.qr.data.repository.CloudAuthRepositoryImpl
import com.scanflow.qr.domain.model.AuthUser
import com.scanflow.qr.feature.auth.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Audit & simulasi 100% nyata untuk sistem otentikasi ScanFlow QR:
 * 1. Validasi Form Masuk, Form Daftar, dan Mode Tamu
 * 2. Hash Kredensial SHA-256 & Penyimpanan Registry
 * 3. Pencegahan Duplikasi Email & Error Handling
 * 4. SIMULASI RESTART APLIKASI: Membuktikan akun TIDAK PERNAH HILANG saat aplikasi ditutup dan dibuka kembali.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthRegistrationAndPersistenceSimulationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()
    private var dataStoreScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    private lateinit var datastoreFile: File
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var authRepository: CloudAuthRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dataStoreScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
        datastoreFile = tempFolder.newFile("test_scanflow_preferences_${System.nanoTime()}.preferences_pb")

        val testDataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { datastoreFile }
        )

        preferencesManager = PreferencesManager(customDataStore = testDataStore)
        authRepository = CloudAuthRepositoryImpl(preferencesManager)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        Dispatchers.resetMain()
    }

    // =========================================================================
    // 1. AUDIT VALIDASI FORM & FORMAT INPUT
    // =========================================================================

    @Test
    fun emailValidation_rejectsInvalidAndAcceptsValidEmails() {
        // Email tidak valid
        assertThat(PreferencesManager.isValidEmail("")).isFalse()
        assertThat(PreferencesManager.isValidEmail("   ")).isFalse()
        assertThat(PreferencesManager.isValidEmail("plainaddress")).isFalse()
        assertThat(PreferencesManager.isValidEmail("@missingusername.com")).isFalse()
        assertThat(PreferencesManager.isValidEmail("user@.com")).isFalse()

        // Email valid
        assertThat(PreferencesManager.isValidEmail("user@scanflow.app")).isTrue()
        assertThat(PreferencesManager.isValidEmail("budi.santoso@smk.sch.id")).isTrue()
        assertThat(PreferencesManager.isValidEmail("test_user-123@gmail.com")).isTrue()
    }

    @Test
    fun signUp_withInvalidEmailFormat_returnsFailure() = runTest {
        val result = authRepository.signUp(
            email = "email_palsu_tanpa_domain",
            password = "password123",
            displayName = "Budi Siswa"
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("Format alamat email tidak valid")
    }

    @Test
    fun signUp_withShortPassword_returnsFailure() = runTest {
        val result = authRepository.signUp(
            email = "user@scanflow.app",
            password = "123", // Kurang dari 6 karakter
            displayName = "User Pendek"
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("Kata sandi minimal harus 6 karakter")
    }

    // =========================================================================
    // 2. AUDIT PENDAFTARAN AKUN (SIGN UP) & SECURITY HASHING
    // =========================================================================

    @Test
    fun signUp_withValidData_createsAccountAndSetsUserSession() = runTest {
        val result = authRepository.signUp(
            email = "ahmad.fauzi@smk.sch.id",
            password = "KataSandiKuat99!",
            displayName = "Ahmad Fauzi"
        )

        // 1. Verifikasi hasil pendaftaran berhasil
        assertThat(result.isSuccess).isTrue()
        val user = result.getOrNull()
        assertThat(user).isNotNull()
        assertThat(user?.email).isEqualTo("ahmad.fauzi@smk.sch.id")
        assertThat(user?.displayName).isEqualTo("Ahmad Fauzi")
        assertThat(user?.isGuest).isFalse()

        // 2. Verifikasi kata sandi di-hash dengan SHA-256 (bukan plaintext)
        val expectedHash = preferencesManager.hashPassword("KataSandiKuat99!")
        val record = preferencesManager.getRegisteredUserRecord("ahmad.fauzi@smk.sch.id")
        assertThat(record).isNotNull()
        assertThat(record?.first).isEqualTo(expectedHash)
        assertThat(record?.first).isNotEqualTo("KataSandiKuat99!") // Keamanan terjamin

        // 3. Verifikasi status onboarding otomatis tuntas
        val settings = preferencesManager.settingsFlow.first()
        assertThat(settings.isOnboardingCompleted).isTrue()
    }

    @Test
    fun signUp_withAlreadyRegisteredEmail_isRejected() = runTest {
        // Pendaftaran pertama
        authRepository.signUp("kembar@scanflow.app", "pass12345", "User Pertama")

        // Pendaftaran kedua dengan email yang sama persis
        val secondResult = authRepository.signUp("kembar@scanflow.app", "passBaru99", "User Kedua")

        assertThat(secondResult.isFailure).isTrue()
        assertThat(secondResult.exceptionOrNull()?.message).contains("Email sudah terdaftar. Silakan masuk.")
    }

    // =========================================================================
    // 3. AUDIT LOGIN (SIGN IN) DENGAN KREDENSIAL TERDAFTAR & DEMO
    // =========================================================================

    @Test
    fun signIn_withUnregisteredEmail_returnsDescriptiveError() = runTest {
        val result = authRepository.signIn("tidak_ada@scanflow.app", "password123")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("belum terdaftar. Silakan daftar akun baru.")
    }

    @Test
    fun signIn_withWrongPassword_returnsPasswordMismatchError() = runTest {
        // Daftarkan akun dulu
        authRepository.signUp("citra@scanflow.app", "citraRahasia", "Citra Dewi")

        // Coba masuk dengan kata sandi salah
        val result = authRepository.signIn("citra@scanflow.app", "kataSandiSalah")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("Kata sandi yang Anda masukkan salah")
    }

    @Test
    fun signIn_withCorrectPassword_succeedsAndRestoresSession() = runTest {
        // 1. Daftarkan akun
        authRepository.signUp("dewi@scanflow.app", "dewiAman2026", "Dewi Lestari")

        // 2. Simulasi keluar akun (sign out)
        authRepository.signOut()

        val afterSignOut = preferencesManager.currentUserFlow.first()
        assertThat(afterSignOut.isGuest).isTrue()

        // 3. Masuk kembali dengan kredensial yang benar
        val signInResult = authRepository.signIn("dewi@scanflow.app", "dewiAman2026")

        assertThat(signInResult.isSuccess).isTrue()
        val activeUser = signInResult.getOrNull()
        assertThat(activeUser?.displayName).isEqualTo("Dewi Lestari")
        assertThat(activeUser?.isGuest).isFalse()

        val currentSession = preferencesManager.currentUserFlow.first()
        assertThat(currentSession.email).isEqualTo("dewi@scanflow.app")
        assertThat(currentSession.isGuest).isFalse()
    }

    @Test
    fun signIn_withDemoAccount_succeedsDirectly() = runTest {
        val result = authRepository.signIn("user@scanflow.app", "password123")

        assertThat(result.isSuccess).isTrue()
        val user = result.getOrNull()
        assertThat(user?.displayName).isEqualTo("ScanFlow Pro User")
        assertThat(user?.email).isEqualTo("user@scanflow.app")
        assertThat(user?.isGuest).isFalse()
    }

    // =========================================================================
    // 4. SIMULASI 100% NYATA: RESTART APLIKASI TANPA KEHILANGAN AKUN
    // =========================================================================

    @Test
    fun appRestartSimulation_userNeverLosesAccountWhenReopeningApp() = runTest {
        // STEP 1: Pengguna pertama kali mendaftar akun di ScanFlow QR
        val registerResult = authRepository.signUp(
            email = "ridwan.kamil@smk.sch.id",
            password = "SemangatSMKBisa123!",
            displayName = "Ridwan Kamil"
        )

        assertThat(registerResult.isSuccess).isTrue()
        val registeredUser = registerResult.getOrNull()!!
        assertThat(registeredUser.displayName).isEqualTo("Ridwan Kamil")
        assertThat(registeredUser.isGuest).isFalse()

        // STEP 2: SIMULASI PENGGUNA KELUAR DARI APLIKASI (KILL PROCESS / REBOOT HP)
        // DataStore lama ditutup dengan menghentikan coroutine scope-nya:
        dataStoreScope.cancel()

        // STEP 3: SIMULASI MEMBUKA APLIKASI LAGI DARI AWAL (COLD START)
        // Di proses MainActivity baru, dibuat scope & instance DataStore baru yang membaca file fisik disk yang sama:
        val restartedScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
        val restartedDataStore = PreferenceDataStoreFactory.create(
            scope = restartedScope,
            produceFile = { datastoreFile }
        )
        val freshPreferencesManager = PreferencesManager(customDataStore = restartedDataStore)
        val freshAuthRepository = CloudAuthRepositoryImpl(freshPreferencesManager)

        try {
            // STEP 4: MEMVERIFIKASI STATE SAAT APLIKASI DIBUKA KEMBALI
            val restoredSettings = freshPreferencesManager.settingsFlow.first()
            val restoredUser = freshAuthRepository.getCurrentUser().first()

            // A. Bukti 1: Onboarding completed bernilai TRUE (SplashScreen langsung lompat ke MainScreen, tidak balik ke Onboarding/Login)
            assertThat(restoredSettings.isOnboardingCompleted).isTrue()

            // B. Bukti 2: Akun TIDAK KEMBALI KE NOL (bukan guest, nama & email tetap milik pengguna)
            assertThat(restoredUser.isGuest).isFalse()
            assertThat(restoredUser.email).isEqualTo("ridwan.kamil@smk.sch.id")
            assertThat(restoredUser.displayName).isEqualTo("Ridwan Kamil")
            assertThat(restoredUser.id).isEqualTo(registeredUser.id)

            // C. Bukti 3: Pengguna tidak pernah kehilangan akunnya di aplikasi!
            assertThat(restoredUser.isGuest).isNotEqualTo(true)
            assertThat(restoredUser.displayName).isNotEqualTo("Guest User")
        } finally {
            restartedScope.cancel()
        }
    }

    // =========================================================================
    // 5. AUDIT AUTH VIEWMODEL & STATE PENGGUNA
    // =========================================================================

    @Test
    fun authViewModel_integrationFlow_verifiesLoadingAndSuccessStates() = runTest {
        val viewModel = AuthViewModel(authRepository)
        var callbackUser: AuthUser? = null
        var errorMessage: String? = null

        // Test Sign Up via ViewModel
        viewModel.signUp(
            email = "siswabaru@smk.sch.id",
            pass = "rahasia123",
            displayName = "Siswa Baru",
            onSuccess = { callbackUser = it },
            onError = { errorMessage = it }
        )

        // Tunggu operasi background IO DataStore selesai di thread pool
        var attempts = 0
        while (callbackUser == null && errorMessage == null && attempts < 50) {
            Thread.sleep(30)
            attempts++
        }

        assertThat(errorMessage).isNull()
        assertThat(callbackUser).isNotNull()
        assertThat(callbackUser?.email).isEqualTo("siswabaru@smk.sch.id")
        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.successUser?.displayName).isEqualTo("Siswa Baru")

        // Test Guest Mode via ViewModel
        var guestCallbackCalled = false
        viewModel.continueAsGuest {
            guestCallbackCalled = true
        }

        attempts = 0
        while (!guestCallbackCalled && attempts < 50) {
            Thread.sleep(30)
            attempts++
        }

        assertThat(guestCallbackCalled).isTrue()
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }
}
