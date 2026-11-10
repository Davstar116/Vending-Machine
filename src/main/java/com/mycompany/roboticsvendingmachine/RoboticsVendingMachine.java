/*
Autor: David Solorzano Argueta
Proyecto: Maquina Expendedora (En Consola) 
*/

package com.mycompany.roboticsvendingmachine;

import java.awt.Desktop;
import java.io.File;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class RoboticsVendingMachine {
    private static final Scanner leer = new Scanner(System.in);
    private static UserManager um;
    private static VendingLog log = new VendingLog();
    private static Map<Integer, VendingMachine> machines;
    private static final String SALES_LOG_FILE = "sales_log.txt";
    public static final String FREE_REWARDS_FILE = "free_rewards.txt";
    private static final Random RANDOM = new Random();
    
    private static class Selection {
        String pid;
        String name;
        int qty;
        double price;
        
        Selection(String pid, String name, int qty, double price) {
            this.pid = pid;
            this.name = name;
            this.qty = qty;
            this.price = price;
        }
    }

    public static void main(String[] args) throws IOException, VendingException {
        showAbout();
        um = new UserManager();
        initMachines();
        
        while(true) {
            System.out.println("\n--- Menu Principal ---");
            System.out.println("1) Iniciar sesion");
            System.out.println("2) Crear nueva cuenta");
            System.out.println("3) Salir");
            System.out.print("Seleccione una opcion: ");
            String opt = leer.nextLine().trim();
            
            switch (opt) {
                case "1":
                    try {
                        User user = loginFlow();
                        simulateLoading("Cargando sesion...");
                        if(user instanceof Client) {
                            clientSession((Client) user);
                        }
                        else {
                            adminSession((Admin) user);
                        }
                    }
                    catch(VendingException ex) {
                        System.err.printf("Error [%d]: %s%n", ex.getErrorCode(), ex.getMessage());
                    }
                    break;
                case "2":
                    try {
                        registerFlow();
                    }
                    catch(VendingException | IOException ex) {
                        System.err.println("No fue posible registrar la cuenta: " + ex.getMessage());
                    }
                    break;
                case "3":
                    System.out.println("Rob: Hasta pronto!");
                    return;
                default:
                    System.out.println("Opcion invalida. Intente de nuevo.");
            }
        }
    }
    
    private static void showAbout() {
        System.out.println("*** Proyecto Final POO ***");
        System.out.println("Grupo: 06, Semestre 2025-2");
        System.out.println("Brigada: 03");
        System.out.println("Integrantes:\n- Molina Gutierrez Miguel Alberto (Realizo codigo y video de memoria tecnica)\n- Pena Sanchez David Melesio (Realizo codigo y manuales de usuario)\n- Ramirez Gudino Jeancarlo (Realizo codigo y diagramas UML)\n- Solorzano Argueta David (Realizo codigo y documentacion)");
        System.out.println();
    }
    
    private static User loginFlow() throws VendingException {
        System.out.print("Usuario: ");
        String u = leer.nextLine().trim();
        System.out.print("Password: ");
        String p = leer.nextLine().trim();
        
        User user = um.authenticate(u, p);
        System.out.printf("Rob: Bienvenid@, %s!%n", user.getName());
        return user;
    }
    
    private static void registerFlow() throws IOException, VendingException {
        System.out.println("\n--- Nueva Cuenta ---");
        System.out.print("Nombre completo: ");
        String name = leer.nextLine().trim();
        
        String username;
        while(true) {
            System.out.print("Nombre de usuario: ");
            username = leer.nextLine().trim();
            if(username.isEmpty()) {
                System.out.println("El nombre de usuario no puede estar vacío.");
                continue;
            }
            if(um.getClientsMap().containsKey(username) || um.getAdminsMap().containsKey(username)) {
                System.out.println("Nombre de usuario ya existente. Intente otro.");
            }
            else {
                break;
            }
        }
        
        System.out.print("Password: ");
        String password = leer.nextLine().trim();
        System.out.print("Correo electronico: ");
        String email = leer.nextLine().trim();
        System.out.print("Telefono: ");
        String phone = leer.nextLine().trim();
        
        um.registerNewClient(name, username, password, email, phone);
        System.out.println("Cuenta creada exitosamente. Ya puede iniciar sesion.");
    }
    
    private static void initMachines() throws VendingException {
        machines = new HashMap<>();
        for(int i = 1; i <= 4; i++) {
            String machineId = String.valueOf(i);
            String serial = "SN00" + i;
            String location;
            
            switch(i) {
                case 1 -> location = "Terraza Coapa - Entrada";
                case 2 -> location = "Terraza Coapa - Centro";
                case 3 -> location = "Terraza Coapa - Nivel 1";
                default -> location = "Terraza Coapa - Nivel 2";
            }
            
            List<Product> products = ProductFactory.getProductsForMachine(machineId);
            machines.put(i, new VendingMachine(machineId, serial, location, products));
        }
    }
    
    private static void displayMachinesMenu() {
        System.out.println("\n--- Maquinas Disponibles en Terraza Coapa ---");
        for(Map.Entry<Integer, VendingMachine> entry : machines.entrySet()) {
            int num = entry.getKey();
            VendingMachine vm = entry.getValue();
            System.out.printf("Maquina %d: No.Serie: %s | Ubicacion: %s%n", num, vm.getSerialNumber(), vm.getLocation());
            System.out.println("  Productos y stock:");
            for(Product p : vm.listProducts()) {
                System.out.printf("   %s - %s: Stock: %d%n", p.getId(), p.getName(), p.getStock());
            }
            System.out.println();
        }
    }
    
    private static void clientSession(Client client) {
        boolean seguirComprando = true;
        while(seguirComprando) {
            clientFlow(client);
            System.out.print("Desea comprar en otra maquina? (s/n): ");
            String respuesta = leer.nextLine().trim().toLowerCase();
            if(!(respuesta.equals("s") || respuesta.equals("si"))) {
                seguirComprando = false;
            }
        }
        simulateLoading("Cerrando sesion y regresando al menu principal....");
        System.out.println("Cerrando sesion. Regresando al menu principal.");
    }
    
    private static void adminSession(Admin admin) {
        Map<Integer, List<Product>> lowStockMap = new HashMap<>();
        for(int i = 1; i <= machines.size(); i++) {
            VendingMachine vm = machines.get(i);
            List<Product> lowList = new ArrayList<>();
            for(Product p : vm.listProducts()) {
                int threshold = 3;
                if(p.getStock() <= threshold) {
                    lowList.add(p);
                }
            }
            
            if(!lowList.isEmpty()) {
                lowStockMap.put(i, lowList);
            }
        }
        
        if(!lowStockMap.isEmpty()) {
            System.out.println("\n=================================================");
            System.out.println("  ALERTA: Stock bajo de los siguientes productos:");
            System.out.println("=================================================");
            for(int i = 1; i <= machines.size(); i++) {
                if(!lowStockMap.containsKey(i)) continue;
                VendingMachine vm = machines.get(i);
                List<Product> lowList = lowStockMap.get(i);
                System.out.printf("Maquina %s (No.Serie: %s | Ubicacion: %s)%n", vm.getMachineId(), vm.getSerialNumber(), vm.getLocation());
                for(Product p : lowList) {
                    System.out.printf("  - %s (%s) - Stock actual: %d%n", p.getName(), p.getId(), p.getStock());
                }
                System.out.println("-------------------------------------------------");
            }
        }
        else {
            System.out.println("\n| No hay productos con stock bajo en ninguna máquina. |");
        }
        
        boolean salir = false;
        while(!salir) {
            System.out.println("\n--- Menu Administrador ---");
            System.out.println("1) Listar stock");
            System.out.println("2) Buscar producto por ID o nombre");
            System.out.println("3) Agregar stock");
            System.out.println("4) Reportes");
            System.out.println("5) Administrar dinero");
            System.out.println("6) Crear nueva cuenta de Administrador");
            System.out.println("7) Cerrar sesion");
            System.out.print("Seleccione una opcion: ");
            String opcion = leer.nextLine().trim();
            
            switch(opcion) {
                case "1":
                    listarStockSubmenu(admin);
                    break;
                case "2":
                    buscarProducto(admin);
                    break;
                case "3":
                    anadirStock(admin);
                    break;
                case "4":
                    reportesSubmenu(admin);
                    break;
                case "5":
                    administrarDineroSubmenu(admin);
                    break;
                case "6":
                    crearAdminSubmenu();
                    break;
                case "7":
                    salir = true;
                    break;
                default:  System.out.println("Opcion invalida. Ingrese un numero del 1 al 6.");
            }
        }
        
        simulateLoading("Cerrando sesion y regresando al menu principal...");
        System.out.println("Cerrando sesion. Hasta pronto!");
    }
    
    private static void crearAdminSubmenu() {
        System.out.println("\n--- Crear Nueva Cuenta de Administrador ---");
        System.out.print("Nombre completo: ");
        String name = leer.nextLine().trim();
        
        System.out.print("Direccion: ");
        String address = leer.nextLine().trim();
        
        System.out.print("RFC: ");
        String rfc = leer.nextLine().trim();
        
        System.out.print("Telefono: ");
        String phone = leer.nextLine().trim();
        
        String username;
        while(true) {
            System.out.print("Nombre de usuario: ");
            username = leer.nextLine().trim();
            if(username.isEmpty()) {
                System.out.println("El nombre de usuario no puede estar vacio.");
                continue;
            }
            if(um.getAdminsMap().containsKey(username) || um.getClientsMap().containsKey(username)) {
                System.out.println("Ese nombre de usuario ya existe. Intente otro.");
            }
            else {
                break;
            }
        }
        
        System.out.print("Password: ");
        String password = leer.nextLine().trim();
        
        try {
            um.registerNewAdmin(name, address, rfc, phone, username, password);
            System.out.println("Cuenta de administrador creada exitosamente.");
        }
        catch(VendingException | IOException ex) {
            System.err.println("No fue posible crear cuenta de admin: " + ex.getMessage());
        }
    }
    
    private static void listarStockSubmenu(Admin admin) {
        System.out.println("\n--- Listar Stock ---");
        System.out.println("1) Listado completo");
        System.out.println("2) Listado por maquina");
        System.out.print("Seleccione una opcion: ");
        String opt = leer.nextLine().trim();
        switch(opt) {
            case "1":
                System.out.println("\n─── Stock completo ───");
                for(VendingMachine vm : machines.values()) {
                    System.out.printf("Maquina %s (Serie: %s | Ubicacion: %s)%n", vm.getMachineId(), vm.getSerialNumber(), vm.getLocation());
                    admin.listStock(vm);
                    System.out.println();
                }
                break;
            case "2":
                try {
                    VendingMachine vm = selectMachine();
                    System.out.printf("\n─── Stock de Maquina %s (%s) ───%n", vm.getMachineId(), vm.getLocation());
                    admin.listStock(vm);
                }
                catch (VendingException ve) {
                    System.err.printf("Error [%d]: %s%n", ve.getErrorCode(), ve.getMessage());
                }
                break;
            default:
                System.out.println("Opcion invalida en Listar Stock.");
        }
    }
    
    private static void buscarProducto(Admin admin) {
        System.out.println("\n--- Buscar Producto ---");
        System.out.print("Seleccione una maquina (1-4): ");
        String entrada = leer.nextLine().trim();
        
        int machineNum;
        try {
            machineNum = Integer.parseInt(entrada);
        }
        catch(NumberFormatException e) {
            System.err.println("Error: Debe ingresar un numero de maquina valido.");
            return;
        }
        
        VendingMachine vm = machines.get(machineNum);
        if(vm == null) {
            System.err.println("Error: La maquina \"" + machineNum + "\" no existe.");
            return;
        }
        
        System.out.print("Ingrese ID o nombre del producto: ");
        String termino = leer.nextLine().trim();
        
        Product encontrado = admin.searchProduct(vm, termino);
        if(encontrado != null) {
            System.out.printf("- %s (%s) - Stock: %d - Precio: $%.2f%n", encontrado.getName(), encontrado.getId(), encontrado.getStock(), encontrado.getPrice());
        }
        else {
            System.out.println("No se encontro ningun producto con ese termino.");
        }
    }
    
    private static void anadirStock(Admin admin) {
        System.out.println("\n--- Agregar Stock ---");
        System.out.print("Seleccione una maquina (1-4): ");
        String entrada = leer.nextLine().trim();
        
        int machineNum;
        try {
            machineNum = Integer.parseInt(entrada);
        }
        catch(NumberFormatException e) {
            System.err.println("Error: Debe ingresar un numero de maquina valido.");
            return;
        }
        
        VendingMachine vm = machines.get(machineNum);
        if(vm == null) {
            System.err.println("Error: La maquina \"" + machineNum + "\" no existe.");
            return;
        }
        
        while(true) {
            System.out.printf("\nStock actual de Maquina %s (Serie: %s | Ubicacion: %s):%n", vm.getMachineId(), vm.getSerialNumber(), vm.getLocation());
            for(Product p : vm.listProducts()) {
                System.out.printf("  - ID: %s | %s | Stock: %d%n", p.getId(), p.getName(), p.getStock());
            }
            
            System.out.print("\nID del producto a aumentar stock: ");
            String idProd = leer.nextLine().trim();
            Product seleccionado = vm.findProduct(idProd);
            if(seleccionado == null) {
                System.err.println("Error: Ese ID de producto no existe en la maquina.");
                continue;
            }
            
            System.out.print("Cantidad a agregar: ");
            String cantidadStr = leer.nextLine().trim();
            int cantidad;
            try {
                cantidad = Integer.parseInt(cantidadStr);
            }
            catch(NumberFormatException nfe) {
                System.err.println("Cantidad invalida. Debe ser un numero entero.");
                continue;
            }
            
            int maxAllowed = seleccionado.isBeverage() ? Product.MAX_STOCK_DRINKS : Product.MAX_STOCK_SNACKS;
            int currentStock = seleccionado.getStock();
            int spaceLeft = maxAllowed - currentStock;
            
            try {
                admin.addStock(vm, idProd, cantidad);
                Product actualizado = vm.findProduct(idProd);
                System.out.printf("Stock actualizado. Maquina %s - Producto %s ahora tiene %d unidades.%n", vm.getMachineId(), idProd, actualizado.getStock());
                break;
            }
            catch(VendingException ve) {
                if(ve.getErrorCode() == ErrorCode.INVALID_SELECTION.getCode()) {
                    System.err.printf("Error [%d]: Excedio el limite de stock para %s.%n" + "El limite maximo es %d, actualmente quedan %d espacios libres.%n", ve.getErrorCode(), seleccionado.getName(), maxAllowed, spaceLeft);
                }
                else {
                    System.err.printf("Error [%d]: %s%n", ve.getErrorCode(), ve.getMessage());
                    break;
                }
            }
        }
    }
    
    private static void reportesSubmenu(Admin admin) {
        System.out.println("\n--- Reportes ---");
        System.out.println("1) Ver ventas por producto");
        System.out.println("2) Ver total de ventas");
        System.out.print("Seleccione una opcion: ");
        String opt = leer.nextLine().trim();
        switch(opt) {
            case "1":
                simulateLoading("Generando reporte de ventas por producto...");
                try {
                    admin.reportVentasPorProducto(machines);
                }
                catch(IOException e) {
                    System.err.println("Error al generar reporte por producto: " + e.getMessage());
                }
                break;
            case "2":
                simulateLoading("Generando reporte de ventas totales del mes...");
                try {
                    admin.reportVentasTotalesMes(machines);
                }
                catch(IOException e) {
                    System.err.println("Error al genrar reporte total mensual: " + e.getMessage());
                }
                break;  
            default:
                System.out.println("Opcion invalida en Reportes.");
        }
    }
    
    private static void administrarDineroSubmenu(Admin admin) {
        while(true) {
            System.out.println("\n--- Administrar Dinero ---");
            System.out.println("1) Ver dinero total");
            System.out.println("2) Retirar dinero");
            System.out.println("3) Depositar dinero");
            System.out.println("4) Volver al menu anterior");
            System.out.print("Seleccione una opcion: ");
            String opt = leer.nextLine().trim();
            
            switch(opt) {
                case "1":
                    admin.viewAllMachinesMoney(machines);
                    break;
                case "2":
                    System.out.println("\n--- Retirar Dinero ---");
                    admin.viewAllMachinesMoney(machines);
                    System.out.print("Seleccione una maquina (1-4) para retirar: ");
                    String ent1 = leer.nextLine().trim();
                    int mNum1;
                    try {
                        mNum1 = Integer.parseInt(ent1);
                    }
                    catch(NumberFormatException e) {
                        System.err.println("Debe ingresar un numero de maquina valido.");
                        break;
                    }
                    VendingMachine vm1 = machines.get(mNum1);
                    if(vm1 == null) {
                        System.err.println("Maquina invalida.");
                        break;
                    }
                    double current1 = vm1.getMoneyBalance();
                    double maxAllowed = current1 - MoneyManager.MIN_BALANCE;
                    if(maxAllowed <= 0) {
                        System.out.println("No puede retirar dinero de esta maquina (saldo minimo).");
                        break;
                    }
                    System.out.printf("Balance actual: $%.2f | Maximo a retirar: $%.2f%n", current1, maxAllowed);
                    System.out.print("Cantidad a retirar: ");
                    String retiroStr = leer.nextLine().trim();
                    double retiro;
                    try {
                        retiro = Double.parseDouble(retiroStr);
                    }
                    catch (NumberFormatException e) {
                        System.err.println("Cantidad invalida.");
                        break;
                    }
                    try {
                        admin.withdrawMoneyFromMachine(vm1, retiro);
                        System.out.printf("Se retiro $%.2f de Maquina %s.%n", retiro, vm1.getMachineId());
                    }
                    catch(VendingException ve) {
                        System.err.println("Error al retirar: excede el maximo perimitido de $700 o invalido.");
                    }
                    break;
                case "3":
                    System.out.println("\n--- Depositar Dinero ---");
                    admin.viewAllMachinesMoney(machines);
                    System.out.print("Seleccione una Maquina (1-4) para depositar: ");
                    String ent2 = leer.nextLine().trim();
                    int mNum2;
                    try {
                        mNum2 = Integer.parseInt(ent2);
                    }
                    catch(NumberFormatException e) {
                        System.err.println("Debe ingresar un numero de maquina valido.");
                        break;
                    }
                    VendingMachine vm2 = machines.get(mNum2);
                    if(vm2 == null) {
                        System.err.println("Maquina invalida.");
                        break;
                    }
                    double current2 = vm2.getMoneyBalance();
                    System.out.printf("Balance actual: $%.2f | Deposito maximo: $%.2f%n", current2, MoneyManager.MAX_DEPOSIT_PER_OP);
                    System.out.print("Cantidad a depositar: ");
                    String depoStr = leer.nextLine().trim();
                    double deposito;
                    try {
                        deposito = Double.parseDouble(depoStr);
                    }
                    catch(NumberFormatException e) {
                        System.err.println("Cantidad invalida.");
                        break;
                    }
                    if(deposito > MoneyManager.MAX_DEPOSIT_PER_OP) {
                        System.err.printf("No puede depositar mas de $%.2f%n", MoneyManager.MAX_DEPOSIT_PER_OP);
                        break;
                    }
                    try {
                        admin.depositMoneyToMachine(vm2, deposito);
                        System.out.printf("Se deposito $%.2f en Maquina %s.%n", deposito, vm2.getMachineId());
                    }
                    catch(VendingException ve) {
                        System.err.println("Error al depositar: cantidad invalida.");
                    }
                    break;
                case "4":
                    return;
                default:
                    System.out.println("Opcion invalida. Ingrese un numero del 1 al 4.");
            }
        }
    }
    
    private static void clientFlow(Client client) {
        System.out.printf("Rob: Bienvenid@ %s. A continuacion se muestran las maquinas disponibles:%n", client.getName());
        
        try {
            displayMachinesMenu();
            VendingMachine vm = selectMachine();
            
            List<Selection> tempSelections = new ArrayList<>();
            double totalCost = 0.0;
            
            while(true) {
                System.out.printf("\nProductos en Maquina %s (%s):%n", vm.getMachineId(), vm.getLocation());
                vm.listProducts().forEach(p -> System.out.printf("%s - %s: $%.2f (Stock: %d)%n", p.getId(), p.getName(), p.getPrice(), p.getStock()));
                
                System.out.print("ID producto: ");
                String pid = leer.nextLine().trim();
                System.out.print("Cantidad: ");
                int qty;
                try {
                    qty = Integer.parseInt(leer.nextLine().trim());
                }
                catch(NumberFormatException e) {
                    System.err.println("Cantidad invalida. Debe ser numero entero.");
                    continue;
                }
                
                Product product = vm.findProduct(pid);
                if(product == null) {
                    System.err.println("Error [" + ErrorCode.INVALID_SELECTION.getCode() + "]: " + ErrorCode.INVALID_SELECTION.getMessage());
                    continue;
                }
                if(product.getStock() < qty) {
                    System.err.println("Error [" + ErrorCode.OUT_OF_STOCK.getCode() + "]: " + ErrorCode.OUT_OF_STOCK.getMessage());
                    continue;
                }
                
                tempSelections.add(new RoboticsVendingMachine.Selection(pid, product.getName(), qty, product.getPrice()));
                totalCost += product.getPrice() * qty;
                
                System.out.print("Desea agregar otro producto de esta maquina? (s/n): ");
                String resp = leer.nextLine().trim().toLowerCase();
                if(!(resp.equals("s") || resp.equals("si"))) {
                    break;
                }

            }
            
            System.out.printf("El costo total de todos los articulos es: $%.2f%n", totalCost);
            double accumulated = 0.0;
            
            while(accumulated < totalCost) {
                double remaining = totalCost - accumulated;
                System.out.printf("Inserte dinero. Faltan $%.2f (Maximo $1000): ", remaining);
                double inserted;
                try {
                    inserted = Double.parseDouble(leer.nextLine().trim());
                }
                catch(NumberFormatException e) {
                    System.err.println("Entrada invalida. Ingrese un numero.");
                    continue;
                }
                if(inserted < 0) {
                    System.err.println("No puede insertar cantidades negativas.");
                    continue;
                }
                if(inserted > 1000) {
                    System.err.println("No puede insertar mas de $1000.");
                    continue;
                }
                accumulated += inserted;
            }
            double currentBalance = vm.getMoneyBalance();
            MoneyManager.updateBalance(vm.getMachineId(), currentBalance + accumulated);
            
            List<VendingMachine.Selection> vmSelections = new ArrayList<>();
            for(Selection sel : tempSelections) {
                vmSelections.add(new VendingMachine.Selection(sel.pid, sel.name, sel.qty, sel.price));
            }
            double saleAmount = vm.processSale(vmSelections);
            
            double change = accumulated - totalCost;
            if(change > 0) {
                vm.dispenseChange(change);
            }
            
            simulateLoading("Procesando compra y despachando productos...");
            
            for(VendingMachine.Selection sel : vmSelections) {
                Transaction tx = VendingLog.createTransaction(vm.getMachineId(), sel.pid, sel.qty, sel.price * sel.qty);
                log.record(tx);
                double sub = sel.price * sel.qty;
                String ticketId = vm.getMachineId() + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                client.recordPurchase(ticketId, sub);
            }
            
            double prevTotal = client.getTotalConsumed();
            double newTotal = prevTotal + saleAmount;
            boolean rewarded = false;
            double leftover = newTotal - 1000.0;
            
            if(newTotal >= 1000.0) {
                System.out.println("\n========================================");
                System.out.printf("FELICIDADES, %s! Ha alcanzado $1000 en consumo.%n", client.getName());
                System.out.println("Ha gando un producto GRATIS.");
                System.out.println("========================================\n");
                
                List<Product> available = new ArrayList<>();
                for(Product p : vm.listProducts()) {
                    if(p.getStock() > 0) {
                        available.add(p);
                    }
                }
                if(!available.isEmpty()) {
                    Product freeProduct = available.get(RANDOM.nextInt(available.size()));
                    freeProduct.reduceStock(1);
                    System.out.printf("Sistema entrego gratis: %s (%s)%n%n", freeProduct.getName(), freeProduct.getId());
                    
                    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    try(PrintWriter pw = new PrintWriter(new FileWriter(FREE_REWARDS_FILE, true))) {
                        pw.printf("Fecha/Hora: %s | Cliente: %s (%s) | Maquina: %s | Producto Gratis: %s (%s)%n", timestamp, client.getName(), client.getId(), vm.getMachineId(), freeProduct.getName(), freeProduct.getId());
                    }
                    catch(IOException e) {
                        System.err.println("No se pudo registrar el producto gratis: " + e.getMessage());
                    }
                }
                else {
                    System.out.println("Lo sentimos, no hay productos disponibles para tu premio.");
                }
                
                client.setTotalConsumed(Math.max(0.0, leftover));
                rewarded = true;
            }
            else {
                client.setTotalConsumed(newTotal);
            }
            
            um.persistAllClients();
            
            String ticketFile = createDetailedTicket(client, vm, tempSelections, totalCost, accumulated, change);
            
            appendSaleToLog(client, vm, tempSelections, totalCost, accumulated, change);
            
            if(client.qualifiesForFreeProduct()) {
                client.grantFreeProduct(vm);
                System.out.println("Se ha ganado un producto gratis!");
            }
            
            System.out.println("Puede tomar sus productos");
            System.out.println("Presione Enter para continuar...");
            leer.nextLine();
            
            simulateLoading("Entregando cambio...");
            System.out.println("Porfavor, tome su cambio: $" + String.format("%.2f", change));
            System.out.println("Presione Enter para continuar...");
            leer.nextLine();
            
            simulateLoading("Generando ticket...");
            System.out.println("Porfavor, tome su ticket: " + ticketFile);
            openFile(ticketFile);
            System.out.println("Presione Enter para continuar...");
            leer.nextLine();
            
            System.out.printf("Venta realizada correctamente. Cambio: $%.2f.%n", change);
        }
        catch(VendingException vex) {
            System.err.printf("Error [%d]: %s%n", vex.getErrorCode(), vex.getMessage());
        }
        catch(IOException ex) {
            System.err.println("Error al generar ticket o log: " + ex.getMessage());
        }
    }
    
    private static String createDetailedTicket(Client client, VendingMachine vm, List<Selection> selections, double totalCost, double paid, double change) throws IOException {
        LocalDateTime now = LocalDateTime.now();
        String datePart = now.format(DateTimeFormatter.ofPattern("yyyy_MM_dd"));
        String timePart = now.format(DateTimeFormatter.ofPattern("HHmmss"));
        String ticketId = timePart;
        String filename = String.format("%s_%s_%s_ticket.txt", datePart, vm.getMachineId(), ticketId);
        
        try(PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.printf("=== Ticket de Venta ===%n");
            pw.printf("Fecha y Hora: %s%n", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            pw.printf("Cliente ID/Name: %s / %s%n", client.getId(), client.getName());
            pw.printf("Maquina ID: %s%n", vm.getMachineId());
            pw.printf("No.Serie: %s%n", vm.getSerialNumber());
            pw.printf("Ubicacion: %s%n", vm.getLocation());
            pw.println("-------------------------------");
            pw.println("Productos:");
            pw.printf("%-10s %-20s %-8s %-10s %-10s%n", "ID", "Nombre", "Cant.", "P.Unit.", "Subtotal");
            pw.println("---------------------------------------------------------------");
            
            for(Selection sel : selections) {
                double lineTotal = sel.price * sel.qty;
                pw.printf("%-10s %-20s %-8d $%-9.2f $%-9.2f%n", sel.pid, sel.name, sel.qty, sel.price, lineTotal);
            }
            pw.println("---------------------------------------------------------------");
            pw.printf("TOTAL COMPRA: $%.2f%n", totalCost);
            pw.printf("Efectivo ingresado: $%.2f%n", paid);
            pw.printf("Cambio: $%.2f%n", change);
            pw.println();
            
            double acumulado = client.getTotalConsumed();
            pw.printf("Total gastado acumulado: $%.2f%n", acumulado);
            double faltante = Math.max(0.0, 1000.0 - acumulado);
            pw.printf("Falta para producto gratis: $%.2f%n", faltante);
        }
        return filename;
    }
    
    private static void appendSaleToLog(Client client, VendingMachine vm, List<Selection> selections, double totalCost, double paid, double change) {
        LocalDateTime now = LocalDateTime.now();
        try(PrintWriter pw = new PrintWriter(new FileWriter(SALES_LOG_FILE, true))) {
            pw.printf("=== Venta Registrada ===%n");
            pw.printf("Fecha/Hora: %s%n", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            pw.printf("Cliente: %s / %s%n", client.getId(), client.getName());
            pw.printf("Maquina ID: %s%n", vm.getMachineId());
            pw.printf("No.Serie: %s%n", vm.getSerialNumber());
            pw.printf("Ubicacion: %s%n", vm.getLocation());
            pw.println("Productos vendidos:");
            for(Selection sel : selections) {
                double lineTotal = sel.price * sel.qty;
                pw.printf("  - %s (%s) x%d -> $%.2f%n", sel.name, sel.pid, sel.qty, lineTotal);
            }
            
            pw.printf("Total Compra: $%.2f%n", totalCost);
            pw.printf("Efectivo ingresado: $%.2f%n", paid);
            pw.printf("Cambio: $%.2f%n", change);
            pw.println();
        }
        catch(IOException e) {
            System.err.println("No se pudo registrar la venta en: " + e.getMessage());
        }
    }
    
    private static VendingMachine selectMachine() throws VendingException {
        System.out.print("Selecione maquina (1-4): ");
        int choice = Integer.parseInt(leer.nextLine());
        VendingMachine vm = machines.get(choice);
        if(vm == null) {
            throw new VendingException(ErrorCode.MACHINE_NOT_FOUND);
        }
        return vm;
    }
    
    private static void simulateLoading(String message) {
        System.out.println("\nPor favor, espere. " + message);
        try {
            Thread.sleep(3000);
        }
        catch(InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        System.out.println();
    }
    
    private static void openFile(String filename) {
        try {
            File file = new File(filename);
            if(Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
            else {
                System.out.println("Ticket guardado en: " + filename);
            }
        }
        catch(IOException e) {
            System.err.println("No se pudo abrir el archivo de ticket: " + e.getMessage());
        }
    }
}
