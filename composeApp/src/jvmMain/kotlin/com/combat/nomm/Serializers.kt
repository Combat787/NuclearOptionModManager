package com.combat.nomm

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
            .map { it.toInt() }
            .toIntArray()

        return Version(*parts)
    }
}

object ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Color) {
        val argb = value.toArgb()
        val hex = (argb.toUInt()).toString(16).uppercase().padStart(8, '0')
        encoder.encodeString("#$hex")
    }

    override fun deserialize(decoder: Decoder): Color {
        val hex = decoder.decodeString().removePrefix("#")
        val argb = hex.toLong(16).toInt()
        return Color(argb)
    }
}