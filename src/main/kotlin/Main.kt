import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.events.Event
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.*
import org.openrndr.math.Vector3
import org.openrndr.window

// Globale Kamera-Werte für 3D Scene
var cameraEyeGlobal = Vector3(7.36, 6.93, 4.96)

enum class SceneType {
    AURORA_FLOW,
    NEON_INTERFERENCE,
    NEON_INTERFERENCE_2,
    HYPNOTIC,
    DREAM_HAZE,
    HYPNOTIC_3D
}

class Settings {
    @OptionParameter("Scene")
    var sceneType = SceneType.NEON_INTERFERENCE

    @BooleanParameter("Auto Scene Change")
    var autoSceneChange = true

    // 3D Camera Controls
    @DoubleParameter("Eye X", -20.0, 20.0)
    var eyeX = 0.017

    @DoubleParameter("Eye Y", -20.0, 20.0)
    var eyeY = -10.0

    @DoubleParameter("Eye Z", -20.0, 20.0)
    var eyeZ = 1.381
}

fun main() = application {
    configure {
        width = Config.WIDTH
        height = Config.HEIGHT
        title = Config.TITLE
        windowResizable = true
    }

    program {
        val settings = Settings() // Für zukünftige GUI

        // Globale Kamera-Werte für 3D Scene
        var cameraEye = Vector3(settings.eyeX, settings.eyeY, settings.eyeZ)

        // GUI Window
        window {
            configure {
                width = 400
                height = 600
                title = "Controller"
            }
            val gui = GUI()
            gui.add(settings, "Settings")
            extend(gui)
            extend {
                drawer.clear(ColorRGBa.fromHex("2D2D2D"))
            }
        }

        // Module initialisieren
        val audio = AudioEngine()
        audio.setup(this)

        val scenes = listOf(SceneType.AURORA_FLOW, SceneType.NEON_INTERFERENCE, SceneType.NEON_INTERFERENCE_2, SceneType.HYPNOTIC, SceneType.DREAM_HAZE, SceneType.HYPNOTIC_3D)
        var lastSceneChange = 0.0
        var lastSceneType = settings.sceneType
        var scene: Scene = NeonInterferenceScene() // Initiale Scene

        // Funktion zum Scene-Wechsel
        fun switchToScene(sceneType: SceneType) {
            settings.sceneType = sceneType
            lastSceneType = sceneType
            scene = when (sceneType) {
                SceneType.AURORA_FLOW -> AuroraFlowScene()
                SceneType.NEON_INTERFERENCE -> NeonInterferenceScene()
                SceneType.NEON_INTERFERENCE_2 -> NeonInterferenceScene2()
                SceneType.HYPNOTIC -> HypnoticRadialScene()
                SceneType.DREAM_HAZE -> DreamHazeScene()
                SceneType.HYPNOTIC_3D -> Hypnotic3DScene()
            }
            settings.autoSceneChange = false // Deaktiviere auto bei manuellem Wechsel
        }

        // Initiale Scene setzen
        switchToScene(settings.sceneType)

        // Grafik-Setup
        // val bloom = Bloom().apply {
        //     brightness = 1.0
        // }

        // Offscreen Buffer entfernt, da wir direkt zeichnen

        extend {
            // Check for scene change from GUI
            if (settings.sceneType != lastSceneType) {
                switchToScene(settings.sceneType)
            }

            // 1. Daten Update
            audio.update()
            val time = seconds * 0.5

            // Update camera for 3D scene
            Scene.cameraEye = Vector3(settings.eyeX, settings.eyeY, settings.eyeZ)

            // Szene-Wechsel alle 2 Minuten (120 Sekunden) - nur wenn autoSceneChange aktiv
            if (settings.autoSceneChange) {
                val currentCycle = (seconds / 120.0).toInt()
                if (currentCycle != lastSceneChange.toInt()) {
                    lastSceneChange = currentCycle.toDouble()
                    val currentIndex = scenes.indexOf(settings.sceneType)
                    val nextIndex = (currentIndex + 1) % scenes.size
                    switchToScene(scenes[nextIndex])
                }
            }

        // Tastatursteuerung für Scene-Wechsel
        keyboard.keyDown.listen {
            when (it.name) {
                "arrow-right" -> {
                    val currentIndex = scenes.indexOf(settings.sceneType)
                    val nextIndex = (currentIndex + 1) % scenes.size
                    switchToScene(scenes[nextIndex])
                }
                "arrow-left" -> {
                    val currentIndex = scenes.indexOf(settings.sceneType)
                    val prevIndex = if (currentIndex - 1 < 0) scenes.size - 1 else currentIndex - 1
                    switchToScene(scenes[prevIndex])
                }
                "space" -> { // Space zum Toggle des automatischen Wechsels
                    settings.autoSceneChange = !settings.autoSceneChange
                }
            }
        }

            // 2. Zeichnen
            drawer.clear(ColorRGBa.BLACK)
            
            // Wir übergeben nur die Daten, die die Szene braucht
            scene.draw(
                drawer = drawer, 
                time = time, 
                bassEnergy = audio.smoothedBass, 
                midEnergy = audio.smoothedMids,
                highEnergy = audio.smoothedHighs,
                width = width, 
                height = height
            )

            // 3. Post-Processing (Bloom) & Ausgabe auf Screen
            // bloom.apply(offscreen.colorBuffer(0), offscreen.colorBuffer(0))
            // drawer.image(offscreen.colorBuffer(0))

            // GUI kann später hinzugefügt werden
        }
    }
}