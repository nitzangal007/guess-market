package guessmarket.ui.console;

import guessmarket.engine.GuessMarketEngine;
import guessmarket.engine.GuessMarketEngineImpl;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public final class ConsoleMain {
    private ConsoleMain() {
    }

    public static void main(String[] args) {
        GuessMarketEngine engine = new GuessMarketEngineImpl();
        Scanner scanner = new Scanner(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        PrintWriter output = new PrintWriter(
                new OutputStreamWriter(System.out, StandardCharsets.UTF_8),
                true);
        ConsoleInput input = new ConsoleInput(scanner, output);
        ConsoleRenderer renderer = new ConsoleRenderer(output);
        GuessMarketConsoleApp app = new GuessMarketConsoleApp(engine, input, renderer);
        app.run();
    }
}
