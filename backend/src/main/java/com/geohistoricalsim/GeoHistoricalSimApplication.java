package com.geohistoricalsim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ponto de entrada do nucleo do GeoHistoricalSim.
 *
 * O backend concentra as regras de negocio do jogo (economia, empresas,
 * investimentos, politica e turnos) e serve o frontend estatico.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class GeoHistoricalSimApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeoHistoricalSimApplication.class, args);
    }
}
