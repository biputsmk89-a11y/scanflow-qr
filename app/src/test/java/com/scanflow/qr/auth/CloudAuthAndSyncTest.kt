package com.scanflow.qr.auth

import com.google.common.truth.Truth.assertThat
import com.scanflow.qr.domain.model.AuthUser
import com.scanflow.qr.domain.model.SyncReport
import com.scanflow.qr.domain.model.SyncStatus
import com.scanflow.qr.domain.repository.AuthRepository
import com.scanflow.qr.domain.repository.SyncRepository
import com.scanflow.qr.feature.auth.AuthViewModel
import com.scanflow.qr.feature.profile.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class FakeCloudAuthRepository : AuthRepository {
    private val _currentUser = MutableStateFlow(
        AuthUser(id = "local_guest_user", email = null, displayName = "Guest User", isGuest = true)
    )
    val registeredUsers = mutableMapOf<String, Pair<String, String>>() // email -> (pass, name)

    override fun getCurrentUser(): Flow<AuthUser> = _currentUser.asStateFlow()

    override suspend fun signIn(email: String, password: String): Result<AuthUser> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail == "user@scanflow.app" && password == "password123") {
            val user = AuthUser("user_demo", "user@scanflow.app", "Demo User", isGuest = false)
            _currentUser.value = user
            return Result.success(user)
        }
        val registered = registeredUsers[cleanEmail]
        if (registered != null && registered.first == password) {
            val user = AuthUser("user_${cleanEmail.hashCode()}", cleanEmail, registered.second, isGuest = false)
            _currentUser.value = user
            return Result.success(user)
        }
        return Result.failure(IllegalArgumentException("Kredensial tidak valid"))
    }

    override suspend fun signUp(email: String, password: String, displayName: String): Result<AuthUser> {
        val cleanEmail = email.trim().lowercase()
        registeredUsers[cleanEmail] = password to displayName
        val user = AuthUser("user_${cleanEmail.hashCode()}", cleanEmail, displayName, isGuest = false)
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun signInAsGuest(): AuthUser {
        val guest = AuthUser(id = "local_guest_user", email = null, displayName = "Guest User", isGuest = true)
        _currentUser.value = guest
        return guest
    }

    override suspend fun signOut() {
        _currentUser.value = AuthUser(id = "local_guest_user", email = null, displayName = "Guest User", isGuest = true)
    }
}

class FakeCloudSyncRepository : SyncRepository {
    private val _status = MutableStateFlow(SyncStatus.LOCAL_ONLY)
    private val _lastSync = MutableStateFlow<Long?>(null)

    override fun getSyncStatus(): Flow<SyncStatus> = _status.asStateFlow()
    override fun getLastSyncTime(): Flow<Long?> = _lastSync.asStateFlow()

    override suspend fun requestSync(): Result<SyncReport> {
        _status.value = SyncStatus.SYNCING
        val now = System.currentTimeMillis()
        _lastSync.value = now
        _status.value = SyncStatus.SYNCED
        return Result.success(SyncReport(success = true, syncedScansCount = 5, syncedQrsCount = 2, message = "Sinkronisasi Berhasil", timestamp = now))
    }

    override suspend fun backupToCloud(): Result<String> {
        _status.value = SyncStatus.SYNCED
        return Result.success("content://scanflow/cloud_vault.json")
    }

    override suspend fun restoreFromCloud(uri: android.net.Uri?): Result<Pair<Int, Int>> {
        _status.value = SyncStatus.SYNCED
        return Result.success(5 to 2)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CloudAuthAndSyncTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: FakeCloudAuthRepository
    private lateinit var syncRepository: FakeCloudSyncRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeCloudAuthRepository()
        syncRepository = FakeCloudSyncRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun authViewModel_signInWithDemoAccount_succeeds() = runTest {
        val authViewModel = AuthViewModel(authRepository)
        var signedInUser: AuthUser? = null

        authViewModel.signIn(
            email = "user@scanflow.app",
            pass = "password123",
            onSuccess = { signedInUser = it },
            onError = {}
        )

        advanceUntilIdle()

        assertThat(signedInUser).isNotNull()
        assertThat(signedInUser?.email).isEqualTo("user@scanflow.app")
        assertThat(signedInUser?.isGuest).isFalse()
    }

    @Test
    fun authViewModel_signUpNewAccount_registersAndUpdatesSession() = runTest {
        val authViewModel = AuthViewModel(authRepository)
        var registeredUser: AuthUser? = null

        authViewModel.signUp(
            email = "siswa@smk.sch.id",
            pass = "rahasia123",
            displayName = "Budi Siswa",
            onSuccess = { registeredUser = it },
            onError = {}
        )

        advanceUntilIdle()

        assertThat(registeredUser).isNotNull()
        assertThat(registeredUser?.displayName).isEqualTo("Budi Siswa")
        assertThat(registeredUser?.email).isEqualTo("siswa@smk.sch.id")
    }

    @Test
    fun profileViewModel_syncNow_triggersCloudSyncAndUpdatesStatus() = runTest {
        val profileViewModel = ProfileViewModel(authRepository, syncRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            profileViewModel.uiState.collect {}
        }

        advanceUntilIdle()
        assertThat(profileViewModel.uiState.value.syncStatus).isEqualTo(SyncStatus.LOCAL_ONLY)

        profileViewModel.syncNow()
        advanceUntilIdle()

        val state = profileViewModel.uiState.value
        assertThat(state.syncStatus).isEqualTo(SyncStatus.SYNCED)
        assertThat(state.lastSyncTime).isNotNull()
        assertThat(state.feedbackMessage).contains("Sinkronisasi Berhasil")
    }
}
