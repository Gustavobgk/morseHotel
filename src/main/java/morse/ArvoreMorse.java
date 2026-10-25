package morse;

import java.util.StringJoiner;

// requisito 5 - lógica da árvore morse
public class ArvoreMorse {
    // tabela com os códigos de A a Z, só serve para inserir as letras
    private static final String[] TABELA_MORSE = {
        ".-", "-...", "-.-.", "-..", ".", "..-.", "--.", "....", "..", ".---", "-.-", ".-..", "--",
        "-.", "---", ".--.", "--.-", ".-.", "...", "-", "..-", "...-", ".--", "-..-", "-.--", "--.."
    };

    private final NoMorse raiz = new NoMorse(); // a raiz começa vazia

    public ArvoreMorse() {
        montarArvore();
    }

    public NoMorse getRaiz() {
        return raiz;
    }

    // requisito 1 - monta a árvore inserindo as letras de A a Z
    private void montarArvore() {
        for (char letra = 'A'; letra <= 'Z'; letra++) {
            inserir(letra, TABELA_MORSE[letra - 'A']);
        }
    }

    // requisito 1 - inserção: ponto desce para a esquerda, traço para a direita e cria nó vazio se não existir
    public void inserir(char letra, String codigoMorse) {
        NoMorse atual = raiz;
        for (char simbolo : codigoMorse.toCharArray()) {
            if (simbolo == '.') {
                if (atual.esquerda == null) atual.esquerda = new NoMorse();
                atual = atual.esquerda;
            } else {
                if (atual.direita == null) atual.direita = new NoMorse();
                atual = atual.direita;
            }
        }
        atual.character = letra;
    }

    // requisito 2 - acha o código da letra procurando na árvore (null se não achar)
    public String buscarCodigo(char letra) {
        return buscarCaminho(raiz, letra, "");
    }

    // requisito 2 - busca recursiva, vai juntando '.' quando desce para a esquerda e '-' quando desce para a direita
    private String buscarCaminho(NoMorse no, char letra, String caminho) {
        if (no == null) return null;
        if (no.character == letra) return caminho;
        String caminhoEsquerda = buscarCaminho(no.esquerda, letra, caminho + ".");
        return caminhoEsquerda != null ? caminhoEsquerda : buscarCaminho(no.direita, letra, caminho + "-");
    }

    // requisito 2 - codificar: letras separadas por espaço e palavras por " / "
    public String codificarTexto(String texto) {
        StringJoiner palavras = new StringJoiner(" / ");
        for (String palavra : texto.trim().toUpperCase().split("\\s+")) {
            StringJoiner codigos = new StringJoiner(" ");
            for (char letra : palavra.toCharArray()) {
                String codigo = buscarCodigo(letra);
                codigos.add(codigo == null ? "?" : codigo);
            }
            palavras.add(codigos.toString());
        }
        return palavras.toString();
    }

    // requisito 3 - decodificar: palavras separadas por " / " e letras por espaço
    public String decodificarMorse(String textoMorse) {
        StringJoiner palavras = new StringJoiner(" ");
        for (String palavra : textoMorse.trim().split("\\s*/\\s*")) {
            StringBuilder letras = new StringBuilder();
            for (String codigo : palavra.split("\\s+")) {
                if (!codigo.isEmpty()) letras.append(decodificarLetra(codigo));
            }
            palavras.add(letras);
        }
        return palavras.toString();
    }

    // requisito 3 - desce da raiz seguindo o código, se o caminho não existe ou o nó é vazio exibe ?
    private char decodificarLetra(String codigo) {
        NoMorse atual = raiz;
        for (char simbolo : codigo.toCharArray()) {
            if (simbolo == '.') atual = atual.esquerda;
            else if (simbolo == '-') atual = atual.direita;
            else return '?';
            if (atual == null) return '?';
        }
        return atual.estaVazio() ? '?' : atual.character;
    }
}
