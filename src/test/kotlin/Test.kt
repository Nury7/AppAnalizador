import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

enum class WindowTypes {
    First,
    Second,
}

fun main() = application {
    val windowFocusRequestSharedFlow = remember { MutableSharedFlow<WindowTypes>() }

    WindowTypes.values().forEach { windowType ->
        key(windowType) {
            Window(
                title = windowType.toString(),
                onCloseRequest = ::exitApplication,
            ) {
                LaunchedEffect(Unit) {
                    windowFocusRequestSharedFlow
                        .filter { it == windowType }
                        .collect {
                            window.toFront()
                        }
                }
                val scope = rememberCoroutineScope()
                Button({
                    scope.launch {
                        val windowTypeToFocus = WindowTypes.values().run {
                            get((indexOf(windowType) + 1) % count())
                        }
                        windowFocusRequestSharedFlow.emit(windowTypeToFocus)
                    }
                }) {
                    Text("next window")
                }
            }
        }
    }
}