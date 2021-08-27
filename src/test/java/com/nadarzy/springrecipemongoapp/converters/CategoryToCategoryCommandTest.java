package com.nadarzy.springrecipemongoapp.converters;

import com.nadarzy.springrecipemongoapp.commands.CategoryCommand;
import com.nadarzy.springrecipemongoapp.model.Category;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CategoryToCategoryCommandTest {

  public static final String ID_VALUE = "1";
  public static final String DESCRIPTION = "descript";
  CategoryToCategoryCommand converter;

  @BeforeEach
  public void setUp() throws Exception {
    converter = new CategoryToCategoryCommand();
  }

  @Test
  public void testNullObject() throws Exception {
    Assertions.assertNull(converter.convert(null));
  }

  @Test
  public void testEmptyObject() throws Exception {
    Assertions.assertNotNull(converter.convert(new Category()));
  }

  @Test
  public void convert() throws Exception {
    // given
    Category category = new Category();
    category.setId(ID_VALUE);
    category.setDescription(DESCRIPTION);

    // when
    CategoryCommand categoryCommand = converter.convert(category);

    // then
    Assertions.assertEquals(ID_VALUE, categoryCommand.getId());
    Assertions.assertEquals(DESCRIPTION, categoryCommand.getDescription());
  }
}
