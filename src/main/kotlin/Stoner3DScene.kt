import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.objloader.loadOBJ
import org.openrndr.math.Vector3
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.*
import kotlin.math.cos
import kotlin.math.sin

class Stoner3DScene : Scene() {

    private var mesh: VertexBuffer? = null
    private var hasTriedLoading = false

    override fun draw(drawer: Drawer, time: Double, bassEnergy: Double, midEnergy: Double, highEnergy: Double, width: Int, height: Int) {

        // --- 1. LADE-LOGIK (Unverändert) ---
        if (!hasTriedLoading && mesh == null) {
            hasTriedLoading = true
            try {
                // Pfad ggf. anpassen, falls du ein anderes Modell testen willst
                val rawGroups = loadOBJ("data/models/bust.obj")
                val allFaces = rawGroups.values.flatten()

                if (allFaces.isNotEmpty()) {
                    val allPositions = mutableListOf<Vector3>()
                    for (face in allFaces) { allPositions.addAll(face.positions) }

                    val center = if (allPositions.isNotEmpty()) {
                        val sum = allPositions.reduce { acc, vec -> acc + vec }
                        sum / allPositions.size.toDouble()
                    } else { Vector3.ZERO }

                    var vertexCount = 0
                    for (face in allFaces) {
                        if (face.positions.size == 4) vertexCount += 6 else vertexCount += 3
                    }

                    val vFormat = vertexFormat {
                        position(3)
                        normal(3)
                        // WICHTIG FÜR ORGANISCHE TEXTUREN: Texturkoordinaten (UVs)
                        textureCoordinate(2)
                    }
                    mesh = vertexBuffer(vFormat, vertexCount)

                    mesh?.put {
                        for (face in allFaces) {
                            val pos = face.positions
                            val uvs = face.textureCoords // Versuchen, UVs aus dem Modell zu laden
                            
                            val p0 = pos.getOrElse(0) { Vector3.ZERO } - center
                            val p1 = pos.getOrElse(1) { Vector3.ZERO } - center
                            val p2 = pos.getOrElse(2) { Vector3.ZERO } - center
                            var normal = (p1 - p0).cross(p2 - p0).normalized
                            if (normal.x.isNaN()) normal = Vector3.UNIT_Y

                            // Fallback, falls das Modell keine UVs hat: Wir nutzen die Position als UV
                            val uv0 = uvs.getOrElse(0) { Vector2(p0.x, p0.y) * 0.01 }
                            val uv1 = uvs.getOrElse(1) { Vector2(p1.x, p1.y) * 0.01 }
                            val uv2 = uvs.getOrElse(2) { Vector2(p2.x, p2.y) * 0.01 }

                            fun writePoint(index: Int, uv: Vector2) {
                                val centeredPos = pos.getOrElse(index) { Vector3.ZERO } - center
                                write(centeredPos)
                                write(normal)
                                write(uv)
                            }

                            writePoint(0, uv0); writePoint(1, uv1); writePoint(2, uv2)
                            if (pos.size == 4) {
                                val p3 = pos.getOrElse(3) { Vector3.ZERO } - center
                                val uv3 = uvs.getOrElse(3) { Vector2(p3.x, p3.y) * 0.01 }
                                writePoint(0, uv0); writePoint(2, uv2); writePoint(3, uv3)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("Ladefehler: ${'$'}{e.message}")
            }
        }

        if (mesh == null) return

        // --- 2. STONER ATMOSPHÄRE (Hintergrund & Kamera) ---
        // Dunkler, sumpfiger Hintergrund statt Cyber-Schwarz
        drawer.clear(ColorRGBa.fromHex("150d1a"))

        drawer.perspective(60.0, width.toDouble() / height, 0.1, 2000.0)
        drawer.depthWrite = true
        drawer.depthTestPass = DepthTestPass.LESS_OR_EQUAL
        drawer.drawStyle.cullTestPass = CullTestPass.BACK

        // Kamera: VIEL langsamer und träger
        // Der Radius atmet ganz langsam mit dem Bass
        val radius = 220.0 + sin(time * 0.1) * 20.0
        // Die Höhe schwankt langsam wie auf einem Schiff
        val heightCam = sin(time * 0.15) * 80.0
        // Sehr langsame Drehung
        val angle = time * 0.15
        Scene.cameraEye = Vector3(cos(angle) * radius, sin(angle) * radius, heightCam)
        // Der Blickpunkt driftet leicht um die Mitte
        val target = Vector3(sin(time*0.2)*20.0, cos(time*0.25)*10.0, 0.0)
        drawer.view = lookAt(Scene.cameraEye, target, Vector3.UNIT_Y)


        // --- Gitter entfernt ---

        // --- 3. DER STONER-PLASMA SHADER ---
        drawer.shadeStyle = shadeStyle {
            parameter("time", time)
            parameter("bass", bassEnergy)
            parameter("mid", midEnergy)
            parameter("high", highEnergy)

            fragmentPreamble = """
                vec3 palette( in float t, in vec3 a, in vec3 b, in vec3 c, in vec3 d )
                {
                    return a + b*cos( 6.28318*(c*t+d) );
                }
            """.trimIndent()

            fragmentTransform = """
                // Wir nutzen die 3D-Position des Modells, um das Muster zu erzeugen.
                // va_position ist die rohe Position im Raum.
                // Wir skalieren sie, damit das Muster nicht zu klein ist.
                vec3 posScaled = va_position * 0.015;
                
                // Audio-Einfluss auf die Bewegung
                float flowSpeed = p_time * 0.2 + p_bass * 0.1;
                
                // --- Organisches "Warping" ---
                // Wir verzerren die Koordinaten mit Sinus-Funktionen.
                // Das ist der Schlüssel zum "geschmolzenen" Look.
                float warp1 = sin(posScaled.x * 4.0 + flowSpeed) * sin(posScaled.z * 3.5 + flowSpeed * 1.2);
                float warp2 = cos(posScaled.y * 5.0 - flowSpeed * 0.8) * sin(posScaled.x * 4.2);
                
                // Die verzerrte Koordinate
                vec3 warpedPos = posScaled + vec3(warp1, warp2, sin(flowSpeed)*0.5);

                // --- Muster-Generierung ---
                // Ein sich bewegendes Wellenmuster basierend auf den verzerrten Koordinaten
                float pattern = sin(warpedPos.x * 5.0 + p_time) * cos(warpedPos.y * 6.0 + p_time*0.5) * sin(warpedPos.z * 4.0);
                
                // Den Bass nutzen, um das Muster "aufzubrechen"
                pattern += sin(length(warpedPos) * 10.0 - p_time * 2.0) * p_bass * 0.5;

                // Normalisierung des Musters in den Bereich 0.0 - 1.0 für die Farbe
                float t = pattern * 0.5 + 0.5;
                
                // --- Stoner Farb-Palette (Erdig, Warm, Psychedelisch) ---
                // Definition der Farben für die Paletten-Funktion (siehe Preamble)
                // Diese Werte erzeugen einen Verlauf von tiefem Orange über Lila zu Moosgrün.
                vec3 colA = vec3(0.5, 0.5, 0.5);    // Helligkeits-Mitte
                vec3 colB = vec3(0.5, 0.5, 0.5);    // Kontrast-Stärke
                vec3 colC = vec3(1.0, 1.0, 1.0);    // Frequenz der Farben
                // Der wichtigste Teil: Die Phasenverschiebung bestimmt die Farbtöne
                // 0.00, 0.33, 0.67 = Regenbogen
                // 0.0, 0.10, 0.20 = Warm/Erdig (Rot/Orange/Gelb)
                vec3 colD = vec3(0.0, 0.15, 0.30); // Rot-Orange-Lila Verschiebung
                
                // Berechne die finale Farbe basierend auf dem Muster 't'
                vec3 finalColor = palette(t + p_bass*0.1, colA, colB, colC, colD);
                
                // --- Beleuchtung (optional, macht es plastischer) ---
                vec3 n = normalize(v_viewNormal); // Normale im Kameraraum
                vec3 lightDir = normalize(vec3(1.0, 1.0, 1.0)); // Licht von oben rechts
                float diff = max(dot(n, lightDir), 0.2); // Weiches diffuses Licht
                
                // Die Farbe wird dunkler in den Schatten, reagiert aber auf Höhen (highEnergy)
                // Wenn "Highs" kommen, leuchtet es stärker (wie glühende Lava).
                float brightness = diff * (0.6 + p_high * 0.8);

                x_fill = vec4(finalColor * brightness, 1.0);
            """.trimIndent()
        }

        // --- 4. ZEICHNEN (Unverändert) ---
        drawer.isolated {
            drawer.scale(6.0)
            drawer.rotate(Vector3.UNIT_X, -90.0)
            mesh?.let { drawer.vertexBuffer(it, DrawPrimitive.TRIANGLES) }
        }
    }
}