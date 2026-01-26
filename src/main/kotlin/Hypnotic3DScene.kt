import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.camera.OrbitalCamera
import org.openrndr.extra.objloader.loadOBJ
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.lookAt

class Hypnotic3DScene : Scene() {

    // Wir speichern das fertige Mesh hier, wenn es gebaut ist
    private var mesh: VertexBuffer? = null
    private var hasTriedLoading = false

    // Berechnete Modell-Infos zum Zentrieren / Kameraposition
    private var meshCenter: Vector3? = null
    private var meshRadius = 1.0

    override fun draw(drawer: Drawer, time: Double, bassEnergy: Double, midEnergy: Double, highEnergy: Double, width: Int, height: Int) {

        // --- 1. LADE- UND BAU-PHASE ---
        if (!hasTriedLoading && mesh == null) {
            hasTriedLoading = true
            try {
                println("--- VERSUCHE DATEI ZU PARSEN (MIT QUAD-SUPPORT) ---")
                val rawGroups = loadOBJ("data/models/7of9.obj")
                val allFaces = rawGroups.values.flatten()
                
                if (allFaces.isNotEmpty()) {
                    println("GEFUNDEN: ${'$'}{allFaces.size} Roh-Flächen.")

                    // 1. ZÄHLEN: Wie viel Platz brauchen wir wirklich?
                    var vertexCount = 0
                    for (face in allFaces) {
                        if (face.positions.size == 4) vertexCount += 6
                        else vertexCount += 3
                    }

                    println("BENÖTIGTER SPEICHER: ${'$'}vertexCount Vertices.")

                    // 2. BUFFER ERSTELLEN
                    val vFormat = vertexFormat {
                        position(3)
                        normal(3)
                    }
                    mesh = vertexBuffer(vFormat, vertexCount)

                    // 3. DATEN SCHREIBEN (MIT QUAD-SPLIT)
                    // bounding box init
                    var minX = Double.POSITIVE_INFINITY
                    var minY = Double.POSITIVE_INFINITY
                    var minZ = Double.POSITIVE_INFINITY
                    var maxX = Double.NEGATIVE_INFINITY
                    var maxY = Double.NEGATIVE_INFINITY
                    var maxZ = Double.NEGATIVE_INFINITY

                    mesh?.put {
                        for (face in allFaces) {
                            val pos = face.positions
                            // A. Normale berechnen (für das erste Dreieck)
                            val p0 = pos.getOrElse(0) { Vector3.ZERO }
                            val p1 = pos.getOrElse(1) { Vector3.ZERO }
                            val p2 = pos.getOrElse(2) { Vector3.ZERO }

                            val edge1 = p1 - p0
                            val edge2 = p2 - p0
                            var normal = edge1.cross(edge2).normalized
                            if (normal.x.isNaN() || normal.y.isNaN() || normal.z.isNaN()) normal = Vector3.UNIT_Y

                            // helper to update bbox and write a point
                            fun writePoint(index: Int) {
                                val v = pos.getOrElse(index) { Vector3.ZERO }
                                if (v.x < minX) minX = v.x
                                if (v.y < minY) minY = v.y
                                if (v.z < minZ) minZ = v.z
                                if (v.x > maxX) maxX = v.x
                                if (v.y > maxY) maxY = v.y
                                if (v.z > maxZ) maxZ = v.z
                                write(v)
                                write(normal)
                            }

                            // B. erstes Dreieck
                            writePoint(0)
                            writePoint(1)
                            writePoint(2)

                            // C. falls Viereck: zweites Dreieck 0,2,3
                            if (pos.size == 4) {
                                writePoint(0)
                                writePoint(2)
                                writePoint(3)
                            }
                        }
                    }

                    // center + radius
                    val cx = (minX + maxX) / 2.0
                    val cy = (minY + maxY) / 2.0
                    val cz = (minZ + maxZ) / 2.0
                    meshCenter = Vector3(cx, cy, cz)
                    val dx = maxX - minX
                    val dy = maxY - minY
                    val dz = maxZ - minZ
                    val maxDim = maxOf(dx, dy, dz)
                    meshRadius = (maxDim / 2.0).coerceAtLeast(0.0001)

                    println("ERFOLG: Mesh sauber trianguliert und gebaut! center=${'$'}{meshCenter} radius=${'$'}meshRadius")
                }

            } catch (e: Exception) {
                println("CRASH BEIM LADEN: ${'$'}{e.message}")
                e.printStackTrace()
            }
        }

        drawer.clear(ColorRGBa.fromHex("050510"))

        // --- DER FIX FÜR DURCHSICHTIGE OBJEKTE ---
        // 1. Tiefen-Check einschalten: "Ist da schon was?"
        drawer.depthTestPass = DepthTestPass.LESS_OR_EQUAL
        // 2. Tiefen-Schreiben einschalten: "Merk dir, dass hier jetzt was ist!"
        drawer.depthWrite = true
        // 3. Misch-Modus normalisieren: Keine Transparenz oder Additives Leuchten
        drawer.drawStyle.blendMode = BlendMode.BLEND
        // 4. Rückseiten ausblenden (verhindert, dass man ins Innere guckt)
        drawer.drawStyle.cullTestPass = CullTestPass.FRONT

        // ------------------------------------------

        // --- ERROR SCREEN ---
        if (mesh == null) {
            drawer.fill = ColorRGBa.RED
            drawer.text("LOADING FAILED - SEE CONSOLE", 10.0, 30.0)
            return
        }

        // --- 2. SETUP & RENDER ---
        drawer.perspective(60.0, width.toDouble() / height, 0.1, 1000.0)
        drawer.depthWrite = true
        drawer.depthTestPass = DepthTestPass.LESS_OR_EQUAL
        drawer.drawStyle.cullTestPass = CullTestPass.FRONT 

        // Kamera: wenn wir meshCenter haben, zentrieren und positioniere die Kamera passend
        val center = meshCenter ?: Vector3.ZERO
        val camEye = if (Scene.cameraEye == Vector3.ZERO) {
            // Positioniere die Kamera auf der Z-Achse oberhalb des Modells
            Vector3(center.x, center.y, center.z + meshRadius * 2.5)
        } else Scene.cameraEye
        drawer.view = org.openrndr.math.transforms.lookAt(camEye, center, Vector3.UNIT_Y)

        // --- DER NEUE, SCHÖNE SHADER ---
        drawer.shadeStyle = shadeStyle {
            // Wir übergeben die Zeit für eventuelle Animationen
            parameter("time", time)
            // Coole Farben definieren (kannst du anpassen!)
            parameter("topColor", ColorRGBa.fromHex("00FFCC")) // Türkis/Cyan für oben
            parameter("botColor", ColorRGBa.fromHex("440088")) // Dunkles Lila für unten

            fragmentTransform = """
                // 1. Wichtige Vektoren holen
                vec3 n = normalize(va_normal); // Die Ausrichtung der Fläche
                vec3 viewDir = normalize(vec3(0.0, 0.0, 1.0)); // Blickrichtung der Kamera (vereinfacht)
                // Licht kommt von oben rechts vorne
                vec3 lightDir = normalize(vec3(0.5, 0.8, 1.0));

                // 2. Beleuchtung berechnen (Soft Shading)
                float lightIntensity = max(dot(n, lightDir), 0.15);

                // 3. Farbverlauf basierend auf Höhe (Y-Position)
                float heightGradient = smoothstep(-10.0, 10.0, va_position.y);
                vec3 baseColor = mix(p_botColor.rgb, p_topColor.rgb, heightGradient);

                // 4. Extra: "Rim Light" (Kantenglanz von hinten) für 3D-Effekt
                float rim = 1.0 - max(dot(viewDir, n), 0.0);
                rim = pow(rim, 4.0) * 0.7;
                vec3 rimColor = vec3(1.0, 1.0, 1.0) * rim;

                // 5. Alles zusammenmischen
                x_fill = vec4((baseColor * lightIntensity) + rimColor, 1.0);
            """.trimIndent()
        }

        // Zeichnen
        drawer.isolated {
            // Skalierung: Hier anpassen, falls zu groß/klein
            val scaleFactor = 1.0
            val center = meshCenter ?: Vector3.ZERO

            // Transformations um das Modellzentrum: translate->scale->rotate->translateBack
            drawer.translate(center.x, center.y, center.z)
            drawer.scale(scaleFactor)
            // Langsame Eigenrotation um Y (zeitbasiert)
            drawer.rotate(Vector3.UNIT_Y, time * 20.0)
            // Rotation Fix für OBJ (-90 auf X ist Standard für Blender->OpenRNDR)
            drawer.rotate(Vector3.UNIT_X, -90.0)
            // Falls sie nach hinten guckt:
            // drawer.rotate(Vector3.UNIT_Y, 180.0)
            drawer.translate(-center.x, -center.y, -center.z)

            mesh?.let {
                drawer.vertexBuffer(it, DrawPrimitive.TRIANGLES)
            }
        }
    }
}