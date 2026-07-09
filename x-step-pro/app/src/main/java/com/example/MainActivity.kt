package com.example

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StepViewModel : ViewModel(), SensorEventListener {
    private lateinit var prefs: SharedPreferences
    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null

    private val _totalSteps = MutableStateFlow(0)
    val totalSteps: StateFlow<Int> = _totalSteps.asStateFlow()

    private val _multiplier = MutableStateFlow("1")
    val multiplier: StateFlow<String> = _multiplier.asStateFlow()

    private val _manualInput = MutableStateFlow("")
    val manualInput: StateFlow<String> = _manualInput.asStateFlow()

    private val _autoWalkEnabled = MutableStateFlow(false)
    val autoWalkEnabled: StateFlow<Boolean> = _autoWalkEnabled.asStateFlow()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.getSharedPreferences("XStepProPrefs", Context.MODE_PRIVATE)
        _totalSteps.value = prefs.getInt("TOTAL_STEPS", 0)

        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        stepSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }

        viewModelScope.launch {
            while (true) {
                if (_autoWalkEnabled.value) {
                    addSteps(10)
                }
                delay(1000)
            }
        }
    }

    fun updateMultiplier(value: String) {
        _multiplier.value = value
    }

    fun updateManualInput(value: String) {
        _manualInput.value = value
    }

    fun addManualSteps() {
        val stepsToAdd = _manualInput.value.toIntOrNull() ?: 0
        if (stepsToAdd > 0) {
            addSteps(stepsToAdd)
            _manualInput.value = ""
        }
    }

    fun toggleAutoWalk(enabled: Boolean) {
        _autoWalkEnabled.value = enabled
    }

    private fun addSteps(count: Int) {
        val newTotal = _totalSteps.value + count
        _totalSteps.value = newTotal
        prefs.edit().putInt("TOTAL_STEPS", newTotal).apply()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
            val multi = _multiplier.value.toIntOrNull() ?: 1
            addSteps(1 * multi)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onCleared() {
        super.onCleared()
        sensorManager?.unregisterListener(this)
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel: StepViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModel.init(this)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = true) {
                XStepProApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XStepProApp(viewModel: StepViewModel) {
    val totalSteps by viewModel.totalSteps.collectAsState()
    val multiplier by viewModel.multiplier.collectAsState()
    val manualInput by viewModel.manualInput.collectAsState()
    val autoWalkEnabled by viewModel.autoWalkEnabled.collectAsState()
    
    val context = LocalContext.current
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Real qadamlarni sanash uchun ruxsat kerak!", Toast.LENGTH_SHORT).show()
        }
    }
    
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
    }

    val backgroundColor = Color(0xFF0A0A0B)
    val surfaceColor = Color(0xFF141416)
    val accentColor = Color(0xFFBFFF00)
    val textPrimary = Color(0xFFF0F0F0)
    val textSecondary = Color(0xFF666666)
    val borderColor = Color(0xFF2A2A2C)
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = backgroundColor,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = "SYSTEM STATUS: ACTIVE",
                    color = textSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "X-Step Pro",
                    color = textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.5).sp
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            Spacer(modifier = Modifier.height(16.dp))
            
            StepProgressCircle(steps = totalSteps, color = accentColor)
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Real-time Stats
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("MULTIPLIER", color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${multiplier}x", color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(borderColor))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("AUTO-WALK", color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(if (autoWalkEnabled) "On" else "Off", color = if (autoWalkEnabled) accentColor else textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Controls Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = surfaceColor,
                        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
                    )
                    .padding(32.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Multiplier Input
                        Column(modifier = Modifier.weight(1f)) {
                            Text("KO'PAYTIRGICH (X)", color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = multiplier,
                                onValueChange = { viewModel.updateMultiplier(it) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = backgroundColor,
                                    unfocusedContainerColor = backgroundColor,
                                    focusedBorderColor = borderColor,
                                    unfocusedBorderColor = borderColor,
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary
                                ),
                                trailingIcon = {
                                    Text("X", color = accentColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 16.dp))
                                },
                                singleLine = true
                            )
                        }
                        
                        // Auto-Walk Switch
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AUTO-WALK MODE", color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .background(backgroundColor, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("On / Off", color = Color(0xFF888888), fontSize = 14.sp)
                                    Switch(
                                        checked = autoWalkEnabled,
                                        onCheckedChange = { viewModel.toggleAutoWalk(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = accentColor,
                                            uncheckedThumbColor = Color.DarkGray,
                                            uncheckedTrackColor = Color(0xFF1A1A1C),
                                            uncheckedBorderColor = borderColor
                                        )
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Manual Injection
                    Text("QO'LDA QO'SHISH (INJECTION)", color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = manualInput,
                            onValueChange = { viewModel.updateManualInput(it) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            placeholder = { Text("5000", color = Color(0xFF444444)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = backgroundColor,
                                unfocusedContainerColor = backgroundColor,
                                focusedBorderColor = borderColor,
                                unfocusedBorderColor = borderColor,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            ),
                            singleLine = true
                        )
                        Button(
                            onClick = { viewModel.addManualSteps() },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                            modifier = Modifier.height(56.dp).padding(horizontal = 8.dp)
                        ) {
                            Text("ADD", fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Bottom Primary Action
                    Button(
                        onClick = { viewModel.toggleAutoWalk(!autoWalkEnabled) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black)
                    ) {
                        Text(
                            if (autoWalkEnabled) "YURISHNI TO'XTATISH" else "YURISHNI BOSHLASH", 
                            fontWeight = FontWeight.Black, 
                            fontSize = 14.sp, 
                            letterSpacing = 2.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun StepProgressCircle(steps: Int, color: Color) {
    val maxSteps = 10000
    val progress = (steps.toFloat() / maxSteps.toFloat()).coerceIn(0f, 1f)
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progressAnim"
    )
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(280.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw background glow loosely
            drawCircle(
                color = color.copy(alpha = 0.05f),
                radius = size.width / 2 + 40.dp.toPx()
            )
            
            // Outer thick dark border
            drawCircle(
                color = Color(0xFF1A1A1C),
                radius = size.width / 2 - 12.dp.toPx(),
                style = Stroke(width = 24.dp.toPx())
            )
            
            val strokeWidth = 8.dp.toPx()
            // Progress arc
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(16.dp.toPx(), 16.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(size.width - 32.dp.toPx(), size.height - 32.dp.toPx())
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "JAMI QADAMLAR",
                color = Color(0xFF666666),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "%,d".format(steps),
                color = Color(0xFFF0F0F0),
                fontSize = 52.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-2).sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .background(color, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "ACTIVE",
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
