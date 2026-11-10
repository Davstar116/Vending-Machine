/*
Autor: David Solorzano Argueta
Proyecto: Maquina Expendedora (En Consola) 
*/

package com.mycompany.roboticsvendingmachine;

import java.util.List;

public class VendingMachine {
    private final String machineId;
    private final String serialNumber;
    private final String location;
    private final List<Product> products;
    
    public VendingMachine(String machineId, String serialNumber, String location, List<Product> products) {
        this.machineId = machineId;
        this.serialNumber = serialNumber;
        this.location = location;
        this.products = products;
        
        MoneyManager.getBalance(machineId);
    }
    
    public String getMachineId() {
        return machineId;
    }
    
    public String getSerialNumber() {
        return serialNumber;
    }
    
    public String getLocation() {
        return location;
    }
    
    public List<Product> listProducts() {
        return List.copyOf(products);
    }
    
    public Product findProduct(String productId) {
        return products.stream().filter(p -> p.getId().equalsIgnoreCase(productId)).findFirst().orElse(null);
    }
    
    public double getMoneyBalance() {
        return MoneyManager.getBalance(machineId);
    }
    
    public void withdrawMoney(double amount) throws VendingException {
        double current = MoneyManager.getBalance(machineId);
        if(amount < 0 || current - amount < MoneyManager.MIN_BALANCE) {
            throw new VendingException(ErrorCode.INVALID_SELECTION);
        }
        MoneyManager.updateBalance(machineId, current - amount);
    }
    
    public void depositMoney(double amount) throws VendingException {
        if(amount < 0 || amount > MoneyManager.MAX_DEPOSIT_PER_OP) {
            throw new VendingException(ErrorCode.INVALID_SELECTION);
        }
        double current = MoneyManager.getBalance(machineId);
        MoneyManager.updateBalance(machineId, current + amount);
    }
    
    public void dispenseChange(double amount) {
        double current = MoneyManager.getBalance(machineId);
        MoneyManager.updateBalance(machineId, current - amount);
    }
    
    public double processSale(List<Selection> selections) throws VendingException {
        double totalAmount = 0.0;
        for(Selection sel : selections) {
            Product p = findProduct(sel.pid);
            if(p == null) {
                throw new VendingException(ErrorCode.INVALID_SELECTION);
            }
            if(p.getStock() < sel.qty) {
                throw new VendingException(ErrorCode.OUT_OF_STOCK);
            }
            p.reduceStock(sel.qty);
            totalAmount += sel.price * sel.qty;
        }
        return totalAmount;
    }
    
    public static class Selection {
        public final String pid;
        public final String name;
        public final int qty;
        public final double price;
        
        public Selection(String pid, String name, int qty, double price) {
            this.pid = pid;
            this.name = name;
            this.qty = qty;
            this.price = price;
        }
    }
}
