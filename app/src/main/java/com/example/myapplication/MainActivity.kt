package com.example.myapplication

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import com.example.myapplication.data.HaPreferences
import com.example.myapplication.data.HomeAssistantApi
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.widget.HaCompositeWidget
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ConfigurationScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun ConfigurationScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { HaPreferences(context) }
    val scope = rememberCoroutineScope()

    var serverUrl by remember { mutableStateOf(prefs.serverUrl) }
    var token by remember { mutableStateOf(prefs.token) }

    var cameraEntity by remember { mutableStateOf(prefs.cameraEntity) }

    var s1Entity by remember { mutableStateOf(prefs.sensor1Entity) }
    var s1Name by remember { mutableStateOf(prefs.sensor1Name) }
    var s1Unit by remember { mutableStateOf(prefs.sensor1Unit) }

    var s2Entity by remember { mutableStateOf(prefs.sensor2Entity) }
    var s2Name by remember { mutableStateOf(prefs.sensor2Name) }
    var s2Unit by remember { mutableStateOf(prefs.sensor2Unit) }

    var s3Entity by remember { mutableStateOf(prefs.sensor3Entity) }
    var s3Name by remember { mutableStateOf(prefs.sensor3Name) }
    var s3Unit by remember { mutableStateOf(prefs.sensor3Unit) }

    var b1Entity by remember { mutableStateOf(prefs.button1Entity) }
    var b1Name by remember { mutableStateOf(prefs.button1Name) }

    var b2Entity by remember { mutableStateOf(prefs.button2Entity) }
    var b2Name by remember { mutableStateOf(prefs.button2Name) }

    var b3Entity by remember { mutableStateOf(prefs.button3Entity) }
    var b3Name by remember { mutableStateOf(prefs.button3Name) }

    var b4Entity by remember { mutableStateOf(prefs.button4Entity) }
    var b4Name by remember { mutableStateOf(prefs.button4Name) }

    var requireConfirmation by remember { mutableStateOf(prefs.requireConfirmation) }

    var isTesting by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "🏠 Home Assistant Widget Config",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 1. CREDENTIALS CARD
        SectionCard(title = "Connessione Home Assistant") {
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("URL Server (es. http://192.168.1.100:8123)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Long-Lived Access Token") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. CAMERA SECTION
        SectionCard(title = "📷 Fotocamera Live / Snapshot") {
            OutlinedTextField(
                value = cameraEntity,
                onValueChange = { cameraEntity = it },
                label = { Text("Entity ID Fotocamera (es. camera.ingresso)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. SENSORS SECTION
        SectionCard(title = "⚡ 3 Sensori (Volt, Watt, Temp, ecc.)") {
            SensorInputRowWithUnit(
                label = "Sensore 1",
                name = s1Name,
                onNameChange = { s1Name = it },
                entity = s1Entity,
                onEntityChange = { s1Entity = it },
                unit = s1Unit,
                onUnitChange = { s1Unit = it }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SensorInputRowWithUnit(
                label = "Sensore 2",
                name = s2Name,
                onNameChange = { s2Name = it },
                entity = s2Entity,
                onEntityChange = { s2Entity = it },
                unit = s2Unit,
                onUnitChange = { s2Unit = it }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SensorInputRowWithUnit(
                label = "Sensore 3",
                name = s3Name,
                onNameChange = { s3Name = it },
                entity = s3Entity,
                onEntityChange = { s3Entity = it },
                unit = s3Unit,
                onUnitChange = { s3Unit = it }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. BUTTONS SECTION
        SectionCard(title = "🔘 4 Pulsanti di Comando") {
            SensorInputRow(
                label = "Pulsante 1",
                name = b1Name,
                onNameChange = { b1Name = it },
                entity = b1Entity,
                onEntityChange = { b1Entity = it }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SensorInputRow(
                label = "Pulsante 2",
                name = b2Name,
                onNameChange = { b2Name = it },
                entity = b2Entity,
                onEntityChange = { b2Entity = it }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SensorInputRow(
                label = "Pulsante 3",
                name = b3Name,
                onNameChange = { b3Name = it },
                entity = b3Entity,
                onEntityChange = { b3Entity = it }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SensorInputRow(
                label = "Pulsante 4",
                name = b4Name,
                onNameChange = { b4Name = it },
                entity = b4Entity,
                onEntityChange = { b4Entity = it }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. SECURITY & CONFIRMATION CARD
        SectionCard(title = "🛡️ Sicurezza Azioni Widget") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Conferma al doppio tocco", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        text = "Richiede di toccare due volte il pulsante sul widget prima di eseguire l'azione per prevenire tocchi accidentali.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = requireConfirmation,
                    onCheckedChange = { requireConfirmation = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ACTIONS
        Button(
            onClick = {
                scope.launch {
                    isTesting = true
                    // Save prefs
                    prefs.serverUrl = serverUrl
                    prefs.token = token
                    prefs.requireConfirmation = requireConfirmation
                    prefs.cameraEntity = cameraEntity

                    prefs.sensor1Entity = s1Entity
                    prefs.sensor1Name = s1Name
                    prefs.sensor1Unit = s1Unit

                    prefs.sensor2Entity = s2Entity
                    prefs.sensor2Name = s2Name
                    prefs.sensor2Unit = s2Unit

                    prefs.sensor3Entity = s3Entity
                    prefs.sensor3Name = s3Name
                    prefs.sensor3Unit = s3Unit

                    prefs.button1Entity = b1Entity
                    prefs.button1Name = b1Name
                    prefs.button2Entity = b2Entity
                    prefs.button2Name = b2Name
                    prefs.button3Entity = b3Entity
                    prefs.button3Name = b3Name
                    prefs.button4Entity = b4Entity
                    prefs.button4Name = b4Name

                    val api = HomeAssistantApi(prefs)
                    val resultMsg = api.testConnectionDetailed()
                    isTesting = false

                    if (resultMsg == "OK") {
                        Toast.makeText(context, "Connessione riuscita! Impostazioni salvate.", Toast.LENGTH_LONG).show()
                        HaCompositeWidget().updateAll(context)
                    } else {
                        Toast.makeText(context, "Connessione fallita: $resultMsg", Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isTesting
        ) {
            Text(if (isTesting) "Verifica in corso..." else "💾 Salva e Verifica Connessione")
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun SensorInputRowWithUnit(
    label: String,
    name: String,
    onNameChange: (String) -> Unit,
    entity: String,
    onEntityChange: (String) -> Unit,
    unit: String,
    onUnitChange: (String) -> Unit
) {
    Column {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Nome") },
                modifier = Modifier.weight(0.3f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedTextField(
                value = entity,
                onValueChange = onEntityChange,
                label = { Text("Entity ID") },
                modifier = Modifier.weight(0.5f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedTextField(
                value = unit,
                onValueChange = onUnitChange,
                label = { Text("Unità") },
                modifier = Modifier.weight(0.2f),
                singleLine = true
            )
        }
    }
}

@Composable
fun SensorInputRow(
    label: String,
    name: String,
    onNameChange: (String) -> Unit,
    entity: String,
    onEntityChange: (String) -> Unit
) {
    Column {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Nome") },
                modifier = Modifier.weight(0.4f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = entity,
                onValueChange = onEntityChange,
                label = { Text("Entity ID") },
                modifier = Modifier.weight(0.6f),
                singleLine = true
            )
        }
    }
}
