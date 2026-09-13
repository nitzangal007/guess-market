package guessmarket.dto.world;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
public record OrderResult(WorldSnapshot world,OrderPreview execution) {}
