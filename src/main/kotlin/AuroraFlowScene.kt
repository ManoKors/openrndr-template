import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random

class AuroraFlowScene : Scene() {

    // Ein Partikel ist hier nur eine Position. Wir brauchen tausende davon.
    private val particleCount = 2000
    private val particles = MutableList(particleCount) { Vector2.ZERO }
    
    // Farben für den Verlauf (Nordlicht-Style: Grün zu Violett)
    private val color1 = ColorRGBa.fromHex("00FFCC") // Türkis
    private val color2 = ColorRGBa.fromHex("BD00FF") // Violett

    private var initialized = false

    // Initialisierung: Verteile Punkte zufällig auf dem Schirm
    private fun initParticles(width: Int, height: Int) {
        for (i in 0 until particleCount) {
            particles[i] = Vector2(
                Random.nextDouble() * width,
                Random.nextDouble() * height
            )
        }
        initialized = true
    }

    override fun draw(drawer: Drawer, time: Double, bassEnergy: Double, midEnergy: Double, highEnergy: Double, width: Int, height: Int) {
        if (!initialized) initParticles(width, height)

        // TRICK 1: Trails erzeugen
        // Statt drawer.clear() zeichnen wir ein fast transparentes Rechteck über alles.
        // Das lässt die alten Zeichnungen langsam verblassen -> Schlieren-Effekt.
        drawer.isolated {
            drawer.stroke = null
            // Das Schwarz hat nur 10% Deckkraft (0.1). Je kleiner die Zahl, desto länger die Spuren.
            drawer.fill = ColorRGBa.BLACK.opacify(0.5) 
            drawer.rectangle(0.0, 0.0, width.toDouble(), height.toDouble())
        }

        drawer.strokeWeight = 1.5

        // Wir gehen jeden Partikel durch und bewegen ihn
        for (i in 0 until particleCount) {
            val p = particles[i]

            // TRICK 2: Das Flow Field (Die Mathematik)
            // Wir fragen die Noise-Funktion nach einem Wert für die aktuelle Position.
            // Aber: Wir interpretieren das Ergebnis als WINKEL (Rotation).
            
            // scale: Wie "zoomt" man in das Rauschen? 0.003 = weiche, große Kurven.
            val scale = 0.003
            
            // Der Bass beeinflusst, wie chaotisch das Feld sich verändert
            val noiseVal = simplex(
                seed = 100, 
                x = p.x * scale, 
                y = p.y * scale, 
                z = time * 0.1 // Z verändert das Muster langsam über die Zeit
            )
            
            // Noise (-1 bis 1) wird zu Winkel (0 bis 2 PI * 2) gemappt -> Volle Drehung möglich
            val angle = noiseVal * PI * 4 

            // Berechne die neue Richtung
            val direction = Vector2(cos(angle), sin(angle))
            
            // Bewegung: Geschwindigkeit erhöht sich leicht mit dem Bass
            val speed = 2.0 + (bassEnergy * 0.05)
            val newPos = p + (direction * speed)

            // --- Zeichnen ---
            
            // Farbe basierend auf x-Position (Links Türkis, Rechts Violett)
            val mixFactor = (p.x / width).coerceIn(0.0, 1.0)
            drawer.stroke = color1.mix(color2, mixFactor).opacify(0.6)
            
            // Wir zeichnen eine winzige Linie von alt nach neu
            drawer.lineSegment(p, newPos)

            // --- Reset ---
            // Wenn der Punkt den Bildschirm verlässt, setzen wir ihn zufällig neu
            // ODER wir "wrappen" ihn um (kommt links wieder rein). Hier: Random Reset für besseren Look.
            if (newPos.x < 0 || newPos.x > width || newPos.y < 0 || newPos.y > height) {
                particles[i] = Vector2(Random.nextDouble() * width, Random.nextDouble() * height)
            } else {
                particles[i] = newPos
            }
        }
    }
}