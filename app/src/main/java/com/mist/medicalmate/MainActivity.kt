package com.mist.medicalmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.mist.medicalmate.auth.ui.LoginRoute
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MedicalMateTheme {
                // 네비게이션은 아직 없다. 목적지가 둘 이상 생기면 NavHost로 바꾼다.
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LoginRoute(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
