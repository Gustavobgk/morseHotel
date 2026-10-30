package morse;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

// fala o texto em português sintetizando a voz com o piper
public class VozPortugues {
    private static final String MODELO_PADRAO = "pt_BR-faber-medium.onnx";
    private static final String[] TOCADORES = {"paplay", "pw-play", "aplay"};

    private final AtomicReference<Process> sinteseAtual = new AtomicReference<>();
    private final AtomicReference<Process> falaAtual = new AtomicReference<>();
    private final AtomicLong geracao = new AtomicLong();

    // sintetiza e toca a fala numa thread separada para não travar a interface
    public void falar(String texto) {
        if (texto == null || texto.isBlank()) return;
        long minhaGeracao = geracao.incrementAndGet();
        pararProcessos();
        Thread thread = new Thread(() -> executar(texto, minhaGeracao));
        thread.setDaemon(true);
        thread.start();
    }

    // interrompe a síntese e a fala que estiverem acontecendo
    public void parar() {
        geracao.incrementAndGet();
        pararProcessos();
    }

    private void executar(String texto, long minhaGeracao) {
        Path piper = localizarPiper();
        Path modelo = localizarModelo();
        if (piper == null || modelo == null) return;

        Path wav = null;
        Process sintese = null;
        Process fala = null;
        try {
            wav = Files.createTempFile("voz-", ".wav");
            sintese = iniciarSintese(piper, modelo, wav);
            sinteseAtual.set(sintese);
            try (OutputStream entrada = sintese.getOutputStream()) {
                entrada.write(texto.getBytes(StandardCharsets.UTF_8));
            }
            boolean terminou = sintese.waitFor(60, TimeUnit.SECONDS);
            if (!terminou) sintese.destroy();
            sinteseAtual.compareAndSet(sintese, null);
            if (!continua(minhaGeracao) || !terminou || sintese.exitValue() != 0) return;
            if (!Files.exists(wav) || Files.size(wav) <= 44) return;

            String tocador = localizarTocador();
            if (tocador == null) return;
            fala = new ProcessBuilder(tocador, wav.toString())
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            falaAtual.set(fala);
            fala.waitFor();
            falaAtual.compareAndSet(fala, null);
        } catch (Exception erro) {
            // sem piper, modelo ou saída de áudio apenas ignora
        } finally {
            if (sintese != null) sinteseAtual.compareAndSet(sintese, null);
            if (fala != null) falaAtual.compareAndSet(fala, null);
            if (wav != null) {
                try {
                    Files.deleteIfExists(wav);
                } catch (IOException erro) {
                    // arquivo temporário some quando o sistema limpar
                }
            }
        }
    }

    private Process iniciarSintese(Path piper, Path modelo, Path wav) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(piper.toString(), "-m", modelo.toString(),
                "-f", wav.toString(), "-q");
        builder.environment().put("LD_LIBRARY_PATH", bibliotecas(piper));
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        return builder.start();
    }

    private boolean continua(long minhaGeracao) {
        return geracao.get() == minhaGeracao;
    }

    private void pararProcessos() {
        encerrar(sinteseAtual);
        encerrar(falaAtual);
    }

    private void encerrar(AtomicReference<Process> referencia) {
        Process processo = referencia.getAndSet(null);
        if (processo != null) processo.destroy();
    }

    private Path localizarPiper() {
        Path fixo = Paths.get("/opt/piper-tts/piper");
        if (Files.isExecutable(fixo)) return fixo;

        String variavel = System.getenv("PIPER_BIN");
        if (variavel != null) {
            Path caminho = Paths.get(variavel);
            if (Files.isExecutable(caminho)) return caminho;
        }
        Path noPath = procurarNoPath("piper");
        if (noPath != null) return noPath;

        Path local = Paths.get(System.getProperty("user.home"), ".local", "bin", "piper");
        return Files.isExecutable(local) ? local : null;
    }

    private Path localizarModelo() {
        String variavel = System.getenv("PIPER_MODEL");
        if (variavel != null) {
            Path caminho = Paths.get(variavel);
            if (Files.isRegularFile(caminho)) return caminho;
        }

        List<Path> pastas = List.of(
                Paths.get(System.getProperty("user.home"), ".local", "share", "piper-voices"),
                Paths.get(System.getProperty("user.home"), ".local", "share", "piper"),
                Paths.get("/usr/share/piper-voices"),
                Paths.get("/usr/local/share/piper-voices"),
                Paths.get("/opt/piper-tts/voices"));
        for (Path pasta : pastas) {
            if (!Files.isDirectory(pasta)) continue;
            Path exato = pasta.resolve(MODELO_PADRAO);
            if (Files.isRegularFile(exato)) return exato;
            Path portugues = primeiroModeloPortugues(pasta);
            if (portugues != null) return portugues;
        }
        return null;
    }

    private Path primeiroModeloPortugues(Path pasta) {
        try (DirectoryStream<Path> arquivos = Files.newDirectoryStream(pasta, "*.onnx")) {
            for (Path arquivo : arquivos) {
                if (arquivo.getFileName().toString().toLowerCase().contains("pt")) return arquivo;
            }
        } catch (IOException erro) {
            // pasta ilegível, tenta a próxima
        }
        return null;
    }

    private String localizarTocador() {
        for (String tocador : TOCADORES) {
            Path caminho = procurarNoPath(tocador);
            if (caminho != null) return caminho.toString();
        }
        return null;
    }

    private Path procurarNoPath(String nome) {
        String caminho = System.getenv("PATH");
        if (caminho == null) return null;
        for (String pasta : caminho.split(File.pathSeparator)) {
            Path candidato = Paths.get(pasta, nome);
            if (Files.isExecutable(candidato)) return candidato;
        }
        return null;
    }

    // o piper precisa encontrar as bibliotecas do espeak-ng ao lado dele ou em ~/.local/lib
    private String bibliotecas(Path piper) {
        List<String> pastas = new ArrayList<>();
        String atual = System.getenv("LD_LIBRARY_PATH");
        if (atual != null && !atual.isBlank()) pastas.add(atual);
        if (piper.getParent() != null) pastas.add(piper.getParent().toString());
        pastas.add(Paths.get(System.getProperty("user.home"), ".local", "lib").toString());
        pastas.add("/opt/piper-tts");
        pastas.add("/opt/piper-tts/lib");
        return String.join(File.pathSeparator, pastas);
    }
}
