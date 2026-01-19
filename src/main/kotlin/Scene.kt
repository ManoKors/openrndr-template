import org.openrndr.draw.Drawer

abstract class Scene {
    abstract fun draw(drawer: Drawer, time: Double, bassEnergy: Double, midEnergy: Double, highEnergy: Double, width: Int, height: Int)
}