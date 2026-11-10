package com.akergez.superreader2000.shared.sync

import com.akergez.tresse.kmp.TresseConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ConfigImportTest {
    private val server = TresseConfig(
        storageType = "http",
        serverUrl = "https://sync.example",
        repoId = "books",
        token = "old-token",
        encryptionKey = "current-key",
    )

    @Test
    fun stringWithKeyKeepsItsKey() = runTest {
        val imported = importConfigString(
            "tresse://http?url=https://sync.example&repo_id=other&token=t&encryption_key=incoming",
            server,
        ) { error("no key needed") }
        assertEquals("incoming", imported.encryptionKey)
        assertEquals("other", imported.repoId)
    }

    @Test
    fun newRepositoryGetsKeyGeneratedHere() = runTest {
        val imported = importConfigString(
            "tresse://http?url=https://sync.example&repo_id=fresh&token=t&new=1",
            TresseConfig(),
        ) { "generated" }
        assertEquals("generated", imported.encryptionKey)
        assertEquals("https://sync.example", imported.serverUrl)
        assertEquals("t", imported.token)
    }

    @Test
    fun sameRepositoryKeepsCurrentKeyWithNewToken() = runTest {
        val imported = importConfigString(
            "tresse://http?url=https://sync.example/&repo_id=books&token=new-token",
            server,
        ) { error("no key needed") }
        assertEquals("current-key", imported.encryptionKey)
        assertEquals("new-token", imported.token)
    }

    @Test
    fun foreignRepositoryWithoutKeyIsRefused() = runTest {
        val refused = assertFailsWith<ConfigImportRefused> {
            importConfigString("tresse://http?url=https://sync.example&repo_id=someone-else&token=t", server) { "x" }
        }
        assertEquals(ServerConnectFailure.NoKey, refused.failure)
    }

    @Test
    fun importKeepsDevicePolicyAndSchedule() = runTest {
        val current = server.copy(ignorePatterns = listOf("*.tmp"), autoSyncEnabled = false, autoSyncIntervalMinutes = 42)
        val imported = importConfigString(
            "tresse://s3?repo_id=p&endpoint=https://s3.example&bucket=b&access_key_id=a&secret_access_key=s&encryption_key=k",
            current,
        ) { "x" }
        assertEquals("s3", imported.storageType)
        assertEquals(listOf("*.tmp"), imported.ignorePatterns)
        assertEquals(false, imported.autoSyncEnabled)
        assertEquals(42, imported.autoSyncIntervalMinutes)
    }

    @Test
    fun connectServerPrefersConfiguredHttpServer() {
        assertEquals("https://sync.example", connectServerFor(server, "https://default.example"))
        assertEquals("https://default.example", connectServerFor(server.copy(storageType = "s3"), "https://default.example"))
        assertEquals("https://default.example", connectServerFor(TresseConfig(), "https://default.example"))
    }

    @Test
    fun acceptFailuresMapToScreenReasons() {
        assertEquals(ServerConnectFailure.LinkMismatch, mapServerConnectFailure("this link does not belong to the connection started here"))
        assertEquals(ServerConnectFailure.NoConfig, mapServerConnectFailure("no config in the returned link"))
        assertEquals(ServerConnectFailure.OtherServer, mapServerConnectFailure("the link points at another server than the one opened"))
        assertEquals(ServerConnectFailure.NoKey, mapServerConnectFailure("this device has no encryption key for that repository"))
        assertEquals(ServerConnectFailure.Decode, mapServerConnectFailure("something else"))
        assertEquals(ServerConnectFailure.Decode, mapServerConnectFailure(null))
        assertNull(chooseImportKey(TresseConfig(repoId = "x"), newRepository = false, current = null))
    }
}
