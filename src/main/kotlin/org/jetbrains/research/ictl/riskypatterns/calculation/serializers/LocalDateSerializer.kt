package org.jetbrains.research.ictl.riskypatterns.calculation.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate


object LocalDateSerializer : KSerializer<LocalDate> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: LocalDate) {
    encoder.encodeString(value.toString())
  }

  override fun deserialize(decoder: Decoder): LocalDate {
    val string = decoder.decodeString()
    return LocalDate.parse(string)
  }
}
