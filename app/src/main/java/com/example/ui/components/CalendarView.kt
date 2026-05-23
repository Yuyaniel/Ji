package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CheckIn
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarView(
    year: Int,
    month: Int, // 0-11
    checkIns: List<CheckIn>,
    onDayClick: (String) -> Unit, // Callback for empty days -> retroactive confirmation
    onDayLongClick: (String) -> Unit, // Callback for checked days -> delete confirmation
    modifier: Modifier = Modifier,
    onNavigateMonth: (Int) -> Unit,
    onMonthYearSelect: (Int, Int) -> Unit = { _, _ -> }
) {
    val calendar = remember(year, month) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    
    // Adjust so that Monday is 0, Tuesday is 1, ..., Sunday is 6
    val startingOffset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

    // Month details formatted
    val monthLabel = remember(year, month) {
        String.format(Locale.CHINA, "%d年 %d月", year, month + 1)
    }

    val todayStr = remember {
        val today = Calendar.getInstance()
        String.format(Locale.US, "%04d-%02d-%02d", 
            today.get(Calendar.YEAR), 
            today.get(Calendar.MONTH) + 1, 
            today.get(Calendar.DAY_OF_MONTH)
        )
    }

    // Interactive Month/Year Select Dialog State
    var showMonthYearPicker by remember { mutableStateOf(false) }

    if (showMonthYearPicker) {
        var tempSelectedYear by remember { mutableStateOf(year) }
        var tempSelectedMonth by remember { mutableStateOf(month) }

        AlertDialog(
            onDismissRequest = { showMonthYearPicker = false },
            title = {
                Text(
                    text = "切换日历年月",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Year selector Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { tempSelectedYear-- }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "上一年"
                            )
                        }
                        Text(
                            text = "${tempSelectedYear}年",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { tempSelectedYear++ }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "下一年"
                            )
                        }
                    }

                    // 12 Months 3x4 Grid
                    val monthGrid = (0..11).chunked(3)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        monthGrid.forEach { rowMonths ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowMonths.forEach { mIdx ->
                                    val isSelected = tempSelectedMonth == mIdx
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            )
                                            .clickable { tempSelectedMonth = mIdx }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${mIdx + 1}月",
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onMonthYearSelect(tempSelectedYear, tempSelectedMonth)
                        showMonthYearPicker = false
                    }
                ) {
                    Text("确定", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMonthYearPicker = false }) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Calendar Navigator Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onNavigateMonth(-1) },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_pref_month")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "上个月",
                        modifier = Modifier.size(28.dp)
                    )
                }

                AnimatedContent(
                    targetState = monthLabel,
                    transitionSpec = {
                        val isNext = targetState > initialState
                        if (isNext) {
                            (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                                     (slideOutHorizontally { width -> -width } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()) togetherWith
                                     (slideOutHorizontally { width -> width } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    },
                    label = "MonthLabelAnimation"
                ) { targetLabel ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showMonthYearPicker = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = targetLabel,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "选择年份月份",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { onNavigateMonth(1) },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_next_month")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "下个月",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Day of weeks Headers
            val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekdays.forEach { weekday ->
                    Text(
                        text = weekday,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (weekday == "六" || weekday == "日") {
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Days Grid Content
            val totalCells = 42 // 6 rows of 7 days
            val checkedInDatesMap = remember(checkIns) {
                checkIns.associateBy { it.date }
            }

            // Chunk days into rows
            val rows = (0 until totalCells).chunked(7)
            
            rows.forEach { rowIndices ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    rowIndices.forEach { cellIndex ->
                        val dayNumber = cellIndex - startingOffset + 1
                        val isValidDay = dayNumber in 1..maxDays

                        val dateStr = if (isValidDay) {
                            String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayNumber)
                        } else ""

                        val checkIn = checkedInDatesMap[dateStr]
                        val isChecked = checkIn != null
                        val isToday = dateStr == todayStr

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isValidDay) {
                                val backgroundColor = when {
                                    isChecked && checkIn?.type == "MAKE_UP" -> MaterialTheme.colorScheme.tertiaryContainer
                                    isChecked -> MaterialTheme.colorScheme.primaryContainer
                                    isToday -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> Color.Transparent
                                }

                                val contentColor = when {
                                    isChecked && checkIn?.type == "MAKE_UP" -> MaterialTheme.colorScheme.onTertiaryContainer
                                    isChecked -> MaterialTheme.colorScheme.onPrimaryContainer
                                    isToday -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }

                                val containerModifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(backgroundColor)
                                    .combinedClickable(
                                        onClick = {
                                            // Click behavior: if checked -> does nothing or show toast, if unchecked -> prompt retroactive 补签
                                            if (!isChecked) {
                                                onDayClick(dateStr)
                                            } else {
                                                // Inform they can long press to delete
                                                onDayLongClick(dateStr)
                                            }
                                        },
                                        onLongClick = {
                                            if (isChecked) {
                                                onDayLongClick(dateStr)
                                            }
                                        }
                                    )
                                    .testTag("day_$dateStr")

                                Box(
                                    modifier = containerModifier,
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isChecked || isToday) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 14.sp
                                            ),
                                            color = contentColor
                                        )
                                        
                                        if (isChecked) {
                                            if (checkIn?.type == "MAKE_UP") {
                                                // Icon or indicator for retro/makeup
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = "已打卡",
                                                    tint = MaterialTheme.colorScheme.tertiary,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            } else {
                                                // Small dot or star
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = "已打卡",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
