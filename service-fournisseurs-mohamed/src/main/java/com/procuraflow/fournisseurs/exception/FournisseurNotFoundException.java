package com.procuraflow.fournisseurs.exception;

public class FournisseurNotFoundException extends RuntimeException {

    public FournisseurNotFoundException(Long id) {
        super("Fournisseur introuvable avec l'identifiant : " + id);
    }
}
