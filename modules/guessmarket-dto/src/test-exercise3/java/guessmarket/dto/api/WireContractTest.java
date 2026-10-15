package guessmarket.dto.api;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WireContractTest {
    private final Gson gson = new GsonBuilder().serializeNulls().setStrictness(Strictness.STRICT)
            .addReflectionAccessFilter(ReflectionAccessFilter.BLOCK_ALL_PLATFORM).create();
    @Test void nestedSnapshotPreservesNullsAndConcreteValues() {
        var account = new AccountDto("Dana", "125.50", false, List.of(1), List.of(), List.of());
        var expected = new DashboardResponse("instance", 11, List.of(),
                List.of(new PublicUserDto("Dana", "125.50", true)), account, null);
        assertEquals(expected, gson.fromJson(gson.toJson(expected), DashboardResponse.class));
        assertTrue(gson.toJson(expected).contains("\"selectedEvent\":null"));
        assertThrows(JsonSyntaxException.class, () -> gson.fromJson("{\"name\":", PublicUserDto.class));
    }
    @Test void snapshotCopiesCollections() {
        var users = new ArrayList<PublicUserDto>();
        users.add(new PublicUserDto("Dana", "0.0", false));
        var snapshot = new DashboardResponse("i", 0, List.of(), users,
                new AccountDto("Dana", "0.0", false, List.of(), List.of(), List.of()), null);
        users.clear();
        assertEquals(1, snapshot.users().size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.users().clear());
    }
}
