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
        // Für internes Audio: Setze BlackHole als Input in Systemeinstellungen > Ton
        player = minim.getLineIn(1) // 1 für MONO

        fft = FFT(player.bufferSize(), player.sampleRate())
        // Kein play() nötig für lineIn, es ist live
    }

    fun update() {
        // FFT Analyse durchführen
        fft.forward(player.mix)

        // Werte berechnen (angepasste Frequenzbereiche für bessere Musik-Reaktion)
        val rawBass = fft.calcAvg(20f, 250f) * 50.0  // Bass: 20-250 Hz, skalierter
        val rawMids = fft.calcAvg(250f, 4000f) * 50.0  // Mids: 250-4000 Hz
        val rawHighs = fft.calcAvg(4000f, 20000f) * 50.0  // Highs: 4000-20000 Hz

        // Smoothing (95% alter Wert, 5% neuer Wert für bessere Reaktivität)
        // Höherer Threshold: Wenn raw unter 5.0, dann auf 0 setzen für weniger Rauschen
        val threshBass = if (rawBass < 5.0) 0.0 else rawBass
        val threshMids = if (rawMids < 5.0) 0.0 else rawMids
        val threshHighs = if (rawHighs < 5.0) 0.0 else rawHighs

        smoothedBass = (smoothedBass * 0.95 + threshBass * 0.05).coerceAtMost(50.0)
        smoothedMids = (smoothedMids * 0.95 + threshMids * 0.05).coerceAtMost(50.0)
        smoothedHighs = (smoothedHighs * 0.95 + threshHighs * 0.05).coerceAtMost(50.0)
        
        // DEBUG: Uncomment, wenn du ohne Musik testen willst
        // smoothedBass = 50.0 
    }
}