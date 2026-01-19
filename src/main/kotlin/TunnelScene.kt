import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform
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

    override fun draw(drawer: Drawer, time: Double, bassEnergy: Double, midEnergy: Double, highEnergy: Double, width: Int, height: Int) {

        // Use isolated to not affect other scenes with our transforms
        drawer.isolated {
            // Set up 3D perspective
            // FOV 60, aspect ratio based on window size, near 0.1, far 1000
            drawer.perspective(60.0, width.toDouble() / height.toDouble(), 0.1, 1000.0)

            // Move camera back a bit? Actually we'll move the world.
            // Let's assume camera is at 0,0,0 looking at -Z

            // Background is cleared in Main.kt, but we want a deep dark feel.
            // Main.kt clears to black, which is good.

            drawer.fill = null
            drawer.strokeWeight = 2.0 + (bassEnergy / 50.0) // Thicker lines on bass kick

            // Tunnel Parameters
            val segments = 40
            val tunnelDepth = 200.0 // How deep the tunnel segment loop goes
            val speed = 20.0 // Movement speed

            // Calculate movement offset based on time
            val zOffset = (time * speed) % (tunnelDepth / segments)

            for (i in 0 until segments) {
                // Calculate Z position for this ring
                // We want rings to come from far (-Z) towards camera (0) or vice versa.
                // Let's make them move towards camera: start at -tunnelDepth, move to 0
                // baseZ is negative.

                var z = -tunnelDepth + (i * (tunnelDepth / segments)) + zOffset

                // Wrap around logic: if z > 0 (behind camera), put it back to far end?
                // Actually the loop + modulo offset handles the "infinite" feel if we structure it right.
                // Let's try: z goes from -tunnelDepth to 0.
                // The modulo logic above `zOffset` shifts everything by [0, segmentSpacing].

                // Let's refine the Z calc:
                val segmentSpacing = tunnelDepth / segments
                val rawZ = -i * segmentSpacing + zOffset
                // rawZ is roughly [-tunnelDepth ... 0]

                // We want them to fade in from the back and fade out near the camera
                val opacity = ((tunnelDepth + rawZ) / tunnelDepth).coerceIn(0.0, 1.0)

                // Dynamic Rotation (twist)
                // Twist depends on depth (i) and time and Mids
                val rotation = (time * 10.0) + (i * 5.0) + (midEnergy * 0.5)

                // Dynamic Color
                // Cycle colors based on index and time
                val colorIndex = ((i + time).toInt() % colors.size).coerceAtLeast(0)
                val nextColorIndex = (colorIndex + 1) % colors.size
                val mixFactor = (time % 1.0)
                val baseColor = colors[colorIndex].mix(colors[nextColorIndex], mixFactor)

                // Add some "Mids" influence to color brightness/alpha
                drawer.stroke = baseColor.opacify(opacity)

                drawer.isolated {
                    // Position the ring
                    drawer.translate(0.0, 0.0, rawZ)

                    // Rotate the ring
                    drawer.rotate(org.openrndr.math.Vector3.UNIT_Z, rotation)

                    // Reactive Scale (Pulse with Bass)
                    // The closer to the camera, the less we might want to shake it to avoid nausea?
                    // Or shake everything uniformly.
                    val scaleFactor = 1.0 + (bassEnergy * 0.005)
                    drawer.scale(scaleFactor, scaleFactor, 1.0)

                    // Draw the shape (e.g. Hexagon)
                    drawHexagon(drawer, radius = 10.0)

                    // Add a second inter-woven shape for complexity?
                    drawer.rotate(org.openrndr.math.Vector3.UNIT_Z, 30.0)
                    drawer.stroke = baseColor.opacify(opacity * 0.5) // Fainter
                    drawHexagon(drawer, radius = 12.0)
                }
            }
        }
    }

    private fun drawHexagon(drawer: Drawer, radius: Double) {
        val points = mutableListOf<org.openrndr.math.Vector2>()
        for (i in 0 until 6) {
            val theta = i * (2 * PI / 6)
            val x = radius * cos(theta)
            val y = radius * sin(theta)
            points.add(org.openrndr.math.Vector2(x, y))
        }
        // Close the loop
        points.add(points[0])
        drawer.lineStrip(points)
    }
}
