/*
Autor: David Solorzano Argueta
Proyecto: Maquina Expendedora (En Consola) 
*/

package com.mycompany.roboticsvendingmachine;

import java.util.Collections;
import java.awt.Desktop;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Admin extends User {
    private final String address;
    private final String rfc;
    private final String phone;
    
    private static final String SALES_LOG_FILE = "sales_log.txt";
    private static final DateTimeFormatter LOG_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter REPORT_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    public Admin(String id, String name, String address, String rfc, String phone, String username, String password) {
        super(id, name, username, password);
        this.address = address;
        this.rfc = rfc;
        this.phone = phone;
    }
    
    public String getAddress() {
        return address;
    }
    
    public String getRfc() {
        return rfc;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void listStock(VendingMachine vm) {
        System.out.printf("Stock Maquina %s (No.Serie: %s | Ubicacion: %s):%n", vm.getMachineId(), vm.getSerialNumber(), vm.getLocation());
        for(Product p : vm.listProducts()) {
            System.out.printf("  - %s (%s): Stock: %d%n", p.getName(), p.getId(), p.getStock());
        }
    }
    
    public Product searchProduct(VendingMachine vm, String term) {
        for(Product p : vm.listProducts()) {
            if(p.getId().equalsIgnoreCase(term) || p.getName().equalsIgnoreCase(term)) {
                return p;
            }
        }
        return null;
    }
    
    public void addStock(VendingMachine vm, String productId, int amount) throws VendingException {
        Product p = vm.findProduct(productId);
        if(p == null) {
            throw new VendingException(ErrorCode.INVALID_SELECTION);
        }
        p.addStock(amount);
    }
    
    public void generateReport(VendingLog log) {
        System.out.println("\n--- Reporte de Transacciones ---");
        List<Transaction> transactions = log.getAllTransactions();
        if(transactions.isEmpty()) {
            System.out.println("No existen transaccines registradas.");
            return;
        }
        for(Transaction tx : transactions) {
            System.out.printf("Maquina %s | Producto %s | Cantidad: %d | Monto: $%.2f | Fecha: %s%n", tx.getMachineId(), tx.getProductId(), tx.getQuantity(), tx.getAmount(), tx.getTimestamp());
        }
    }
    
    public void reportVentasPorProducto(Map<Integer, VendingMachine> machines) throws IOException {
        class ProdInfo {
            String name;
            int quantity;
            double amount;
        }
        
        Map<String, ProdInfo> aggregation = new LinkedHashMap<>();
        
        Map<String, String> idToName = new HashMap<>();
        for(VendingMachine vm : machines.values()) {
            for(Product p : vm.listProducts()) {
                idToName.put(p.getId(), p.getName());
            }
        }
        
        List<String> lines = Files.readAllLines(Paths.get(SALES_LOG_FILE));
        for(String line : lines) {
            line = line.trim();
            if(line.startsWith("-")) {
                int parenOpen = line.indexOf('(');
                int parenClose = line.indexOf(')');
                int xIndex = line.indexOf('x', parenClose);
                int arrow = line.indexOf("-> $", parenClose);
                if(parenOpen < 0 || parenClose < 0 || xIndex < 0 || arrow < 0) continue;
                
                String productId = line.substring(parenOpen + 1, parenClose).trim();
                String qtyStr = line.substring(xIndex + 1, line.indexOf(' ', xIndex + 1)).trim();
                String amountStr = line.substring(arrow + 4).trim();
                
                int qty;
                double sub;
                try {
                    qty = Integer.parseInt(qtyStr);
                    sub = Double.parseDouble(amountStr);
                }
                catch(NumberFormatException e) {
                    continue;
                }
                
                ProdInfo info = aggregation.getOrDefault(productId, new ProdInfo());
                info.name = idToName.getOrDefault(productId, "DESCONOCIDO");
                info.quantity += qty;
                info.amount += sub;
                aggregation.put(productId, info);
            }
        }
        
        if(aggregation.isEmpty()) {
            System.out.println("No hay ventas registradas en sales_log.txt.");
            return;
        }
        
        String timestamp = LocalDateTime.now().format(REPORT_TIMESTAMP);
        String filename = String.format("reporteVentasPorProducto_%s_%s.txt", this.getId(), timestamp);
        
        try(PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("=== Reporte Detallado de Ventas por Producto ===\n");
            pw.printf("Generado por Admin ID: %s   Fecha/Hora: %s%n%n", this.getId(), LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            pw.printf("%-10s | %-20s | %-10s | %-12s%n", "ProductoID", "Nombre", "Cantidad", "Monto Total");
            pw.println("---------------------------------------------------------------------------");
            for(Map.Entry<String, ProdInfo> entry : aggregation.entrySet()) {
                String pid = entry.getKey();
                ProdInfo info = entry.getValue();
                pw.printf("%-10s | %-20s | %-10d | $%10.2f%n", pid, info.name, info.quantity, info.amount);
            }
        }
        
        System.out.println("Reporte generado: " + filename);
        abrirArchivo(filename);
    }
    
    public void reportVentasTotalesMes(Map<Integer, VendingMachine> machines) throws IOException {
        LocalDateTime now = LocalDateTime.now();
        List<String> logLines = Files.readAllLines(Paths.get(SALES_LOG_FILE));
        List<String> detalleCompras = new ArrayList<>();
        int totalTransactions = 0;
        double totalSales = 0.0;
        
        LocalDate firstOfMonth = now.toLocalDate().withDayOfMonth(1);
        LocalDate firstOfNextMonth = firstOfMonth.plusMonths(1);
        
        LocalDateTime txDateTime = null;
        String currentMachine = null;
        
        Pattern maquinaPattern = Pattern.compile("Maquinas\\s*:?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        
        for(String line : logLines) {
            line = line.trim();
            
            Matcher m = maquinaPattern.matcher(line);
            if(m.find()) {
                currentMachine = m.group(1);
                continue;
            }
            
            if(line.toLowerCase().startsWith("fecha/hora")) {
                String[] parts = line.split(":", 2);
                if(parts.length == 2) {
                    try {
                        txDateTime = LocalDateTime.parse(parts[1].trim(), LOG_DATE_FORMAT);
                    }
                    catch(Exception e) {
                        txDateTime = null;
                    }
                }
                continue;
            }
            
            if(txDateTime != null && !txDateTime.toLocalDate().isBefore(firstOfMonth) && txDateTime.toLocalDate().isBefore(firstOfNextMonth) && line.startsWith("-")) {
                detalleCompras.add(String.format("Maquina %s | %s | %s", (currentMachine != null ? currentMachine : "N/A"), txDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), line));
                
                int arrow = line.indexOf("-> $");
                if(arrow >= 0) {
                    String amountStr = line.substring(arrow + 4).trim();
                    try {
                        double monto = Double.parseDouble(amountStr);
                        totalSales += monto;
                    }
                    catch(NumberFormatException ignored) {
                        
                    }
                }
                totalTransactions++;
            }
        }
        
        List<String> freeLines = new ArrayList<>();
        if(Files.exists(Paths.get(RoboticsVendingMachine.FREE_REWARDS_FILE))) {
            List<String> rewardsLog = Files.readAllLines(Paths.get(RoboticsVendingMachine.FREE_REWARDS_FILE));
            for(String line : rewardsLog) {
                if(line.toLowerCase().startsWith("fecha/hora")) {
                    String[] parts = line.split("\\|");
                    if(parts.length > 0) {
                        String datePart = parts[0].replaceFirst("(?i)Fecha/Hora:", "").trim();
                        try {
                            LocalDateTime d = LocalDateTime.parse(datePart, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                            if(!d.toLocalDate().isBefore(firstOfMonth) && d.toLocalDate().isBefore(firstOfNextMonth)) {
                                freeLines.add(line);
                            }
                        }
                        catch(Exception ignored) {
                            
                        }
                    }
                }
            }
        }
        
        String timestamp = now.format(REPORT_TIMESTAMP);
        String filename = String.format("reporteVentas_%s_%s.txt", this.getId(), timestamp);
        
        try(PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("=== Reporte Detallado de Ventas Totales al Mes ===\n");
            pw.printf("Generado por Admin ID: %s   Fecha/Hora: %s%n%n", this.getId(), now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            pw.println("-- Detalle de Compras Individuales ---");
            if(detalleCompras.isEmpty()) {
                pw.println("No hay compras registradas este mes.\n");
            }
            else {
                for(String detalle : detalleCompras) {
                    pw.println(detalle);
                }
                pw.println();
            }
            
            pw.println("--- Resumen General ---");
            pw.printf("Mes Actual: %s%n", now.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            pw.printf("Numero total de transacciones: %d%n", totalTransactions);
            pw.printf("Monto total de ventas: $%.2f%n%n", totalSales);
            
            pw.println(">>> Detalle por Maquina y Stock Actual <<<");
            for(Map.Entry<Integer, VendingMachine> entry : machines.entrySet()) {
                VendingMachine vm = entry.getValue();
                pw.printf("Maquina %d (ID %s | Ubicacion: %s):%n", entry.getKey(), vm.getMachineId(), vm.getLocation());
                pw.printf("  Balance actual: $%.2f%n", vm.getMoneyBalance());
                pw.println("  Stock por producto:");
                for(Product p : vm.listProducts()) {
                    pw.printf("   - %-20s (%s): %d unidades%n", p.getName(), p.getId(), p.getStock());
                }
                pw.println();
            }
            
            pw.println("--- Productos Gratis Entregados Este Mes ---");
            if(freeLines.isEmpty()) {
                pw.println("No se entregaron productos gratis este mes.");
            }
            else {
                for(String f : freeLines) {
                    pw.println(f);
                }
            }
        }
        
        System.out.println("Reporte generado: " + filename);
        abrirArchivo(filename);
    }
    
    private void abrirArchivo(String filename) {
        try {
            File file = new File(filename);
            if(Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
            else {
                System.out.println("El archivo esta guardado en: " + filename);
            }
        }
        catch(IOException e) {
            System.err.println("No se pudo abrir el archivo de reporte: " + e.getMessage());
        }
    }
    
    public void viewAllMachinesMoney(Map<Integer, VendingMachine> machines) {
        System.out.println("\n--- Dinero Actual en Cada Maquina ---");
        for(Map.Entry<Integer, VendingMachine> entry : machines.entrySet()) {
            VendingMachine vm = entry.getValue();
            System.out.printf("Maquina %d (ID %s): $%.2f%n", entry.getKey(), vm.getMachineId(), vm.getMoneyBalance());
        }
    }
    
    public double withdrawMoneyFromMachine(VendingMachine vm, double amount) throws VendingException {
        double current = vm.getMoneyBalance();
        double maxAllowed = current - MoneyManager.MIN_BALANCE;
        if(amount < 0 || amount > maxAllowed) {
            throw new VendingException(ErrorCode.INVALID_SELECTION);
        }
        vm.withdrawMoney(amount);
        return maxAllowed;
    }
    
    public void depositMoneyToMachine(VendingMachine vm, double amount) throws VendingException {
        vm.depositMoney(amount);
    }
}
