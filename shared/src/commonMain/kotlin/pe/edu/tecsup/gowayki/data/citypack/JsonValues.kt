package pe.edu.tecsup.gowayki.data.citypack

internal fun parseJson(text: String): Any? {
    val parser = JsonParser(text)
    val value = parser.parseValue()
    parser.skip()
    if (!parser.exhausted()) throw IllegalArgumentException("JSON sobrante.")
    return value
}

private class JsonParser(private val text: String) {
    private var index = 0

    fun exhausted(): Boolean = index >= text.length

    fun parseValue(): Any? {
        skip()
        if (exhausted()) throw IllegalArgumentException("JSON vacío.")
        val char = text[index]
        return when {
            char == '{' -> readObject()
            char == '[' -> readArray()
            char == '"' -> readString()
            char == 't' -> readLiteral("true", true)
            char == 'f' -> readLiteral("false", false)
            char == 'n' -> readLiteral("null", null)
            char == '-' || char.isDigit() -> readNumber()
            else -> throw IllegalArgumentException("JSON inválido en '$char'.")
        }
    }

    fun skip() {
        while (index < text.length && text[index].isWhitespace()) index++
    }

    private fun readObject(): Map<String, Any?> {
        expect('{')
        val map = LinkedHashMap<String, Any?>()
        skip()
        if (peek('}')) {
            index++
            return map
        }
        while (true) {
            skip()
            val key = readString()
            skip()
            expect(':')
            map[key] = parseValue()
            skip()
            when {
                peek(',') -> index++
                peek('}') -> {
                    index++
                    return map
                }
                else -> throw IllegalArgumentException("Objeto JSON inválido.")
            }
        }
    }

    private fun readArray(): List<Any?> {
        expect('[')
        val list = ArrayList<Any?>()
        skip()
        if (peek(']')) {
            index++
            return list
        }
        while (true) {
            list.add(parseValue())
            skip()
            when {
                peek(',') -> index++
                peek(']') -> {
                    index++
                    return list
                }
                else -> throw IllegalArgumentException("Arreglo JSON inválido.")
            }
        }
    }

    private fun readString(): String {
        expect('"')
        val builder = StringBuilder()
        while (index < text.length) {
            val char = text[index++]
            when (char) {
                '"' -> return builder.toString()
                '\\' -> {
                    if (index >= text.length) throw IllegalArgumentException("Escape JSON incompleto.")
                    when (val escaped = text[index++]) {
                        '"', '\\', '/' -> builder.append(escaped)
                        'b' -> builder.append('\b')
                        'f' -> builder.append('\u000c')
                        'n' -> builder.append('\n')
                        'r' -> builder.append('\r')
                        't' -> builder.append('\t')
                        'u' -> {
                            if (index + 4 > text.length) throw IllegalArgumentException("Unicode JSON incompleto.")
                            val hex = text.substring(index, index + 4)
                            builder.append(hex.toInt(16).toChar())
                            index += 4
                        }
                        else -> throw IllegalArgumentException("Escape JSON inválido.")
                    }
                }
                else -> builder.append(char)
            }
        }
        throw IllegalArgumentException("Texto JSON sin cierre.")
    }

    private fun readNumber(): Double {
        val start = index
        if (text[index] == '-') index++
        if (index >= text.length || !text[index].isDigit()) throw IllegalArgumentException("Número JSON inválido.")
        while (index < text.length && text[index].isDigit()) index++
        if (index < text.length && text[index] == '.') {
            index++
            if (index >= text.length || !text[index].isDigit()) throw IllegalArgumentException("Número JSON inválido.")
            while (index < text.length && text[index].isDigit()) index++
        }
        if (index < text.length && (text[index] == 'e' || text[index] == 'E')) {
            index++
            if (index < text.length && (text[index] == '+' || text[index] == '-')) index++
            if (index >= text.length || !text[index].isDigit()) throw IllegalArgumentException("Número JSON inválido.")
            while (index < text.length && text[index].isDigit()) index++
        }
        return text.substring(start, index).toDouble()
    }

    private fun readLiteral(literal: String, value: Any?): Any? {
        if (!text.startsWith(literal, index)) throw IllegalArgumentException("Literal JSON inválido.")
        index += literal.length
        return value
    }

    private fun expect(char: Char) {
        if (index >= text.length || text[index] != char) {
            throw IllegalArgumentException("Se esperaba '$char' en el JSON.")
        }
        index++
    }

    private fun peek(char: Char): Boolean = index < text.length && text[index] == char
}

internal fun jsonDoubles(value: Any?): List<Double> {
    val list = value as? List<*> ?: throw IllegalArgumentException("Lista numérica inválida.")
    return list.map { item -> (item as Number).toDouble() }
}

private val ISO_UTC = Regex(
    """^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.(\d+))?(Z|\+00:00)$""",
)

internal fun parseIso8601UtcMicros(text: String): Long {
    val match = ISO_UTC.matchEntire(text) ?: throw IllegalArgumentException("Fecha UTC inválida.")
    val fraction = match.groupValues[7]
    val micros = if (fraction.isEmpty()) {
        0
    } else {
        fraction.take(6).padEnd(6, '0').toInt()
    }
    return pe.edu.tecsup.gowayki.domain.planner.UtcDateTime.of(
        year = match.groupValues[1].toInt(),
        month = match.groupValues[2].toInt(),
        day = match.groupValues[3].toInt(),
        hour = match.groupValues[4].toInt(),
        minute = match.groupValues[5].toInt(),
        second = match.groupValues[6].toInt(),
        micros = micros,
    ).epochMicros
}

internal fun cityPackInfoFromManifest(json: String): pe.edu.tecsup.gowayki.domain.models.CityPackInfo {
    val root = parseJson(json) as? Map<*, *> ?: throw IllegalArgumentException("Manifiesto inválido.")
    val city = root["city_code"] as? String ?: throw IllegalArgumentException("Manifiesto sin ciudad.")
    val version = (root["version"] as? Number)?.toInt() ?: throw IllegalArgumentException("Manifiesto sin versión.")
    val sha = root["sha256"] as? String ?: throw IllegalArgumentException("Manifiesto sin hash.")
    val generated = root["generated_at"] as? String ?: throw IllegalArgumentException("Manifiesto sin fecha.")
    return pe.edu.tecsup.gowayki.domain.models.CityPackInfo(
        cityCode = city,
        version = version,
        generatedAtEpochMicros = parseIso8601UtcMicros(generated),
        sha256 = sha,
    )
}
