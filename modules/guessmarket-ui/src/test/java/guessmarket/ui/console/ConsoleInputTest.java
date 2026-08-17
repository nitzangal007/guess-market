package guessmarket.ui.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Scanner;
import org.junit.jupiter.api.Test;

class ConsoleInputTest {

    @Test
    void menuChoiceTrimsInputRetriesLocallyAndUsesTheApprovedPrompt() throws Exception {
        StringWriter text = new StringWriter();
        ConsoleInput input = input("  \nletters\n2147483648\n0\n9\n 4 \n", text);

        assertEquals(4, input.readMenuChoice());
        assertEquals("""
                Choose a command [1-8]:
                Invalid menu choice. Enter a number from 1 to 8.
                Choose a command [1-8]:
                Invalid menu choice. Enter a number from 1 to 8.
                Choose a command [1-8]:
                Invalid menu choice. Enter a number from 1 to 8.
                Choose a command [1-8]:
                Invalid menu choice. Enter a number from 1 to 8.
                Choose a command [1-8]:
                Invalid menu choice. Enter a number from 1 to 8.
                Choose a command [1-8]:
                """, text.toString());
    }

    @Test
    void eventSelectionUsesCurrentRangeAndLeavesOtherPromptsUntouched() throws Exception {
        StringWriter text = new StringWriter();
        ConsoleInput input = input("0\n4\n2\n", text);

        assertEquals(2, input.readEventSelection(3));
        assertEquals("""
                Choose an event [1-3]:
                Invalid selection. Enter a number from 1 to 3.
                Choose an event [1-3]:
                Invalid selection. Enter a number from 1 to 3.
                Choose an event [1-3]:
                """, text.toString());
    }

    @Test
    void purchaseAndWinningOptionsUseTheirDistinctD072Prompts() throws Exception {
        StringWriter purchaseText = new StringWriter();
        ConsoleInput purchaseInput = input("3\n1\n", purchaseText);
        assertEquals(1, purchaseInput.readPurchaseOption());
        assertEquals("""
                Choose an option [1-2]:
                Invalid option. Enter 1 or 2.
                Choose an option [1-2]:
                """, purchaseText.toString());

        StringWriter winnerText = new StringWriter();
        ConsoleInput winnerInput = input("0\n2\n", winnerText);
        assertEquals(2, winnerInput.readWinningOption());
        assertEquals("""
                Choose the winning option [1-2]:
                Invalid option. Enter 1 or 2.
                Choose the winning option [1-2]:
                """, winnerText.toString());
    }

    @Test
    void quantityAndPathsRetryAtTheirOwnPromptsAndPreserveInternalSpaces() throws Exception {
        StringWriter quantityText = new StringWriter();
        ConsoleInput quantityInput = input("-1\n0\n2147483648\n 12 \n", quantityText);
        assertEquals(12, quantityInput.readPositiveQuantity());
        assertEquals("""
                Enter shares to purchase (positive whole number):
                Invalid quantity. Enter a positive whole number.
                Enter shares to purchase (positive whole number):
                Invalid quantity. Enter a positive whole number.
                Enter shares to purchase (positive whole number):
                Invalid quantity. Enter a positive whole number.
                Enter shares to purchase (positive whole number):
                """, quantityText.toString());

        StringWriter pathText = new StringWriter();
        ConsoleInput pathInput = input("   \n C:\\events with spaces\\market.xml \n", pathText);
        assertEquals(Path.of("C:\\events with spaces\\market.xml"), pathInput.readXmlPath());
        assertEquals("""
                Enter the full path to the XML file:
                Invalid path. Enter a valid full path.
                Enter the full path to the XML file:
                """, pathText.toString());
    }

    @Test
    void saveAndRestorePathsUseTheirExactSeparatePrompts() throws Exception {
        StringWriter saveText = new StringWriter();
        assertEquals(Path.of("C:\\state files\\market"), input("C:\\state files\\market\n", saveText).readSavePath());
        assertEquals("Enter the full path and file name to save, without an extension:\n", saveText.toString());

        StringWriter restoreText = new StringWriter();
        assertEquals(Path.of("C:\\state files\\market"), input("C:\\state files\\market\n", restoreText).readRestorePath());
        assertEquals("Enter the full path and file name to restore, without an extension:\n", restoreText.toString());
    }

    @Test
    void endOfInputIsACheckedSignalAtAnyPrompt() {
        assertThrows(ConsoleInput.EndOfInputException.class, () -> input("", new StringWriter()).readMenuChoice());
        assertThrows(ConsoleInput.EndOfInputException.class, () -> input("", new StringWriter()).readEventSelection(1));
        assertThrows(ConsoleInput.EndOfInputException.class, () -> input("", new StringWriter()).readPurchaseOption());
        assertThrows(ConsoleInput.EndOfInputException.class, () -> input("", new StringWriter()).readWinningOption());
        assertThrows(ConsoleInput.EndOfInputException.class, () -> input("", new StringWriter()).readPositiveQuantity());
        assertThrows(ConsoleInput.EndOfInputException.class, () -> input("", new StringWriter()).readXmlPath());
    }

    private static ConsoleInput input(String script, StringWriter text) {
        return new ConsoleInput(new Scanner(script), new PrintWriter(text, true));
    }
}
