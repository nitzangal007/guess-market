package guessmarket.dto.world;

/** Engine-calculated information for confirmation, not permission to skip revalidation. */
public record OpeningPreview(long worldRevision, int eventId, String eventName, String actingUser,
                             double currentBalance, double requiredFunding, double balanceAfterOpening) {}
