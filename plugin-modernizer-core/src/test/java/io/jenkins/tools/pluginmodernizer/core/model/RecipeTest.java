package io.jenkins.tools.pluginmodernizer.core.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

public class RecipeTest {

    @Test
    public void testRequiresBuildWithNoTags() {
        Recipe recipe = new Recipe();
        recipe.setTags(null);
        assertTrue(recipe.requiresBuild(), "Should require build if tags are null");
    }

    @Test
    public void testRequiresBuildWithNoCompileTag() {
        Recipe recipe = new Recipe();
        recipe.setTags(Set.of("no-compile"));
        assertFalse(recipe.requiresBuild(), "Should not require build if 'no-compile' tag is present");
    }

    @Test
    public void testRequiresBuildWithOtherTags() {
        Recipe recipe = new Recipe();
        recipe.setTags(Set.of("foo", "bar"));
        assertTrue(recipe.requiresBuild(), "Should require build if 'no-compile' tag is absent");
    }
}
