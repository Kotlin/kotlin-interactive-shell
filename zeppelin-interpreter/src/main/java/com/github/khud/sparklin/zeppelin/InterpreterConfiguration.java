package com.github.khud.sparklin.zeppelin;

import org.jetbrains.annotations.NotNull;
import com.github.khud.sparklin.kshell.configuration.PropertyBasedConfiguration;

import java.util.Collections;
import java.util.Properties;

class InterpreterConfiguration extends PropertyBasedConfiguration {
    InterpreterConfiguration(@NotNull Properties props) {
        super(props, Collections.<String>emptyList());
    }
}
