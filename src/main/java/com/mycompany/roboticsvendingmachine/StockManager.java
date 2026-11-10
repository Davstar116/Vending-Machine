/*
Autor: David Solorzano Argueta
Proyecto: Maquina Expendedora (En Consola) 
*/

package com.mycompany.roboticsvendingmachine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class StockManager {
    private static final String STOCK_FILE = "stock.txt";
    private static final Map<String, Integer> stockMap = new HashMap<>();
    
    static {
        loadStock();
    }
    
    public static int getStock(String machineId, String productId, int defaultStock) {
        String key = machineId + "-" + productId;
        if(stockMap.containsKey(key)) {
            return stockMap.get(key);
        }
        else {
            stockMap.put(key, defaultStock);
            persistAllStock();
            return defaultStock;
        }
    }
    
    public static void updateStock(String machineId, String productId, int newStock) {
        String key = machineId + "-" + productId;
        stockMap.put(key, newStock);
        persistAllStock();
    }
    
    private static void loadStock() {
        File file = new File(STOCK_FILE);
        if(!file.exists()) {
            return;
        }
        try(BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if(parts.length < 2) continue;
                String key = parts[0].trim();
                try {
                    int st = Integer.parseInt(parts[1].trim());
                    stockMap.put(key, st);
                }
                catch(NumberFormatException e) {
                    
                }
            }
        }
        catch(IOException e) {
            System.err.println("Error al leer " + STOCK_FILE + ": " + e.getMessage());
        }
    }
    
    private static void persistAllStock() {
        try(PrintWriter pw = new PrintWriter(new FileWriter(STOCK_FILE))) {
            for(Map.Entry<String, Integer> entry : stockMap.entrySet()) {
                pw.printf("%s,%d%n", entry.getKey(), entry.getValue());
            }
        }
        catch(IOException e) {
            System.err.println("Error al persistir stock en " + STOCK_FILE + ": " + e.getMessage());
        }
    }
}
