package com.pagoflex.usuarios.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RutTest {

    @Test
    void clave_quitaPuntosYGuion() {
        assertEquals("16248317K", Rut.clave(" 16.248.317-k "));
    }

    @Test
    void clave_conNulo_devuelveTextoVacio() {
        assertEquals("", Rut.clave(null));
    }

    @Test
    void valido_conDigitoCorrecto_devuelveVerdadero() {
        assertTrue(Rut.valido("15.834.207-3"));
        assertTrue(Rut.valido("16.248.317-K"));
        assertTrue(Rut.valido("19.287.430-0"));
    }

    @Test
    void valido_conDigitoIncorrecto_devuelveFalso() {
        assertFalse(Rut.valido("15.834.207-4"));
    }

    @Test
    void valido_conLetrasOMuyCorto_devuelveFalso() {
        assertFalse(Rut.valido("12A45678-9"));
        assertFalse(Rut.valido("1"));
    }
}
