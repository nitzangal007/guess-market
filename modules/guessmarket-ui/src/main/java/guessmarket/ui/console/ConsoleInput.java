package guessmarket.ui.console;

import java.io.PrintWriter;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Scanner;

final class ConsoleInput {
    private static final String MENU_PROMPT = "Choose a command [1-8]:";
    private static final String PURCHASE_OPTION_PROMPT = "Choose an option [1-2]:";
    private static final String WINNING_OPTION_PROMPT = "Choose the winning option [1-2]:";
    private static final String QUANTITY_PROMPT = "Enter shares to purchase (positive whole number):";
    private static final String XML_PATH_PROMPT = "Enter the full path to the XML file:";
    private static final String SAVE_PATH_PROMPT = "Enter the full path and file name to save, without an extension:";
    private static final String RESTORE_PATH_PROMPT = "Enter the full path and file name to restore, without an extension:";

    private final Scanner scanner;
    private final PrintWriter output;

    ConsoleInput(Scanner scanner, PrintWriter output) {
        this.scanner = Objects.requireNonNull(scanner, "scanner");
        this.output = Objects.requireNonNull(output, "output");
    }

    int readMenuChoice() throws EndOfInputException {
        return readBoundedInteger(
                MENU_PROMPT,
                8,
                "Invalid menu choice. Enter a number from 1 to 8.");
    }

    int readEventSelection(int eventCount) throws EndOfInputException {
        if (eventCount < 1) {
            throw new IllegalArgumentException("Event count must be positive");
        }
        return readBoundedInteger(
                "Choose an event [1-" + eventCount + "]:",
                eventCount,
                "Invalid selection. Enter a number from 1 to " + eventCount + ".");
    }

    int readPurchaseOption() throws EndOfInputException {
        return readBoundedInteger(PURCHASE_OPTION_PROMPT, 2, "Invalid option. Enter 1 or 2.");
    }

    int readWinningOption() throws EndOfInputException {
        return readBoundedInteger(WINNING_OPTION_PROMPT, 2, "Invalid option. Enter 1 or 2.");
    }

    int readPositiveQuantity() throws EndOfInputException {
        return readBoundedInteger(QUANTITY_PROMPT, Integer.MAX_VALUE, "Invalid quantity. Enter a positive whole number.");
    }

    Path readXmlPath() throws EndOfInputException {
        return readPath(XML_PATH_PROMPT);
    }

    Path readSavePath() throws EndOfInputException {
        return readPath(SAVE_PATH_PROMPT);
    }

    Path readRestorePath() throws EndOfInputException {
        return readPath(RESTORE_PATH_PROMPT);
    }

    private int readBoundedInteger(String prompt, int maximum, String invalidMessage)
            throws EndOfInputException {
        while (true) {
            writeLine(prompt);
            String line = readLine();
            try {
                int value = Integer.parseInt(line.trim());
                if (value >= 1 && value <= maximum) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // The prompt-local message below handles blank, non-numeric, and overflowing values alike.
            }
            writeLine(invalidMessage);
        }
    }

    private Path readPath(String prompt) throws EndOfInputException {
        while (true) {
            writeLine(prompt);
            String text = readLine().trim();
            if (!text.isEmpty()) {
                try {
                    return Path.of(text);
                } catch (InvalidPathException ignored) {
                    // Keep invalid path text local to this prompt.
                }
            }
            writeLine("Invalid path. Enter a valid full path.");
        }
    }

    private String readLine() throws EndOfInputException {
        if (!scanner.hasNextLine()) {
            throw new EndOfInputException();
        }
        return scanner.nextLine();
    }

    private void writeLine(String text) {
        output.print(text);
        output.print('\n');
        output.flush();
    }

    static final class EndOfInputException extends Exception {
        private static final long serialVersionUID = 1L;
    }
}
