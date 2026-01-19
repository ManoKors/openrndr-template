import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.blur.Bloom

fun main() = application {
    configure {
        width = Config.WIDTH
        height = Config.HEIGHT
        title = Config.TITLE
    }

    program {
        // Module initialisieren
        val audio = AudioEngine()
        audio.setup(this)

        val scene = InterferenceScene()

        // Grafik-Setup
        val bloom = Bloom().apply {
            brightness = 1.0
        }

        val offscreen = renderTarget(width, height) {
            colorBuffer()
            depthBuffer()
        }

        extend {
            // 1. Daten Update
            audio.update()
            val time = seconds * 0.5

            // 2. Zeichnen (in den Offscreen Buffer)
            drawer.isolatedWithTarget(offscreen) {
                drawer.clear(ColorRGBa.BLACK)
                
                // Wir übergeben nur die Daten, die die Szene braucht
                scene.draw(
                    drawer = drawer, 
                    time = time, 
                    bassEnergy = audio.smoothedBass, 
                    width = width, 
                    height = height
                )
            }

            // 3. Post-Processing (Bloom) & Ausgabe auf Screen
            // bloom.apply(offscreen.colorBuffer(0), offscreen.colorBuffer(0))
            drawer.image(offscreen.colorBuffer(0))
        }
    }
}