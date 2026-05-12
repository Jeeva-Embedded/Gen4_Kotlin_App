package com.gen4.spinning.machines.df

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gen4.spinning.ui.components.StatusBox
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
    val pagerState = rememberPagerState(pageCount = { dfPages.size })

    LaunchedEffect(pagerState.currentPage) {
        while (true) {
            vm.sendCarouselRequest(dfPages[pagerState.currentPage].motorId)
            delay(2_000)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val entry = dfPages[page]
            val data = carouselData[entry.motorId] ?: emptyMap()
            DfCarouselCard(title = entry.label, data = data, isProduction = entry.motorId == 0x0Au.toUByte())
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
private fun DfCarouselCard(title: String, data: Map<String, String>, isProduction: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF5F5F5),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            if (isProduction) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBox(label = "Output (m)",       value = data["outputMtrs"]  ?: "-", modifier = Modifier.weight(1f))
                    StatusBox(label = "Total Power (W)",  value = data["totalPower"]  ?: "-", modifier = Modifier.weight(1f))
                }
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
        }
    }
}
