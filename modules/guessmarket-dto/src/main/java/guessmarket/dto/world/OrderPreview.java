package guessmarket.dto.world;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
public record OrderPreview(long worldRevision,OrderRequest request,List<OrderBookTrade> fills,int incomingRemaining,double balanceBefore,double balanceAfter,boolean blockedAfter,int cancelledOrderCount,int releasedReservationQuantity) { public OrderPreview { fills=List.copyOf(fills); } }
