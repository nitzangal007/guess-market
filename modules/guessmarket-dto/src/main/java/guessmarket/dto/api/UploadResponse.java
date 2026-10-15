package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record UploadResponse(String serverInstanceId, long revision, String requestId,
                      OperationKind operation, List<AddedEventDto> addedEvents) {
    public UploadResponse {
        addedEvents = List.copyOf(addedEvents);
    }
}
