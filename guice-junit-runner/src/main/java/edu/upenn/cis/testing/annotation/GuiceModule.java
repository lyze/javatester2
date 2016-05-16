package edu.upenn.cis.testing.annotation;

import com.google.inject.Module;

import java.lang.annotation.*;

/**
 * @author davix
 */
@Repeatable(GuiceModules.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GuiceModule {
    Class<? extends Module> value();
}
