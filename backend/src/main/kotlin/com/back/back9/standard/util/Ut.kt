package com.back.back9.standard.util

import com.fasterxml.jackson.databind.ObjectMapper

object Ut {
    object json {
        val objectMapper: ObjectMapper = ObjectMapper()

        fun toString(obj: Any?): String? = toString(obj, null)

        fun toString(obj: Any?, defaultValue: String?): String? {
            return try {
                objectMapper.writeValueAsString(obj)
            } catch (_: Exception) {
                defaultValue
            }
        }
    }
}