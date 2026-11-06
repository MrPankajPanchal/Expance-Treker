package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppCurrency
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.CurrencyFormatter

data class SpendingBarData(
    val label: String,
    val amountCents: Long
)

@Composable
fun MonthlySpendingBarChart(
    data: List<SpendingBarData>,
    currency: AppCurrency,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty() || data.all { it.amountCents == 0L }) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No spending activity recorded",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxAmount = (data.maxOfOrNull { it.amountCents } ?: 1L).coerceAtLeast(1L)
    val barColor = MaterialTheme.colorScheme.primary

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(600))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { item ->
                val heightFraction = (item.amountCents.toFloat() / maxAmount.toFloat()) * animProgress.value
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(110.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // Background track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(6.dp)
                                )
                        )
                        // Active bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(heightFraction.coerceIn(0.04f, 1f))
                                .background(
                                    barColor,
                                    RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun IncomeVsExpenseComparison(
    incomeCents: Long,
    expenseCents: Long,
    currency: AppCurrency,
    modifier: Modifier = Modifier
) {
    val total = (incomeCents + expenseCents).coerceAtLeast(1L)
    val incomeFraction = incomeCents.toFloat() / total.toFloat()
    val expenseFraction = expenseCents.toFloat() / total.toFloat()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(IncomeGreen, CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Income: ${CurrencyFormatter.formatCents(incomeCents, currency)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(ExpenseRed, CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Expenses: ${CurrencyFormatter.formatCents(expenseCents, currency)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Multi-segment progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(7.dp))
        ) {
            Row(modifier = Modifier.fillMaxWidth().height(14.dp)) {
                if (incomeCents > 0L) {
                    Box(
                        modifier = Modifier
                            .weight(incomeFraction)
                            .fillMaxHeight()
                            .background(
                                IncomeGreen,
                                RoundedCornerShape(
                                    topStart = 7.dp,
                                    bottomStart = 7.dp,
                                    topEnd = if (expenseCents == 0L) 7.dp else 0.dp,
                                    bottomEnd = if (expenseCents == 0L) 7.dp else 0.dp
                                )
                            )
                    )
                }
                if (expenseCents > 0L) {
                    Box(
                        modifier = Modifier
                            .weight(expenseFraction)
                            .fillMaxHeight()
                            .background(
                                ExpenseRed,
                                RoundedCornerShape(
                                    topStart = if (incomeCents == 0L) 7.dp else 0.dp,
                                    bottomStart = if (incomeCents == 0L) 7.dp else 0.dp,
                                    topEnd = 7.dp,
                                    bottomEnd = 7.dp
                                )
                            )
                    )
                }
            }
        }
    }
}
