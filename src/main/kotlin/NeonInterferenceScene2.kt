import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.math.Vector2
import kotlin.math.cos
import kotlin.math.sin

class NeonInterferenceScene2 : Scene() {
    // Erweiterte Farbpalette für mehr Dynamik
    private val colors = listOf(
        ColorRGBa.fromHex("00FF41"), // Neon Grün
        ColorRGBa.fromHex("EBFF00"), // Neon Gelb
        ColorRGBa.fromHex("FF0080"), // Neon Pink
        ColorRGBa.fromHex("00FFFF"), // Cyan
        ColorRGBa.fromHex("FF8000"), // Orange
        ColorRGBa.fromHex("8000FF"), // Lila
        ColorRGBa.fromHex("FF0000"), // Rot
        ColorRGBa.fromHex("00FF00"), // Grün
        ColorRGBa.fromHex("0000FF"), // Blau
        ColorRGBa.fromHex("FFFF00")  // Gelb
    )

    private fun getDynamicColor(time: Double, bassEnergy: Double, midEnergy: Double, highEnergy: Double): ColorRGBa {
        val period = 2.0 + (bassEnergy + midEnergy + highEnergy) * 0.01 // Sich ändernde Periode
        val t = (time / period) % 1.0
        val index = (time / period).toInt() % colors.size
        val nextIndex = (index + 1) % colors.size
        val baseColor = colors[index].mix(colors[nextIndex], t)
        // Helligkeit begrenzt
        val brightnessFactor = (0.6 + (bassEnergy + midEnergy + highEnergy) * 0.01).coerceAtMost(0.9)
        return baseColor * brightnessFactor
    }

    // Dynamische Einstellungen: dickere Linien, weniger Linien
    private fun getLineCount(energy: Double): Int = (50 + energy * 0.3).toInt().coerceIn(30, 80)
    private fun getLineWeight(energy: Double): Double = 2.0 + energy * 0.05

    override fun draw(drawer: Drawer, time: Double, bassEnergy: Double, midEnergy: Double, highEnergy: Double, width: Int, height: Int) {
        val dynamicLineCount = getLineCount(bassEnergy + midEnergy + highEnergy)
        val dynamicLineWeight = getLineWeight(bassEnergy + midEnergy + highEnergy)

        drawer.strokeWeight = dynamicLineWeight
        drawer.fill = null // Keine Füllung, nur Linien

        for (i in 0 until dynamicLineCount) {
            val iNorm = i.toDouble() / dynamicLineCount

            // Dynamische Farbe basierend auf allen Energies
            drawer.stroke = getDynamicColor(time + iNorm * 10.0, bassEnergy, midEnergy, highEnergy)

            val points = mutableListOf<Vector2>()

            // Punkte für die Linie berechnen, mit mehr Steps für Glattheit
            for (x in 0..width step 1) { // Feiner für mehr Detail
                val xNorm = x.toDouble() / width

                val yOffset = calculateCrazyInterference(xNorm, iNorm, time, bassEnergy, midEnergy, highEnergy)

                val y = (height / 2.0) + yOffset
                points.add(Vector2(x.toDouble(), y))
            }

            drawer.lineStrip(points)
        }
    }

    // Krazzy Version der Formel
    private fun calculateCrazyInterference(x: Double, i: Double, time: Double, bassEnergy: Double, midEnergy: Double, highEnergy: Double): Double {
        // Mehr Wellen für Komplexität, Ausschlag reduziert
        val baseWave = sin(x * 15.0 + time * 1.5 + i * 3.0) * (1.0 + bassEnergy * 0.02) // Bass
        val secondaryWave = sin(x * 8.0 - time * 0.8 + i * 7.0) * (1.0 + midEnergy * 0.02) // Mids

        // Starke Reaktion, Ausschlag reduziert
        val interference = sin(x * (25.0 + highEnergy * 0.04) - time * 2.0 + i * 6.0) // Highs
        val twist = cos(x * 7.0 + time * 1.2 + (bassEnergy + midEnergy) * 0.01)

        // Chaos, Ausschlag reduziert
        val chaosWave = sin(x * 50.0 + time * 3.0 + i * 10.0) * (0.5 + (midEnergy + highEnergy) * 0.01)

        // Amplitude reduziert für weniger Ausschlag
        val spread = 25.0 + (bassEnergy * 0.1 + midEnergy * 0.08 + highEnergy * 0.05)

        return (baseWave * interference * twist + secondaryWave * chaosWave) * spread
    }
}