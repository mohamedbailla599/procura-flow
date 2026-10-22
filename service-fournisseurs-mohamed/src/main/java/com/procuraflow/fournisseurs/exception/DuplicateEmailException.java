package com.procuraflow.fournisseurs.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("Un fournisseur avec l'adresse e-mail '" + email + "' existe déjà.");
    }
}
