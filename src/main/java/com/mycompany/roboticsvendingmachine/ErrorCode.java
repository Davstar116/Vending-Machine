/*
Autor: David Solorzano Argueta
Proyecto: Maquina Expendedora (En Consola) 
*/

package com.mycompany.roboticsvendingmachine;

public enum ErrorCode {
    INVALID_SELECTION(100, "Seleccion invalida"),
    OUT_OF_STOCK(101, "Producto sin stock o stock insuficiente"),
    INSUFFICIENT_FUNDS(102, "Fondos insuficientes"),
    MACHINE_NOT_FOUND(103, "Maquina no encontrada"),
    ADMIN_AUTH_FAILED(104, "Autenticacion de administrador fallida"),
    CLIENT_AUTH_FAILED(105, "Autenticacion de cliente fallida"),
    STOCK_LIMIT_EXCEEDED(106, "Limite de stock excedido"),
    INVALID_CREDENTIALS(107, "Credenciales invalidas"),
    USERNAME_ALREADY_EXISTS(108, "El nombre de usuario ya existe");
    
    private final int code;
    private final String message;
    
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}
