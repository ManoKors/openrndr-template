import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector2
import kotlin.math.sin

class DreamHazeScene : Scene() {

    // Eine Palette wie ein Sonnenuntergang im Nebel
    // Sehr weiche, nicht zu gesättigte Farben
    private val colors = listOf(
        ColorRGBa.fromHex("FF9A9E"), // Sanftes Pfirsich-Pink
        ColorRGBa.fromHex("FECFEF"), // Helles Pastell-Lila
        ColorRGBa.fromHex("A18CD1"), // Lavendel
        ColorRGBa.fromHex("FBC2EB"), // Rose
        ColorRGBa.fromHex("84FAB0")  // Ein Hauch Minze für Kontrast
    )

    // Wir erstellen 5 große "Lichtquellen"
    private val orbs = List(5) { id -> Orb(id) }

    data class Orb(val id: Int)

    override fun draw(drawer: Drawer, time: Double, bassEnergy: Double, midEnergy: Double, highEnergy: Double, width: Int, height: Int) {
        // 1. Hintergrund: Ein warmes Off-White oder sehr helles Grau
        // Das wirkt papierartiger und entspannter als hartes Schwarz
        drawer.clear(ColorRGBa.fromHex("FFF0F5")) 

        // Wir nutzen "MULTIPLY" oder "SUBTRACT" für dunkle Farben, 
        // aber hier wollen wir Licht mischen -> Normales Blending mit Transparenz reicht oft.
        // Für diesen "Traum-Look" ist einfaches Überlagern am besten.
        
        drawer.stroke = null

        orbs.forEachIndexed { index, orb ->
            val color = colors[index % colors.size]

            // --- Die Chill-Mathematik ---
            
            // Bewegung: Simplex Noise ist der König des "Smoothness".
            // Wir bewegen uns extrem langsam durch das Rauschfeld (time * 0.05).
            // Die Kreise sollen sich über den ganzen Bildschirm verteilen können.
            val x = simplex(index, time * 0.05) * (width * 0.9) + (width / 2.0)
            val y = simplex(index + 100, time * 0.06) * (height * 0.9) + (height / 2.0)
            
            // Radius: Die Kreise sollen groß sein, aber nicht den ganzen Bildschirm ausfüllen.
            // Das ermöglicht es, dass sich die Farben besser abheben.
            val baseRadius = width * 0.3
            val breathing = sin(time * 0.5 + index) * 50.0
            val bassPunch = bassEnergy * 30.0 // Reagiert nur sanft
            val radius = baseRadius + breathing + bassPunch

            // Farbe: Etwas weniger transparent, damit die Farben besser sichtbar bleiben
            drawer.fill = color.opacify(0.6)
            
            drawer.circle(x, y, radius)
        }

        // 2. Der "Lo-Fi" Effekt (Filmkorn)
        // Das ist das Geheimnis für "Aesthetic" Looks. Ein bisschen Rauschen darüberlegen,
        // damit es nicht zu digital/glatt wirkt.
        drawFilmGrain(drawer, width, height)
    }

    private fun drawFilmGrain(drawer: Drawer, width: Int, height: Int) {
        // Wir zeichnen zufällige winzige Punkte
        // Um Performance zu sparen, machen wir das nicht zu dicht, aber genug für Textur.
        val grainCount = (width * height) / 400 
        
        drawer.strokeWeight = 1.0
        // Dunkle, fast transparente Punkte
        drawer.stroke = ColorRGBa.BLACK.opacify(0.05) 
        drawer.fill = null

        // batching für Performance (points statt loop)
        val points = List(grainCount) {
            Vector2(Math.random() * width, Math.random() * height)
        }
        drawer.points(points)
    }
}
