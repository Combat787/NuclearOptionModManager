package com.combat.nomm

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


object VersionSerializer : KSerializer<Version> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Version", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Version) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Version {
        val string = decoder.decodeString()
        val parts = string.split('.')
            .map { it.replace("\\D+".toRegex() ,"").toInt() }
            .toIntArray()

        return Version(*parts)
    }
}

