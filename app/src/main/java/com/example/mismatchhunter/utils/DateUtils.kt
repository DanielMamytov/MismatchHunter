package com.example.mismatchhunter.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("ru"))

    fun epochDayNow(): Long = LocalDate.now().toEpochDay()

    fun todayIso(): String = LocalDate.now().toString()

    fun parseIsoToEpochDay(value: String): Long? = runCatching { LocalDate.parse(value).toEpochDay() }.getOrNull()

    fun formatEpochDay(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dateFormatter)

    fun formatMillis(millis: Long): String = Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale("ru")))
}
