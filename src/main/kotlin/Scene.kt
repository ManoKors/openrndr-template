import org.openrndr.draw.Drawer

abstract class Scene {
    abstract fun draw(drawer: Drawer, time: Double, bassEnergy: Double, width: Int, height: Int)
}