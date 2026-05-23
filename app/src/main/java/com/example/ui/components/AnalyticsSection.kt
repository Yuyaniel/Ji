package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertChartOutlined
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CheckIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsSection(
    checkIns: List<CheckIn>,
    modifier: Modifier = Modifier,
    isFloatingBarEnabled: Boolean = false
) {
    val calendar = remember { Calendar.getInstance() }
    val currentYear = remember { calendar.get(Calendar.YEAR) }
    val currentMonth = remember { calendar.get(Calendar.MONTH) } // 0-11
    val currentWeekOfYear = remember { calendar.get(Calendar.WEEK_OF_YEAR) }

    // Computations
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    // 1. Weekly Stats calculations
    val weeklyDaysChecked = remember(checkIns) {
        val weekCal = Calendar.getInstance()
        // Find Monday of the current week
        weekCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val weekDates = (0..6).map { idx ->
            val temp = weekCal.clone() as Calendar
            temp.add(Calendar.DAY_OF_YEAR, idx)
            String.format(Locale.US, "%04d-%02d-%02d", 
                temp.get(Calendar.YEAR), 
                temp.get(Calendar.MONTH) + 1, 
                temp.get(Calendar.DAY_OF_MONTH)
            )
        }

        val checkedMap = checkIns.associateBy { it.date }
        weekDates.map { date ->
            val isChecked = checkedMap.containsKey(date)
            val note = checkedMap[date]?.note ?: ""
            Pair(date, isChecked)
        }
    }
    val weeklyCount = weeklyDaysChecked.filter { it.second }.size

    // 2. Monthly Stats calculations
    val monthlyTotalDays = remember(currentYear, currentMonth) {
        val temp = Calendar.getInstance()
        temp.set(Calendar.YEAR, currentYear)
        temp.set(Calendar.MONTH, currentMonth)
        temp.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    val thisMonthPrefix = remember(currentYear, currentMonth) {
        String.format(Locale.US, "%04d-%02d", currentYear, currentMonth + 1)
    }
    val checkInsThisMonth = remember(checkIns, thisMonthPrefix) {
        checkIns.filter { it.date.startsWith(thisMonthPrefix) }
    }
    val monthlyCount = checkInsThisMonth.size
    val monthlyPercentage = if (monthlyTotalDays > 0) {
        (monthlyCount.toFloat() / monthlyTotalDays.toFloat() * 100).toInt()
    } else 0

    // 3. Annual Summary Calculations
    val annualPrefix = remember(currentYear) { currentYear.toString() }
    val annualCheckIns = remember(checkIns, annualPrefix) {
        checkIns.filter { it.date.startsWith(annualPrefix) }
    }
    val annualCount = annualCheckIns.size

    // Monthly distribution for Annual summary
    val monthlyDistribution = remember(annualCheckIns) {
        val distribution = IntArray(12) { 0 }
        annualCheckIns.forEach { item ->
            try {
                // "yyyy-MM-dd"
                val monthPart = item.date.substring(5, 7).toIntOrNull()
                if (monthPart != null && monthPart in 1..12) {
                    distribution[monthPart - 1]++
                }
            } catch (e: Exception) {
                // Ignore parsing issues
            }
        }
        distribution
    }

    val bestMonthIndex = remember(monthlyDistribution) {
        var maxIndex = 0
        var maxValue = -1
        for (i in 0..11) {
            if (monthlyDistribution[i] > maxValue) {
                maxValue = monthlyDistribution[i]
                maxIndex = i
            }
        }
        if (maxValue > 0) maxIndex else -1
    }

    val monthNames = listOf("一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        // Tab Heading Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.InsertChartOutlined,
                            contentDescription = "统计",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "打卡统计与年度分析",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "洞悉您坚持不懈的日常习惯与年度打卡分析",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section 1: Weekly Statistics
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("weekly_stats_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "每周打卡",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "本周周打卡",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = "$weeklyCount / 7 天",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { weeklyCount.toFloat() / 7f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Weekly Grid representation Monday to Sunday
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val daysLabel = listOf("一", "二", "三", "四", "五", "六", "日")
                        weeklyDaysChecked.forEachIndexed { index, pair ->
                            val isChecked = pair.second
                            val dayLabel = daysLabel[index]

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isChecked) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isChecked) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "已打卡",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Text(
                                            text = dayLabel,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                if (isChecked) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = dayLabel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Monthly Statistics
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("monthly_stats_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "每月打卡",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${currentMonth + 1}月 打卡月度报告",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = "$monthlyPercentage%",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Progress ring or custom elegant stat column
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$monthlyCount",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "已完结天数",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        // Text reports
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "本月至今已打卡 $monthlyCount 天，还剩 ${monthlyTotalDays - monthlyCount} 天未标记。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            val statusMsg = when {
                                monthlyPercentage >= 80 -> "✨ 极为出色！您在本月展示了极高的自律性。"
                                monthlyPercentage >= 50 -> "💪 表现亮眼！请继续维持这份积极的势头。"
                                monthlyPercentage > 0 -> "🌱 正在播种！每天已在脚下积累习惯。"
                                else -> "🏁 还没开始！点击日历，留下本月的第一份印迹吧。"
                            }
                            Text(
                                text = statusMsg,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Annual Analysis (年度总结 - 纯数据分析)
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("annual_analysis_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "年度总结数据",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${currentYear}年度 迹之足迹分析",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dynamic summary stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "年度总计",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$annualCount 次",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "最活跃月份",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (bestMonthIndex >= 0) monthNames[bestMonthIndex] else "暂无数据",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        // Avoid overlap with the bottom navigation bar by adding a bottom Spacer
        if (isFloatingBarEnabled) {
            item {
                Spacer(modifier = Modifier.height(90.dp))
            }
        } else {
            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
