package morse;

import java.io.IOException;
import java.io.InputStream;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

// sala de aula do habbo com o boneco na frente do quadro, falando o morse num balão
public class HabboBoneco extends Pane {
    // um png por frame, na ordem dos índices abaixo
    private static final String[] ARQUIVOS = {"parado.png", "falando.png", "piscando.png"};
    private static final int PARADO = 0, BOCA_ABERTA = 1, PISCANDO = 2;

    // sala e boneco ampliados 2x para o boneco ficar do tamanho do professor; posições já na escala ampliada
    private static final double ESCALA = 2;
    private static final double BONECO_X = 80, BONECO_Y = 85;
    private static final double BALAO_X = 60, BALAO_BASE = 130;
    // abaixo disso (metade do tamanho cheio) a sala fica pequena demais e é escondida
    private static final double ZOOM_MINIMO = 0.5;

    // tempos da fala em milissegundos
    private static final int PONTO = 120, TRACO = 360, INTERVALO = 120;
    private static final int PAUSA_LETRA = 360, PAUSA_PALAVRA = 600;

    private final Label balao = new Label("oi! codifique algo");
    private final ImageView sprite = new ImageView();
    private final Image[] quadros = new Image[ARQUIVOS.length];
    private final boolean temSprite;
    private final Timeline piscar;
    private Timeline fala;

    // zoom da sala e do boneco conforme o espaço livre na janela; o balão não encolhe para o texto continuar legível
    private final Scale zoom = new Scale(1, 1, 0, 0);
    private final DoubleProperty baseBalao = new SimpleDoubleProperty(BALAO_BASE);
    private double largura, altura;

    // peças do boneco reserva, usadas quando não tem o png
    private final Rectangle boca = new Rectangle(6, 2);
    private final Rectangle olhoEsquerdo = new Rectangle(3, 3);
    private final Rectangle olhoDireito = new Rectangle(3, 3);

    public HabboBoneco() {
        temSprite = carregarSprite();
        balao.getStyleClass().addAll("bubble", "habbo-balao");

        Node boneco = temSprite ? sprite : criarBonecoReserva();
        boneco.relocate(BONECO_X, BONECO_Y);
        // o balão cresce para cima a partir da cabeça do boneco
        balao.setLayoutX(BALAO_X);
        balao.layoutYProperty().bind(baseBalao.subtract(balao.heightProperty()));

        Group cenario = new Group(criarSala(), boneco);
        cenario.getTransforms().add(zoom);
        getChildren().addAll(cenario, balao);
        ajustar(largura, altura);
        getStyleClass().add("habbo-sala");

        // pisca a cada 3 segundos quando está parado
        piscar = new Timeline(
                new KeyFrame(Duration.millis(3000), evento -> mostrarFrame(PISCANDO)),
                new KeyFrame(Duration.millis(3150), evento -> mostrarFrame(PARADO)));
        piscar.setCycleCount(Animation.INDEFINITE);
        mostrarFrame(PARADO);
        piscar.play();
    }

    // encolhe a sala para caber no espaço livre (nunca passa do tamanho cheio) ou esconde se não couber
    public void ajustar(double larguraLivre, double alturaLivre) {
        double escala = Math.min(1, Math.min(larguraLivre / largura, alturaLivre / altura));
        boolean cabe = escala >= ZOOM_MINIMO;
        setVisible(cabe);
        setManaged(cabe);
        if (!cabe) return;
        zoom.setX(escala);
        zoom.setY(escala);
        setMinSize(largura * escala, altura * escala);
        setPrefSize(largura * escala, altura * escala);
        setMaxSize(largura * escala, altura * escala);
        balao.setLayoutX(BALAO_X * escala);
        baseBalao.set(BALAO_BASE * escala);
    }

    // fala o morse símbolo por símbolo: boca curta no ponto, longa no traço
    public void falar(String morse) {
        Timeline nova = new Timeline();
        double tempo = 0;
        for (int i = 0; i < morse.length(); i++) {
            String parcial = morse.substring(0, i + 1);
            char simbolo = morse.charAt(i);
            if (simbolo == '.' || simbolo == '-') {
                nova.getKeyFrames().add(new KeyFrame(Duration.millis(tempo), evento -> {
                    balao.setText(parcial);
                    mostrarFrame(BOCA_ABERTA);
                }));
                tempo += simbolo == '.' ? PONTO : TRACO;
                nova.getKeyFrames().add(new KeyFrame(Duration.millis(tempo), evento -> mostrarFrame(PARADO)));
                tempo += INTERVALO;
            } else {
                nova.getKeyFrames().add(new KeyFrame(Duration.millis(tempo), evento -> balao.setText(parcial)));
                tempo += simbolo == '/' ? PAUSA_PALAVRA : simbolo == ' ' ? PAUSA_LETRA : INTERVALO;
            }
        }
        balao.setText("");
        tocar(nova, tempo);
    }

