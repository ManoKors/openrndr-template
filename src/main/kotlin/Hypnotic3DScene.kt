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
                
                // 2. Scan-Position berechnen
                // Der Laser fährt hoch und runter (Sinus-Welle)
                // Die '15.0' bestimmt, wie weit er fährt (-15 bis +15 auf der Y-Achse)
                // Falls der Laser verschwindet, ändere diese Zahl!
                float scanPos = sin(p_time * 1.5) * 15.0; 
                
                // Abstand dieses Pixels zur Laser-Linie
                float dist = abs(va_position.y - scanPos);
                
                // 3. Laser-Strahl zeichnen
                // smoothstep(Breite, 0.0, dist) -> Je kleiner die erste Zahl, desto schärfer der Laser
                float beam = smoothstep(1.2, 0.0, dist); 
                
                // 4. Audio-Reaktion
                float bassGlow = p_bass * 1.5; // Körper leuchtet bei Bass auf
                float highFlash = p_high * 2.0; // Laser blitzt weiß bei Höhen
                
                // 5. Farben mischen
                vec3 bodyColor = p_colorBody.rgb * light;
                bodyColor += p_colorBody.rgb * bassGlow * 0.3; // Leichter Glow auf dem Körper
                
                vec3 laserColor = p_colorLaser.rgb * beam * 2.5; // Laser ist sehr hell
                laserColor += vec3(highFlash) * beam; // Weißer Blitz im Laser
                
                // Finale Farbe
                x_fill = vec4(bodyColor + laserColor, 1.0);
            """.trimIndent()
        }

        // --- 4. ZEICHNEN (Deine funktionierende Ausrichtung) ---
        drawer.isolated {
            drawer.scale(1.0) 
            // Rotation beibehalten, da dein Modell so perfekt steht
            drawer.rotate(Vector3.UNIT_X, -90.0)

            mesh?.let { drawer.vertexBuffer(it, DrawPrimitive.TRIANGLES) }
        }
    }
}