import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.blur.Bloom
import org.openrndr.extra.minim.minim
import org.openrndr.math.Vector2
import ddf.minim.analysis.FFT
import ddf.minim.Minim
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos

fun main() = application {
    configure {
        width = 1920
        height = 1080
        title = "Neon Interference"
    }

    program {
        val minim = minim()
        // Audio File Input
        val player = minim.loadFile("data/audio/track.mp3")
        player.play()
        // player.mute()  // Entfernt, damit Audio spielt und FFT Daten hat
        val fft = FFT(player.bufferSize(), player.sampleRate())

        // Bloom für den Neon-Effekt (wie im Bild)
        val bloom = Bloom().apply {
            brightness = 0.0
        }

        val offscreen = renderTarget(width, height) {
            colorBuffer()
            depthBuffer()
        }

        // Farben aus deinem Bild (Grün zu Gelb Verlauf)
        val neonGreen = ColorRGBa.fromHex("00FF41")
        val neonYellow = ColorRGBa.fromHex("EBFF00")

        var smoothedBass = 0.0
        var smoothedHighs = 0.0

        extend {
            fft.forward(player.mix)

            // Audio Smoothing
            smoothedBass = smoothedBass * 0.9 + (fft.calcAvg(0f, 100f) * 100.0) * 0.1
            smoothedHighs = smoothedHighs * 0.9 + (fft.calcAvg(1000f, 5000f) * 100.0) * 0.1
            
            // Debug: Setze smoothedBass auf einen festen Wert, um zu testen
            smoothedBass = 50.0
            
            val time = seconds * 0.5

            drawer.isolatedWithTarget(offscreen) {
                drawer.clear(ColorRGBa.BLACK)
                
                // Wir zeichnen viele Linien übereinander
                // In deinem Bild sind es feine Linien, die sich kreuzen
                
                val lines = 80 // Anzahl der Linien
                drawer.strokeWeight = 3.0 
                drawer.fill = null

                // Test: Zeichne eine rote Linie in der Mitte
                drawer.stroke = ColorRGBa.RED
                drawer.lineSegment(0.0, height / 2.0, width.toDouble(), height / 2.0)

                // Loop für das Netz
                for (i in 0 until lines) {
                    // Verlauf von Grün nach Gelb basierend auf Index i
                    drawer.stroke = neonGreen.mix(neonYellow, i / lines.toDouble())

                    val points = mutableListOf<Vector2>()
                    
                    // Wir zeichnen die Linie über die Breite des Schirms
                    for (x in 0..width step 10) {
                        val xNorm = x.toDouble() / width
                        val iNorm = i.toDouble() / lines
                        
                        // --- DIE MATHE MAGIE ---
                        // 1. Basis-Welle (groß und langsam)
                        val baseWave = sin(xNorm * 10.0 + time + iNorm * 2.0) 
                        
                        // 2. Interferenz-Welle (schneller, reagiert auf Bass)
                        // Bass verändert die Amplitude (Höhe) und Frequenz
                        val interference = sin(xNorm * (20.0 + smoothedBass * 0.1) - time * 2.0 + iNorm * 5.0)

                        // 3. Twist (dreht die Wellen ineinander)
                        val twist = cos(xNorm * 5.0 + time)

                        // Position berechnen
                        val yBase = height / 2.0
                        
                        // Wie weit die Wellen ausschlagen (Spread)
                            val spread = 100.0 + smoothedBass * 0.5
                        val y = yBase + (baseWave * interference * twist) * spread
                        
                        points.add(Vector2(x.toDouble(), y))
                    }
                    drawer.lineStrip(points)
                }
            }

            // Bloom anwenden (temporär deaktiviert zum Testen)
            // bloom.apply(offscreen.colorBuffer(0), offscreen.colorBuffer(0))
            drawer.image(offscreen.colorBuffer(0))
        }
    }
}
