package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..22 -> "Good Evening"
            else -> "Good Night"
        }
    }

    fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatTime(millis: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun getDateGroupHeader(millis: Long): String {
        val target = Calendar.getInstance().apply { timeInMillis = millis }
        val now = Calendar.getInstance()

        val isSameYear = target.get(Calendar.YEAR) == now.get(Calendar.YEAR)
        val isSameDay = isSameYear && target.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)

        if (isSameDay) return "TODAY"

        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = target.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                target.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)

        if (isYesterday) return "YESTERDAY"

        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        return sdf.format(Date(millis)).uppercase(Locale.getDefault())
    }

    fun getStartAndEndOfMonth(calendar: Calendar = Calendar.getInstance(), startDayOfMonth: Int = 1): Pair<Long, Long> {
        val startCal = calendar.clone() as Calendar
        val day = startDayOfMonth.coerceIn(1, 28)
        
        if (startCal.get(Calendar.DAY_OF_MONTH) < day) {
            startCal.add(Calendar.MONTH, -1)
        }
        startCal.set(Calendar.DAY_OF_MONTH, day)
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)

        val endCal = startCal.clone() as Calendar
        endCal.add(Calendar.MONTH, 1)
        endCal.add(Calendar.MILLISECOND, -1)

        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }

    fun getStartAndEndOfLastMonth(startDayOfMonth: Int = 1): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        return getStartAndEndOfMonth(cal, startDayOfMonth)
    }

    fun getStartAndEndOfWeek(): Pair<Long, Long> {
        val startCal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = startCal.clone() as Calendar
        endCal.add(Calendar.DAY_OF_WEEK, 7)
        endCal.add(Calendar.MILLISECOND, -1)

        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }
}
