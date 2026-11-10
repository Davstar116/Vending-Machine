/*
Autor: David Solorzano Argueta
Proyecto: Maquina Expendedora (En Consola) 
*/

package com.mycompany.roboticsvendingmachine;

import java.util.ArrayList;
import java.util.*;
import java.util.List;

public class Client extends User {
    private final String email;
    private final String phone;
    private final List<String> purchaseHistory = new ArrayList<>();
    private double totalConsumed = 0.0;
    
    public Client(String id, String name, String username, String password, String email, String phone) {
        super(id, name, username, password);
        this.email = email;
        this.phone = phone;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void recordPurchase(String ticketId, double amount) {
        purchaseHistory.add(ticketId);
        totalConsumed += amount;
    }
    
    public double getTotalConsumed() {
        return totalConsumed;
    }
    
    public void setTotalConsumed(double totalCantidad) {
        this.totalConsumed = totalCantidad;
    }
    
    public boolean qualifiesForFreeProduct() {
        return totalConsumed >= 1000.0;
    }
    
    public void grantFreeProduct(VendingMachine vm) {
        List<Product> list = new ArrayList<>(vm.listProducts());
        if(!list.isEmpty()) {
            Product p = list.get(new Random().nextInt(list.size()));
            try {
                p.reduceStock(1);
            }
            catch(VendingException ignored) {
            }
        }
    }
    
    public List<String> getPurchaseHistory() {
        return purchaseHistory;
    }
}
