package com.ticklog.data.database.converter

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Room type converters bridging `java.time` types and the primitive columns Room
 * can persist.
 *
 * Storage choices are deliberate and stable:
 *  - [LocalDate] is stored as its **epoch day** (days since 1970-01-01). This is
 *    timezone-independent, sorts correctly and indexes efficiently — ideal for a
 *    calendar app keyed by calendar date.
 *  - [Instant] is stored as **epoch milliseconds** for precise audit timestamps
 *    (created/updated/completed).
 *  - [LocalTime] is stored as its **second of day** for reminder times.
 *
 * `null` round-trips to `null` so nullable columns work transparently.
 */
class Converters {

    // --- LocalDate <-> epoch day -------------------------------------------
    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long?): LocalDate? =
        epochDay?.let(LocalDate::ofEpochDay)

    // --- Instant <-> epoch millis ------------------------------------------
    @TypeConverter
    fun instantToEpochMillis(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(epochMillis: Long?): Instant? =
        epochMillis?.let(Instant::ofEpochMilli)

    // --- LocalTime <-> second of day ---------------------------------------
    @TypeConverter
    fun localTimeToSecondOfDay(time: LocalTime?): Int? = time?.toSecondOfDay()

    @TypeConverter
    fun secondOfDayToLocalTime(secondOfDay: Int?): LocalTime? =
        secondOfDay?.let { LocalTime.ofSecondOfDay(it.toLong()) }
}
