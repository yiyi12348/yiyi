/**
 * @author cwuom
 * ctime: 2024.10.21
 */

package com.cwuom.ouo.helper

import android.util.Log
import com.cwuom.ouo.service.QQInterfaces
import com.google.protobuf.ByteString
import com.google.protobuf.CodedOutputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.putJsonObject
import java.io.ByteArrayOutputStream
import kotlin.random.Random
import kotlin.random.nextUInt


/**
 * Sends a message by constructing a JSON payload, encoding it to Protobuf, and sending it via QQInterfaces.
 *
 * @param content The content of the message.
 * @param id The identifier for the message.
 * @param isGroupMsg Indicates if the message is a group message.
 * @param type The type of the message, e.g., "element".
 */
@OptIn(ExperimentalSerializationApi::class)
fun sendMessage(content: String, id: String, isGroupMsg: Boolean, type: String) {
    val TAG = "ouom!"
    val json = Json { ignoreUnknownKeys = true }

    try {
        var basePbContent = buildBasePbContent(id, isGroupMsg)

        when (type){
            "element" -> {
                Log.d("$TAG!pbcontent", content)
                val jsonElement = json.decodeFromString<JsonElement>(content)
                val updatedElements = when (jsonElement) {
                    is JsonArray -> jsonElement.filterIsInstance<JsonObject>()
                    is JsonObject -> listOf(jsonElement)
                    else -> {
                        Log.e("$TAG!err", "Invalid JSON!")
                        return
                    }
                }

                val jsonArray = buildJsonArray {
                    updatedElements.forEach { element ->
                        add(element)
                        Log.d("$TAG!elem:", element.toString())
                    }
                }

                basePbContent = buildJsonObject {
                    basePbContent.forEach { (key, value) ->
                        when (key) {
                            "3" -> {
                                // Navigate to ["3"]["1"]
                                val path1 = value.jsonObject["1"]?.jsonObject?.toMutableMap() ?: mutableMapOf<String, JsonElement>()
                                // Set ["3"]["1"]["2"] to the new JsonArray
                                path1["2"] = jsonArray
                                // Put the modified nested object back
                                put("3", buildJsonObject {
                                    put("1", JsonObject(path1))
                                })
                            }
                            else -> {
                                // Retain other keys as is
                                put(key, value)
                            }
                        }
                    }
                }
            }
            else -> {
                throw IllegalArgumentException("Unsupported content type '$type'")
            }
        }

        basePbContent = buildJsonObject {
            basePbContent.forEach { (key, value) ->
                put(key, value)
            }
            put("4", JsonPrimitive(Random.nextUInt()))
            put("5", JsonPrimitive(Random.nextUInt()))
        }

        val basePbContentString = json.encodeToString(basePbContent)
        Log.d("$TAG!pbcontent", "basePbContent = $basePbContentString")
        val parsedJsonElement: JsonElement = basePbContent
        val map = parseJsonToMap(parsedJsonElement)
        Log.d("$TAG!debug", "Parsed JSON to Map: $map")
        val byteArray = encodeMessage(map)

        QQInterfaces.sendBuffer("MessageSvc.PbSendMsg", true, byteArray)
    } catch (e: Exception) {
        Log.e("$TAG!err", "sendMessage failed: ${e.message}", e)
    }
}

/**
 * Builds the base JSON content based on whether the message is a group message.
 *
 * @param id The identifier for the message.
 * @param isGroupMsg Indicates if the message is a group message.
 * @return The base JsonObject.
 */
fun buildBasePbContent(id: String, isGroupMsg: Boolean): JsonObject = buildJsonObject {
    putJsonObject("1") {
        if (isGroupMsg) {
            val idLong = id.toLongOrNull() ?: throw IllegalArgumentException("id must be Long for group messages")
            putJsonObject("2") {
                put("1", JsonPrimitive(idLong))
            }
        } else {
            putJsonObject("1") {
                put("2", JsonPrimitive(id))
            }
        }
    }
    putJsonObject("2") {
        put("1", JsonPrimitive(1))
        put("2", JsonPrimitive(0))
        put("3", JsonPrimitive(0))
    }
    putJsonObject("3") {
        putJsonObject("1") {
            put("2", buildJsonArray { /* Initialize as needed */ })
        }
    }
}

