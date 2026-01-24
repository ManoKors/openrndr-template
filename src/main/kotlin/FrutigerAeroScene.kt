import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.shadeStyle
import org.openrndr.math.map
import kotlin.math.sin
import kotlin.random.Random

class FrutigerAeroScene : Scene() {

    // --- 1. Die klassische Farbpalette ---
    private val aeroCyan = ColorRGBa.fromHex("00D2FF")
    private val aeroGreen = ColorRGBa.fromHex("7CFC00")
    private val aeroGlass = ColorRGBa.fromHex("E0FFFF").opacify(0.3) // Etwas transparenter

    // --- 2. Erweiterte Bubble Klasse ---
    private data class Bubble(
        var x: Double,
        var y: Double,
        var baseRadius: Double,
        var speed: Double,
        var wobbleOffset: Double,
        var isReactive: Boolean // Geändert zu var für einfacheres Respawning
    )

    // Wir erstellen 60 Blasen
    private val bubbles = MutableList(60) {
        spawnBubble(it * 100.0, 1920.0) // Breite als Parameter
    }

    // Hilfsfunktion: Neue Blase erstellen
    private fun spawnBubble(startY: Double? = null, width: Double = 1920.0): Bubble {
        // 40% Chance, dass eine Blase auf Musik reagiert (erhöht von 20%)
        val isReactive = Random.nextDouble() > 0.6 

        return Bubble(
            // Reactive Bubbles spawnen öfter in der Mitte für bessere Sichtbarkeit
            x = if (isReactive) {
                // Reactive: Konzentriert in der Mitte (40% der Breite um die Mitte)
                val centerX = width / 2.0
                val spread = width * 0.2 // 20% links und rechts von der Mitte
                Random.nextDouble(centerX - spread, centerX + spread)
            } else {
                // Ambient: Über die gesamte Breite
                Random.nextDouble(0.0, width)
            },
            y = startY ?: 1200.0,
            // Reactive Bubbles sind tendenziell etwas größer
            baseRadius = if (isReactive) Random.nextDouble(30.0, 70.0) else Random.nextDouble(10.0, 40.0),
            
            // WICHTIG: Viel langsamer!
            // Ambient: 0.2 bis 0.5 (sehr langsam)
            // Reactive: 0.5 bis 1.0 (etwas schneller)
            speed = if (isReactive) Random.nextDouble(0.5, 1.0) else Random.nextDouble(0.2, 0.5),
            
            wobbleOffset = Random.nextDouble(0.0, 10.0),
            isReactive = isReactive
        )
    }

    override fun draw(drawer: Drawer, time: Double, bassEnergy: Double, width: Int, height: Int) {
        
        // --- 3. Hintergrund (Wie vorher, leicht verlangsamt) ---
        drawer.shadeStyle = shadeStyle {
            fragmentTransform = """
                vec2 uv = c_boundsPosition.xy;
                float wave = sin(uv.x * 2.0 + p_time * 0.2) * 0.15; // Langsamere Welle
                float mixFactor = smoothstep(0.0, 1.0, uv.y + wave);
                vec4 top = p_col1;
                vec4 bot = p_col2;
                x_fill = mix(top, bot, mixFactor);
                float glow = 1.0 - length(uv - vec2(0.5, 0.3));
                x_fill += vec4(1.0) * glow * 0.2; 
            """.trimIndent()
            parameter("time", time)
            parameter("col1", aeroCyan)
            parameter("col2", aeroGreen)
        }
        drawer.rectangle(0.0, 0.0, width.toDouble(), height.toDouble())
        drawer.shadeStyle = null

        // --- 4. Bubbles Update & Draw ---
        bubbles.forEach { bubble ->
            
            var currentRadius = bubble.baseRadius
            var currentYMove = bubble.speed

            // UNTERSCHEIDUNG: Ambient vs. Reactive
            if (bubble.isReactive) {
                // REACTIVE: Reagiert stark auf Bass
                // Bass (0-100) mappen auf Radius-Zuwachs
                val beatKick = map(0.0, 100.0, 0.0, 40.0, bassEnergy).coerceAtLeast(0.0)
                currentRadius += beatKick
                
                // Blase wird schneller wenn Musik laut ist
                currentYMove += (bassEnergy * 0.05)
            } else {
                // AMBIENT: Reagiert fast gar nicht, schwebt nur
                // Minimales Pulsieren für "Leben"
                currentRadius += sin(time * 2.0 + bubble.wobbleOffset) * 2.0
            }

            // Bewegung anwenden
            bubble.y -= currentYMove
            
            // Seitliches Wackeln (Reactive wackelt stärker)
            val wobbleAmp = if (bubble.isReactive) 30.0 else 10.0
            val currentX = bubble.x + sin(time * 1.5 + bubble.wobbleOffset) * wobbleAmp

            // Respawn Logik
            // Wir geben einen Puffer oben (-150), damit große Blasen ganz verschwinden
            if (bubble.y < -150.0) {
                val newB = spawnBubble(height.toDouble() + 50.0, width.toDouble())
                // Komplett neue Blase erstellen
                bubble.x = newB.x
                bubble.y = newB.y
                bubble.speed = newB.speed
                bubble.baseRadius = newB.baseRadius
                bubble.isReactive = newB.isReactive
                bubble.wobbleOffset = newB.wobbleOffset
            }

            // ZEICHNEN
            
            // 1. Körper
            drawer.stroke = null
            // Reactive Bubbles leuchten in hellem Cyan wenn der Bass kickt
            // Schwelle gesenkt auf 30, Mapping angepasst
            drawer.fill = if (bubble.isReactive && bassEnergy > 30.0) {
                // Reactive bubbles now use aeroGreen instead of cyan
                val intensity = map(30.0, 100.0, 0.6, 1.0, bassEnergy).coerceIn(0.4, 1.0)
                aeroGreen.opacify(intensity)
            } else {
                aeroGlass
            }
            
            drawer.circle(currentX, bubble.y, currentRadius)

            // 2. Rand
            drawer.fill = null
            drawer.stroke = if (bubble.isReactive && bassEnergy > 30.0) {
                aeroGreen.opacify(0.95) // Hellerer grüner Rand für reactive bubbles
            } else {
                ColorRGBa.WHITE.opacify(0.6)
            }
            drawer.strokeWeight = if (bubble.isReactive && bassEnergy > 30.0) 2.5 else 1.5 // Dickere Linie für reactive
            drawer.circle(currentX, bubble.y, currentRadius)

            // 3. Glanzlicht (Highlight)
            drawer.fill = if (bubble.isReactive && bassEnergy > 30.0) {
                aeroGreen.opacify(1.0)
            } else {
                ColorRGBa.WHITE.opacify(0.9)
            }
            drawer.stroke = null
            val highlightOffset = currentRadius * 0.35
            val highlightSize = currentRadius * 0.25
            drawer.circle(currentX - highlightOffset, bubble.y - highlightOffset, highlightSize)
        }
        
        // --- 5. Optional: Bokeh Flash im Vordergrund ---
        // Nur ganz subtil bei sehr starken Beats
        if (bassEnergy > 80.0) {
             drawer.fill = ColorRGBa.WHITE.opacify(0.1)
             drawer.rectangle(0.0, 0.0, width.toDouble(), height.toDouble())
        }
    }
}