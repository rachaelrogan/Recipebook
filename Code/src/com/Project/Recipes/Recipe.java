package com.Project.Recipes;

import java.util.Arrays;

public class Recipe {
    private int recipeId;
    private String title;
    private String cuisine;
    private String type;
    private int servings;
    private int prepTimeMin;
    private String linkToRecipe;
    private Equipment[] equipment;
    private Ingredient mainIngredient;
    private Ingredient[] ingredients;

    public Recipe(int recipeId, String title,Ingredient mainIngredient, int servings, int prepTimeMin,
                  String cuisine, String type,   String linkToRecipe,
                   Ingredient[] ingredients, Equipment[] equipment) {
        this.recipeId = recipeId;
        this.title = title;
        this.cuisine = cuisine;
        this.type = type;
        this.servings = servings;
        this.prepTimeMin = prepTimeMin;
        this.linkToRecipe = linkToRecipe;
        this.mainIngredient = mainIngredient;
        this.ingredients = ingredients;
        this.equipment = equipment;
    }

    public int getRecipeId() {
        return recipeId;
    }

    public String getTitle() {
        return title;
    }

    public String getCuisine() {
        return cuisine;
    }

    public String getType() {
        return type;
    }

    public int getServings() {
        return servings;
    }

    public int getPrepTimeMin() {
        return prepTimeMin;
    }

    public Ingredient getMainIngredient() {
        return mainIngredient;
    }

    public Ingredient[] getIngredients() {
        return ingredients;
    }

    public Equipment[] getEquipment() {
        return equipment;
    }

    public String getLinkToRecipe() {
        return linkToRecipe;
    }

    @Override
    public String toString() {
        String mainString = "Recipe Id Number: " + recipeId + "\n" +
                "Main Ingredient: " + mainIngredient + "\n" +
                "Title: " + title + "\n" +
                "Cuisine: " + cuisine + "\n" +
                "Type: " + type + "\n" +
                "Servings: " + servings + "\n" +
                "Prep Time (min): " + prepTimeMin + "\n";
        if(!linkToRecipe.equals("")){
            mainString += "Link: " + linkToRecipe + "\n";
        }
        if(equipment.length != 0){
            for(Equipment ep : equipment){
                mainString += ep.toString() + "\n";
            }
        }
        for(Ingredient ingredient: ingredients){
            mainString += ingredient.toString() + '\n';
        }

        return mainString;
    }
}
