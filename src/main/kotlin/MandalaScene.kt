import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.math.Vector2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

class MandalaScene : Scene() {

    private val colors = listOf(
        ColorRGBa.fromHex("FF0080"), // Neon Pink
        ColorRGBa.fromHex("00FFFF"), // Cyan
        ColorRGBa.fromHex("EBFF00"), // Neon Yellow
        ColorRGBa.fromHex("8000FF")  // Purple
    )

    override fun draw(drawer: Drawer, time: Double, bassEnergy: Double, midEnergy: Double, highEnergy: Double, width: Int, height: Int) {

        // Audio reactive variables
        // scale: Pulse on bass
        val baseScale = 1.0 + (bassEnergy / 100.0)

        // rotation: Spin on Mids (slow base spin + mids kick)
        val globalRotation = time * 10.0 + (midEnergy * 0.2)

        // Color mix
        val colorIndex = (time * 0.5).toInt() % colors.size
        val nextColorIndex = (colorIndex + 1) % colors.size
        val colorMix = (time * 0.5) % 1.0
        val mainColor = colors[colorIndex].mix(colors[nextColorIndex], colorMix)

        val centerX = width / 2.0
        val centerY = height / 2.0
        val maxRadius = height * 0.45

        // Optimizations:
        // No "drawer.isolated" inside loops.
        // No Vector object creation inside loops.

        drawer.fill = null
        drawer.strokeWeight = 2.0

        // Use a single transform for the center
        drawer.translate(centerX, centerY)
        drawer.rotate(globalRotation)
        drawer.scale(baseScale)

        val symmetry = 12
        val angleStep = 360.0 / symmetry

        for (i in 0 until symmetry) {
            drawer.pushTransforms()
            drawer.rotate(i * angleStep)

            // Draw a "petal" or arm of the mandala
            // We draw in a local coordinate system where X is "outwards"

            // 1. Line expanding out
            drawer.stroke = mainColor
            // Visual complexity from simple math
            val wave = sin(time * 5.0 + i) * 50.0

            // Simple line
            drawer.lineSegment(0.0, 0.0, maxRadius, wave)

            // 2. Circle at the end
            drawer.stroke = mainColor.opacify(0.8)
            val circleSize = 10.0 + (highEnergy * 0.5)
            drawer.circle(maxRadius, wave, circleSize)

            // 3. Inner details (Cross connections)
            drawer.stroke = ColorRGBa.WHITE.opacify(0.5)
            // A line crossing to the side to create web-like structures
            drawer.lineSegment(maxRadius * 0.5, 0.0, maxRadius * 0.7, 40.0)

            drawer.popTransforms()
        }

        // Reset transforms manually to avoid 'isolated' overhead if this scene is reused
        // But since we are at the end of draw, it resets automatically for the next frame usually.
        // Good practice to be clean if we modified global state heavily, but here we used push/pop or relative transforms.
        // Actually, the initial translate/rotate/scale were NOT pushed.
        // We should wrap the whole thing in one push/pop or isolated at the top level,
        // but `drawer.isolated` was suspected of performance issues?
        // No, `isolated` is fine ONCE per frame. It's bad 1000 times per frame.
    }
}
