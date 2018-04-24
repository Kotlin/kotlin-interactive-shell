package sparklin.zeppelin;

import org.jetbrains.annotations.NotNull;
import sparklin.kshell.configuration.PropertyBasedConfiguration;

import java.util.Collections;
import java.util.Properties;

class InterpreterConfiguration extends PropertyBasedConfiguration {
    InterpreterConfiguration(@NotNull Properties props) {
        super(props, Collections.<String>emptyList());
    }
}
