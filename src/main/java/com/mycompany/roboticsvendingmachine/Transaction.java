/*
Autor: David Solorzano Argueta
Proyecto: Maquina Expendedora (En Consola) 
*/

package com.mycompany.roboticsvendingmachine;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transaction {
    private final String machineId;
    private final String productId;
    private final int quantity;
    private final double amount;
    private final String timestamp;
    
    public Transaction(String machineId, String productId, int quantity, double amount, String timestmap) {
        this.machineId = machineId;
        this.productId = productId;
        this.quantity = quantity;
        this.amount = amount;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    public String getMachineId() {
        return machineId;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
}
