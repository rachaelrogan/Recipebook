package com.Project.Recipes;

public class Ingredient {
    private String name;
    private boolean inStock;

    public Ingredient(String n, boolean iS){
        name = n;
        inStock = iS;
    }

    public String getName() {
        return name;
    }

    public boolean isInStock() {
        return inStock;
    }

    @Override
    public String toString() {
        String inStockStr = "";
        if(inStock){
            inStockStr = " (In Stock)";
        }
        else{
            inStockStr = " (Not in Stock)";
        }
        return  name + inStockStr;
    }
}
