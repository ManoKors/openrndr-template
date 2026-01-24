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
        val rawHighs = fft.calcAvg(1000f, 5000f) * 100.0

        // Smoothing (99.5% alter Wert, 0.5% neuer Wert für maximal zuckelfreie Reaktion)
        smoothedBass = smoothedBass * 0.995 + rawBass * 0.005
        smoothedHighs = smoothedHighs * 0.995 + rawHighs * 0.005
        
        // DEBUG: Uncomment, wenn du ohne Musik testen willst
        // smoothedBass = 50.0 
    }
}