package codes.rorak.hamley.util

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import java.awt.Color
import java.time.OffsetDateTime

class OffsetDateTimeSerializer : JsonSerializer<OffsetDateTime?>() {
    override fun serialize(value: OffsetDateTime?, gen: JsonGenerator?, serializers: SerializerProvider?) {
        if (value != null)
            gen!!.writeString(Config.DATE_TIME_FORMATTER.format(value));
        else
            gen!!.writeString("forever");
    }
}

class OffsetDateTimeDeserializer : JsonDeserializer<OffsetDateTime?>() {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): OffsetDateTime? {
        val text = p?.text!!;
        if (text == "forever") return null;
        return OffsetDateTime.parse(text, Config.DATE_TIME_FORMATTER);
    }
}

class ColorSerializer : JsonSerializer<Color>() {
    override fun serialize(value: Color?, gen: JsonGenerator?, serializers: SerializerProvider?) =
        gen!!.writeString(value.toText());
}

class ColorDeserializer : JsonDeserializer<Color>() {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): Color =
        Color.decode(p?.text!!);
}