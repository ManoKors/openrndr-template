import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.math.Vector2
import kotlin.math.cos
import kotlin.math.sin

class NeonInterferenceScene : Scene() {
    // Liste von Farben, die zufällig aktiviert werden
    private val colors = listOf(
        ColorRGBa.fromHex("00FF41"), // Neon Grün
        ColorRGBa.fromHex("EBFF00"), // Neon Gelb
        ColorRGBa.fromHex("FF0080"), // Neon Pink
        ColorRGBa.fromHex("00FFFF"), // Cyan
        ColorRGBa.fromHex("FF8000"), // Orange
        ColorRGBa.fromHex("8000FF")  // Lila
    )

    private fun getColorStart(time: Double): ColorRGBa {
        val period = 5.0 // Zeit für einen kompletten Übergang
        val t = (time / period) % 1.0 // 0 bis 1 für Interpolation
        val index = (time / period).toInt() % colors.size
        val nextIndex = (index + 1) % colors.size
        return colors[index].mix(colors[nextIndex], t)
    }

    private fun getColorEnd(time: Double): ColorRGBa {
        val period = 5.0
        val t = ((time / period) + 0.5) % 1.0 // Versetzt für Variation
        val index = ((time / period) + 0.5).toInt() % colors.size
        val nextIndex = (index + 1) % colors.size
        return colors[index].mix(colors[nextIndex], t)
    }

    // Einstellungen für den Look
    var lineCount = 60 // Anzahl der Linien
    var lineWeight = 1.0 // Noch dicker
    
    override fun draw(drawer: Drawer, time: Double, bassEnergy: Double, midEnergy: Double, highEnergy: Double, width: Int, height: Int) {
        drawer.strokeWeight = lineWeight
        drawer.fill = null // Keine Füllung, nur Linien

        val currentColorStart = getColorStart(time)
        val currentColorEnd = getColorEnd(time)

        for (i in 0 until lineCount) {
            // Normierte Werte (0.0 bis 1.0)
            val iNorm = i.toDouble() / lineCount
            
            // Farbe mischen basierend auf Index
            drawer.stroke = currentColorStart.mix(currentColorEnd, iNorm)

            val points = mutableListOf<Vector2>()
            
            // Punkte für die Linie berechnen
            // step 2 für noch glattere Linien
            for (x in 0..width step 2) {
                val xNorm = x.toDouble() / width
                
                // Die Mathematik ausgelagert in eine Funktion (siehe unten)
                val yOffset = calculateInterference(xNorm, iNorm, time, bassEnergy)
                
                val y = (height / 2.0) + yOffset
                points.add(Vector2(x.toDouble(), y))
            }
            
            drawer.lineStrip(points)
        }
    }

    // Die reine Mathematik-Formel isoliert -> einfacher zu ändern!
    private fun calculateInterference(x: Double, i: Double, time: Double, energy: Double): Double {
        // 1. Große langsame Welle
        val baseWave = sin(x * 10.0 + time * 1.0 + i * 2.0)
        
        // 2. Schnelle Welle (Reagiert auf Musik, minimal)
        val interference = sin(x * (20.0 + energy * 0.01) - time * 1.2 + i * 5.0)
        
        // 3. Verdrehung
        val twist = cos(x * 5.0 + time * 1.0)
        
        // Amplitude (Wie hoch die Welle ist, weniger stark)
        val spread = 100.0 + energy * 1.0
        
        return (baseWave * interference * twist) * spread
    }
}