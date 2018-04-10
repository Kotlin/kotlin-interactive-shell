package sparklin.zeppelin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sparklin.kshell.configuration.Configuration;
import sparklin.kshell.configuration.PropertyBasedConfiguration;
import sparklin.kshell.console.Completer;
import sparklin.kshell.console.ConsoleReader;

import java.util.Collections;
import java.util.Properties;

class InterpreterConfiguration extends PropertyBasedConfiguration {

    InterpreterConfiguration(@NotNull Properties props) {
        super(props, Collections.<String>emptyList());
    }

    @NotNull
    @Override
    public ConsoleReader getConsoleReader() {
        return CONSOLE;
    }

    private static final ConsoleReader CONSOLE = new DummyConsoleReader();

    static class DummyConsoleReader implements ConsoleReader {
        public void init(@NotNull Configuration config) {

        }

        public void addCompleter(@NotNull Completer completer) {

        }

        public void setPrompt(@NotNull String prompt) {

        }

        public void dropHistory(int n) {

        }

        public void addHistoryItem(@NotNull String item) {

        }

        @Nullable
        public String readLine() {
            return null;
        }

        public void println(@NotNull String s) {
            System.out.println(s);
        }

        public void cleanUp() {

        }
    }
}
