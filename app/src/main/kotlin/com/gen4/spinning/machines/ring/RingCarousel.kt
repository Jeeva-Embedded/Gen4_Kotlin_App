package com.gen4.spinning.machines.ring

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gen4.spinning.ui.components.StatusBox
import com.gen4.spinning.ui.theme.SpinColors
import kotlinx.coroutines.flow.distinctUntilChanged

private data class RingCarouselPage(val label: String, val motorId: UByte)

private val ringPages = listOf(
    RingCarouselPage("Production", 0x0Au),
    RingCarouselPage("Calender",   0x01u),
    RingCarouselPage("Lift Left",  0x08u),
    RingCarouselPage("Lift Right", 0x09u),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RingCarousel(vm: RingViewModel) {
    val carouselData by vm.carouselData.collectAsState()
    val pagerState = rememberPagerState(pageCount = { ringPages.size })

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.distinctUntilChanged().collect { index ->
            vm.sendCarouselRequest(ringPages[index].motorId)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        ) { page ->
            val entry = ringPages[page]
            val data = carouselData[entry.motorId] ?: emptyMap()
            RingCarouselCard(title = entry.label, isProduction = entry.motorId == 0x0Au.toUByte(), data = data)
        }

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(ringPages.size) { i ->
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
private fun RingCarouselCard(title: String, isProduction: Boolean, data: Map<String, String>) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SpinColors.Blue,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
            Spacer(Modifier.height(12.dp))

            if (isProduction) {
                CarouselRow("meters/Spindle", data["outputMtrs"] ?: "–", "m")
                CarouselRow("Total Power",    data["totalPower"] ?: "–", "W")
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBox(label = "RPM",        value = data["rpm"]        ?: "-", modifier = Modifier.weight(1f))
                    StatusBox(label = "Current(A)", value = data["current"]    ?: "-", modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBox(label = "Motor°C",    value = data["motorTemp"]  ?: "-", modifier = Modifier.weight(1f))
                    StatusBox(label = "MOSFET°C",   value = data["mosfetTemp"] ?: "-", modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(76.dp))
        }
    }
}

@Composable
private fun CarouselRow(label: String, value: String, unit: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(text = if (unit.isNotEmpty()) "$value $unit" else value, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
    }
}
