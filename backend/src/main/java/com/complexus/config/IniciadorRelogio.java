package com.complexus.config;

import com.complexus.core.ServicoEstadoJogo;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * Cria a linha do relogio da partida antes de o servidor aceitar requisicao.
 *
 * {@code ServicoEstadoJogo.obter()} cria a linha unica de {@code estado_jogo}
 * quando ela ainda nao existe. A carga inicial roda como {@code ApplicationRunner},
 * ou seja, <b>depois</b> que o Tomcat ja esta atendendo: em uma partida nova, uma
 * requisicao que chegasse nesse intervalo criava a mesma linha em paralelo e
 * derrubava a carga do mundo com violacao de chave primaria.
 *
 * A inicializacao de bean acontece antes de o servidor abrir a porta, entao
 * garantir a linha aqui elimina a corrida sem mudar a regra de ninguem.
 */
@Component
public class IniciadorRelogio implements InitializingBean {

    private final ServicoEstadoJogo estadoJogo;

    public IniciadorRelogio(ServicoEstadoJogo estadoJogo) {
        this.estadoJogo = estadoJogo;
    }

    @Override
    public void afterPropertiesSet() {
        estadoJogo.obter();
    }
}
