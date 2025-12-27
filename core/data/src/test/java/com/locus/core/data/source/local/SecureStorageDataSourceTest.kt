package com.locus.core.data.source.local

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.locus.core.data.model.BootstrapCredentialsDto
import com.locus.core.data.model.RuntimeCredentialsDto
import com.locus.core.domain.result.LocusResult
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.RuntimeCredentials
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.serialization.builtins.nullable
import java.io.File

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SecureStorageDataSourceTest {

    private lateinit var context: Context
    private lateinit var aead: Aead
    private lateinit var dataSource: SecureStorageDataSource

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Use standard Tink for Robolectric/Local tests to avoid Keystore dependencies
        com.google.crypto.tink.aead.AeadConfig.register()
        val keysetHandle = com.google.crypto.tink.KeysetHandle.generateNew(KeyTemplates.get("AES256_GCM"))
        aead = keysetHandle.getPrimitive(Aead::class.java)

        val bootstrapSerializer = EncryptedDataStoreSerializer(
            aead = aead,
            serializer = BootstrapCredentialsDto.serializer().nullable,
            defaultValueProvider = { null }
        )
        val bootstrapStore = DataStoreFactory.create(
            serializer = bootstrapSerializer,
            produceFile = { File(context.filesDir, "test_bootstrap.pb") }
        )

        val runtimeSerializer = EncryptedDataStoreSerializer(
            aead = aead,
            serializer = RuntimeCredentialsDto.serializer().nullable,
            defaultValueProvider = { null }
        )
        val runtimeStore = DataStoreFactory.create(
            serializer = runtimeSerializer,
            produceFile = { File(context.filesDir, "test_runtime.pb") }
        )

        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        dataSource = SecureStorageDataSource(bootstrapStore, runtimeStore, prefs)
    }

    @Test
    fun saveAndGetBootstrapCredentials_success() = runTest {
        val creds = BootstrapCredentials(
            accessKeyId = "test-id",
            secretAccessKey = "test-secret",
            sessionToken = "test-token",
            region = "us-east-1"
        )

        val saveResult = dataSource.saveBootstrapCredentials(creds)
        assertThat(saveResult).isInstanceOf(LocusResult.Success::class.java)

        val getResult = dataSource.getBootstrapCredentials()
        assertThat(getResult).isInstanceOf(LocusResult.Success::class.java)
        assertThat((getResult as LocusResult.Success).data).isEqualTo(creds)
    }

    @Test
    fun clearBootstrapCredentials_success() = runTest {
        val creds = BootstrapCredentials("id", "secret", "token", "region")
        dataSource.saveBootstrapCredentials(creds)

        val clearResult = dataSource.clearBootstrapCredentials()
        assertThat(clearResult).isInstanceOf(LocusResult.Success::class.java)

        val getResult = dataSource.getBootstrapCredentials()
        assertThat((getResult as LocusResult.Success).data).isNull()
    }

    @Test
    fun saveAndGetRuntimeCredentials_success() = runTest {
        val creds = RuntimeCredentials(
            accessKeyId = "run-id",
            secretAccessKey = "run-secret",
            bucketName = "run-bucket",
            region = "us-west-2",
            accountId = "12345",
            telemetrySalt = "salty"
        )

        val saveResult = dataSource.saveRuntimeCredentials(creds)
        assertThat(saveResult).isInstanceOf(LocusResult.Success::class.java)

        val getResult = dataSource.getRuntimeCredentials()
        assertThat(getResult).isInstanceOf(LocusResult.Success::class.java)
        assertThat((getResult as LocusResult.Success).data).isEqualTo(creds)
    }

    @Test
    fun getTelemetrySalt_retrievesFromSecureStorage() = runTest {
        val creds = RuntimeCredentials(
            accessKeyId = "id", secretAccessKey = "secret",
            bucketName = "b", region = "r", accountId = "a",
            telemetrySalt = "secure-salt"
        )
        dataSource.saveRuntimeCredentials(creds)

        val salt = dataSource.getTelemetrySalt()
        assertThat(salt).isEqualTo("secure-salt")
    }

    @Test
    fun getTelemetrySalt_fallbacksToSharedPreferences() = runTest {
        // Clear secure storage
        dataSource.clearRuntimeCredentials()

        // Manually set legacy salt
        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(SecureStorageDataSource.KEY_SALT, "legacy-salt").commit()

        val salt = dataSource.getTelemetrySalt()
        assertThat(salt).isEqualTo("legacy-salt")
    }
}
