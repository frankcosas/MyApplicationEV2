package com.example.myapplication

import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DashboardActivity : AppCompatActivity() {

    private lateinit var tvEstadoAlarma: TextView
    private lateinit var tvSensorMagnetico: TextView
    private lateinit var tvSensorInfrarrojo: TextView
    private lateinit var tvDistancia: TextView
    private lateinit var seekBarDistancia: SeekBar
    private lateinit var btnToggleArmado: Button
    private lateinit var btnSimularPuerta: Button
    private lateinit var btnSimularMovimiento: Button
    private lateinit var btnCerrarSesion: Button

    private var estadoActualSistema = "Desactivada"
    private var puertaAbierta = false
    private var infrarrojoEnAlerta = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        tvEstadoAlarma = findViewById(R.id.tvEstadoAlarma)
        tvSensorMagnetico = findViewById(R.id.tvSensorMagnetico)
        tvSensorInfrarrojo = findViewById(R.id.tvSensorInfrarrojo)
        tvDistancia = findViewById(R.id.tvDistancia)
        seekBarDistancia = findViewById(R.id.seekBarDistancia)
        btnToggleArmado = findViewById(R.id.btnToggleArmado)
        btnSimularPuerta = findViewById(R.id.btnSimularPuerta)
        btnSimularMovimiento = findViewById(R.id.btnSimularIntruso)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)

        val databaseReference = FirebaseDatabase.getInstance("https://alarmaantirrobo-7f6dd-default-rtdb.firebaseio.com").getReference("alarma")

        // 1. Escuchar cambios en tiempo real desde Firebase
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val valorFirebase = snapshot.getValue(String::class.java) ?: "Desconocido"
                estadoActualSistema = valorFirebase
                tvEstadoAlarma.text = "Estado: $valorFirebase"

                if (valorFirebase.contains("Intruso", ignoreCase = true)) {
                    reproducirSonidoAlarma()
                    Toast.makeText(this@DashboardActivity, "🚨 ¡ALERTA DE INTRUSO DETECTADA!", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                tvEstadoAlarma.text = "Error al leer datos"
            }
        })

        // 2. Control de la barra de distancia (SeekBar de 0 a 10 metros)
        seekBarDistancia.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvDistancia.text = "📏 Distancia IR: $progress metros (Umbral > 5m)"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 3. Control de Armado / Desarmado
        btnToggleArmado.setOnClickListener {
            val nuevoEstado = if (estadoActualSistema == "Desactivada") {
                "Sistema Armado (Protegido)"
            } else {
                puertaAbierta = false
                infrarrojoEnAlerta = false
                tvSensorMagnetico.text = "🚪 Sensor Puerta: Cerrada"
                tvSensorInfrarrojo.text = "🔴 Sensor Infrarrojo: Apagado"
                seekBarDistancia.progress = 0
                "Desactivada"
            }

            databaseReference.setValue(nuevoEstado)
                .addOnSuccessListener {
                    Toast.makeText(this, "Sistema: $nuevoEstado", Toast.LENGTH_SHORT).show()
                }
        }

        // 4. Simular Apertura de Puerta (Activa el Infrarrojo en modo alerta)
        btnSimularPuerta.setOnClickListener {
            puertaAbierta = !puertaAbierta

            if (puertaAbierta) {
                infrarrojoEnAlerta = true
                tvSensorMagnetico.text = "🚪 Sensor Puerta: Abierta"
                tvSensorInfrarrojo.text = "🔴 Sensor Infrarrojo: Encendido (En Alerta ⚠️)"
                Toast.makeText(this, "Puerta abierta. Infrarrojo armado.", Toast.LENGTH_SHORT).show()
            } else {
                infrarrojoEnAlerta = false
                tvSensorMagnetico.text = "🚪 Sensor Puerta: Cerrada"
                tvSensorInfrarrojo.text = "🔴 Sensor Infrarrojo: Apagado"
                Toast.makeText(this, "Puerta cerrada. Infrarrojo en reposo.", Toast.LENGTH_SHORT).show()
            }
        }

        // 5. Simular Movimiento evaluando la distancia seleccionada (> 5 metros)
        btnSimularMovimiento.setOnClickListener {
            if (estadoActualSistema.contains("Desactivada", ignoreCase = true)) {
                Toast.makeText(this, "El sistema está desactivado. Movimiento ignorado.", Toast.LENGTH_SHORT).show()
            } else if (!infrarrojoEnAlerta) {
                Toast.makeText(this, "Infrarrojo apagado. Abre la puerta primero.", Toast.LENGTH_SHORT).show()
            } else {
                val distanciaActual = seekBarDistancia.progress

                // REGLA: Si está en 5 metros o menos, no detecta nada. Si es mayor a 5, dispara la alarma.
                if (distanciaActual > 5) {
                    databaseReference.setValue("¡Intruso Detectado a ${distanciaActual}m! 🚨")
                        .addOnSuccessListener {
                            Toast.makeText(this, "¡Intruso a ${distanciaActual}m (> 5m)! Alarma activada.", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Distancia de ${distanciaActual}m (<= 5m): Sin amenaza detectada.", Toast.LENGTH_LONG).show()
                }
            }
        }

        // 6. Cerrar Sesión
        btnCerrarSesion.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun reproducirSonidoAlarma() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1500)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}