/*
Autor: David Solorzano Argueta
Proyecto: Maquina Expendedora (En Consola) 
*/

package com.mycompany.roboticsvendingmachine;

import java.util.ArrayList;
import java.util.List;

public class ProductFactory {
    public static List<Product> getProductsForMachine(String machineId) throws VendingException {
        List<Product> products = new ArrayList<>();
        
        products.add(new Product(machineId, "A01", "Cheetos", 16.50, 12, false));
        products.add(new Product(machineId, "A02", "Doritos Nachos", 16.50, 5, false));
        products.add(new Product(machineId, "A03", "Sabritas", 16.50, 0, false));
        products.add(new Product(machineId, "A04", "Sabritas Adobadas", 16.50, 3, false));
        products.add(new Product(machineId, "B01", "Ruffles Queso", 20.00, 15, false));
        products.add(new Product(machineId, "B02", "Takis", 18.00, 5, false));
        products.add(new Product(machineId, "B03", "Emperador Chocolate", 17.50, 5, false));
        products.add(new Product(machineId, "B04", "Emperador Limon", 17.50, 10, false));
        products.add(new Product(machineId, "C01", "Trident 18's", 18.50, 6, false));
        products.add(new Product(machineId, "C02", "Halls Miel", 7.50, 15, false));
        products.add(new Product(machineId, "C03", "Halls Colores", 16.50, 9, false));
        products.add(new Product(machineId, "C04", "Chokis", 14.50, 0, false));
        products.add(new Product(machineId, "D01", "Panditas", 12.50, 1, false));
        
        products.add(new Product(machineId, "D02", "Manzanita Sol 355ml", 17.50, 9, true));
        products.add(new Product(machineId, "D03", "Mirinda 355ml", 17.50, 3, true));
        products.add(new Product(machineId, "D04", "Coca Cola 355ml", 19.00, 8, true));
        
        return products;
    }
}
