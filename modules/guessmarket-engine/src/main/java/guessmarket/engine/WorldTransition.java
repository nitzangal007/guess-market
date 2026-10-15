package guessmarket.engine;
import java.util.List;
record WorldTransition(MarketWorld world,List<AccountCashEffect> accountEffects) {
    WorldTransition { accountEffects=List.copyOf(accountEffects); }
}
