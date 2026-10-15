package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record CommandStatusDto(String serverInstanceId, long revision, String requestId,
                        ReceiptStatus status, CommandResponse command,
                        UploadResponse upload, ApiError error) {}
