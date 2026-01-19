import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.math.Vector3
import org.openrndr.math.Vector2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

class TunnelScene : Scene() {

    private val colors = listOf(
        ColorRGBa.fromHex("00FF41"), // Neon Green
        ColorRGBa.fromHex("00FFFF"), // Cyan
        ColorRGBa.fromHex("8000FF"), // Purple
        ColorRGBa.fromHex("FF0080")  // Hot Pink
    )

    // Optimization: Pre-calculate hexagon points (radius 1.0)
    // This avoids creating thousands of Vector2 and List objects per second.
    private val hexPoints: List<Vector2> = (0..6).map { i ->
        val theta = i * (2 * PI / 6)
        Vector2(cos(theta), sin(theta))
    }

    override fun draw(drawer: Drawer, time: Double, bassEnergy: Double, midEnergy: Double, highEnergy: Double, width: Int, height: Int) {

        // Use isolated to not affect other scenes with our transforms
        drawer.isolated {
            // Set up 3D perspective
            drawer.perspective(60.0, width.toDouble() / height.toDouble(), 0.1, 1000.0)

            drawer.fill = null
            drawer.strokeWeight = 2.0 + (bassEnergy / 50.0) // Thicker lines on bass kick

            // Tunnel Parameters
            val segments = 40
            val tunnelDepth = 200.0
            val speed = 20.0

            val zOffset = (time * speed) % (tunnelDepth / segments)
            val segmentSpacing = tunnelDepth / segments

            for (i in 0 until segments) {
                val rawZ = -i * segmentSpacing + zOffset

                // Fade out near camera and far back
                val opacity = ((tunnelDepth + rawZ) / tunnelDepth).coerceIn(0.0, 1.0)

                val rotation = (time * 10.0) + (i * 5.0) + (midEnergy * 0.5)

                // Color calc
                val colorIndex = ((i + time).toInt() % colors.size).coerceAtLeast(0)
                val nextColorIndex = (colorIndex + 1) % colors.size
                val mixFactor = (time % 1.0)
                val baseColor = colors[colorIndex].mix(colors[nextColorIndex], mixFactor)

                // Optimization: Use push/popTransforms instead of isolated
                drawer.pushTransforms()

                // Position and Global Rotation
                drawer.translate(0.0, 0.0, rawZ)
                drawer.rotate(Vector3.UNIT_Z, rotation)

                // Pulse Scale
                val scaleFactor = 1.0 + (bassEnergy * 0.005)
                drawer.scale(scaleFactor, scaleFactor, 1.0)

                // 1. Primary Hexagon (Radius 10.0)
                drawer.stroke = baseColor.opacify(opacity)

                drawer.pushTransforms()
                drawer.scale(10.0)
                drawer.lineStrip(hexPoints)
                drawer.popTransforms()

                // 2. Secondary Hexagon (Radius 12.0, Rotated)
                drawer.rotate(Vector3.UNIT_Z, 30.0)
                drawer.stroke = baseColor.opacify(opacity * 0.5)

                drawer.pushTransforms()
                drawer.scale(12.0)
                drawer.lineStrip(hexPoints)
                drawer.popTransforms()

                drawer.popTransforms()
            }
        }
    }
}
