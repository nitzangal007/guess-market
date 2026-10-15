package guessmarket.engine;
import java.util.List;
record AccountCashEffect(String username,double storedBefore,double storedAfter,List<CashComponent> components) {
    AccountCashEffect { components=List.copyOf(components); }
}
