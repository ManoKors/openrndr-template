import org.openrndr.Program
import org.openrndr.extra.minim.minim
import ddf.minim.analysis.FFT
import ddf.minim.AudioPlayer

class AudioEngine {
    private lateinit var player: AudioPlayer
    private lateinit var fft: FFT
    
    // Öffentliche Werte, auf die wir zugreifen wollen
    var smoothedBass = 0.0
    var smoothedMids = 0.0
    var smoothedHighs = 0.0

    fun setup(program: Program) {
        val minim = program.minim()
        // Lade Datei (oder nutze lineIn() für Mikrofon)
        player = minim.loadFile(Config.AUDIO_PATH)
        // player = minim.lineIn() // Alternative für Mikrofon
        
        fft = FFT(player.bufferSize(), player.sampleRate())
        player.play()
    }

    fun update() {
        // FFT Analyse durchführen
        fft.forward(player.mix)

        // Werte berechnen
        val rawBass = fft.calcAvg(0f, 100f) * 100.0
        val rawMids = fft.calcAvg(250f, 2500f) * 100.0
        val rawHighs = fft.calcAvg(1000f, 5000f) * 100.0

        // Smoothing (90% alter Wert, 10% neuer Wert)
        smoothedBass = smoothedBass * 0.9 + rawBass * 0.1
        smoothedMids = smoothedMids * 0.9 + rawMids * 0.1
        smoothedHighs = smoothedHighs * 0.9 + rawHighs * 0.1
        
        // DEBUG: Uncomment, wenn du ohne Musik testen willst
        smoothedBass = 50.0 
        smoothedMids = 30.0
        smoothedHighs = 20.0
    }
}