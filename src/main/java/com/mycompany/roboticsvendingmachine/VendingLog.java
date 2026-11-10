/*
Autor: David Solorzano Argueta
Proyecto: Maquina Expendedora (En Consola) 
*/

package com.mycompany.roboticsvendingmachine;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class VendingLog {
    public final List<Transaction> transactions  = new ArrayList<>();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public void record(Transaction tx) {
        transactions.add(tx);
    }
    
    public List<Transaction> getAllTransactions() {
        return List.copyOf(transactions);
    }
    
    public Map<String, Double> getTotalSalesByProduct() {
        Map<String, Double> ventasPorProducto = new HashMap<>();
        for(Transaction tx : transactions) {
            ventasPorProducto.merge(tx.getProductId(), tx.getAmount(), Double::sum);
        }
        return ventasPorProducto;
    }
    
    public double getTotalSalesAllProducts() {
        double total = 0.0;
        for(Transaction tx : transactions) {
            total += tx.getAmount();
        }
        return total;
    }
    
    public static Transaction createTransaction(String machineId, String productId, int quantity, double amount) {
        String now = LocalDateTime.now().format(FORMATTER);
        return new Transaction(machineId, productId, quantity, amount, now);
    }
    
    public static class Entry {
        int count;
        double total;
        Entry(int c, double t) {
            count = c;
            total = t;
        }
    }
}
