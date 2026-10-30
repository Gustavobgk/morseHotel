package morse;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

// emite o som do morse inteiro de uma vez, com o ponto e o traço em frequências diferentes
public class SomMorse {
    private static final float TAXA_AMOSTRAGEM = 44100f;
    private static final AudioFormat FORMATO = new AudioFormat(TAXA_AMOSTRAGEM, 16, 1, true, false);

    // o ponto é agudo e curto, o traço é grave e longo
    private static final double FREQUENCIA_PONTO = 1000;
    private static final double FREQUENCIA_TRACO = 600;
    private static final double VOLUME = 0.85;
    private static final double FADE_MS = 6;

    // mesmos tempos usados na animação do HabboBoneco
    private static final int PONTO = 120, TRACO = 360, INTERVALO = 120;
    private static final int PAUSA_LETRA = 360, PAUSA_PALAVRA = 600;

    // tocadores do sistema, que usam a saída de áudio padrão (a linha direta do Java pode cair no HDMI)
    private static final String[] TOCADORES = {"paplay", "pw-play", "aplay"};

    private final AtomicReference<Process> processoAtual = new AtomicReference<>();
    private final AtomicReference<SourceDataLine> linhaAtual = new AtomicReference<>();

    // monta a onda do texto morse inteiro e toca numa thread separada
    public void tocarMorse(String morse) {
        byte[] pcm = gerarMorse(morse);
        Thread thread = new Thread(() -> executar(pcm));
        thread.setDaemon(true);
        thread.start();
    }

    // interrompe o som que estiver tocando
    public void parar() {
        Process processo = processoAtual.getAndSet(null);
        if (processo != null) processo.destroy();
        SourceDataLine linha = linhaAtual.getAndSet(null);
        if (linha == null) return;
        try {
            linha.stop();
            linha.close();
        } catch (RuntimeException erro) {
            // se a linha já foi fechada não precisa fazer nada
        }
    }

    private void executar(byte[] pcm) {
        String tocador = localizarTocador();
        if (tocador != null && tocarComTocador(tocador, montarWav(pcm))) return;
        tocarComJava(pcm);
    }

    // usa paplay/pw-play/aplay para sair na saída padrão do sistema
    private boolean tocarComTocador(String tocador, byte[] wav) {
        Path arquivo = null;
        try {
            arquivo = Files.createTempFile("morse-", ".wav");
            Files.write(arquivo, wav);
            Process processo = new ProcessBuilder(tocador, arquivo.toString())
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            processoAtual.set(processo);
            processo.waitFor();
            return true;
        } catch (Exception erro) {
            return false;
        } finally {
            processoAtual.set(null);
            if (arquivo != null) {
                try {
                    Files.deleteIfExists(arquivo);
                } catch (Exception erro) {
                    // arquivo temporário some quando o sistema limpar
                }
            }
        }
    }

    // reserva: toca direto pela linha de áudio do Java
    private void tocarComJava(byte[] pcm) {
        SourceDataLine linha;
        try {
            linha = AudioSystem.getSourceDataLine(FORMATO);
        } catch (Exception erro) {
            return;
        }
        linhaAtual.set(linha);
        try (SourceDataLine aberta = linha) {
            aberta.open(FORMATO);
            aberta.start();
            aberta.write(pcm, 0, pcm.length);
            aberta.drain();
        } catch (Exception erro) {
            // sem placa de som ou som interrompido, apenas ignora
        } finally {
            linhaAtual.compareAndSet(linha, null);
        }
    }

    private String localizarTocador() {
        String caminho = System.getenv("PATH");
        if (caminho == null) return null;
        for (String tocador : TOCADORES) {
            for (String pasta : caminho.split(File.pathSeparator)) {
                Path candidato = Paths.get(pasta, tocador);
                if (Files.isExecutable(candidato)) return candidato.toString();
            }
        }
        return null;
    }

    private byte[] gerarMorse(String morse) {
        ByteArrayOutputStream som = new ByteArrayOutputStream();
        for (char simbolo : morse.toCharArray()) {
            if (simbolo == '.') {
                escrever(som, gerarTom(FREQUENCIA_PONTO, PONTO));
                escrever(som, gerarSilencio(INTERVALO));
            } else if (simbolo == '-') {
                escrever(som, gerarTom(FREQUENCIA_TRACO, TRACO));
                escrever(som, gerarSilencio(INTERVALO));
            } else {
                int pausa = simbolo == '/' ? PAUSA_PALAVRA : simbolo == ' ' ? PAUSA_LETRA : INTERVALO;
                escrever(som, gerarSilencio(pausa));
            }
        }
        return som.toByteArray();
    }

    // onda com harmônicos para o som ficar mais encorpado e fácil de ouvir
    private byte[] gerarTom(double frequencia, int duracaoMs) {
        int total = (int) (TAXA_AMOSTRAGEM * duracaoMs / 1000);
        int fade = Math.max(1, (int) (TAXA_AMOSTRAGEM * FADE_MS / 1000));
        byte[] amostras = new byte[total * 2];
        for (int i = 0; i < total; i++) {
            double tempo = i / TAXA_AMOSTRAGEM;
            double envelope = Math.min(1, Math.min(i, total - 1 - i) / (double) fade);
            double onda = Math.sin(2 * Math.PI * frequencia * tempo)
                    + 0.35 * Math.sin(2 * Math.PI * frequencia * 2 * tempo)
                    + 0.20 * Math.sin(2 * Math.PI * frequencia * 3 * tempo);
            double valor = Math.max(-1, Math.min(1, onda * VOLUME / 1.55 * envelope));
            short amostra = (short) (valor * Short.MAX_VALUE);
            amostras[2 * i] = (byte) (amostra & 0xff);
            amostras[2 * i + 1] = (byte) ((amostra >> 8) & 0xff);
        }
        return amostras;
    }

    private byte[] gerarSilencio(int duracaoMs) {
        int total = (int) (TAXA_AMOSTRAGEM * duracaoMs / 1000);
        return new byte[total * 2];
    }

    private void escrever(ByteArrayOutputStream destino, byte[] parte) {
        destino.write(parte, 0, parte.length);
    }

    // cabeçalho WAV PCM 16 bits mono para os tocadores do sistema
    private byte[] montarWav(byte[] pcm) {
        ByteBuffer wav = ByteBuffer.allocate(44 + pcm.length).order(ByteOrder.LITTLE_ENDIAN);
        wav.put("RIFF".getBytes(StandardCharsets.US_ASCII));
        wav.putInt(36 + pcm.length);
        wav.put("WAVE".getBytes(StandardCharsets.US_ASCII));
        wav.put("fmt ".getBytes(StandardCharsets.US_ASCII));
        wav.putInt(16);
        wav.putShort((short) 1);
        wav.putShort((short) 1);
        wav.putInt((int) TAXA_AMOSTRAGEM);
        wav.putInt((int) TAXA_AMOSTRAGEM * 2);
        wav.putShort((short) 2);
        wav.putShort((short) 16);
        wav.put("data".getBytes(StandardCharsets.US_ASCII));
        wav.putInt(pcm.length);
        wav.put(pcm);
        return wav.array();
    }
}
