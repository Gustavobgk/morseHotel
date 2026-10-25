package morse;

// requisito 5 - nó da árvore, o ponto vai para a esquerda e o traço para a direita
public class NoMorse {
    char character;    // letra do nó ('\0' quando o nó está vazio)
    NoMorse esquerda;  // filho do ponto (.)
    NoMorse direita;   // filho do traço (-)

    boolean estaVazio() {
        return character == '\0';
    }
}
