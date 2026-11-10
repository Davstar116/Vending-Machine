/*
Autor: David Solorzano Argueta
Proyecto: Maquina Expendedora (En Consola) 
*/

package com.mycompany.roboticsvendingmachine;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserManager {
    private final Map<String, Client> clients = new HashMap<>();
    private final Map<String, Admin> admins = new HashMap<>();
    
    private static final String CLIENTS_FILE = "clients.txt";
    private static final String ADMINS_FILE = "admins.txt";
    
    public UserManager() throws IOException {
        loadAdmins();
        loadClients();
    }
    
    public Map<String, Client> getClientsMap() {
        return clients;
    }
    
    public Map<String, Admin> getAdminsMap() {
        return admins;
    }
    
    private void loadAdmins() throws IOException {
        File file = new File(ADMINS_FILE);
        if(!file.exists()) {
            file.createNewFile();
            return;
        }
        
        try(BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if(parts.length < 7) continue;
                String id = parts[0].trim();
                String name = parts[1].trim();
                String address = parts[2].trim();
                String rfc = parts[3].trim();
                String phone = parts[4].trim();
                String username = parts[5].trim();
                String password = parts[6].trim();
                Admin admin = new Admin(id, name, address, rfc, phone, username, password);
                admins.put(username, admin);
            }
        }
    }
    
    private void loadClients() throws IOException {
        File file =  new File(CLIENTS_FILE);
        if(!file.exists()) {
            file.createNewFile();
            return;
        }
        
        try(BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if(parts.length < 7) continue;
                String id = parts[0].trim();
                String name = parts[1].trim();
                String username = parts[2].trim();
                String password = parts[3].trim();
                String email = parts[4].trim();
                String phone = parts[5].trim();
                double totalConsumed = 0.0;
                
                if(parts.length >= 7) {
                    try{
                        totalConsumed = Double.parseDouble(parts[6].trim());
                    }
                    catch(NumberFormatException e) {
                        totalConsumed = 0.0;
                    }
                }
                
                Client client = new Client(id, name, username, password, email, phone);
                client.setTotalConsumed(totalConsumed);
                clients.put(username, client);
            }
        }
    }
    
    public void registerNewClient(String name, String username, String password, String email, String phone) throws IOException, VendingException {
        if(clients.containsKey(username) || admins.containsKey(username)) {
            throw new VendingException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        
        String newId = "C" + String.format("%03d", clients.size() + 1);
        Client client = new Client(newId, name, username, password, email, phone);
        clients.put(username, client);
        
        appendClientToFile(client);
    }
    
    private void appendClientToFile(Client client) throws IOException {
        try(FileWriter fw = new FileWriter(CLIENTS_FILE, true); PrintWriter pw = new PrintWriter(fw)) {
            pw.printf("%s,%s,%s,%s,%s,%s,%.2f%n", client.getId(), client.getName(), client.getUsername(), client.getPassword(), client.getEmail(), client.getPhone(), client.getTotalConsumed());
        }
    }
    
    public User authenticate(String username, String password) throws VendingException {
        if(admins.containsKey(username)) {
            Admin admin = admins.get(username);
            if(admin.getPassword().equals(password)) {
                return admin;
            }
            else {
                throw new VendingException(ErrorCode.INVALID_CREDENTIALS);
            }
        }
        
        if(clients.containsKey(username)) {
            Client client = clients.get(username);
            if(client.getPassword().equals(password)) {
                return client;
            }
            else {
                throw new VendingException(ErrorCode.INVALID_CREDENTIALS);
            }
        }
        throw new VendingException(ErrorCode.INVALID_CREDENTIALS);
    }
    
    public void persistAllClients() throws IOException {
        try(PrintWriter pw = new PrintWriter(new FileWriter(CLIENTS_FILE))) {
            for(Client client : clients.values()) {
                pw.printf("%s,%s,%s,%s,%s,%s,%.2f%n", client.getId(), client.getName(), client.getUsername(), client.getPassword(), client.getEmail(), client.getPhone(), client.getTotalConsumed());
            }
        }
        catch(IOException e) {
            System.err.println("Error al persistir clientes: " + e.getMessage());
        }
    }
    
    public void persistAllAdmins() throws IOException {
        try(PrintWriter pw = new PrintWriter(new FileWriter(ADMINS_FILE))) {
            for(Admin a : admins.values()) {
                pw.printf("%s,%s,%s,%s,%s,%s,%s%n", a.getId(), a.getName(), a.getAddress(), a.getRfc(), a.getPhone(), a.getUsername(), a.getPassword());
            }
        }
    }
    
    public void registerNewAdmin(String name, String address, String rfc, String phone, String username, String password) throws IOException, VendingException {
        if(admins.containsKey(username) || clients.containsKey(username)) {
            throw new VendingException(ErrorCode.INVALID_SELECTION);
        }
        String id = "A" + System.currentTimeMillis();
        Admin a = new Admin(id, name, address, rfc, phone, username, password);
        admins.put(username, a);
        persistAllAdmins();
    }
}
