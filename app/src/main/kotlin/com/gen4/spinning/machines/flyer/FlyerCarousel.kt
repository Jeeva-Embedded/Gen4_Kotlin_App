package com.gen4.spinning.machines.flyer

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gen4.spinning.ui.theme.SpinColors
import kotlinx.coroutines.delay

private data class FlyerCarouselPage(val label: String, val motorId: UByte)

private val flyerPages = listOf(
    FlyerCarouselPage("Production",   0x0Au),
    FlyerCarouselPage("Flyer",        0x01u),
    FlyerCarouselPage("Bobbin",       0x02u),
    FlyerCarouselPage("Front Roller", 0x03u),
    FlyerCarouselPage("Back Roller",  0x04u),
    FlyerCarouselPage("Lift Left",    0x08u),
    FlyerCarouselPage("Lift Right",   0x09u),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FlyerCarousel(vm: FlyerViewModel) {
    val carouselData by vm.carouselData.collectAsState()
    val pagerState = rememberPagerState(pageCount = { flyerPages.size })

    LaunchedEffect(pagerState.currentPage) {
        while (true) {
            vm.sendCarouselRequest(flyerPages[pagerState.currentPage].motorId)
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
            val entry = flyerPages[page]
            val data = carouselData[entry.motorId] ?: emptyMap()
            FlyerCarouselCard(title = entry.label, isProduction = entry.motorId == 0x0Au.toUByte(), data = data)
        }

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(flyerPages.size) { i ->
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
private fun FlyerCarouselCard(title: String, isProduction: Boolean, data: Map<String, String>) {
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
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            Spacer(Modifier.height(12.dp))

            if (isProduction) {
                FlyerCarouselRow("Output/Spindle", data["outputMtrs"] ?: "–", "m")
                FlyerCarouselRow("Total Power",    data["totalPower"] ?: "–", "W")
            } else {
                FlyerCarouselRow("RPM",      data["rpm"]        ?: "–", "")
                FlyerCarouselRow("Current",  data["current"]    ?: "–", "A")
                FlyerCarouselRow("Motor°C",  data["motorTemp"]  ?: "–", "°C")
                FlyerCarouselRow("MOSFET°C", data["mosfetTemp"] ?: "–", "°C")
            }
        }
    }
}

@Composable
private fun FlyerCarouselRow(label: String, value: String, unit: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp)
        Text(text = if (unit.isNotEmpty()) "$value $unit" else value, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
    }
}
