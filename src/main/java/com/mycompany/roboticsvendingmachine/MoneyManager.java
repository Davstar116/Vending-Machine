/*
Autor: David Solorzano Argueta
Proyecto: Maquina Expendedora (En Consola) 
*/

package com.mycompany.roboticsvendingmachine;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class MoneyManager {
    private static final String MONEY_FILE = "dinero.txt";
    private static final Map<String, Double> moneyMap = new HashMap<>();
    
    public static final double MIN_BALANCE = 2750.0;
    public static final double MAX_DEPOSIT_PER_OP = 700;
    
    static {
        loadMoneyFromFile();
    }
    
    public static double getBalance(String machineId) {
        if(!moneyMap.containsKey(machineId)) {
            moneyMap.put(machineId, MIN_BALANCE);
            persistAllMoney();
        }
        return moneyMap.get(machineId);
    }
    
    public static void updateBalance(String machineId, double newBalance) {
        moneyMap.put(machineId, newBalance);
        persistAllMoney();
    }
    
    private static void loadMoneyFromFile() {
        File file = new File(MONEY_FILE);
        if(!file.exists()) return;
        try(BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if(parts.length < 2) continue;
                String id = parts[0].trim();
                try {
                    double bal = Double.parseDouble(parts[1].trim());
                    moneyMap.put(id, bal);
                }
                catch(NumberFormatException ignored) {
                    
                }
            }
        }
        catch(IOException e) {
            System.err.println("Error leyendo " + MONEY_FILE + ": " + e.getMessage());
        }
    }
    
    private static void persistAllMoney() {
        try(PrintWriter pw = new PrintWriter(new FileWriter(MONEY_FILE))) {
            for(Map.Entry<String, Double> entry : moneyMap.entrySet()) {
                pw.printf("%s,%.2f%n", entry.getKey(), entry.getValue());
            }
        }
        catch(IOException e) {
            System.err.println("Error al persistir dinero en " + MONEY_FILE + ": " + e.getMessage());
        }
    }
}
