package guessmarket.ui.javafx;

import guessmarket.dto.CommissionMode;
import guessmarket.dto.world.*;
import java.util.List;
import java.util.Locale;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/** Snapshot presentation only; controllers attach actions with an explicit user. */
final class EventDetailsView extends VBox {
    EventDetailsView(EventSnapshot event){this(event,"",(UserSnapshot)null);}
    EventDetailsView(EventSnapshot event,String prefix){this(event,prefix,(UserSnapshot)null);}
    EventDetailsView(EventSnapshot event,String prefix,boolean includeHistory){this(event,prefix,(UserSnapshot)null);}
    EventDetailsView(EventSnapshot event,String prefix,UserSnapshot user){
        super(18);getStyleClass().add("event-detail");
        Label title=label(event.name(),"section-title");title.setId(prefix+"detailName");
        Label method=label(method(event),"status");method.setId(prefix+"methodName");
        Label state=label("Event "+event.id()+"  /  "+status(event)+"  /  "+commission(event),"muted");state.setId(prefix+"eventState");
        FlowPane identity=new FlowPane(18,8,method,state,label("Market maker: "+event.marketMakerName(),"muted"));
        getChildren().addAll(title,identity);
        TabPane sections=new TabPane();sections.setId(prefix+"eventSections");
        sections.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        sections.getTabs().add(tab("Overview",overview(event,prefix)));
        if(event.orderBook().isPresent())sections.getTabs().add(tab("Order books",OrderBookView.books(event)));
        if(user==null&&event.orderBook().isPresent())sections.getTabs().add(tab("Participants",OrderBookView.participants(event)));
        if(user!=null)sections.getTabs().add(tab("Your position",event.orderBook().isPresent()?OrderBookView.position(event,user):position(event,user)));
        sections.getTabs().add(tab("Trade history",history(event,user)));
        sections.getTabs().add(tab("Settlement",settlement(event)));
        getChildren().add(sections);
    }
    private static Tab tab(String name,VBox content){
        Tab tab=new Tab(name,content);content.getStyleClass().add("detail-section");return tab;
    }
    private static VBox overview(EventSnapshot event,String prefix){
        VBox box=new VBox(18,label(event.description(),"description"));
        Label cash=label(money(event.contractBalance()),"stat-value");cash.setId(prefix+"contractBalance");
        Label caption=label("Contract balance","muted");caption.setId(prefix+"accountMeaning");
        FlowPane metrics=new FlowPane(30,18,new VBox(4,caption,cash));metrics.getStyleClass().add("metrics");
        if(event.pricing() instanceof LmsrConfiguration lmsr){
            metrics.getChildren().add(metric("Liquidity (b)",Integer.toString(lmsr.b())));
            event.lmsrTrading().ifPresent(t->metrics.getChildren().add(metric("Commission collected",
                    money(t.totalPurchaseCommission()+t.settlement().map(ClosePreview::totalCommission).orElse(0.0)))));
        }
        box.getChildren().add(metrics);
        if(event.pricing() instanceof OrderBookConfiguration book){
            box.getChildren().addAll(label("Opening investment: "+book.initial()+"  /  Base value (d): "+book.d(),"description"),
                    label("Automatic mint: "+(book.allowMint()?"Enabled":"Disabled"),"description"),
                    label("Issued pairs: "+event.orderBook().orElseThrow().issuedPairs()+" / Waiting orders: "+event.orderBook().orElseThrow().orders().size(),"muted"));
        }
        FlowPane outcomes=new FlowPane(18,18);
        for(int i=0;i<2;i++){
            Label name=label(event.optionLabels().get(i),"subheading");name.setId(prefix+"optionLabel"+(i+1));
            VBox option=new VBox(8,name);option.getStyleClass().add("option-panel");option.setPrefWidth(300);option.setMinWidth(0);
            if(event.lmsrTrading().isPresent()){
                var t=event.lmsrTrading().orElseThrow();
                option.getChildren().addAll(label(money(i==0?t.priceOne():t.priceTwo()),"stat-value"),
                        label((i==0?t.quantityOne():t.quantityTwo())+" outstanding shares","muted"));
            }
            outcomes.getChildren().add(option);
        }
        box.getChildren().add(outcomes);
        box.getChildren().add(label(switch(event.status()){
            case NOT_STARTED->"Not started. Its market maker can review opening funding in Users.";
            case ACTIVE->"Active. Financial actions are available under an explicit user in Users.";
            case CLOSED->"Closed. Final holdings, trade history and settlement remain available.";
        },"muted"));
        return box;
    }
    private static VBox metric(String caption,String value){return new VBox(6,label(caption,"muted"),label(value,"stat-value"));}
    private static VBox position(EventSnapshot event,UserSnapshot user){
        var held=user.positions().stream().filter(p->p.eventId()==event.id()).findFirst()
                .orElse(new UserEventPosition(event.id(),0,0,0,0,0));
        Label summary=label("Your shares: "+event.optionLabels().get(0)+" = "+held.quantityOne()+" | "
                +event.optionLabels().get(1)+" = "+held.quantityTwo()+" | Total paid: "+money(held.totalPaid()),"subheading");
        summary.setId("positionSummary");
        VBox box=new VBox(18,label(user.name()+" / Your position","subheading"),summary,
                new FlowPane(30,18,metric(event.optionLabels().get(0)+" shares",Integer.toString(held.quantityOne())),
                        metric(event.optionLabels().get(1)+" shares",Integer.toString(held.quantityTwo())),
                        metric("Total paid",money(held.totalPaid()))));
        if(held.quantityOne()==0&&held.quantityTwo()==0)box.getChildren().add(label("No purchases yet.","muted"));
        event.lmsrTrading().orElseThrow().settlement().ifPresent(s->box.getChildren().add(label("Winning outcome: "+s.winningLabel(),"status")));
        return box;
    }
    private static VBox history(EventSnapshot event,UserSnapshot user){
        if(event.orderBook().isPresent())return OrderBookView.history(event,user);
        VBox box=new VBox(16);
        if(event.lmsrTrading().isEmpty()){box.getChildren().add(label("Order Book trading is not available yet.","muted"));return box;}
        List<PurchaseEntry> all=event.lmsrTrading().orElseThrow().newestFirstHistory();
        TableView<PurchaseEntry> table=new TableView<>();table.setId("tradeHistory");
        column(table,"Trade",t->"#"+t.sequence(),70);column(table,"User",PurchaseEntry::userName,115);
        column(table,"Outcome",PurchaseEntry::optionLabel,155);column(table,"Shares",t->Integer.toString(t.quantity()),75);
        column(table,"Cost",t->money(t.shareCost()),100);column(table,"Fee",t->money(t.commission()),85);
        column(table,"Total",t->money(t.totalPaid()),110);
        table.setPlaceholder(label("No purchases yet.","muted"));table.setPrefHeight(300);
        if(user!=null){
            ComboBox<String> scope=new ComboBox<>();scope.setId("historyScope");scope.getItems().setAll("Your trades","All event trades");scope.getSelectionModel().selectFirst();
            scope.valueProperty().addListener((o,before,value)->table.getItems().setAll(
                    "Your trades".equals(value)?all.stream().filter(t->t.userName().equals(user.name())).toList():all));
            table.getItems().setAll(all.stream().filter(t->t.userName().equals(user.name())).toList());
            box.getChildren().addAll(label("History scope","muted"),scope);
        }else table.getItems().setAll(all);
        box.getChildren().addAll(label("Trade history, newest first","subheading"),table);return box;
    }
    static <T> void column(TableView<T> table,String name,java.util.function.Function<T,String> value,double width){
        TableColumn<T,String> column=new TableColumn<>(name);
        column.setCellValueFactory(cell->new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        column.setCellFactory(ignored->new TableCell<>(){
            @Override protected void updateItem(String text,boolean empty){
                super.updateItem(text,empty);setText(empty?null:text);setWrapText(true);
                setTooltip(empty||text==null?null:new Tooltip(text));
            }
        });
        column.setPrefWidth(width);column.setSortable(false);table.getColumns().add(column);
    }
    private static VBox settlement(EventSnapshot event){
        VBox box=new VBox(16);
        var settled=event.orderBook().isPresent()?event.orderBook().orElseThrow().settlement():event.lmsrTrading().flatMap(LmsrTradingSnapshot::settlement);
        if(settled.isEmpty()){
            box.getChildren().add(label(event.status()==WorldEventStatus.ACTIVE?
                    "This event is still active. Final payouts will appear here after it closes.":
                    "No final settlement is available for this event.","muted"));return box;
        }
        var s=settled.orElseThrow();
        box.getChildren().addAll(label("Winning outcome: "+s.winningLabel(),"subheading"),
                new FlowPane(30,18,metric("Gross payouts",money(s.totalGrossPayout())),
                        metric("Closing fees",money(s.totalCommission()))));
        if(event.orderBook().isEmpty())box.getChildren().add(metric("Subsidy returned",money(s.subsidyRefund())));
        else box.getChildren().add(label("Winning shares pay "+((OrderBookConfiguration)event.pricing()).d()+" each. Waiting orders are cancelled and reservations released.","muted"));
        TableView<SettlementPayment> table=new TableView<>();table.setId("settlementPayments");
        column(table,"Recipient",SettlementPayment::userName,160);
        column(table,"Winning shares",p->Integer.toString(p.winningShares()),160);
        column(table,"Gross",p->money(p.grossPayout()),115);column(table,"Fee",p->money(p.commission()),100);
        column(table,"Net payout",p->money(p.netPayout()),120);
        table.getItems().setAll(s.payments());table.setPrefHeight(260);
        table.setPlaceholder(label(event.orderBook().isPresent()?"No winning holdings. No payout is due.":"No winning shares. The unused subsidy returns to the market maker.","muted"));
        box.getChildren().add(table);return box;
    }
    static Label label(String text,String style){
        Label label=new Label(text);label.setWrapText(true);label.setMinWidth(0);label.setMaxWidth(Double.MAX_VALUE);
        label.getStyleClass().add(style);return label;
    }
    static String money(double value){
        String text=String.format(Locale.US,"%,.2f",value);return "-0.00".equals(text)?"0.00":text;
    }
    static Label tradeLine(PurchaseEntry trade){
        return label("#"+trade.sequence()+" | "+trade.userName()+" | "+trade.optionLabel()+" | "+trade.quantity()
                +" shares | Shares cost "+money(trade.shareCost())+" | Fee "+money(trade.commission())
                +" | Total paid "+money(trade.totalPaid()),"description");
    }
    static String commission(EventSnapshot event){return event.commissionPercentage()+"% "+(event.commissionMode()==CommissionMode.ON_PURCHASE?"on purchase":"on close");}
    static String method(EventSnapshot event){return event.pricing() instanceof LmsrConfiguration?"LMSR":"Order Book";}
    static String status(EventSnapshot event){return switch(event.status()){case NOT_STARTED->"Not started";case ACTIVE->"Active";case CLOSED->"Closed";};}
}

