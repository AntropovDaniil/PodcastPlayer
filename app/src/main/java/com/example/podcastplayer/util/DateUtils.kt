package com.example.podcastplayer.util

import android.util.Log
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    fun jsonDateToShortDate(jsonDate: String?): String {
        if (jsonDate == null) {
            return "_"
        }

        val inFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = inFormat.parse(jsonDate) ?: return "-"
        val outputFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())

        return outputFormat.format(date)
    }

    fun xmlDateToDate(date: String?): Date {
        val date = date ?: return Date()
        val inFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z")
        //return inFormat.parse(date)
        return Date()
    }

    fun dateToShortDate(date: Date): String{
        val outputFormat = DateFormat.getDateInstance(
            DateFormat.SHORT, Locale.getDefault()
        )
        return outputFormat.format(date)
    }
}