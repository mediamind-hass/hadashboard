package com.example.myapplication.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.myapplication.data.HaPreferences
import com.example.myapplication.data.HomeAssistantApi

class HaCompositeWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = HaPreferences(context)
        val api = HomeAssistantApi(prefs, context)

        var cameraBitmap: Bitmap? = null
        var s1Val = prefs.cachedSensor1Val
        var s2Val = prefs.cachedSensor2Val
        var s3Val = prefs.cachedSensor3Val

        var b1On = prefs.cachedBtn1On
        var b2On = prefs.cachedBtn2On
        var b3On = prefs.cachedBtn3On
        var b4On = prefs.cachedBtn4On

        if (prefs.isConfigured) {
            try {
                // Try fetching fresh states via OkHttp in background. If One UI 9 restricts background socket, catch and use cache.
                val allStatesMap = api.fetchAllStatesMap()

                if (allStatesMap.isNotEmpty()) {
                    val s1Res = allStatesMap[prefs.sensor1Entity]
                    if (s1Res != null) {
                        s1Val = formatSensorVal(s1Res.state, prefs.sensor1Unit)
                        prefs.cachedSensor1Val = s1Val
                    }

                    val s2Res = allStatesMap[prefs.sensor2Entity]
                    if (s2Res != null) {
                        s2Val = formatSensorVal(s2Res.state, prefs.sensor2Unit)
                        prefs.cachedSensor2Val = s2Val
                    }

                    val s3Res = allStatesMap[prefs.sensor3Entity]
                    if (s3Res != null) {
                        s3Val = formatSensorVal(s3Res.state, prefs.sensor3Unit)
                        prefs.cachedSensor3Val = s3Val
                    }

                    val b1Res = allStatesMap[prefs.button1Entity]
                    if (b1Res != null) {
                        b1On = isEntityOn(b1Res.state)
                        prefs.cachedBtn1On = b1On
                    }

                    val b2Res = allStatesMap[prefs.button2Entity]
                    if (b2Res != null) {
                        b2On = isEntityOn(b2Res.state)
                        prefs.cachedBtn2On = b2On
                    }

                    val b3Res = allStatesMap[prefs.button3Entity]
                    if (b3Res != null) {
                        b3On = isEntityOn(b3Res.state)
                        prefs.cachedBtn3On = b3On
                    }

                    val b4Res = allStatesMap[prefs.button4Entity]
                    if (b4Res != null) {
                        b4On = isEntityOn(b4Res.state)
                        prefs.cachedBtn4On = b4On
                    }
                }

                // Try fetching camera snapshot
                cameraBitmap = try { api.fetchCameraSnapshot(prefs.cameraEntity) } catch (_: Exception) { null }
                if (cameraBitmap != null) {
                    try {
                        val cacheFile = java.io.File(context.cacheDir, "camera_cache.jpg")
                        cacheFile.outputStream().use { out ->
                            cameraBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {
                // Background network restricted -> fall back to cache values already loaded above
            }

            // Fallback to cache file for camera if null
            if (cameraBitmap == null) {
                try {
                    val cacheFile = java.io.File(context.cacheDir, "camera_cache.jpg")
                    if (cacheFile.exists()) {
                        cameraBitmap = android.graphics.BitmapFactory.decodeFile(cacheFile.absolutePath)
                    }
                } catch (_: Exception) {}
            }
        }

        provideContent {
            WidgetContent(
                isConfigured = prefs.isConfigured,
                cameraBitmap = cameraBitmap,
                s1Name = prefs.sensor1Name,
                s1Val = s1Val,
                s2Name = prefs.sensor2Name,
                s2Val = s2Val,
                s3Name = prefs.sensor3Name,
                s3Val = s3Val,
                b1Entity = prefs.button1Entity,
                b1Name = prefs.button1Name,
                b1On = b1On,
                b2Entity = prefs.button2Entity,
                b2Name = prefs.button2Name,
                b2On = b2On,
                b3Entity = prefs.button3Entity,
                b3Name = prefs.button3Name,
                b3On = b3On,
                b4Entity = prefs.button4Entity,
                b4Name = prefs.button4Name,
                b4On = b4On
            )
        }
    }

    private fun isEntityOn(state: String?): Boolean {
        if (state == null) return false
        val s = state.trim().lowercase()
        return s == "on" || s == "open" || s == "unlocked" || s == "playing" || s == "true" || s == "home"
    }

    private fun formatSensorVal(raw: String?, unit: String): String {
        if (raw == null || raw == "N/A" || raw == "unavailable") return "---"
        val trimmedRaw = raw.trim()
        val trimmedUnit = unit.trim()
        if (trimmedUnit.isEmpty()) return trimmedRaw
        if (trimmedRaw.endsWith(trimmedUnit, ignoreCase = true)) return trimmedRaw
        return "$trimmedRaw $trimmedUnit"
    }

    @Composable
    private fun WidgetContent(
        isConfigured: Boolean,
        cameraBitmap: Bitmap?,
        s1Name: String, s1Val: String,
        s2Name: String, s2Val: String,
        s3Name: String, s3Val: String,
        b1Entity: String, b1Name: String, b1On: Boolean,
        b2Entity: String, b2Name: String, b2On: Boolean,
        b3Entity: String, b3Name: String, b3On: Boolean,
        b4Entity: String, b4Name: String, b4On: Boolean
    ) {
        val size = LocalSize.current
        val totalWidth = size.width

        val cameraWidth = (totalWidth * 0.65f) - 6.dp

        val bgColor = Color(0xFF1E1E2C)
        val cardColor = Color(0xFF2A2B3D)
        val accentColor = Color(0xFF00ADB5)
        val textColor = Color(0xFFEEEEEE)
        val mutedTextColor = Color(0xFFAAAAAA)
        val activeBtnBg = Color(0xFF00ADB5)
        val inactiveBtnBg = Color(0xFF393E46)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgColor)
                .padding(6.dp)
                .cornerRadius(12.dp)
        ) {
            if (!isConfigured) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Apri l'app per configurare Home Assistant",
                        style = TextStyle(color = ColorProvider(day = textColor, night = textColor), fontSize = 12.sp)
                    )
                }
                return@Column
            }

            // 1. TOP SECTION: CAMERA (65% Width X) + 3 SENSORS
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
            ) {
                // CAMERA SNAPSHOT (65% Width X)
                Box(
                    modifier = GlanceModifier
                        .width(cameraWidth.coerceAtLeast(150.dp))
                        .fillMaxHeight()
                        .background(cardColor)
                        .cornerRadius(10.dp)
                ) {
                    if (cameraBitmap != null) {
                        Image(
                            provider = ImageProvider(cameraBitmap),
                            contentDescription = "Camera Live",
                            contentScale = ContentScale.Crop,
                            modifier = GlanceModifier.fillMaxSize().cornerRadius(10.dp)
                        )
                    } else {
                        Box(
                            modifier = GlanceModifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📷\nNo Cam",
                                style = TextStyle(color = ColorProvider(day = mutedTextColor, night = mutedTextColor), fontSize = 11.sp)
                            )
                        }
                    }

                    // Refresh Button Overlay
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(4.dp),
                        horizontalAlignment = Alignment.End,
                        verticalAlignment = Alignment.Top
                    ) {
                        Button(
                            text = "🔄",
                            onClick = actionRunCallback<RefreshWidgetAction>(),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = ColorProvider(day = Color(0xAA000000), night = Color(0xAA000000)),
                                contentColor = ColorProvider(day = textColor, night = textColor)
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.width(4.dp))

                // 3 SENSORS COLUMN
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SensorTile(
                        name = s1Name,
                        value = s1Val,
                        cardColor = cardColor,
                        accentColor = accentColor,
                        textColor = textColor,
                        mutedTextColor = mutedTextColor,
                        modifier = GlanceModifier.defaultWeight().fillMaxWidth()
                    )
                    Spacer(modifier = GlanceModifier.height(3.dp))
                    SensorTile(
                        name = s2Name,
                        value = s2Val,
                        cardColor = cardColor,
                        accentColor = accentColor,
                        textColor = textColor,
                        mutedTextColor = mutedTextColor,
                        modifier = GlanceModifier.defaultWeight().fillMaxWidth()
                    )
                    Spacer(modifier = GlanceModifier.height(3.dp))
                    SensorTile(
                        name = s3Name,
                        value = s3Val,
                        cardColor = cardColor,
                        accentColor = accentColor,
                        textColor = textColor,
                        mutedTextColor = mutedTextColor,
                        modifier = GlanceModifier.defaultWeight().fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(5.dp))

            // 2. BOTTOM SECTION: 4 BUTTONS HORIZONTAL ROW
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionButton(
                    entityId = b1Entity,
                    name = b1Name,
                    isOn = b1On,
                    activeBg = activeBtnBg,
                    inactiveBg = inactiveBtnBg,
                    textColor = textColor,
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                )
                Spacer(modifier = GlanceModifier.width(2.dp))
                ActionButton(
                    entityId = b2Entity,
                    name = b2Name,
                    isOn = b2On,
                    activeBg = activeBtnBg,
                    inactiveBg = inactiveBtnBg,
                    textColor = textColor,
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                )
                Spacer(modifier = GlanceModifier.width(2.dp))
                ActionButton(
                    entityId = b3Entity,
                    name = b3Name,
                    isOn = b3On,
                    activeBg = activeBtnBg,
                    inactiveBg = inactiveBtnBg,
                    textColor = textColor,
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                )
                Spacer(modifier = GlanceModifier.width(2.dp))
                ActionButton(
                    entityId = b4Entity,
                    name = b4Name,
                    isOn = b4On,
                    activeBg = activeBtnBg,
                    inactiveBg = inactiveBtnBg,
                    textColor = textColor,
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                )
            }
        }
    }

    @Composable
    private fun SensorTile(
        name: String,
        value: String,
        cardColor: Color,
        accentColor: Color,
        textColor: Color,
        mutedTextColor: Color,
        modifier: GlanceModifier
    ) {
        Column(
            modifier = modifier
                .background(cardColor)
                .cornerRadius(6.dp)
                .padding(vertical = 2.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = TextStyle(
                    color = ColorProvider(day = mutedTextColor, night = mutedTextColor),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = value,
                style = TextStyle(
                    color = ColorProvider(day = accentColor, night = accentColor),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    @Composable
    private fun ActionButton(
        entityId: String,
        name: String,
        isOn: Boolean,
        activeBg: Color,
        inactiveBg: Color,
        textColor: Color,
        modifier: GlanceModifier
    ) {
        val bg = if (isOn) activeBg else inactiveBg
        val prefix = if (isOn) "⚡" else "○"
        val displayText = if (name.isNotBlank()) "$prefix $name" else prefix

        Button(
            text = displayText,
            onClick = actionRunCallback<ToggleEntityAction>(
                actionParametersOf(ToggleEntityAction.KEY_ENTITY_ID to entityId)
            ),
            modifier = modifier.cornerRadius(6.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = ColorProvider(day = bg, night = bg),
                contentColor = ColorProvider(day = textColor, night = textColor)
            ),
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
