package com.gen4.spinning.machines.carding

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
import com.gen4.spinning.core.protocol.CardingProtocol
import com.gen4.spinning.ui.theme.SpinColors
import kotlinx.coroutines.delay

private data class CarouselPage(val motorId: UByte, val label: String)

private val pages = listOf(
    CarouselPage(CardingProtocol.CAROUSEL_PRODUCTION,     "Production"),
    CarouselPage(CardingProtocol.MOTOR_CYLINDER,          "Cylinder"),
    CarouselPage(CardingProtocol.MOTOR_BEATER,            "Beater"),
    CarouselPage(CardingProtocol.MOTOR_CAGE,              "Cage"),
    CarouselPage(CardingProtocol.MOTOR_CYLINDER_FEED,     "Cylinder Feed"),
    CarouselPage(CardingProtocol.MOTOR_BEATER_FEED,       "Beater Feed"),
    CarouselPage(CardingProtocol.MOTOR_COILER,            "Coiler"),
    CarouselPage(CardingProtocol.MOTOR_PICKER_CYLINDER,   "Picker Cylinder"),
    CarouselPage(CardingProtocol.MOTOR_AF_FEED,           "AF Feed"),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardingCarousel(vm: CardingViewModel) {
    val carouselData by vm.carouselData.collectAsState()
    val pagerState = rememberPagerState(pageCount = { pages.size })

    LaunchedEffect(pagerState.currentPage) {
        while (true) {
            vm.sendCarouselRequest(pages[pagerState.currentPage].motorId)
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
        ) { index ->
            val page = pages[index]
            val data = carouselData[page.motorId] ?: emptyMap()
            CarouselCard(label = page.label, isProduction = page.motorId == CardingProtocol.CAROUSEL_PRODUCTION, data = data)
        }

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(pages.size) { i ->
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
private fun CarouselCard(label: String, isProduction: Boolean, data: Map<String, String>) {
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
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            Spacer(Modifier.height(12.dp))

            if (isProduction) {
                CarouselRow("Total Length (m)", data["outputMtrs"] ?: "–", "m")
                CarouselRow("Total Power (W)",  data["totalPower"] ?: "–", "W")
            } else {
                CarouselRow("Motor Temp",  data["motorTemp"]  ?: "–", "°C")
                CarouselRow("MOSFET Temp", data["mosfetTemp"] ?: "–", "°C")
                CarouselRow("Current",     data["current"]    ?: "–", "A")
                CarouselRow("RPM",         data["rpm"]        ?: "–", "")
            }
        }
    }
}

@Composable
private fun CarouselRow(label: String, value: String, unit: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp)
        Text(text = if (unit.isNotEmpty()) "$value $unit" else value, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
    }
}
