package com.congdong4g.vpn.utils

import java.text.SimpleDateFormat
import java.util.*

object Utils {

    // Format bytes to human readable
    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.2f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.2f MB", mb)
        val gb = mb / 1024.0
        return String.format("%.2f GB", gb)
    }

    // Format speed
    fun formatSpeed(bytesPerSecond: Long): String {
        if (bytesPerSecond < 1024) return "$bytesPerSecond B/s"
        val kb = bytesPerSecond / 1024.0
        if (kb < 1024) return String.format("%.1f KB/s", kb)
        val mb = kb / 1024.0
        return String.format("%.2f MB/s", mb)
    }

    // Format duration
    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, secs)
            else -> String.format("%02d:%02d", minutes, secs)
        }
    }

    // Format timestamp to date
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp * 1000))
    }

    // Format expiry date
    fun formatExpiry(timestamp: Long?): String {
        if (timestamp == null) return "Không giới hạn"
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp * 1000))
    }

    // Check if expired
    fun isExpired(timestamp: Long?): Boolean {
        if (timestamp == null) return false
        return System.currentTimeMillis() / 1000 > timestamp
    }

    // Format price
    fun formatPrice(amount: Int): String {
        return String.format("%,d đ", amount)
    }

    // Calculate remaining data percentage
    fun calculateDataPercentage(used: Long, total: Long): Int {
        if (total == 0L) return 0
        return ((used.toDouble() / total) * 100).toInt().coerceIn(0, 100)
    }

    // Get remaining days
    fun getRemainingDays(timestamp: Long?): Int {
        if (timestamp == null) return -1
        val now = System.currentTimeMillis() / 1000
        if (timestamp <= now) return 0
        return ((timestamp - now) / 86400).toInt()
    }

    // Get flag emoji from country code
    fun getCountryFlag(name: String): String {
        return when {
            name.contains("Việt Nam", ignoreCase = true) || name.contains("VN", ignoreCase = true) -> "🇻🇳"
            name.contains("Singapore", ignoreCase = true) || name.contains("SG", ignoreCase = true) -> "🇸🇬"
            name.contains("Hong Kong", ignoreCase = true) || name.contains("HK", ignoreCase = true) -> "🇭🇰"
            name.contains("Japan", ignoreCase = true) || name.contains("JP", ignoreCase = true) -> "🇯🇵"
            name.contains("Korea", ignoreCase = true) || name.contains("KR", ignoreCase = true) -> "🇰🇷"
            name.contains("Taiwan", ignoreCase = true) || name.contains("TW", ignoreCase = true) -> "🇹🇼"
            name.contains("US", ignoreCase = true) || name.contains("United States", ignoreCase = true) -> "🇺🇸"
            name.contains("UK", ignoreCase = true) || name.contains("United Kingdom", ignoreCase = true) -> "🇬🇧"
            name.contains("Germany", ignoreCase = true) || name.contains("DE", ignoreCase = true) -> "🇩🇪"
            name.contains("France", ignoreCase = true) || name.contains("FR", ignoreCase = true) -> "🇫🇷"
            name.contains("Australia", ignoreCase = true) || name.contains("AU", ignoreCase = true) -> "🇦🇺"
            name.contains("China", ignoreCase = true) || name.contains("CN", ignoreCase = true) -> "🇨🇳"
            name.contains("Thailand", ignoreCase = true) || name.contains("TH", ignoreCase = true) -> "🇹🇭"
            name.contains("Malaysia", ignoreCase = true) || name.contains("MY", ignoreCase = true) -> "🇲🇾"
            name.contains("Indonesia", ignoreCase = true) || name.contains("ID", ignoreCase = true) -> "🇮🇩"
            name.contains("India", ignoreCase = true) || name.contains("IN", ignoreCase = true) -> "🇮🇳"
            else -> "🌐"
        }
    }
}
