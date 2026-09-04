package com.mist.medicalmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mist.medicalmate.auth.ui.LoginRoute
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.home.ui.HomeRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MedicalMateTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MedicalMateApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/**
 * 목적지가 둘뿐이라 임시 전환으로 둔다. 화면이 늘어나면 NavHost로 바꾼다.
 * 네비게이션 라이브러리 선택이 아직 미정이라 여기서 미리 정하지 않는다.
 *
 * 프로세스가 죽으면 로그인 화면부터 다시 시작한다. 자동 로그인은 서버 토큰
 * 교환과 로컬 저장이 들어온 뒤에 붙인다.
 */
@Composable
private fun MedicalMateApp(modifier: Modifier = Modifier) {
    var authenticated by remember { mutableStateOf(false) }

    if (authenticated) {
        HomeRoute(modifier = modifier)
    } else {
        LoginRoute(
            onAuthenticated = { authenticated = true },
            modifier = modifier,
        )
    }
}
