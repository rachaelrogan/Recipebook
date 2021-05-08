package com.Project.Recipes;

public class Equipment {
    private String name;

    public Equipment(String n){
        name = n;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
