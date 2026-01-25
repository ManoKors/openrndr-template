import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.events.Event

class Settings {
    var sceneName = "neon interference"
    var autoSceneChange = true // Neue Variable für automatischen Wechsel
}

fun main() = application {
    configure {
        width = Config.WIDTH
        height = Config.HEIGHT
        title = Config.TITLE
        windowResizable = true
    }

    program {
        val settings = Settings() // Für zukünftige GUI

        // Module initialisieren
        val audio = AudioEngine()
        audio.setup(this)

        val scenes = listOf("aurora flow", "neon interference", "neon interference 2", "hypnotic", "dream haze")
        var lastSceneChange = 0.0
        var currentSceneIndex = scenes.indexOf(settings.sceneName)
        var scene: Scene = NeonInterferenceScene() // Initiale Scene

        // Funktion zum Scene-Wechsel
        fun switchToScene(index: Int) {
            currentSceneIndex = index
            settings.sceneName = scenes[currentSceneIndex]
            scene = when (settings.sceneName) {
                "aurora flow" -> AuroraFlowScene()
                "neon interference" -> NeonInterferenceScene()
                "neon interference 2" -> NeonInterferenceScene2()
                "hypnotic" -> HypnoticRadialScene()
                "dream haze" -> DreamHazeScene()
                else -> NeonInterferenceScene()
            }
        }

        // Initiale Scene setzen
        switchToScene(currentSceneIndex)

        // Grafik-Setup
        // val bloom = Bloom().apply {
        //     brightness = 1.0
        // }

        // Offscreen Buffer entfernt, da wir direkt zeichnen

        extend {
            // 1. Daten Update
            audio.update()
            val time = seconds * 0.5

            // Szene-Wechsel alle 2 Minuten (120 Sekunden) - nur wenn autoSceneChange aktiv
            if (settings.autoSceneChange) {
                val currentCycle = (seconds / 120.0).toInt()
                if (currentCycle != lastSceneChange.toInt()) {
                    lastSceneChange = currentCycle.toDouble()
                    val nextIndex = (currentSceneIndex + 1) % scenes.size
                    switchToScene(nextIndex)
                }
            }

        // Tastatursteuerung für Scene-Wechsel
        keyboard.keyDown.listen {
            when (it.name) {
                "arrow-right" -> {
                    val nextIndex = (currentSceneIndex + 1) % scenes.size
                    switchToScene(nextIndex)
                    settings.autoSceneChange = false // Automatischen Wechsel deaktivieren bei manueller Steuerung
                }
                "arrow-left" -> {
                    val prevIndex = if (currentSceneIndex - 1 < 0) scenes.size - 1 else currentSceneIndex - 1
                    switchToScene(prevIndex)
                    settings.autoSceneChange = false // Automatischen Wechsel deaktivieren bei manueller Steuerung
                }
                "space" -> { // Space zum Toggle des automatischen Wechsels
                    settings.autoSceneChange = !settings.autoSceneChange
                }
            }
        }

            // 2. Zeichnen
            drawer.clear(ColorRGBa.BLACK)
            
            // Wir übergeben nur die Daten, die die Szene braucht
            scene.draw(
                drawer = drawer, 
                time = time, 
                bassEnergy = audio.smoothedBass, 
                midEnergy = audio.smoothedMids,
                highEnergy = audio.smoothedHighs,
                width = width, 
                height = height
            )

            // 3. Post-Processing (Bloom) & Ausgabe auf Screen
            // bloom.apply(offscreen.colorBuffer(0), offscreen.colorBuffer(0))
            // drawer.image(offscreen.colorBuffer(0))

            // GUI kann später hinzugefügt werden
        }
    }
}