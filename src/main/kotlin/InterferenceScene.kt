import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.math.Vector2
import kotlin.math.cos
import kotlin.math.sin

class InterferenceScene {
    // Farben definieren
    private val colorStart = ColorRGBa.fromHex("00FF41") // Neon Grün
    private val colorEnd = ColorRGBa.fromHex("EBFF00")   // Neon Gelb

    // Einstellungen für den Look
    var lineCount = 60 // Anzahl der Linien
    var lineWeight = 1.0 // Noch dicker
    
    fun draw(drawer: Drawer, time: Double, bassEnergy: Double, width: Int, height: Int) {
        drawer.strokeWeight = lineWeight
        drawer.fill = null // Keine Füllung, nur Linien

        for (i in 0 until lineCount) {
            // Normierte Werte (0.0 bis 1.0)
            val iNorm = i.toDouble() / lineCount
            
            // Farbe mischen basierend auf Index
            drawer.stroke = colorStart.mix(colorEnd, iNorm)

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
        val baseWave = sin(x * 10.0 + time + i * 2.0)
        
        // 2. Schnelle Welle (Reagiert auf Musik)
        val interference = sin(x * (20.0 + energy * 0.1) - time * 2.0 + i * 5.0)
        
        // 3. Verdrehung
        val twist = cos(x * 5.0 + time)
        
        // Amplitude (Wie hoch die Welle ist)
        val spread = 100.0 + energy * 2.5
        
        return (baseWave * interference * twist) * spread
    }
}