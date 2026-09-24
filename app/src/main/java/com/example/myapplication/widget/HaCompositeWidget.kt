package com.example.myapplication.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
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
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull

class HaCompositeWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = HaPreferences(context)
        val api = HomeAssistantApi(prefs)

        var cameraBitmap: Bitmap? = null
        var s1Val = "---"
        var s2Val = "---"
        var s3Val = "---"

        var b1On = false
        var b2On = false
        var b3On = false
        var b4On = false

        if (prefs.isConfigured) {
            try {
                supervisorScope {
                    val cameraDef = async {
                        withTimeoutOrNull(3000) {
                            try { api.fetchCameraSnapshot(prefs.cameraEntity) } catch (_: Exception) { null }
                        }
                    }
                    val s1Def = async {
                        withTimeoutOrNull(3000) {
                            try { api.fetchEntityState(prefs.sensor1Entity) } catch (_: Exception) { null }
                        }
                    }
                    val s2Def = async {
                        withTimeoutOrNull(3000) {
                            try { api.fetchEntityState(prefs.sensor2Entity) } catch (_: Exception) { null }
                        }
                    }
                    val s3Def = async {
                        withTimeoutOrNull(3000) {
                            try { api.fetchEntityState(prefs.sensor3Entity) } catch (_: Exception) { null }
                        }
                    }

                    val b1Def = async {
                        withTimeoutOrNull(3000) {
                            try { api.fetchEntityState(prefs.button1Entity) } catch (_: Exception) { null }
                        }
                    }
                    val b2Def = async {
                        withTimeoutOrNull(3000) {
                            try { api.fetchEntityState(prefs.button2Entity) } catch (_: Exception) { null }
                        }
                    }
                    val b3Def = async {
                        withTimeoutOrNull(3000) {
                            try { api.fetchEntityState(prefs.button3Entity) } catch (_: Exception) { null }
                        }
                    }
                    val b4Def = async {
                        withTimeoutOrNull(3000) {
                            try { api.fetchEntityState(prefs.button4Entity) } catch (_: Exception) { null }
                        }
                    }

                    cameraBitmap = cameraDef.await()
                    s1Val = formatSensorVal(s1Def.await()?.state, prefs.sensor1Unit)
                    s2Val = formatSensorVal(s2Def.await()?.state, prefs.sensor2Unit)
                    s3Val = formatSensorVal(s3Def.await()?.state, prefs.sensor3Unit)

                    b1On = b1Def.await()?.state == "on"
                    b2On = b2Def.await()?.state == "on"
                    b3On = b3Def.await()?.state == "on"
                    b4On = b4Def.await()?.state == "on"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        provideContent {
            WidgetContent(
                isConfigured = prefs.isConfigured,
                requireConfirmation = prefs.requireConfirmation,
                confirmingId = prefs.confirmingEntityId,
                confirmingTs = prefs.confirmingTimestamp,
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
        requireConfirmation: Boolean,
        confirmingId: String,
        confirmingTs: Long,
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
        val totalWidth = if (size.width.isSpecified && size.width > 0.dp) size.width else 250.dp

        // Calculate exact 65% width X from actual widget size provided by SizeMode.Exact
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

            // 1. TOP SECTION: CAMERA (65% Width X) + 3 SENSORS (Fills all vertical space above buttons)
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

                // 3 SENSORS COLUMN (35% Width X, 3 lines stacked vertically)
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

            // 2. BOTTOM SECTION: 4 BUTTONS HORIZONTAL ROW (Height 56dp for plenty of space, 11sp font, 2dp gaps)
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
                    confirmingId = confirmingId,
                    confirmingTs = confirmingTs,
                    isConfirmingEnabled = requireConfirmation,
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
                    confirmingId = confirmingId,
                    confirmingTs = confirmingTs,
                    isConfirmingEnabled = requireConfirmation,
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
                    confirmingId = confirmingId,
                    confirmingTs = confirmingTs,
                    isConfirmingEnabled = requireConfirmation,
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
                    confirmingId = confirmingId,
                    confirmingTs = confirmingTs,
                    isConfirmingEnabled = requireConfirmation,
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
        confirmingId: String,
        confirmingTs: Long,
        isConfirmingEnabled: Boolean,
        activeBg: Color,
        inactiveBg: Color,
        textColor: Color,
        modifier: GlanceModifier
    ) {
        val now = System.currentTimeMillis()
        val isPendingConfirm = isConfirmingEnabled &&
                confirmingId.isNotBlank() &&
                confirmingId == entityId &&
                (now - confirmingTs) < 5000

        val bg = if (isPendingConfirm) Color(0xFFFF9800) else (if (isOn) activeBg else inactiveBg)
        val prefix = if (isPendingConfirm) "⚠️" else (if (isOn) "⚡" else "○")
        val displayText = if (isPendingConfirm) "⚠️ Confermi?" else (if (name.isNotBlank()) "$prefix $name" else prefix)

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
