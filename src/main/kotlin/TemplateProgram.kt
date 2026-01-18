import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.camera.OrbitalCamera
import org.openrndr.extra.fx.blur.Bloom
import org.openrndr.extra.minim.minim
import org.openrndr.extra.noise.perlin
import org.openrndr.math.Vector3
import org.openrndr.math.map
import org.openrndr.Keyboard
import ddf.minim.analysis.FFT
import kotlin.math.abs
import kotlin.math.sin

fun main() = application {
    configure {
        width = 1920
        height = 1080
        title = "Neon Horizon: Ultimate Edition"
        windowResizable = true
    }

    program {
        // --- 1. AUDIO SETUP ---
        val minim = minim()
        // Nutzt Mikrofon/System-Mix. Für MP3: minim.loadFile("pfad.mp3")
        val player = minim.loadFile("data/audio/track.mp3")
        player.play()
        player.mute()  // Start muted
        val fft = FFT(player.bufferSize(), player.sampleRate())

        // --- 2. GRAFIK SETUP (BUFFERS) ---
        // Wir rendern in ein Offscreen-Target, um Bloom anwenden zu können
        val mainTarget = renderTarget(width, height) {
            colorBuffer()
            depthBuffer()
        }

        // FX: Bloom Setup (Das Glühen)
        val bloom = Bloom().apply {
            brightness = 0.1 // Schwelle: Alles was dunkler ist, glüht nicht
            // sigma = 20.0     // Streuung: Wie breit das Glühen ist
            // gain = 2.0       // Stärke: Wie hell es ist
        }

        // Kamera: Tief und nah am Boden
        val cam = OrbitalCamera(Vector3(0.0, 20.0, 150.0), Vector3(0.0, 10.0, 0.0), 90.0, 0.1, 1000.0)

        // Grid Einstellungen
        val cols = 60
        val rows = 50
        val cellW = 15.0
        val cellD = 15.0
        
        // Farben definieren
        val deepPurple = ColorRGBa.fromHex("120024")
        val neonPink = ColorRGBa.fromHex("FF007F")
        val neonCyan = ColorRGBa.fromHex("00FFFF")
        val sunColorTop = ColorRGBa.fromHex("FFD700") // Gold
        val sunColorBot = ColorRGBa.fromHex("FF007F") // Pink

        var isMuted = true  // Track mute status

        extend(cam)
        extend {
            // Audio Update
            fft.forward(player.mix)

            // Bass Energie berechnen (0 - 150Hz)
            val bass = fft.calcAvg(0f, 150f) * 10.0
            
            // Zeit für Meer-Bewegung (langsamer für smooth)
            val time = seconds * 0.4

            // Tastensteuerung für Mute
            keyboard.keyDown.listen {
                if (it.name == "m" || it.name == "M") {
                    if (isMuted) {
                        player.unmute()
                        isMuted = false
                    } else {
                        player.mute()
                        isMuted = true
                    }
                }
            }

            // --- 3. RENDERING START ---
            drawer.isolatedWithTarget(mainTarget) {
                drawer.clear(deepPurple) // Hintergrundfarbe
                drawer.depthWrite = true
                drawer.depthTestPass = DepthTestPass.LESS_OR_EQUAL

                // A. DIE SONNE (Hintergrund)
                drawer.isolated {
                    drawer.defaults() // Kamera resetten für 2D Hintergrund-Elemente
                    
                    // Sonne Position (fest im Hintergrund)
                    val sunX = width / 2.0
                    val sunY = height / 3.0
                    val sunRadius = 150.0

                    // 1. Reflexion der Sonne (Unscharf auf dem Boden)
                    // Wir zeichnen ein gestrecktes Oval unter der Sonne
                    drawer.shadeStyle = shadeStyle {
                        fragmentTransform = """
                            vec2 pos = c_boundsPosition.xy;
                            float alpha = 1.0 - smoothstep(0.0, 1.0, abs(pos.y - 0.5) * 2.0);
                            x_fill.a *= alpha * 0.3; // 30% Transparenz
                        """.trimIndent()
                    }
                    drawer.fill = neonPink
                    drawer.stroke = null
                    // Gespiegelte Sonne, vertikal gestreckt
                    drawer.rectangle(sunX - sunRadius * 1.5 / 2, sunY + sunRadius + 50.0 - sunRadius * 3.0 / 2, sunRadius * 1.5, sunRadius * 3.0) 

                    // 2. Die eigentliche Sonne
                    drawer.shadeStyle = shadeStyle {
                        // Shader für den Farbverlauf + Scanlines (Jalousien)
                        fragmentTransform = """
                            vec2 uv = c_boundsPosition.xy;
                            // Verlauf von Gelb (oben) zu Pink (unten)
                            vec4 c = mix(p_colBot, p_colTop, uv.y);
                            
                            // Scanlines: Schneide Streifen aus
                            float stripes = smoothstep(0.0, 0.1, sin(uv.y * 40.0 + 1.0) - 0.2);
                            
                            // Mache die unteren Streifen dicker
                            if(uv.y < 0.5) {
                                stripes *= smoothstep(0.0, 1.0, uv.y * 2.0);
                            }
                            
                            x_fill = c;
                            x_fill.a *= stripes; 
                        """.trimIndent()
                        parameter("colTop", sunColorTop)
                        parameter("colBot", sunColorBot)
                    }
                    drawer.rectangle(sunX - sunRadius, sunY - sunRadius, sunRadius * 2.0, sunRadius * 2.0)
                }

                // B. DAS GITTER (NEXT GEN WATER PHYSICS)
                drawer.shadeStyle = null // Reset Shader
                drawer.translate(-cols * cellW / 2.0, -10.0, -rows * cellD / 2.0) // Grid zentrieren

                // Linien zeichnen
                drawer.strokeWeight = 1.5
                
                for (z in 0 until rows) {
                    // Wir berechnen die Vertices für einen "Strip"
                    // Wir nutzen LINE_STRIP damit es zusammenhängend ist
                    
                    // Farbe faden: Vorne Hell, hinten Dunkel (Nebel-Effekt)
                    val depthAlpha = map(0.0, rows.toDouble(), 1.0, 0.0, z.toDouble())
                    drawer.stroke = neonCyan.opacify(depthAlpha)

                    // Horizontale Linien (über X)
                    val points = mutableListOf<Vector3>()
                    for (x in 0 until cols) {
                        val px = x * cellW
                        val pz = z * cellD
                        
                        // Meer-artige Perlin-Noise
                        val noiseH = perlin(0, x * 0.05, z * 0.05 + time * 0.2) * 15.0
                        
                        // Audio Reaktivität: Bass skaliert die Wellen, Grundlinie bleibt konstant
                        val scale = 1.0 + bass * 0.01

                        val py = noiseH * scale

                        points.add(Vector3(px, py, pz))
                    }
                    drawer.lineStrip(points)
                }

                // Vertikale Linien (Längsstreifen) - optional für den kompletten Gitter-Look
                // Wir zeichnen sie etwas transparenter
                drawer.stroke = neonCyan.opacify(0.3)
                for (x in 0 until cols step 2) { // Nur jede 2. Linie für Style
                    val points = mutableListOf<Vector3>()
                    for (z in 0 until rows) {
                        val px = x * cellW
                        val pz = z * cellD
                        
                        // Meer-artige Perlin-Noise
                        val noiseH = perlin(0, x * 0.05, z * 0.05 + time * 0.2) * 15.0
                        
                        // Audio Reaktivität: Bass skaliert die Wellen, Grundlinie bleibt konstant
                        val scale = 1.0 + bass * 0.01

                        val py = noiseH * scale
                        
                        points.add(Vector3(px, py, pz))
                    }
                    drawer.lineStrip(points)
                }
            }
            
            // --- 4. POST PROCESSING ---
            bloom.apply(mainTarget.colorBuffer(0), mainTarget.colorBuffer(0))
            
            // --- 5. FINALE AUSGABE ---
            drawer.defaults()
            drawer.image(mainTarget.colorBuffer(0))
        }
    }
}
