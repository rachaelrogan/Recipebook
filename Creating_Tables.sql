DROP DATABASE RecipeBook;
CREATE DATABASE RecipeBook;
USE RecipeBook;
/*
DROP TABLE Cuisines;
DROP TABLE Types;
DROP TABLE Ingredients;
DROP TABLE RecipeInfo;
DROP TABLE RecipeIngredients;
DROP TABLE Equipment;
DROP TABLE RecipeEquipment;
DROP TABLE Calendar;
*/
CREATE TABLE Cuisines(
	CuisineID INT PRIMARY KEY AUTO_INCREMENT, 
    CuisineName VARCHAR(20)
);

CREATE TABLE Types(
	TypeID INT PRIMARY KEY AUTO_INCREMENT,
    TypeName VARCHAR(15) NOT NULL
);

CREATE TABLE Ingredients(
	IngredientID INT PRIMARY KEY AUTO_INCREMENT,
    IngredientName VARCHAR(35) NOT NULL,
    InStock BOOLEAN
);


CREATE TABLE RecipeInfo (
    RecipeID INT PRIMARY KEY AUTO_INCREMENT,
    Title	VARCHAR(40) NOT NULL,
    MainIngredient INT NOT NULL,
    Servings INT, 
    PrepTimeMin INT, 
    CuisineID INT NOT NULL,
    TypeID INT NOT NULL,
    LinkToRecipe VARCHAR(200),
    FOREIGN KEY (MainIngredient) REFERENCES Ingredients(IngredientID) 
		ON DELETE CASCADE,
    FOREIGN KEY (CuisineID) REFERENCES Cuisines(CuisineID) 
		ON DELETE CASCADE
        ON UPDATE CASCADE,
    FOREIGN KEY (TypeID) REFERENCES Types(TypeID)
		ON DELETE CASCADE,
	CHECK (Servings > 0),
    CHECK (PrepTimeMin > 0)
);

CREATE TABLE RecipeIngredients(
	RecipeID INT NOT NULL,
    IngredientID INT NOT NULL, 
    FOREIGN KEY (RecipeID) REFERENCES RecipeInfo(RecipeID) ON DELETE CASCADE,
    FOREIGN KEY (IngredientID) REFERENCES Ingredients(IngredientID) ON DELETE CASCADE
);

CREATE TABLE Equipment(
	EquipmentID INT PRIMARY KEY AUTO_INCREMENT,
    NAME VARCHAR(30)
);

CREATE TABLE RecipeEquipment(
	RecipeID INT NOT NULL,
    EquipmentID INT NOT NULL, 
    FOREIGN KEY (RecipeID) REFERENCES RecipeInfo(RecipeID) ON DELETE CASCADE,
    FOREIGN KEY (EquipmentID) REFERENCES Equipment(EquipmentID) ON DELETE CASCADE
);

CREATE TABLE Calendar(
	LastEaten DATE NOT NULL,
    RecipeID INT NOT NULL,
    FOREIGN KEY (RecipeID) REFERENCES RecipeInfo(RecipeID) ON DELETE CASCADE
);

CREATE VIEW QuickMeals AS
SELECT Title FROM RecipeInfo WHERE PrepTimeMin <= 30;

