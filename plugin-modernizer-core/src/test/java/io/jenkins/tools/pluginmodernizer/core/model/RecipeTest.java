package io.jenkins.tools.pluginmodernizer.core.model;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class RecipeTest {

    @Test
    public void testRequiresCompilationWithNoTags() {
        Recipe recipe = new Recipe();
        recipe.setTags(null);
        assertTrue(recipe.requiresCompilation(), "Should require compilation if tags are null");
    }

    @Test
    public void testRequiresCompilationWithNoCompileTag() {
        Recipe recipe = new Recipe();
        recipe.setTags(Set.of("no-compile"));
        assertFalse(recipe.requiresCompilation(), "Should not require compilation if 'no-compile' tag is present");
    }

    @Test
    public void testRequiresCompilationWithOtherTags() {
        Recipe recipe = new Recipe();
        recipe.setTags(Set.of("foo", "bar"));
        assertTrue(recipe.requiresCompilation(), "Should require compilation if 'no-compile' tag is absent");
    }
}
