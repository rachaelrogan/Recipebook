package com.Project.Recipes;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.*;
import java.util.*;

import static java.util.Map.entry;

public class DatabaseHelper {
    // Created by Robert Willhoft from his video "Using MySQL from Java"
    public static Connection getConnection(String db) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");

        String url = "jdbc:mysql://localhost:3306/" + db;

        Properties props= new Properties();
        props.setProperty("user", "CSUser");
        props.setProperty("password", "Weakpassword");
        return DriverManager.getConnection(url, props);
    }


    public static void printMenu(){
        System.out.println("Please select one of the following options:\n" +
                "A - Add a recipe\n" +
                "I - Import a CSV\n" +
                "R - Remove a recipe\n" +
                "C - Calendar\n" +
                "S - Search for recipes\n" +
                "Q - Show quick recipes\n" +
                "F - Facts about the recipes\n" +
                "X - Exit");
    }

    public static void importCSV(String fileName, Connection conn){
        // Ref: https://www.w3schools.com/java/java_files_read.asp
        try {
            File file = new File(fileName);
            Scanner fileReader = new Scanner(file);
            fileReader.nextLine(); // The first row is column names
            while (fileReader.hasNextLine()) {
                String data = fileReader.nextLine();
                addRecipe(data, conn);
            }
            fileReader.close();
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
    Given a line of data from a csv file, reads data. This will take out the quoted data (in the database case, this
    is the list of ingredients and equipment) and store it in its own array. The method outputs a 2D String array.
    The first element of this array is data that has been split on the commas. The second element is an array of the
    quoted characters that were removed from data.
    TODO: Assert that the "other ingredients" column in the data always has at least two items. *
     */
    public static String[][] getLists(String data){
        // Find all the indices of the quotes
        Vector<Integer> indicesOfQuotes = new Vector<Integer>();
        int lastQuoteIndex = 0;
        while(data.indexOf("\"", lastQuoteIndex) != -1){
            lastQuoteIndex = data.indexOf("\"", lastQuoteIndex) + 1;
            indicesOfQuotes.add(lastQuoteIndex - 1);
        }
        String[] manyItemsLine = new String[2];
        for(int i = 0, j = 0; i < indicesOfQuotes.size(); i += 2, j++){
            manyItemsLine[j] = data.substring(indicesOfQuotes.get(i), (indicesOfQuotes.get(i+1)+1));
            manyItemsLine[j] = manyItemsLine[j].substring(1, manyItemsLine[j].length()-1); // removes the quotes
            data = data.replace("\"" + manyItemsLine[j] + "\"", " ");
            for(int a = 0; a < indicesOfQuotes.size(); a++){
                indicesOfQuotes.set(a, indicesOfQuotes.get(a) - manyItemsLine[j].length() - 1);
            }
        }
        String[] separatedData = data.split(",");
        String[][] out = {separatedData, manyItemsLine};
        return out;
    }

    public static String getRecipeInfoFromUser(Scanner in){
        String userInput;
        String recipe = "";
        String[] addRecipeQuestions = {"What's the title of the new recipe?",
                "What is the main ingredient of the new recipe?",
                "What is the cuisine of the new recipe (Mexican, Asian, etc.) (If there is none, press enter)?",
                "What is the type of the new recipe (Main, Side, Dessert, etc.)?",
                "How many servings does the new recipe serve? (Press enter for unknown.)",
                "How long (in minutes) does the new recipe take to make? (Press enter for unknown.)",
                "Please enter the link to the recipe (If there is none, press enter).",
                "Please enter the list of ingredients, separated by commas (example: Peppers, Onions, Cheese).",
                "Please enter the list of equipment, separated by commas (example: Stove, Frying Pan)."
        };
        for(int i = 0; i < addRecipeQuestions.length; i++){
            System.out.println(addRecipeQuestions[i]);
            userInput = in.nextLine();
            if(i > addRecipeQuestions.length - 3){
                recipe += "\"" + userInput + "\",";
            }
            else{
                recipe += userInput + ",";
            }
        }
        recipe = recipe.substring(0, recipe.length()-1); // delete the trailing ,
        return recipe;
    }

    /*
    Given a line from the data, call the proper functions to execute the SQL commands.
     */
    public static void addRecipe(String data, Connection conn){
        String[][] lists = getLists(data);
        String[] independentElements = lists[0];
        String[] listedElements = lists[1];

        // Add the Main Ingredient, Cuisine, and Type to their respective tables
        addElement(conn, "Ingredients", "IngredientName", independentElements[1],
                new String[]{"IngredientName", "InStock"}, new Object[]{independentElements[1], false});
        addElement(conn, "Cuisines", "CuisineName", independentElements[2],
                new String[]{"CuisineName"}, new Object[]{independentElements[2]});
        addElement(conn, "Types", "TypeName", independentElements[3], new String[]{"TypeName"},
                new String[]{independentElements[3]});

        // Add the ingredients and equipment to their respective tables

        // Get the keys of the Main Ingredient, Cuisine, and Type to use to insert into Recipe
        int mainIngredientId = getKey(conn, "IngredientID", "Ingredients",
                "IngredientName",independentElements[1]);
        int cuisineId = getKey(conn, "CuisineID", "Cuisines",
                "CuisineName", independentElements[2]);
        int typeId = getKey(conn, "TypeID", "Types", "TypeName", independentElements[3]);

        // Determine if we have any null or empty values for any of the columns for RecipeInfo
        Vector<String> columnsToFill = new Vector<String>(Arrays.asList(new String[]{"Title", "MainIngredient", "CuisineID", "TypeID"}));
        Vector<Object> valuesForColumns = new Vector<Object>(Arrays.asList(new Object[]{independentElements[0], mainIngredientId, cuisineId, typeId}));
        if(independentElements[4].equals("") == false){
            columnsToFill.add("Servings");
            valuesForColumns.add(independentElements[4]);
        }
        if(independentElements[5].equals("") == false){
            columnsToFill.add("PrepTimeMin");
            valuesForColumns.add(independentElements[5]);
        }
        if(independentElements[6].equals("") == false){
            columnsToFill.add("LinkToRecipe");
            valuesForColumns.add(independentElements[6]);
        }
        String[] columnsToFillArray = columnsToFill.toArray(new String[columnsToFill.size()]);
        Object[] valuesForColumnsArray = valuesForColumns.toArray(new Object[valuesForColumns.size()]);

        // Add the elements to the RecipeInfo
        addElement(conn, "RecipeInfo", "Title", independentElements[0],
                columnsToFillArray, valuesForColumnsArray);

        // NOTE: We assume that there will always be at least two other ingredients (thus we assume that listedElements
        // has two entries)
        // Check if there is only one additional ingredient (which would be in independentElements)
        String[] ingredients = listedElements[0].split(",");
        for(String ingredient : ingredients){
            addElement(conn, "Ingredients", "IngredientName", ingredient,
                    new String[]{"IngredientName, InStock"}, new Object[]{ingredient, false});
            Object[] valuesToInsert = new Object[]{getKey(conn, "RecipeID", "RecipeInfo",
                    "Title", independentElements[0]), getKey(conn, "IngredientID",
                    "Ingredients", "IngredientName", ingredient)};
            addElement(conn, "RecipeIngredients", new String[]{"RecipeID, IngredientID"}, valuesToInsert);
        }

        // Add equipment to Equipment table

        if(independentElements[8].isBlank()){ // if there is empty spot where there should be ingredients
            if(!listedElements[1].isBlank()){ // if there are entries in the ingredients part of listedElements
                for(String equipment : listedElements[1].split(",")){
                    addElement(conn, "Equipment", "Name", equipment,
                            new String[]{"Name"}, new Object[]{equipment});
                    Object[] valuesToInsert = new Object[]{getKey(conn, "RecipeID", "RecipeInfo",
                            "Title", independentElements[0]), getKey(conn, "EquipmentID",
                            "Equipment", "Name", equipment)};
                    addElement(conn, "RecipeEquipment", new String[]{"RecipeID, EquipmentID"}, valuesToInsert);
                }
            }
        }
        else{
            addElement(conn, "Equipment", "Name", independentElements[8],
                    new String[]{"Name"}, new Object[]{independentElements[8]});
            Object[] valuesToInsert = new Object[]{getKey(conn, "RecipeID", "RecipeInfo",
                    "Title", independentElements[0]), getKey(conn, "EquipmentID",
                    "Equipment", "Name", independentElements[8])};
            addElement(conn, "RecipeEquipment", new String[]{"RecipeID, EquipmentID"}, valuesToInsert);
        }
    }

    /*
    conn - the Connection to the database we are using
    tableName - name of the table into which we are inserting value(s)
    columnCheck - name of the column we will use to see if there is an existing element in the table
    columnCheckValue - the value we will check for to see if the element already exists
    columns - the columns into which we are inserting our values
    valuesToInsert - the values we will insert into the columns of the table
    NOTE: columns and valuesToInsert must be entered in the same order to ensure that the values are inserted into the
    proper column

    addElement(conn, "Ingredients", "IngredientName", independentElements[1],
                new String[]{"IngredientName", "InStock"}, new Object[]{independentElements[1], false});
     */
    public static void addElement(Connection conn, String tableName, String columnCheck, String columnCheckValue,
                                  String[] columns, Object[] valuesToInsert){
        boolean inDB = inDatabase(conn, tableName, columnCheck, columnCheckValue);

        if(inDB == false){
            addElement(conn, tableName, columns, valuesToInsert);
        }
        else {
            System.out.println("Element already exists in the database.");
        }
    }

    /*
    Given a connection, input table, columns, and values to insert, this method inserts the given values into the table.
    conn - the Connection to the database we are using
    tableName - name of the table into which we are inserting value(s)
    columns - the columns into which we are inserting our values
    valuesToInsert - the values we will insert into the columns of the table
    NOTE: columns and valuesToInsert must be entered in the same order to ensure that the values are inserted into the
    proper column
     */
    public static void addElement(Connection conn, String tableName, String[] columns, Object[] valuesToInsert){
        String insertAtColumns = "(";
        for(int i = 0; i < columns.length; i++){
            insertAtColumns += columns[i].strip();
            if(i == columns.length - 1){
                insertAtColumns += ")";
            }
            else{
                insertAtColumns += ",";
            }
        }
        //valuesToInsert
        String insertValues = "(";
        for(int i = 0; i < valuesToInsert.length; i++){
            if(valuesToInsert[i] instanceof Integer){
                Integer insert = (Integer) valuesToInsert[i];
                insertValues += insert;
            }
            else if(valuesToInsert[i] instanceof Float) {
                Float insert = (Float) valuesToInsert[i];
                insertValues += insert;
            }
            else if(valuesToInsert[i] instanceof Boolean){
                Boolean insert = (Boolean) valuesToInsert[i];
                insertValues += insert;
            }
            else{
                insertValues += "\"" + ((String) valuesToInsert[i]).strip() + "\"";
            }
            if(i == valuesToInsert.length - 1){
                insertValues += ")";
            }
            else{
                insertValues += ",";
            }
        }
        try{
            Statement statement = conn.createStatement();
            statement.execute(String.format("INSERT INTO %1$s %2$s " + "VALUES %3$s;", tableName, insertAtColumns, insertValues));
            statement.close();
            System.out.println(String.format("Element added to %s!", tableName));
        }
        catch(SQLException e){
            System.out.println("ERROR " + e.getMessage());
        }

    }

    /*
    Removes a row from the specified table with the specified information.
    conn - the Connection through which we are executing our deletion
    tableName - the name of the table from which we are removing an element
    identifierColumn - the name of the column we are basing our deletion on
    identifierValue - the value in the value that must be present in identifierColumn for the deletion to take place
     */
    public static boolean removeElement(Connection conn, String tableName, String identifierColumn, Object identifierValue){
        boolean rowFound = false;
        if(identifierValue instanceof String){
            identifierValue = "\"" + ((String) identifierValue).strip() + "\"";
        }
        try{
            Statement statement = conn.createStatement();
            ResultSet result = statement.executeQuery(String.format("SELECT * FROM %1$s WHERE %2$s = %3$s",
                    tableName, identifierColumn, identifierValue));
            rowFound = result.next();
        }
        catch(SQLException e){
            System.out.println(e.getMessage());
        }
        if(rowFound){
            try{
                Statement statement = conn.createStatement();
                int result = statement.executeUpdate(String.format("DELETE FROM %1$s WHERE %2$s = %3$s",
                        tableName, identifierColumn, identifierValue));
            }
            catch(SQLException e){
                System.out.println(e.getMessage());
            }
        }
        return rowFound;
    }

    /*
    Looks in the database to see if the input exists;
    conn - the Connection to the database we are using
    tableName - name of the table into which we are inserting value(s)
    columnCheck - name of the column we will use to see if there is an existing element in the table
    columnCheckValue - the value we will check for to see if the element already exists
     */
    public static boolean inDatabase(Connection conn, String tableName,  String columnCheck, Object columnCheckValue){
        boolean result = false;
        if(columnCheckValue instanceof String){
            columnCheckValue = "\"" + ((String) columnCheckValue).strip()  + "\"";
        }
        try{
            Statement statement = conn.createStatement();
            ResultSet results = statement.executeQuery(
                    String.format("SELECT * FROM %1$s WHERE %2$s = %3$s;", tableName, columnCheck, columnCheckValue));
            result = results.next();
            statement.close();
        }
        catch(SQLException e){
            System.out.println(e.getMessage());
        }
        return result;
    }
    /*
    Returns a key given:
    conn - the Connection through which to execute the statement
    idName - the name of the column that stores the key
    tableName - the name of the table from which we are querying
    identifierColumn - the name of the column we are using in our query to find the row of the key we are
        searching for (ie TypeName)
    identifier - the value in identifierColumn ("Dessert"); we are looking for the key where identifier is the value in
        identifierColumn
     */
    public static int getKey(Connection conn, String idName, String tableName,
                             String identifierColumn, Object identifier){
        int key = -1;
        String identifierString = "";
        if(identifier instanceof String){
            identifierString = "\"" + ((String) identifier).strip() + "\"";
        }
        try{
            Statement statement = conn.createStatement();
            ResultSet result = statement.executeQuery(String.format("SELECT %1$s FROM %2$s WHERE %3$s = %4$s",
                    idName, tableName, identifierColumn, identifierString));
            if(result.next()){
                key = result.getInt(idName);
            }
        }
        catch(SQLException e){
            System.out.println(String.format("Issue finding a key: %s", idName));
        }
        return key;
    }

    /*
    Searches for rows in a table.
    columnSearch and columnValue are what will be used in the WHERE clause; if you don't want to search this way, both
    these values must be ""
     */
    public static ResultSet findRows(Connection conn, String tableName, String[] columnSearch, Object[] columnSearchValues,
                                     String[] orderBy, String[] orderBySetting) throws SQLException{
        Vector<String[]> results = new Vector<String[]>();
        String queryString = "SELECT * FROM " + tableName;
        String whereClause = "";
        String orderClause = "";

        if(columnSearch.length != 0){
            whereClause = " WHERE ";
            for(int i = 0; i < columnSearch.length; i++){
                whereClause += columnSearch[i] + " = ";
                if(columnSearchValues[i] instanceof String){
                    columnSearchValues[i] = "\"" + ((String) columnSearchValues[i]).strip() + "\"";
                }
                whereClause += columnSearchValues[i] + " AND ";
            }
            int removeLast = whereClause.lastIndexOf(" AND ");
            whereClause = whereClause.substring(0, removeLast);
        }
        if(orderBy.length != 0){
            orderClause = " ORDER BY ";
            for(int i = 0; i < orderBy.length; i++){
                orderClause += orderBy[i] + " " + orderBySetting[i] + ",";
            }
            int removeLast = orderClause.lastIndexOf(",");
            orderClause = orderClause.substring(0, removeLast);
        }
        queryString += whereClause + " " + orderClause + ";";
        Statement statement = conn.createStatement();
//        System.out.println(queryString);
        ResultSet resultSet = statement.executeQuery(queryString);
        return resultSet;
    }

    /*
    Performs a natural join (and possibly a theta join, if needed) and selects from this joined table.
    Map:
        - Integer - 0 if you want to do a natural join with this table; 1 if you want to do a theta join with the table
        - String[] - the first element should be the name of the table you want joined; the second element should be the
                     the column you want the theta join to be on (if you are doing a thetajoin)
        - columnSearch - for the WHERE clause; the columns we are looking at
        - columnSearchValues - for the WHERE clause; the values that should be in the columns of columnSearch
        NOTE: columnSearch and columnSearchValues should be in the same order
     */
    public static ResultSet searchJoinedTables(Connection conn, Map<String, Integer> tablesToJoin,
                                               String[] thetaJoinColumns, String[] columnSearch,
                                               Object[] columnSearchValues, String[] orderBy,
                                               String[] orderBySettings) throws SQLException{
        String joinString = createJoinedTableString(tablesToJoin, thetaJoinColumns);
        ResultSet results = findRows(conn, joinString, columnSearch, columnSearchValues,orderBy, orderBySettings);
        return results;
    }

    /*
    Map:
        - Integer - 0 if you want to do a natural join with this table; 1 if you want to do a theta join with the table
        - String - the first element should be the name of the table you want joined
                     - NOTE: there can only be one table that we do a theta join on
    columnsToJoin - should list the comparisons that need to take place for the theta join in the order in which they
                    would appear in a SQL statement (should include the table name when appropriate
     */
    public static String createJoinedTableString(Map<String, Integer> tablesToJoin, String[] columnsToJoin){
        String naturalJoinString = "(";
        String thetaJoinString = " JOIN ";
        String joinedTableString = "";
        boolean thetaJoin = false;
        // https://www.geeksforgeeks.org/iterate-map-java/#:~:text=Iterating%20over%20Map.&text=entrySet()%20method%20returns%20a,getValue()%20methods%20of%20Map.
        for (Map.Entry<String,Integer> entry : tablesToJoin.entrySet()){
            if(entry.getValue() == 0){ //do a natural join
                naturalJoinString += entry.getKey()+ " NATURAL JOIN ";
            }
            else{
                thetaJoinString += entry.getKey() + " ON ";
                thetaJoin = true;
            }
        }
        int removeLast = naturalJoinString.lastIndexOf(" NATURAL JOIN ");
        naturalJoinString = naturalJoinString.substring(0, removeLast);
        naturalJoinString += ")";
        if(thetaJoin){
            joinedTableString += naturalJoinString + thetaJoinString;
        }
        else{
            joinedTableString += naturalJoinString;
        }
        for(int i = 0; i < columnsToJoin.length; i+=2){
            joinedTableString += String.format("%1$s = %2$s", columnsToJoin[i], columnsToJoin[i+1]);
        }
        return joinedTableString;
    }

    //Given a ResultSet, returns a Vector<String[]> that contains the results
    public static Vector<String> getRowResults(ResultSet resultSet, String[] columns) throws SQLException{
        Vector<String> results = new Vector<String>();
        int numCols = columns.length;
        do {
            String rowInfo = "";
            for (int i = 0; i < numCols; i++) {
                rowInfo += resultSet.getString(columns[i]) + ", ";
            }
            int removeLast = rowInfo.lastIndexOf(", ");
            rowInfo = rowInfo.substring(0, removeLast);
            results.add(rowInfo);
        }while (resultSet.next());
        return results;
    }

    //Given a Vector<String[]> of results, prints all rows
    public static void printRows(Vector<String> rows){
        Iterator it = rows.iterator();
        while(it.hasNext()){
            System.out.println((String) it.next());
        }
    }

    //Given a Vector<String[]> of results, prints numRows rows
    public static void printRows(Vector<String> rows, int numRows){
        Iterator it = rows.iterator();
        int rowsPrinted = 1;
        while(it.hasNext() && rowsPrinted <= numRows){
            System.out.println((String) it.next());
            rowsPrinted++;
        }
    }

    public static void printRow(Vector<String> rows){
        Iterator it = rows.iterator();
        if(it.hasNext()){
            System.out.println((String) it.next());
        }
    }

    public static void printAllRecipe(Connection c, String ID, char idType){
        ResultSet results;
        String[] columnSearch = new String[1];
        switch (idType){
            case 'R':
                columnSearch[0] = "RecipeID";
                break;
            case 'I':
                columnSearch[0] = "IngredientID";
                break;
            case 'T':
                columnSearch[0] = "TypeID";
                break;
            case 'C':
                columnSearch[0] = "CuisineID";
                break;
            default:
                break;
        }
        try{
            results = DatabaseHelper.searchJoinedTables(c, Map.ofEntries(entry("RecipeInfo", 0),
                    entry("Cuisines", 0), entry("Types", 0), entry("Ingredients", 1)),
                    new String[]{"MainIngredient", "Ingredients.IngredientID"}, columnSearch,
                    new String[]{ID}, new String[]{}, new String[]{});
            if(results.next()) {
                Vector<String> result = DatabaseHelper.getRowResults(results,
                        new String[]{"RecipeID", "Title", "IngredientName", "Servings", "PrepTimeMin",
                                "CuisineName", "TypeName", "LinkToRecipe"});
                String[] recipeIds = getIds(result, 0);
                Iterator it = result.iterator();
                System.out.println("Recipe ID Number, Title, Main Ingredient, Servings, Prep Time (min), Cuisine, Type, Link");
                Integer recipeIdIterator = recipeIds.length;
                while(it.hasNext()) {
                    System.out.println(it.next());
                    System.out.println("Ingredients Needed:");
                    results = DatabaseHelper.searchJoinedTables(c,
                            Map.ofEntries(entry("RecipeIngredients", 0), entry("Ingredients", 0)),
                            new String[]{}, new String[]{"RecipeID"}, new String[]{recipeIdIterator.toString()}, new String[]{},
                            new String[]{});
                    results.next();
                    result = DatabaseHelper.getRowResults(results,
                            new String[]{"IngredientName", "inStock"});
                    DatabaseHelper.printRows(result);
                    System.out.println("Equipment Needed:");
                    results = DatabaseHelper.searchJoinedTables(c,
                            Map.ofEntries(entry("RecipeEquipment", 0), entry("Equipment", 0)),
                            new String[]{}, new String[]{"RecipeID"}, new String[]{recipeIdIterator.toString()}, new String[]{},
                            new String[]{});
                    results.next();
                    result = DatabaseHelper.getRowResults(results,
                            new String[]{"Name"});
                    DatabaseHelper.printRows(result);
                    System.out.println();
                    recipeIdIterator++;
                }
            }

        }
        catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    public static void printRecipes(ArrayList<Recipe> recipes){
        Iterator it = recipes.iterator();
        while(it.hasNext()){
            Recipe recipe = (Recipe) it.next();
            System.out.println(recipe.toString());
        }
    }

    // Given a vector of results from a query and the index at which the ids are contained, returns an array of ids.
    public static String[] getIds(Vector<String> queryResults, int indexOfId){
        String[] ids = new String[queryResults.size()];
        Iterator it = queryResults.iterator();
        int i = 0;
        String[] splitRow;
        while(it.hasNext()){
            splitRow = ((String) it.next()).split(",");
            ids[i] = splitRow[indexOfId].strip();
            i++;
        }
        return ids;
    }

    // TODO: Put all recipe info in Recipe object and then put this through the printRows method
    public static ArrayList<Recipe> getAllResults(Connection c, String ID, char idType){
        ResultSet results;
        String[] columnSearch = new String[1];
        ArrayList<Recipe> recipeArr = new ArrayList<>();
        switch (idType){
            case 'R':
                columnSearch[0] = "RecipeID";
                break;
            case 'I':
                columnSearch[0] = "IngredientID";
                break;
            case 'T':
                columnSearch[0] = "TypeID";
                break;
            case 'C':
                columnSearch[0] = "CuisineID";
                break;
            default:
                break;
        }
        try{
            results = DatabaseHelper.searchJoinedTables(c, Map.ofEntries(entry("RecipeInfo", 0),
                    entry("Cuisines", 0), entry("Types", 0), entry("Ingredients", 1)),
                    new String[]{"MainIngredient", "Ingredients.IngredientID"}, columnSearch,
                    new String[]{ID}, new String[]{}, new String[]{});
            if(results.next()) {
                Vector<String> recipeResult = DatabaseHelper.getRowResults(results,
                        new String[]{"RecipeID", "Title", "IngredientName", "Servings", "PrepTimeMin",
                                "CuisineName", "TypeName", "LinkToRecipe"});
                String[] recipeIds = getIds(recipeResult, 0);
                Iterator recipeIt = recipeResult.iterator();

                // Recipes
                Integer recipeIdIterator = recipeIds.length;
                String[] recipeInfoCleaned = new String[8];
                while(recipeIt.hasNext()) {
                    String[] recipeInfo = ((String) recipeIt.next()).split(",");
                    System.out.println("recipeInfo.length: " + recipeInfo.length);
                    for(int i = 0; i < recipeInfo.length;i++){
                        System.out.println("RAW recipeInfo[" + i + "]" + recipeInfo[i]);
//                        recipeInfo[i] = recipeInfo[i].strip();
                        if(recipeInfo[i].strip().equals("null")){
                            System.out.println("IS NULL");
                            if(i == 3 || i == 4){
                                System.out.println("INSIDE IF");
                                recipeInfoCleaned[i] = "0";
                            }
                            else{
                                recipeInfoCleaned[i] = " ";
                            }
                        }
                        else{
                            recipeInfoCleaned[i] = recipeInfo[i].strip();
                        }
                        System.out.println("recipeInfoCleaned[" + i + "]" + recipeInfoCleaned[i]);
                    }
                    ResultSet mainIngredientResult = findRows(c, "Ingredients", new String[]{"IngredientName"},
                            new String[]{recipeInfo[2]}, new String[]{}, new String[]{});
                    mainIngredientResult.next();
                    Vector<String> mainIngredientInfo = getRowResults(mainIngredientResult,
                            new String[]{"IngredientName", "inStock"});
                    Iterator mainIngredientIt = mainIngredientInfo.iterator();
                    String[] mainIngredientSplit = ((String) mainIngredientIt.next()).split(",");
                    Ingredient mainIngredient = new Ingredient(mainIngredientSplit[0],
                            Boolean.parseBoolean(mainIngredientSplit[1]));
                    // Ingredients
                    results = DatabaseHelper.searchJoinedTables(c,
                            Map.ofEntries(entry("RecipeIngredients", 0), entry("Ingredients", 0)),
                            new String[]{}, new String[]{"RecipeID"}, new String[]{recipeInfoCleaned[0]}, new String[]{},
                            new String[]{});
                    results.next();
                    Vector<String> ingredientsResult = DatabaseHelper.getRowResults(results,
                            new String[]{"IngredientName", "inStock"});
                    Iterator ingredientIt = ingredientsResult.iterator();
                    Ingredient[] ingredients = new Ingredient[ingredientsResult.size()];
                    int ingredientIndex = 0;
                    while(ingredientIt.hasNext()){
                        String[] ingredientInfo = ((String) ingredientIt.next()).split(",");
                        ingredients[ingredientIndex] = new Ingredient(ingredientInfo[0], Boolean.parseBoolean(ingredientInfo[1]));
                        ingredientIndex++;
                    }

                    //Equipment
                    results = DatabaseHelper.searchJoinedTables(c,
                            Map.ofEntries(entry("RecipeEquipment", 0), entry("Equipment", 0)),
                            new String[]{}, new String[]{"RecipeID"}, new String[]{recipeInfoCleaned[0]}, new String[]{},
                            new String[]{});
                    Equipment[] equipmentNeeded;
                    if(results.next()){
                        Vector<String> equipmentResults = DatabaseHelper.getRowResults(results,
                                new String[]{"Name"});
                        Iterator equipmentIt = equipmentResults.iterator();
                        equipmentNeeded = new Equipment[equipmentResults.size()];
                        int equipmentIndex = 0;
                        while(equipmentIt.hasNext()){
                            equipmentNeeded[equipmentIndex] = new Equipment((String) equipmentIt.next());
                            equipmentIndex++;
                        }
                    }
                    equipmentNeeded = new Equipment[]{new Equipment("None")};

                    Recipe newRecipe = new Recipe(Integer.parseInt(recipeInfoCleaned[0]), recipeInfoCleaned[1],
                            mainIngredient, Integer.parseInt(recipeInfoCleaned[3]), Integer.parseInt(recipeInfoCleaned[4]),
                            recipeInfoCleaned[5], recipeInfoCleaned[6], recipeInfoCleaned[7], ingredients, equipmentNeeded);
                    recipeArr.add(newRecipe);
                    recipeIdIterator++;
                }
            }

        }
        catch (SQLException e){
            System.out.println(e.getMessage());
        }
        return recipeArr;
    }

    public static void XMLOption(Scanner in, ArrayList<Recipe> recipes){
        System.out.println("Would you like to export these results to XML? (Y/N)");
        boolean keepAsking = true;
        String userInput;
        while(keepAsking){
            userInput = in.nextLine();
            switch (userInput){
                case "Y":
                    System.out.println("Yay! Let's export to XML. What would you like your brand file to be called?");
                    keepAsking = false;
                    userInput = in.nextLine();
                    try{
                        XMLGenerator xmlG = new XMLGenerator();
                        xmlG.generateXML(recipes, userInput);
                    }
                    catch(ParserConfigurationException e){
                        System.out.println(e.getMessage());
                    }

                    break;
                case "N":
                    System.out.println("Okay no worries!");
                    keepAsking = false;
                    break;
                default:
                    System.out.println("That's not a valid input. Please enter Y for yes and N for no.");
            }
        }
    }

    public static void printFacts(Connection c){
        // Show how many recipes, ingredients, cuisines
        try{
            String[][] queries = {{"COUNT(*)", "RecipeInfo"}, {"COUNT(*)", "Types"},
                    {"COUNT(*)", "Cuisines"} };
            String[] printing = {"Number of recipes; ","Number of types: ", "Number of Cuisines: "};
            int[] results = new int[printing.length];
            for(String[] query : queries){
                for(int i = 0; i < query.length; i++){
                    ResultSet rs = useAggregator(c, new String[]{query[0]}, query[1], new String[]{},
                            new Object[]{}, new String[]{}, new String[]{}, new String[]{});
                    rs.next();
                    results[i] = rs.getInt(0);
                }
            }
            for(int i = 0; i < printing.length; i++){
                System.out.println(printing[i] + results[i]);
            }

        }
        catch (SQLException e){
            System.out.println(e.getMessage());
        }

    }

    public static ResultSet useAggregator(Connection conn, String[] selections, String tableName, String[] columnSearch,
                                     Object[] columnSearchValues, String[] orderBy, String[] orderBySetting,
                                     String[] groupBy) throws SQLException{
        Vector<String[]> results = new Vector<String[]>();
        String queryString = "SELECT ";
        String selectionClause = "";
        String whereClause = "";
        String groupClause = "";
        String orderClause = "";
        //SELECT
        for(int i = 0; i < selections.length; i++){
            selectionClause += selections[i] + ", ";
        }
        int removeLast = selectionClause.lastIndexOf(", ");
        selectionClause = selectionClause.substring(0, removeLast);
        //FROM
        String fromClause = " FROM " + tableName;
        //WHERE
        if(columnSearch.length != 0){
            whereClause = " WHERE ";
            for(int i = 0; i < columnSearch.length; i++){
                whereClause += columnSearch[i] + " = ";
                if(columnSearchValues[i] instanceof String){
                    columnSearchValues[i] = "\"" + ((String) columnSearchValues[i]).strip() + "\"";
                }
                whereClause += columnSearchValues[i] + " AND ";
            }
            removeLast = whereClause.lastIndexOf(" AND ");
            whereClause = whereClause.substring(0, removeLast);
        }
        //GROUP BY
        if(groupBy.length > 0){
            groupClause = " GROUP BY ";
            for(int i = 0; i < groupBy.length; i++){
                groupClause += groupBy[i] + ",";
            }
            removeLast = groupClause.lastIndexOf(",");
            groupClause = groupClause.substring(0, removeLast);
        }

        //ORDER BY
        if(orderBy.length != 0){
            orderClause = " ORDER BY ";
            for(int i = 0; i < orderBy.length; i++){
                orderClause += orderBy[i] + " " + orderBySetting[i] + ",";
            }
            removeLast = orderClause.lastIndexOf(",");
            orderClause = orderClause.substring(0, removeLast);
        }

        queryString += selectionClause +  " " + fromClause + " " + whereClause + " "  + groupClause +
                " " + orderClause + ";";
        System.out.println(queryString);
        Statement statement = conn.createStatement();
        ResultSet resultSet = statement.executeQuery(queryString);
        return resultSet;
    }
}

