package com.complexus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ponto de entrada do nucleo do Complexus.
 *
 * O backend concentra as regras de negocio do jogo (economia, empresas,
 * investimentos, politica e turnos) e serve o frontend estatico.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class ComplexusApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComplexusApplication.class, args);
    }
}
