package morse;

import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

// requisito 4 - desenho da árvore, raiz em cima e um nível por linha
public class TreeRenderer {
    private static final double LARGURA = 640;      // largura do desenho
    private static final double ALTURA = 280;       // altura do desenho
    private static final double TOPO = 20;          // altura onde fica a raiz
    private static final double ALTURA_NIVEL = 60;  // distância entre um nível e outro
    private static final double TAMANHO_NO = 26;    // tamanho do quadradinho de cada nó

    private Pane painel;
    private NoMorse raiz;

    // requisito 4 - desenha a árvore toda e devolve o painel pronto
    public Pane desenharArvore(ArvoreMorse arvore) {
        painel = new Pane();
        painel.setPrefSize(LARGURA, ALTURA);
        painel.getStyleClass().add("tree");
        raiz = arvore.getRaiz();
        desenharNo(raiz, LARGURA / 2, TOPO, LARGURA / 4);
        return painel;
    }

    // requisito 4 - desenha os filhos um nível abaixo (com metade do espaço) e depois o próprio nó
    private void desenharNo(NoMorse no, double x, double y, double espacoHorizontal) {
        double yFilho = y + ALTURA_NIVEL;
        if (no.esquerda != null) {
            desenharAresta(x, y, x - espacoHorizontal, yFilho, ".");
            desenharNo(no.esquerda, x - espacoHorizontal, yFilho, espacoHorizontal / 2);
        }
        if (no.direita != null) {
            desenharAresta(x, y, x + espacoHorizontal, yFilho, "-");
            desenharNo(no.direita, x + espacoHorizontal, yFilho, espacoHorizontal / 2);
        }
        Rectangle quadrado = new Rectangle(TAMANHO_NO, TAMANHO_NO);
        if (no == raiz) quadrado.getStyleClass().add("tree-root");
        StackPane celula = new StackPane(quadrado, new Text(no.estaVazio() ? "" : String.valueOf(no.character)));
        celula.relocate(x - TAMANHO_NO / 2, y - TAMANHO_NO / 2);
        painel.getChildren().add(celula);
    }

    // linha do pai até o filho, com o '.' ou o '-' do lado
    private void desenharAresta(double xPai, double yPai, double xFilho, double yFilho, String simbolo) {
        Text rotulo = new Text((xPai + xFilho) / 2 + (xFilho < xPai ? -14 : 6), (yPai + yFilho) / 2 - 2, simbolo);
        painel.getChildren().addAll(new Line(xPai, yPai, xFilho, yFilho), rotulo);
    }
}
