/*
Autor: David Solorzano Argueta
Proyecto: Maquina Expendedora (En Consola) 
*/

package com.mycompany.roboticsvendingmachine;

public class VendingException extends Exception {
    private final ErrorCode code;
    
    public VendingException(ErrorCode code) {
        super(code.getMessage());
        this.code = code;
    }
    
    public int getErrorCode() {
        return code.getCode();
    }
}
