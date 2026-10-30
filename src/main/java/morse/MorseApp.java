package morse;

import java.io.IOException;
import java.io.InputStream;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

// requisito 5 - janela do morse hotel com os campos de botões e eventos e o main
public class MorseApp extends Application {
    private final ArvoreMorse arvore = new ArvoreMorse();
    private final TextField campoPalavra = new TextField();
    private final TextField campoMorse = new TextField();
    private final Label resultadoMorse = new Label("o código Morse aparece aqui");
    private final Label resultadoPalavra = new Label("a palavra aparece aqui");
    private final HabboBoneco boneco = new HabboBoneco();

    @Override
    public void start(Stage janela) {
        VBox tela = montarTela();
        tela.setMinWidth(Region.USE_PREF_SIZE);
        HBox raiz = new HBox(tela, boneco);
        raiz.getStyleClass().add("app");
        // a escola usa o espaço que sobra ao lado dos módulos; se não couber, some
        raiz.widthProperty().addListener(mudou -> ajustarEscola(raiz, tela));
        raiz.heightProperty().addListener(mudou -> ajustarEscola(raiz, tela));
        raiz.setStyle("-fx-font-family: '" + carregarFontePixel() + "';");
        Scene cena = new Scene(raiz);
        cena.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        janela.setTitle("Morse Hotel");
        janela.setScene(cena);
        janela.show();
    }

    private void ajustarEscola(HBox raiz, VBox tela) {
        double larguraLivre = raiz.getWidth() - raiz.getInsets().getLeft() - raiz.getInsets().getRight()
                - raiz.getSpacing() - tela.prefWidth(-1);
        double alturaLivre = raiz.getHeight() - raiz.getInsets().getTop() - raiz.getInsets().getBottom();
        boneco.ajustar(larguraLivre, alturaLivre);
    }

    // módulos de codificar, decodificar, árvore e legenda no rodapé
    private VBox montarTela() {
        Button botaoCodificar = new Button("Codificar");
        botaoCodificar.setOnAction(evento -> codificar());
        Button botaoDecodificar = new Button("Decodificar");
        botaoDecodificar.setOnAction(evento -> decodificar());
        campoPalavra.setPromptText("digite uma palavra ou frase");
        campoMorse.setPromptText("digite algo em morse");
        HBox.setHgrow(campoPalavra, Priority.ALWAYS);
        HBox.setHgrow(campoMorse, Priority.ALWAYS);
        resultadoMorse.getStyleClass().add("bubble");
        resultadoPalavra.getStyleClass().add("bubble");

        VBox tela = new VBox(
                criarModulo("Codificar", "orange", new HBox(8, campoPalavra, botaoCodificar), resultadoMorse),
                criarModulo("Decodificar", "blue", new HBox(8, campoMorse, botaoDecodificar), resultadoPalavra),
                criarModulo("Árvore", "orange", new ScrollPane(new TreeRenderer().desenharArvore(arvore))),
                new Label(". = esquerda │ - = direita")); 
        tela.getStyleClass().add("tela");
        return tela;
    }

    // módulo do design
    private VBox criarModulo(String titulo, String corCabecalho, Node... conteudo) {
        Label cabecalho = new Label(titulo);
        cabecalho.setMaxWidth(Double.MAX_VALUE);
        cabecalho.getStyleClass().addAll("module-header", corCabecalho);
        VBox corpo = new VBox(conteudo);
        corpo.getStyleClass().add("module-body");
        VBox modulo = new VBox(cabecalho, corpo);
        modulo.getStyleClass().add("module");
        return modulo;
    }

    // requisito 2 - botão para codificar de palavra para morse
    private void codificar() {
        String morse = arvore.codificarTexto(campoPalavra.getText());
        resultadoMorse.setText(morse);
        boneco.falar(morse);
    }

    // requisito 3 - botão decodificar de morse para palavra
    private void decodificar() {
        String palavra = arvore.decodificarMorse(campoMorse.getText());
        resultadoPalavra.setText(palavra);
        boneco.dizer(palavra);
    }

    // carrega a fonte volter, caso não tiver o arquivo usa verdana ou monospaced
    private String carregarFontePixel() {
        try (InputStream arquivo = getClass().getResourceAsStream("fonts/Volter-Goldfish.ttf")) {
            Font fonte = (arquivo == null) ? null : Font.loadFont(arquivo, 9);
            if (fonte != null) return fonte.getFamily();
        } catch (IOException erro) {
            // se der erro usa as fontes reserva
        }
        return Font.getFamilies().contains("Verdana") ? "Verdana" : "Monospaced";
    }

    public static void main(String[] argumentos) {
        launch(argumentos);
    }
}
