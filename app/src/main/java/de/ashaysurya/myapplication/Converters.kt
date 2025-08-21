// File: Converters.kt
package de.ashaysurya.myapplication

import androidx.room.TypeConverter
import java.util.Date

class Converters {
    // For converting Date <-> Long
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // For converting PaymentMethod <-> String
    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String {
        return value.name // Stores the enum as a string, e.g., "CASH"
    }

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod {
        return PaymentMethod.valueOf(value) // Converts the string back to the enum
    }
}