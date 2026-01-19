import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.blur.Bloom
import Scene

class Settings {
    var sceneName = "tunnel"
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

        val scene: Scene = when (settings.sceneName) {
            "neon interference" -> NeonInterferenceScene()
            "tunnel" -> TunnelScene()
            else -> TunnelScene() // Default
        }

        // Grafik-Setup
        val bloom = Bloom().apply {
            brightness = 1.0
        }

        // Offscreen Buffer entfernt, da wir direkt zeichnen

        extend {
            // 1. Daten Update
            audio.update()
            val time = seconds * 0.5

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