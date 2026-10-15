package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record DashboardResponse(String serverInstanceId, long revision,
                         List<EventSummaryDto> events, List<PublicUserDto> users,
                         AccountDto account, EventDetailDto selectedEvent) {
    public DashboardResponse {
        events = List.copyOf(events);
        users = List.copyOf(users);
    }
}
