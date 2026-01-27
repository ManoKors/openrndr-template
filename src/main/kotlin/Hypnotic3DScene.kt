import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.objloader.loadOBJ
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.lookAt
import kotlin.math.cos
import kotlin.math.sin

class Hypnotic3DScene : Scene() {

    private var mesh: VertexBuffer? = null
    private var hasTriedLoading = false

    override fun draw(drawer: Drawer, time: Double, bassEnergy: Double, midEnergy: Double, highEnergy: Double, width: Int, height: Int) {

        // --- 1. LADE-LOGIK (Unverändert & Robust) ---
        if (!hasTriedLoading && mesh == null) {
            hasTriedLoading = true
            try {
                val rawGroups = loadOBJ("data/models/7of9.obj")
                val allFaces = rawGroups.values.flatten()
                
                if (allFaces.isNotEmpty()) {
                    // Sammle alle Positionen, um den Mittelpunkt zu berechnen
                    val allPositions = mutableListOf<Vector3>()
                    for (face in allFaces) {
                        allPositions.addAll(face.positions)
                    }
                    
                    // Berechne den Mittelpunkt
                    val center = if (allPositions.isNotEmpty()) {
                        val sum = allPositions.reduce { acc, vec -> acc + vec }
                        sum / allPositions.size.toDouble()
                    } else {
                        Vector3.ZERO
                    }
                    
                    var vertexCount = 0
                    for (face in allFaces) {
                        if (face.positions.size == 4) vertexCount += 6 else vertexCount += 3
                    }

                    val vFormat = vertexFormat {
                        position(3)
                        normal(3)
                    }
                    mesh = vertexBuffer(vFormat, vertexCount)

                    mesh?.put {
                        for (face in allFaces) {
                            val pos = face.positions
                            val p0 = pos.getOrElse(0) { Vector3.ZERO } - center
                            val p1 = pos.getOrElse(1) { Vector3.ZERO } - center
                            val p2 = pos.getOrElse(2) { Vector3.ZERO } - center
                            var normal = (p1 - p0).cross(p2 - p0).normalized
                            if (normal.x.isNaN()) normal = Vector3.UNIT_Y

                            fun writePoint(index: Int) {
                                val centeredPos = pos.getOrElse(index) { Vector3.ZERO } - center
                                write(centeredPos)
                                write(normal)
                            }

                            writePoint(0); writePoint(1); writePoint(2)
                            if (pos.size == 4) {
                                writePoint(0); writePoint(2); writePoint(3)
                            }
                        }
                    }
                } 
            } catch (e: Exception) {
                println("Ladefehler: ${'$'}{e.message}")
            }
        }

        if (mesh == null) return

        drawer.clear(ColorRGBa.fromHex("050510"))

        // --- 2. RENDER SETTINGS ---
        drawer.perspective(60.0, width.toDouble() / height, 0.1, 1000.0)
        drawer.depthWrite = true
        drawer.depthTestPass = DepthTestPass.LESS_OR_EQUAL
        drawer.drawStyle.cullTestPass = CullTestPass.BACK 

        // Kamera (Bewegte Kamera für Drehen und Zoomen)
        val radius = 200.0 + sin(time * 0.2) * 100.0  // Radius variiert für Zoom-Effekt
        val height = 150.0 + sin(time * 0.1) * 50.0   // Höhe variiert
        val angle = time * 0.5  // Drehgeschwindigkeit
        Scene.cameraEye = Vector3(cos(angle) * radius, sin(angle) * radius, height)
        drawer.view = lookAt(Scene.cameraEye, Vector3.ZERO, Vector3.UNIT_Y)

        // --- 3. DER LASER SHADER (Hier ist der neue Effekt!) ---
        drawer.shadeStyle = shadeStyle {
            // Parameterübergabe
            parameter("time", time)
            parameter("bass", bassEnergy) // Helligkeit bei Bass
            parameter("mid", midEnergy)   // Für Mid-Laser
            parameter("high", highEnergy) // Farbeffekte bei Höhen
            
            // Farben definieren
            parameter("colorBody", ColorRGBa.fromHex("0088AA")) // Körper: Dunkles Türkis
            parameter("colorLaser", ColorRGBa.fromHex("FF0055")) // Laser: Neon Pink/Rot

            fragmentTransform = """
                // Normale holen
                vec3 n = normalize(va_normal);
                
                // 1. Grund-Beleuchtung (damit der Körper plastisch wirkt)
                // Licht kommt von vorne (vec3(0,0,1))
                float light = max(dot(n, vec3(0.0, 0.0, 1.0)), 0.1);
                
                // 2. Mehrere Laser-Scans (Audio-reaktiv) - Erweitert um mehr Farben
                // Bass-Laser: Rot (mit leichtem Blau-Anteil)
                float speedBass = p_bass * 2.0;  // Keine Mindestgeschwindigkeit, bei Stille still
                float rangeBass = 40.0 + p_bass * 30.0;
                float scanPosBass = sin(p_time * speedBass) * rangeBass;
                float distBass = abs(va_position.y - scanPosBass);
                float beamBass = smoothstep(1.2, 0.0, distBass);
                
                // Mid-Laser: Grün (mit leichtem Rot-Anteil)
                float speedMid = p_mid * 2.5;
                float rangeMid = 42.0 + p_mid * 35.0;
                float scanPosMid = sin(p_time * speedMid + 2.094) * rangeMid;
                float distMid = abs(va_position.y - scanPosMid);
                float beamMid = smoothstep(1.2, 0.0, distMid);
                
                // High-Laser: Blau (mit leichtem Grün-Anteil)
                float speedHigh = p_high * 3.0;
                float rangeHigh = 44.0 + p_high * 40.0;
                float scanPosHigh = sin(p_time * speedHigh + 4.188) * rangeHigh;
                float distHigh = abs(va_position.y - scanPosHigh);
                float beamHigh = smoothstep(1.2, 0.0, distHigh);
                
                // Extra-Laser: Cyan (kombiniert Bass und Highs)
                float speedExtra = (p_bass + p_high) * 1.5;
                float rangeExtra = 38.0 + (p_bass + p_high) * 20.0;
                float scanPosExtra = sin(p_time * speedExtra + 1.047) * rangeExtra;
                float distExtra = abs(va_position.y - scanPosExtra);
                float beamExtra = smoothstep(1.2, 0.0, distExtra);
                
                // 3. Audio-Reaktion für Blitze
                float highBoost = 1.0 + p_high * 6.0;
                
                // 4. Farben mischen - Erweitert um mehr dynamische Farben
                vec3 bodyColor = vec3(0.0); // Figur bleibt schwarz für Kontrast
                
                // Zeit-basierte Farbverschiebung für leichte Veränderungen
                float colorShift = sin(p_time * 0.5) * 0.1 + 0.05; // Leichte Oszillation
                
                // Laser-Farben additiv kombinieren (noch krasser und farbenreicher)
                vec3 laserColor = vec3(0.0);
                laserColor += beamBass * vec3(1.0 + colorShift, 0.0, 0.2 + colorShift) * highBoost * 5.0; // Rot mit Blau-Anteil
                laserColor += beamMid * vec3(0.0, 1.0 + colorShift, 0.2 + colorShift) * highBoost * 5.0;  // Grün mit Blau-Anteil
                laserColor += beamHigh * vec3(0.2 + colorShift, 0.0, 1.0 + colorShift) * highBoost * 5.0; // Blau mit Rot-Anteil
                laserColor += beamExtra * vec3(0.0, 1.0 + colorShift, 1.0 + colorShift) * highBoost * 4.0; // Cyan mit Grün-Anteil
                
                // Finale Farbe
                x_fill = vec4(bodyColor + laserColor, 1.0);
            """.trimIndent()
        }

        // --- 4. ZEICHNEN (Deine funktionierende Ausrichtung) ---
        drawer.isolated {
            drawer.scale(3.0)  // Objekt größer machen
            // Rotation beibehalten, da dein Modell so perfekt steht
            drawer.rotate(Vector3.UNIT_X, -90.0)

            mesh?.let { drawer.vertexBuffer(it, DrawPrimitive.TRIANGLES) }
        }
    }
}