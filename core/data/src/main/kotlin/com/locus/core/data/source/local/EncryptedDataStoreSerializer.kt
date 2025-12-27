package com.locus.core.data.source.local

import androidx.datastore.core.Serializer
import com.google.crypto.tink.Aead
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

class EncryptedDataStoreSerializer<T>(
    private val aead: Aead,
    private val serializer: KSerializer<T>,
    private val defaultValueProvider: () -> T,
) : Serializer<T> {
    override val defaultValue: T
        get() = defaultValueProvider()

    override suspend fun readFrom(input: InputStream): T {
        return try {
            val encryptedBytes = input.readBytes()
            if (encryptedBytes.isEmpty()) {
                return defaultValue
            }
            val decryptedBytes = aead.decrypt(encryptedBytes, null)
            val jsonString = decryptedBytes.decodeToString()
            Json.decodeFromString(serializer, jsonString)
        } catch (e: Exception) {
            // "Fail Hard" is handled by the repo, but here we must decide whether to throw
            // DataStore's CorruptionException or propagate the Tink/Serialization exception.
            // Documentation implies "CorruptionException" is the standard for invalid data.
            throw androidx.datastore.core.CorruptionException("Cannot decrypt or deserialize data", e)
        }
    }

    override suspend fun writeTo(
        t: T,
        output: OutputStream,
    ) {
        if (t == null) {
            // This is weird for a non-nullable T, but if we are serializing Nullable T, it might be relevant.
            // However, Json.encodeToString handles null if the serializer is nullable.
            // But we are encrypting.
            // If T is nullable, t can be null.
            // If t is null, we can't easily encrypt "null" bytes?
            // Actually, we can serialize null to "null" string JSON.
        }
        val jsonString = Json.encodeToString(serializer, t)
        val encryptedBytes = aead.encrypt(jsonString.encodeToByteArray(), null)
        output.write(encryptedBytes)
    }
}
