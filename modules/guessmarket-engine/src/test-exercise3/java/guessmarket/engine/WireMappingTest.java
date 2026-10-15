package guessmarket.engine;
import org.junit.jupiter.api.Test;
import guessmarket.dto.world.*;
import guessmarket.dto.api.*;
import java.util.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
class WireMappingTest {
    @Test void realTradesRetainAllEconomicsAndMintRole(){
        var trade=new OrderBookTrade(9,"Dana","Bob",2,7,new BigDecimal(".425"),new BigDecimal("2.975"),new BigDecimal(".14875"),true);
        var mapped=ApiSnapshotMapper.execution(trade);
        assertEquals(9,mapped.sequence());assertEquals(2,mapped.option());assertEquals(7,mapped.quantity());assertEquals("Dana",mapped.buyer());assertEquals("Bob",mapped.counterparty());assertEquals(CounterpartyRole.OPPOSITE_BUYER,mapped.counterpartyRole());assertTrue(mapped.mint());
        assertEquals("0.425",mapped.price());assertEquals("2.975",mapped.principal());assertEquals("0.14875",mapped.commission());
        var gson=new com.google.gson.GsonBuilder().serializeNulls().setStrictness(com.google.gson.Strictness.STRICT).create();assertEquals(mapped,gson.fromJson(gson.toJson(mapped),ExecutionDto.class));
    }
    @Test void settlementRetainsGrossFeeNetAndRefund(){
        var source=new ClosePreview(11,3,"event","Dana",2,"",List.of(new SettlementPayment("Bob",7,7,.35,6.65)),7,.35,2.5);
        var mapped=ApiSnapshotMapper.settlement(source,"0.0","1.25");assertEquals(2,mapped.winningOption());assertEquals("",mapped.winningLabel());
        assertEquals("7.0",mapped.totalGrossPayout());assertEquals("0.35",mapped.totalCommission());assertEquals("2.5",mapped.subsidyRefund());assertEquals("1.25",mapped.commissionTotal());assertEquals("6.65",mapped.payouts().getFirst().netPayout());
        var gson=new com.google.gson.GsonBuilder().serializeNulls().setStrictness(com.google.gson.Strictness.STRICT).create();assertEquals(mapped,gson.fromJson(gson.toJson(mapped),SettlementDto.class));
    }
}
