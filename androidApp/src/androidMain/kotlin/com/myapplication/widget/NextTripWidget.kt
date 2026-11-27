package com.myapplication.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import pe.edu.tecsup.gowayki.domain.widget.NextTripWidgetModel

class NextTripWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val model = NextTripWidgetStore.read(context)
        provideContent {
            GlanceTheme {
                NextTripContent(model)
            }
        }
    }
}

@Composable
private fun NextTripContent(model: NextTripWidgetModel?) {
    Column(
        modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.widgetBackground).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("gowayki go", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium))
        if (model == null) {
            Text("Guarda un favorito para verlo aquí", style = TextStyle(fontSize = 14.sp))
        } else {
            Text("${model.originName} → ${model.destinationName}", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold))
            model.etaLabel?.let { Text(it, style = TextStyle(fontSize = 12.sp)) }
        }
    }
}
