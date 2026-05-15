package com.example.fuzzyfeeder

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val inputTemp = findViewById<EditText>(R.id.inputTemp)
        val inputOxygen = findViewById<EditText>(R.id.inputOxygen)
        val btnCalculate = findViewById<Button>(R.id.btnCalculate)
        val resultText = findViewById<TextView>(R.id.resultText)

        btnCalculate.setOnClickListener {
            val temp = inputTemp.text.toString().toDoubleOrNull()
            val oxygen = inputOxygen.text.toString().toDoubleOrNull()

            if (temp == null || oxygen == null) {
                resultText.text = "Please enter valid numbers."
                return@setOnClickListener
            }

            val feedingPercent = FuzzyFeeder.calculate(temp, oxygen)
            resultText.text = String.format(Locale.US, "Feeding: %.2f%%", feedingPercent)
        }
    }
}
