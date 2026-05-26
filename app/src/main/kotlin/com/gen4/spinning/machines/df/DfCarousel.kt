package com.gen4.spinning.machines.df

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gen4.spinning.ui.theme.SpinColors
import kotlinx.coroutines.delay

private data class DfCarouselPage(val label: String, val motorId: UByte)

private val dfPages = listOf(
    DfCarouselPage("Production",   0x0Au),
    DfCarouselPage("Front Roller", 0x01u),
    DfCarouselPage("Back Roller",  0x02u),
    DfCarouselPage("Creel",        0x03u),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DfCarousel(vm: DfViewModel) {
    val carouselData by vm.carouselData.collectAsState()
    val runState by vm.runState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { dfPages.size })

    LaunchedEffect(pagerState.currentPage) {
        while (true) {
            vm.sendCarouselRequest(dfPages[pagerState.currentPage].motorId)
            delay(2_000)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { page ->
            val entry = dfPages[page]
            val data = carouselData[entry.motorId] ?: emptyMap()
            DfCarouselCard(
                title = entry.label,
                data = data,
                isProduction = entry.motorId == 0x0Au.toUByte(),
                alSensorActive = runState.alSensorActive,
                currentLengthM = runState.currentLength,
                deliveryMtrsPerMin = runState.deliveryMtrsPerMin,
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(dfPages.size) { i ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (pagerState.currentPage == i) SpinColors.LightGreen else Color.LightGray)
                )
            }
        }
    }
}

@Composable
private fun DfCarouselCard(
    title: String,
    data: Map<String, String>,
    isProduction: Boolean = false,
    alSensorActive: Boolean = false,
    currentLengthM: Float = 0f,
    deliveryMtrsPerMin: Float = 0f,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SpinColors.Blue),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(Modifier.height(12.dp))
            if (isProduction) {
                DfCarouselRow("Total Length (m)", "%.1f".format(currentLengthM))
                DfCarouselRow("Total Power (W)",  data["totalPower"] ?: "-")
                DfCarouselRow("Delivery (m/min)", if (deliveryMtrsPerMin > 0f) "%.1f".format(deliveryMtrsPerMin) else "-")
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("AL Sensor", color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (alSensorActive) SpinColors.LightGreen else Color.Red)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (alSensorActive) "ON" else "OFF",
                            color = if (alSensorActive) SpinColors.LightGreen else Color.Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }
                }
            } else {
                DfCarouselRow("RPM",      data["rpm"]        ?: "-")
                DfCarouselRow("Current",  data["current"]    ?: "-")
                DfCarouselRow("Motor°C",  data["motorTemp"]  ?: "-")
                DfCarouselRow("MOSFET°C", data["mosfetTemp"] ?: "-")
            }
        }
    }
}

@Composable
private fun DfCarouselRow(label: String, value: String, valueColor: Color = Color.White) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp)
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = valueColor)
    }
}
