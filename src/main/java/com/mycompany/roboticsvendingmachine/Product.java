/*
Autor: David Solorzano Argueta
Proyecto: Maquina Expendedora (En Consola) 
*/

package com.mycompany.roboticsvendingmachine;

import java.util.Objects;

public class Product {
    private final String machineId;
    private final String id;
    private final String  name;
    private final double price;
    private int stock;
    private final boolean isBeverage;
    
    public static final int MAX_STOCK_SNACKS = 15;
    public static final int MAX_STOCK_DRINKS = 10;
    
    public Product(String machineId, String id, String name, double price, int defaultStock, boolean isBeverage) throws VendingException {
        this.machineId = Objects.requireNonNull(machineId);
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.price = price;
        this.isBeverage = isBeverage;
        
        int maxAllowed = isBeverage ? MAX_STOCK_DRINKS : MAX_STOCK_SNACKS;
        if(defaultStock < 0 || defaultStock > maxAllowed) {
            throw new VendingException(ErrorCode.INVALID_SELECTION);
        }
        
        this.stock = StockManager.getStock(machineId, id, defaultStock);
    }
    
    public String getMachineId() {
        return machineId;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public double getPrice() {
        return price;
    }
    
    public int getStock() {
        return stock;
    }
    
    public boolean isBeverage() {
        return isBeverage;
    }
    
    public void addStock(int amount) throws VendingException {
        if(amount < 0) {
            throw new VendingException(ErrorCode.INVALID_SELECTION);
        }
        int maxAllowed = isBeverage ? MAX_STOCK_DRINKS : MAX_STOCK_SNACKS;
        if(stock + amount > maxAllowed) {
            throw new VendingException(ErrorCode.INVALID_SELECTION);
        }
        stock += amount;
        StockManager.updateStock(machineId, id, stock);
    }
    
    public void reduceStock(int amount) throws VendingException {
        if(amount <= 0) {
            throw new VendingException(ErrorCode.INVALID_SELECTION);
        }
        if(stock < amount) {
            throw new VendingException(ErrorCode.OUT_OF_STOCK);
        }
        stock -= amount;
        StockManager.updateStock(machineId, id, stock);
    }
}
