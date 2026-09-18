package com.geohistoricalsim.comum;

/** Violacao de uma regra de jogo (entrada valida, porem nao permitida no estado atual). */
public class RegraDeNegocioException extends RuntimeException {
    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