    // mostra o texto de uma vez e mexe a boca algumas vezes
    public void dizer(String texto) {
        Timeline nova = new Timeline();
        double tempo = 0;
        for (int i = 0; i < 4; i++) {
            nova.getKeyFrames().add(new KeyFrame(Duration.millis(tempo), evento -> mostrarFrame(BOCA_ABERTA)));
            tempo += 150;
            nova.getKeyFrames().add(new KeyFrame(Duration.millis(tempo), evento -> mostrarFrame(PARADO)));
            tempo += 150;
        }
        balao.setText(texto);
        tocar(nova, tempo);
    }

    // para a fala anterior e a piscada, toca a nova e volta a piscar no final
    private void tocar(Timeline nova, double tempoFinal) {
        if (fala != null) fala.stop();
        piscar.stop();
        nova.getKeyFrames().add(new KeyFrame(Duration.millis(tempoFinal), evento -> {
            mostrarFrame(PARADO);
            piscar.playFromStart();
        }));
        fala = nova;
        fala.play();
    }

    private void mostrarFrame(int frame) {
        if (temSprite) {
            sprite.setImage(quadros[frame]);
        } else {
            boca.setHeight(frame == BOCA_ABERTA ? 5 : 2);
            olhoEsquerdo.setHeight(frame == PISCANDO ? 1 : 3);
            olhoDireito.setHeight(frame == PISCANDO ? 1 : 3);
        }
    }

    // carrega os pngs dos frames, se faltar algum usa o boneco reserva
    private boolean carregarSprite() {
        for (int i = 0; i < ARQUIVOS.length; i++) {
            quadros[i] = carregarImagem(ARQUIVOS[i]);
            if (quadros[i] == null) return false;
        }
        sprite.setSmooth(false);
        sprite.setFitWidth(quadros[0].getWidth() * ESCALA);
        sprite.setFitHeight(quadros[0].getHeight() * ESCALA);
        return true;
    }

    // fundo da sala de aula, se não tiver a imagem fica só a cor de fundo do css
    private Node criarSala() {
        Image escola = carregarImagem("escola.png");
        ImageView sala = new ImageView(escola);
        sala.setSmooth(false);
        largura = (escola == null ? 300 : escola.getWidth()) * ESCALA;
        altura = (escola == null ? 187 : escola.getHeight()) * ESCALA;
        sala.setFitWidth(largura);
        sala.setFitHeight(altura);
        return sala;
    }

    // devolve null se o arquivo não existir ou não abrir
    private Image carregarImagem(String nome) {
        try (InputStream arquivo = getClass().getResourceAsStream("sprites/" + nome)) {
            if (arquivo == null) return null;
            Image imagem = new Image(arquivo);
            return imagem.isError() ? null : imagem;
        } catch (IOException erro) {
            return null;
        }
    }

    // boneco simples de retângulos para quando o png não existe
    private Node criarBonecoReserva() {
        Rectangle cabeca = new Rectangle(8, 0, 24, 24);
        cabeca.getStyleClass().add("habbo-pele");
        Rectangle corpo = new Rectangle(6, 24, 28, 30);
        corpo.getStyleClass().add("habbo-camisa");
        Rectangle pernas = new Rectangle(10, 54, 20, 20);
        pernas.getStyleClass().add("habbo-calca");
        olhoEsquerdo.relocate(14, 8);
        olhoDireito.relocate(23, 8);
        boca.relocate(17, 16);
        for (Rectangle peca : new Rectangle[] {olhoEsquerdo, olhoDireito, boca}) {
            peca.getStyleClass().add("habbo-rosto");
        }
        Pane boneco = new Pane(cabeca, corpo, pernas, olhoEsquerdo, olhoDireito, boca);
        boneco.setMaxSize(40, 74);
        return boneco;
    }
}
