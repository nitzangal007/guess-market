package guessmarket.client.ui;
import guessmarket.dto.api.*;
import javafx.scene.layout.VBox;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import java.util.*;
import java.util.function.IntConsumer;
public final class AccountActivityView extends VBox {
    private final IntConsumer eventSelected;
    private List<AccountEntryDto> previous=List.of();
    public AccountActivityView(IntConsumer eventSelected){this.eventSelected=eventSelected;setSpacing(12);}
    public void clear(){previous=List.of();getChildren().clear();}
    public void update(AccountDto account){if(previous.equals(account.entries())&&!getChildren().isEmpty())return;previous=account.entries();
        var rows=previous.reversed();var content=UiTables.table("Your account activity",rows,List.of(UiTables.number("Entry",e->Long.toString(e.entryId())),UiTables.text("Time",AccountEntryDto::timestamp),UiTables.text("Kind",AccountEntryDto::kind),UiTables.text("Event",e->e.eventId()==null?"Account":(e.eventName().isEmpty()?"Event #"+e.eventId():e.eventName())),UiTables.compact("Signed amount",AccountEntryDto::amount,e->e.kind().equals("REPRESENTATION_ADJUSTMENT")?WireViewMapper.adjustment(e.amount()):WireViewMapper.money(e.amount())),UiTables.money("Journal running balance",AccountEntryDto::journalRunningBalance),UiTables.money("Stored balance after transaction",AccountEntryDto::storedBalanceAfterTransaction),UiTables.text("Transaction",AccountEntryDto::transactionId)));
        @SuppressWarnings("unchecked") var table=(TableView<AccountEntryDto>)content.getChildren().get(1);
        Runnable open=()->{var item=table.getSelectionModel().getSelectedItem();if(item!=null&&item.eventId()!=null)eventSelected.accept(item.eventId());};table.setOnMouseClicked(e->{if(e.getClickCount()==2)open.run();});table.setOnKeyPressed(e->{if(e.getCode()==KeyCode.ENTER){open.run();e.consume();}});
        getChildren().setAll(UiTables.label("Each row is one cash effect. Journal running balance explains components; Stored balance after transaction is authoritative. Select an event row and press Enter to inspect it. Expand exact values to inspect representation adjustments.","muted"),content);
    }
}
