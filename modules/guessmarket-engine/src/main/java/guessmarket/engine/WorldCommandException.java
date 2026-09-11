package guessmarket.engine;

/** Expected EX2 command rejection, kept separate from legacy E1 error contracts. */
public final class WorldCommandException extends Exception {
    private static final long serialVersionUID = 1L;
    public enum Code {
        USER_NOT_FOUND, EVENT_NOT_FOUND, NOT_OWNER, WRONG_METHOD,
        WRONG_STATUS, INSUFFICIENT_FUNDS, STALE_WORLD,
        USER_BLOCKED, INVALID_OPTION, INVALID_QUANTITY, FINANCIAL_CALCULATION_FAILED
    }
    private final Code code;
    public WorldCommandException(Code code, String detail) {
        super(detail);
        this.code = code;
    }
    public Code getCode() { return code; }
}
