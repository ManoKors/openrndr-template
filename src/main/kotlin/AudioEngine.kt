import org.openrndr.Program
import org.openrndr.extra.minim.minim
import ddf.minim.analysis.FFT
import ddf.minim.AudioSource
import ddf.minim.Minim
import ddf.minim.AudioInput

class AudioEngine {
    private lateinit var player: AudioInput
    private lateinit var fft: FFT
    
    // Öffentliche Werte, auf die wir zugreifen wollen
    var smoothedBass = 0.0
    var smoothedMids = 0.0
    var smoothedHighs = 0.0

    fun setup(program: Program) {
        val minim: ddf.minim.Minim = program.minim()
        // Verwende Mikrofon-Input anstatt Datei
        player = minim.getLineIn(1) // 1 für MONO
        
        fft = FFT(player.bufferSize(), player.sampleRate())
        // Kein play() nötig für lineIn, es ist live
    }

    fun update() {
        // FFT Analyse durchführen
        fft.forward(player.mix)

        // Werte berechnen
        val rawBass = fft.calcAvg(0f, 100f) * 100.0
        val rawMids = fft.calcAvg(100f, 1000f) * 100.0
        val rawHighs = fft.calcAvg(1000f, 5000f) * 100.0

        // Smoothing (99.8% alter Wert, 0.2% neuer Wert für extrem ruckelfrei)
        smoothedBass = (smoothedBass * 0.998 + rawBass * 0.002).coerceAtMost(80.0)
        smoothedMids = (smoothedMids * 0.998 + rawMids * 0.002).coerceAtMost(80.0)
        smoothedHighs = (smoothedHighs * 0.998 + rawHighs * 0.002).coerceAtMost(80.0)
        
        // DEBUG: Uncomment, wenn du ohne Musik testen willst
        // smoothedBass = 50.0 
    }
}