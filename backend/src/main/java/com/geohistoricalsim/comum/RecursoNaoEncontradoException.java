package com.geohistoricalsim.comum;

/** Entidade referenciada nao existe. */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String recurso, Object id) {
        super(recurso + " nao encontrado: " + id);
    }
}
