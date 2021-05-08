// Generation of the XML file for this project is VERY closely based
// on Dr. Willhoft's "Read & Write XML" video for CSCI 340 at Houghton College.

package com.Project.Recipes;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import javax.imageio.IIOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class XMLGenerator {
    private DocumentBuilder builder;
    private Document doc;

    public XMLGenerator() throws ParserConfigurationException{
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        builder = factory.newDocumentBuilder();
    }

    // Builds a DOM document for an array of recipes.
    public Document build(ArrayList<Recipe> recipes){
        doc = builder.newDocument();
        doc.appendChild(createRecipes(recipes));
        return doc;
    }

    public void generateXML(ArrayList<Recipe> recipes, String fileName){
        Document doc = build(recipes);
        DOMImplementation impl = doc.getImplementation();
        DOMImplementationLS implLS = (DOMImplementationLS) impl.getFeature("LS", "3.0");
        LSSerializer ser = implLS.createLSSerializer();
        String out = ser.writeToString(doc);
        System.out.println("out: " + out);
//        try{
//            PrintWriter output = new PrintWriter(
//                    new BufferedWriter(
//                            new FileWriter(
//                                    new File(fileName))));
//            File file = new File(fileName);
//            FileWriter output = new FileWriter(String.valueOf(new OutputStreamWriter(new FileOutputStream(fileName), "UTF-16")));
////            FileWriter output = new FileWriter(fileName);
//            output.write(out);
//            output.close();

            try (FileWriter fw = new FileWriter(new File(fileName), StandardCharsets.UTF_16);
                 BufferedWriter writer = new BufferedWriter(fw)) {

                writer.append(out);

            } catch (IOException e) {
                e.printStackTrace();
            }
//        }
//        catch(IOException e){
//            System.out.println(e.getMessage());
//        }
    }

    private Element createRecipes(ArrayList<Recipe> recipes){
        Element e = doc.createElement("recipes");

        for(Recipe recipe : recipes){
            e.appendChild(createRecipe(recipe));
        }
        return e;
    }

    private Element createRecipe(Recipe recipe){
        Element e = doc.createElement("recipe");

        e.appendChild(createTextElement("title", recipe.getTitle()));
        e.appendChild(createTextElement("cuisine", recipe.getCuisine()));
        e.appendChild(createTextElement("type", recipe.getType()));
        e.appendChild(createTextElement("servings", "" + recipe.getServings()));
        e.appendChild(createTextElement("prepTimeMin", "" + recipe.getPrepTimeMin()));
        e.appendChild(createTextElement("linkToRecipe", recipe.getLinkToRecipe()));
        e.appendChild(createMainIngredient(recipe.getMainIngredient()));
        e.appendChild(createIngredients(recipe.getIngredients()));
        e.appendChild(createEquipmentList(recipe.getEquipment()));

        return e;
    }

    public Element createIngredients(Ingredient[] ingredients){
        Element e = doc.createElement("ingredients");

        for(Ingredient ingredient : ingredients){
            e.appendChild(createIngredient(ingredient));
        }

        return e;
    }

    public Element createIngredient(Ingredient ingredient){
        Element e = doc.createElement("ingredient");

        e.appendChild(createTextElement("name", ingredient.getName()));
        e.appendChild(createTextElement("inStock", "" + ingredient.isInStock()));

        return e;
    }

    public Element createMainIngredient(Ingredient ingredient){
        Element e = doc.createElement("mainIngredient");

        e.appendChild(createTextElement("name", ingredient.getName()));
        e.appendChild(createTextElement("inStock", "" + ingredient.isInStock()));
        return e;
    }

    public Element createEquipmentList(Equipment[] equipmentList){
        Element e = doc.createElement("EquipmentNeeded");

        for(Equipment eq : equipmentList){
            e.appendChild(createEquipment(eq));
        }

        return e;

    }

    public Element createEquipment(Equipment equipment){
        Element e = doc.createElement("equipment");

        e.appendChild(createTextElement("name", equipment.getName()));

        return e;
    }

    private Element createTextElement(String name, String text){
        Text t = doc.createTextNode(text);
        Element e = doc.createElement(name);
        e.appendChild(t);
        return e;
    }

}
