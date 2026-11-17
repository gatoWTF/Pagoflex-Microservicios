package com.pagoflex.compromisos.model;

public final class Rut {

    private Rut() {
    }

    public static String clave(String rut) {
        return rut == null ? "" : rut.trim().toUpperCase().replace(".", "").replace("-", "");
    }

    public static boolean valido(String rut) {
        String limpio = clave(rut);
        if (limpio.length() < 2) {
            return false;
        }
        String cuerpo = limpio.substring(0, limpio.length() - 1);
        char digitoVerificador = limpio.charAt(limpio.length() - 1);
        if (!cuerpo.chars().allMatch(c -> c >= '0' && c <= '9')) {
            return false;
        }

        int suma = 0;
        int multiplo = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += (cuerpo.charAt(i) - '0') * multiplo;
            multiplo = multiplo == 7 ? 2 : multiplo + 1;
        }

        int resto = 11 - (suma % 11);
        char esperado = resto == 11 ? '0' : resto == 10 ? 'K' : (char) ('0' + resto);
        return digitoVerificador == esperado;
    }
}