/**
 * Encodes a map into a Protobuf byte array.
 *
 * @param obj The map to encode.
 * @return The encoded byte array.
 */
fun encodeMessage(obj: Map<Int, Any>): ByteArray {
    ByteArrayOutputStream().use { baos ->
        val output = CodedOutputStream.newInstance(baos)
        encodeMapToProtobuf(output, obj)
        output.flush()
        return baos.toByteArray()
    }
}

/**
 * Recursively encodes a map into Protobuf format.
 *
 * @param output The CodedOutputStream to write to.
 * @param obj The map to encode.
 */
fun encodeMapToProtobuf(output: CodedOutputStream, obj: Map<Int, Any>) {
    obj.forEach { (tag, value) ->
        when (value) {
            is Int -> output.writeInt32(tag, value)
            is Long -> output.writeInt64(tag, value)
            is String -> output.writeString(tag, value)
            is ByteArray -> output.writeBytes(tag, ByteString.copyFrom(value))
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val nestedMessage = encodeMessage(value as Map<Int, Any>)
                output.writeTag(tag, com.google.protobuf.WireFormat.WIRETYPE_LENGTH_DELIMITED)
                output.writeUInt32NoTag(nestedMessage.size)
                output.writeRawBytes(nestedMessage)
            }
            is List<*> -> {
                value.forEach { item ->
                    if (item is Map<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        val nestedMessage = encodeMessage(item as Map<Int, Any>)
                        output.writeTag(tag, com.google.protobuf.WireFormat.WIRETYPE_LENGTH_DELIMITED)
                        output.writeUInt32NoTag(nestedMessage.size)
                        output.writeRawBytes(nestedMessage)
                    } else {
                        throw IllegalArgumentException("Unsupported list item type: ${item?.javaClass}")
                    }
                }
            }
            else -> {
                throw IllegalArgumentException("Unsupported type: ${value.javaClass}")
            }
        }
    }
}

/**
 * Parses a JsonElement into a Map<Int, Any>.
 * The key is mapped to 2 only when the path is ['3', '1', '2'].
 *
 * @param jsonElement The JsonElement to parse.
 * @param path The current path in the JSON structure.
 * @return The resulting map.
 */
fun parseJsonToMap(jsonElement: JsonElement, path: List<String> = emptyList()): Map<Int, Any> {
    val resultMap = mutableMapOf<Int, Any>()
    when (jsonElement) {
        is JsonObject -> {
            for ((key, value) in jsonElement) {
                val intKey = key.toIntOrNull()
                if (intKey != null) {
                    val currentPath = path + key
                    // The key is mapped to 2 only when the path is ['3', '1', '2']
                    val mappedKey = if (currentPath == listOf("3", "1", "2")) 2 else intKey
                    when (value) {
                        is JsonObject -> {
                            resultMap[mappedKey] = parseJsonToMap(value, currentPath)
                        }
                        is JsonArray -> {
                            val list = value.map { parseJsonToMap(it, currentPath) }
                            resultMap[mappedKey] = list
                        }
                        is JsonPrimitive -> {
                            when {
                                value.isString -> resultMap[mappedKey] = value.content
                                value.intOrNull != null -> resultMap[mappedKey] = value.int
                                value.longOrNull != null -> resultMap[mappedKey] = value.long
                                else -> resultMap[mappedKey] = value.content
                            }
                        }
                        else -> throw IllegalArgumentException("Unsupported JSON element: $value")
                    }
                } else {
                    throw IllegalArgumentException("Key is not a valid integer: $key")
                }
            }
        }
        is JsonArray -> {
            jsonElement.forEachIndexed { index, element ->
                val parsedMap = parseJsonToMap(element, path + (index + 1).toString())
                resultMap[index + 1] = parsedMap
            }
        }
        else -> throw IllegalArgumentException("Unsupported JSON element: $jsonElement")
    }
    return resultMap
}