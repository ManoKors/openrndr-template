import org.openrndr.draw.Drawer
import org.openrndr.math.Vector3

abstract class Scene {
    companion object {
        var cameraEye = Vector3(0.0, 0.0, 300.0)
    }
    abstract fun draw(drawer: Drawer, time: Double, bassEnergy: Double, midEnergy: Double, highEnergy: Double, width: Int, height: Int)
}