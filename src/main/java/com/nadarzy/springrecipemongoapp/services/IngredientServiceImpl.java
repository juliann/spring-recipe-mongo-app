package com.nadarzy.springrecipemongoapp.services;

import com.nadarzy.springrecipemongoapp.commands.IngredientCommand;
import com.nadarzy.springrecipemongoapp.converters.IngredientCommandToIngredient;
import com.nadarzy.springrecipemongoapp.converters.IngredientToIngredientCommand;
import com.nadarzy.springrecipemongoapp.model.Ingredient;
import com.nadarzy.springrecipemongoapp.model.Recipe;
import com.nadarzy.springrecipemongoapp.repositories.RecipeRepository;
import com.nadarzy.springrecipemongoapp.repositories.UnitOfMeasureRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
public class IngredientServiceImpl implements IngredientService {

  private final IngredientToIngredientCommand ingredientToIngredientCommand;
  private final IngredientCommandToIngredient ingredientCommandToIngredient;
  private final RecipeRepository recipeRepository;
  private final UnitOfMeasureRepository unitOfMeasureRepository;

  public IngredientServiceImpl(
      IngredientToIngredientCommand ingredientToIngredientCommand,
      IngredientCommandToIngredient ingredientCommandToIngredient,
      RecipeRepository recipeRepository,
      UnitOfMeasureRepository unitOfMeasureRepository) {
    this.ingredientToIngredientCommand = ingredientToIngredientCommand;
    this.ingredientCommandToIngredient = ingredientCommandToIngredient;
    this.recipeRepository = recipeRepository;
    this.unitOfMeasureRepository = unitOfMeasureRepository;
  }

  @Override
  public IngredientCommand findByRecipeIdAndIngredientId(String recipeId, String ingredientId) {
    Optional<Recipe> recipeOptional = recipeRepository.findById(recipeId);
    if (recipeOptional.isEmpty()) {
      log.error("recipe id not found " + recipeId);
    }
    Recipe recipe = recipeOptional.get();

    Optional<IngredientCommand> optionalIngredientCommand =
        recipe.getIngredients().stream()
            .filter(ingredient1 -> ingredient1.getId().equals(ingredientId))
            .map(ingredientToIngredientCommand::convert)
            .findFirst();
    if (!optionalIngredientCommand.isPresent()) {
      log.error("ingr id not found" + ingredientId);
    }
    return optionalIngredientCommand.get();
  }

  @Override
  @Transactional
  public IngredientCommand saveIngredientCommand(IngredientCommand command) {
    Optional<Recipe> recipeOptional = recipeRepository.findById(command.getRecipeId());

    if (!recipeOptional.isPresent()) {

      // todo toss error if not found!
      log.error("Recipe not found for id: " + command.getRecipeId());
      return new IngredientCommand();
    } else {
      Recipe recipe = recipeOptional.get();

      Optional<Ingredient> ingredientOptional =
          recipe.getIngredients().stream()
              .filter(ingredient -> ingredient.getId().equals(command.getId()))
              .findFirst();

      if (ingredientOptional.isPresent()) {
        Ingredient ingredientFound = ingredientOptional.get();
        ingredientFound.setDescription(command.getDescription());
        ingredientFound.setAmount(command.getAmount());
        ingredientFound.setUom(
            unitOfMeasureRepository
                .findById(command.getUom().getId())
                .orElseThrow(() -> new RuntimeException("UOM NOT FOUND"))); // todo address this
      } else {
        // add new Ingredient
        Ingredient ingredient = ingredientCommandToIngredient.convert(command);
        ingredient.setRecipe(recipe);
        recipe.addIngredient(ingredient);
      }

      Recipe savedRecipe = recipeRepository.save(recipe);

      Optional<Ingredient> savedIngredientOptional =
          savedRecipe.getIngredients().stream()
              .filter(recipeIngredients -> recipeIngredients.getId().equals(command.getId()))
              .findFirst();

      // check by description
      if (!savedIngredientOptional.isPresent()) {
        // not totally safe... But best guess
        savedIngredientOptional =
            savedRecipe.getIngredients().stream()
                .filter(
                    recipeIngredients ->
                        recipeIngredients.getDescription().equals(command.getDescription()))
                .filter(
                    recipeIngredients -> recipeIngredients.getAmount().equals(command.getAmount()))
                .filter(
                    recipeIngredients ->
                        recipeIngredients.getUom().getId().equals(command.getUom().getId()))
                .findFirst();
      }

      // to do check for fail
      return ingredientToIngredientCommand.convert(savedIngredientOptional.get());
    }
  }

  @Override
  public void deleteIngredientById(String recipeId, String ingredientId) {
    log.debug("deleting ingr id" + ingredientId);

    Optional<Recipe> recipeOptional = recipeRepository.findById(recipeId);
    if (recipeOptional.isPresent()) {
      Recipe recipe = recipeOptional.get();
      log.debug("found recipe");

      Optional<Ingredient> ingredientOptional =
          recipe.getIngredients().stream()
              .filter(ingredient -> ingredient.getId().equals(ingredientId))
              .findFirst();
      if (ingredientOptional.isPresent()) {
        log.debug("found ingredient");
        Ingredient ingredientToDelete = ingredientOptional.get();
        ingredientToDelete.setRecipe(null);
        recipe.getIngredients().remove(ingredientToDelete);
        recipeRepository.save(recipe);
      }
    } else {
      log.debug("recipe id not found" + recipeId);
    }
  }
}