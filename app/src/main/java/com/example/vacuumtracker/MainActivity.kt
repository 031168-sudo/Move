package com.example.vacuumtracker

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null
    private var linearAccelSensor: Sensor? = null

    private lateinit var trackView: TrackView
    private lateinit var textStatus: TextView
    private lateinit var editWidth: EditText
    private lateinit var editHeight: EditText
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnReset: Button

    private val rotationMatrix = FloatArray(9)
    private var haveRotation = false

    private var recording = false
    private var lastTimestampNs: Long = 0L

    private var vx = 0f
    private var vy = 0f
    private var px = 0f
    private var py = 0f

    private val points = mutableListOf(Pair(0f, 0f))

    private val recentMags = ArrayDeque<Float>()
    private val stillWindow = 12
    private val stillThreshold = 0.35f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        linearAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        trackView = findViewById(R.id.trackView)
        textStatus = findViewById(R.id.textStatus)
        editWidth = findViewById(R.id.editRoomWidth)
        editHeight = findViewById(R.id.editRoomHeight)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnReset = findViewById(R.id.btnReset)

        if (rotationSensor == null || linearAccelSensor == null) {
            textStatus.text = "На этом устройстве нет нужных датчиков"
            btnStart.isEnabled = false
        }

        btnStart.setOnClickListener { startRecording() }
        btnStop.setOnClickListener { stopRecording() }
        btnReset.setOnClickListener { resetAll() }

        applyRoomSize()
    }

    private fun applyRoomSize() {
        val wCm = editWidth.text.toString().toFloatOrNull()
        val hCm = editHeight.text.toString().toFloatOrNull()
        trackView.roomWidthM = wCm?.let { it / 100f }
        trackView.roomHeightM = hCm?.let { it / 100f }
    }

    private fun startRecording() {
        applyRoomSize()
        vx = 0f; vy = 0f; px = 0f; py = 0f
        points.clear(); points.add(Pair(0f, 0f))
        recentMags.clear()
        haveRotation = false
        lastTimestampNs = 0L
        recording = true

        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, linearAccelSensor, SensorManager.SENSOR_DELAY_GAME)

        btnStart.isEnabled = false
        btnStop.isEnabled = true
        editWidth.isEnabled = false
        editHeight.isEnabled = false
        textStatus.text = "Запись... положите телефон на пылесос"
    }

    private fun stopRecording() {
        recording = false
        sensorManager.unregisterListener(this)
        btnStart.isEnabled = true
        btnStop.isEnabled = false
        editWidth.isEnabled = true
        editHeight.isEnabled = true
        textStatus.text = "Готово. Точек: ${points.size}"
        trackView.points = points.toList()
    }

    private fun resetAll() {
        recording = false
        sensorManager.unregisterListener(this)
        points.clear(); points.add(Pair(0f, 0f))
        vx = 0f; vy = 0f; px = 0f; py = 0f
        trackView.points = emptyList()
        btnStart.isEnabled = true
        btnStop.isEnabled = false
        editWidth.isEnabled = true
        editHeight.isEnabled = true
        textStatus.text = "Готов"
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!recording) return
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                haveRotation = true
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                if (!haveRotation) return
                val dtNs = if (lastTimestampNs == 0L) 0L else event.timestamp - lastTimestampNs
                lastTimestampNs = event.timestamp
                if (dtNs <= 0L) return
                val dt = (dtNs / 1_000_000_000.0).toFloat()
                if (dt > 0.5f) return

                val ax = event.values[0]; val ay = event.values[1]; val az = event.values[2]

                val wx = rotationMatrix[0] * ax + rotationMatrix[1] * ay + rotationMatrix[2] * az
                val wy = rotationMatrix[3] * ax + rotationMatrix[4] * ay + rotationMatrix[5] * az

                val mag = kotlin.math.sqrt(wx * wx + wy * wy)
                recentMags.addLast(mag)
                if (recentMags.size > stillWindow) recentMags.removeFirst()
                val avgMag = recentMags.average().toFloat()
                val isStill = recentMags.size >= stillWindow && avgMag < stillThreshold

                if (isStill) {
                    vx = 0f; vy = 0f
                } else {
                    vx += wx * dt
                    vy += wy * dt
                }

                px += vx * dt
                py += vy * dt

                points.add(Pair(px, py))
                trackView.points = points.toList()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onPause() {
        super.onPause()
        if (recording) sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        if (recording) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
            sensorManager.registerListener(this, linearAccelSensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }
}
