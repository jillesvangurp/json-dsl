package com.jillesvangurp.jsondsl

class SimpleYamlSerializer(val includeYamlDocumentStart: Boolean=true) {
    fun serialize(properties: JsonDsl): String {
        val buf = StringBuilder()
        if(includeYamlDocumentStart) {
            buf.append("---\n")
        }
        write(buf, 0, properties, isRoot = true)
        return buf.toString().trimEnd()
    }

    private fun write(buf: StringBuilder, indent: Int, obj: Any?, isRoot: Boolean = false) {
        when (obj) {
            null -> buf.append("null\n")
            is RawJson -> buf.append("${obj.value}\n")
            is Number, is Boolean -> buf.append("$obj\n")
            is String, is CharSequence -> {
                if (obj.toString().contains("\n")) {
                    // Multiline string, use literal style
                    buf.append("|-\n")
                    obj.toString().lineSequence().forEach { line ->
                        buf.append(" ".repeat(indent + 2))
                        buf.append(line)
                        buf.append("\n")
                    }
                } else {
                    // Single line string
                    val escaped = obj.toString().yamlEscape()
                    buf.append("$escaped\n")
                }
            }
            is Map<*, *> -> {
                if (!isRoot) buf.append("\n")
                writeMap(buf, indent, obj)
            }
            is Sequence<*> -> {
                if (!isRoot) buf.append("\n")
                writeIterator(buf, indent, obj.iterator())
            }
            is Iterator<*> -> {
                if (!isRoot) buf.append("\n")
                writeIterator(buf, indent, obj)
            }
            is Iterable<*> -> {
                if (!isRoot) buf.append("\n")
                writeIterator(buf, indent, obj.iterator())
            }
            is Array<*> -> {
                if (!isRoot) buf.append("\n")
                writeIterator(buf, indent, obj.iterator())
            }
            else -> buf.append("${obj.toString().yamlEscape()}\n")
        }
    }

    private fun writeMap(buf: StringBuilder, indent: Int, map: Map<*, *>) {
        map.entries.filter { it.value != null }.forEach { entry ->
            buf.append(" ".repeat(indent))
            buf.append("${entry.key.toString().yamlEscape()}:")
            if (entry.value is String && entry.value.toString().contains("\n")) {
                // Handle multiline string
                buf.append(" |\n") // Note the space before the pipe
                val additionalIndent = " ".repeat(indent + 2) // Adjust indent as needed
                entry.value.toString().lineSequence().forEach { line ->
                    buf.append(additionalIndent)
                    buf.append(line)
                    buf.append("\n")
                }
            } else {
                buf.append(" ")
                // Proceed with normal serialization for other types
                write(buf, indent + 2, entry.value)
            }
        }
    }

    private fun writeIterator(buf: StringBuilder, indent: Int, iterable: Iterator<*>) {
        iterable.forEach { element ->
            buf.append(" ".repeat(indent))
            buf.append("-")
            if(element is String) {
                if(!element.contains('\n')) {
                    buf.append(" ")
                }
            } else {
                buf.append(" ")
            }
            write(buf, indent + 2, element, isRoot = false)
        }
    }

}
fun String.yamlEscape(): String {
    // Matches YAML special characters and patterns requiring quotes
    val specialChars = listOf(':', '#', '[', ']', '{', '}', ',', '&', '*', '?', '|', '-', '<', '>', '=', '!', '%', '@', '\\')
    val booleanStrings = setOf("true", "false", "yes", "no", "on", "off", "null")

    // Check for conditions requiring quotes
    val needsQuotes = this.any { it in specialChars || it.isWhitespace() } ||
            this.trim() != this ||
            this.toIntOrNull() != null ||
            this.toDoubleOrNull() != null ||
            booleanStrings.contains(this.toLowerCase()) ||
            this.isEmpty()

    return if (needsQuotes) {
        // Apply YAML escaping rules
        "\"" + this
            .replace("\\", "\\\\") // Escape backslashes
            .replace("\"", "\\\"") + "\"" // Escape quotes
    } else {
        this
    }
}
