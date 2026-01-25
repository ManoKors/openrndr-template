import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.math.Vector2
import org.openrndr.math.Polar
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

class HypnoticRadialScene : Scene() {

    // Wir nutzen eine sanftere Palette (Cyan, Magenta, Tiefblau)
    private val colorA = ColorRGBa.fromHex("00C9FF") // Helles Cyan
    private val colorB = ColorRGBa.fromHex("92FE9D") // Sanftes Grün
    private val colorC = ColorRGBa.fromHex("FC00FF") // Magenta

    // Parameter
    private val rings = 40          // Wie viele Kreise?
    private val pointsPerRing = 120 // Auflösung pro Kreis (je höher, desto runder)

    override fun draw(drawer: Drawer, time: Double, bassEnergy: Double, midEnergy: Double, highEnergy: Double, width: Int, height: Int) {
        // Verschiebe den Nullpunkt in die Mitte des Bildschirms
        drawer.translate(width / 2.0, height / 2.0)
        
        drawer.fill = null
        // Dünnere Linien wirken hier oft eleganter
        drawer.strokeWeight = 1.5 

        for (i in 0 until rings) {
            val iNorm = i.toDouble() / rings
            
            // Dynamische Farbe: Ändert sich von innen nach außen und über die Zeit
            val colorTime = time * 0.5
            val blend = (sin(iNorm * 3.0 + colorTime) + 1.0) / 2.0 
            // Mische zwischen den Hauptfarben
            val ringColor = if (blend < 0.5) {
                colorA.mix(colorB, blend * 2.0)
            } else {
                colorB.mix(colorC, (blend - 0.5) * 2.0)
            }
            
            // Transparenz: Außen verblassen die Ringe leicht
            drawer.stroke = ringColor.opacify(1.0 - iNorm * 0.5)

            val points = mutableListOf<Vector2>()

            // Berechnung der geschlossenen Form (Loop um 360 Grad)
            for (j in 0 until pointsPerRing) {
                // Winkel in Grad und Radiant
                val angleDeg = (j.toDouble() / pointsPerRing) * 360.0
                val angleRad = Math.toRadians(angleDeg)

                // Hier passiert die Magie: Radius-Modulation
                val radius = calculateRadius(angleRad, iNorm, time, bassEnergy, width.toDouble())
                
                // Umwandlung Polar -> Kartesisch (automatisch durch OpenRNDR Polar Klasse oder manuell)
                // Manuell: x = cos(a)*r, y = sin(a)*r
                points.add(Polar(angleDeg, radius).cartesian)
            }
            
            // Schließe den Kreis
            drawer.lineStrip(points + listOf(points.first()))
        }
    }

    private fun calculateRadius(angle: Double, iNorm: Double, time: Double, energy: Double, screenWidth: Double): Double {
        // 1. Basis-Radius: Ringe werden nach außen größer
        // Wir nutzen pow, damit sie in der Mitte dichter sind (Tunneleffekt)
        val baseRadius = (iNorm * iNorm) * (screenWidth * 0.6)

        // 2. Rotation: Jeder Ring dreht sich leicht anders (iNorm beeinflusst speed)
        // Das erzeugt den "Sog"-Effekt
        val rotationOffset = time * (0.5 - iNorm) * 2.0 
        val effectiveAngle = angle + rotationOffset

        // 3. Organische Verzerrung (Blütenform)
        // Wir überlagern zwei Sinuswellen mit unterschiedlichen Frequenzen (5 und 3)
        val wave1 = sin(effectiveAngle * 5.0) 
        val wave2 = cos(effectiveAngle * 3.0 + time)
        
        // Wie stark schlägt die Welle aus?
        // Bass macht die Wellen zackiger/höher
        val waveAmplitude = 10.0 + (50.0 * iNorm) + (energy * 0.5)

        // 4. "Atmen": Der ganze Kreis pulsiert im Takt
        val breathing = sin(time * 2.0 - iNorm * 4.0) * (20.0 + energy)

        return baseRadius + (wave1 * wave2 * waveAmplitude) + breathing
    }
}