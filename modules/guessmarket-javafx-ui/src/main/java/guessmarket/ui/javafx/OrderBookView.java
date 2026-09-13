package guessmarket.ui.javafx;
import guessmarket.dto.world.*;
import java.math.BigDecimal;
import java.util.*;
import javafx.beans.binding.Bindings;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Presents only Engine snapshots. No matching, valuation or financial policy lives here. */
final class OrderBookView {
    private OrderBookView(){}
    private static Label label(String text,String style){
        Label label=EventDetailsView.label(text,style);label.setMinHeight(Region.USE_PREF_SIZE);return label;
    }
    private static <T> void column(TableView<T> table,String name,java.util.function.Function<T,String> value,double width){
        column(table,name,value,width,List.of("Remaining","Qty","Limit","Priority","Price","Principal","Buyer fee").contains(name));
    }
    private static <T> void column(TableView<T> table,String name,java.util.function.Function<T,String> value,double width,boolean numeric){
        TableColumn<T,String> column=new TableColumn<>(name);
        column.setUserData(width);
        column.getProperties().put("numeric",numeric);
        column.setCellValueFactory(cell->new javafx.beans.property.ReadOnlyStringWrapper(value.apply(cell.getValue())));
        column.setCellFactory(ignored->new TableCell<>(){
            final Label content=label("","description");
            {
                content.prefWidthProperty().bind(column.widthProperty().subtract(16));content.setStyle("-fx-font-size: 16px;");
                content.setWrapText(!numeric);
                if(numeric){content.setAlignment(javafx.geometry.Pos.TOP_RIGHT);setAlignment(javafx.geometry.Pos.TOP_RIGHT);}
                prefHeightProperty().bind(Bindings.createDoubleBinding(
                        ()->content.prefHeight(Math.max(1,column.getWidth()-16))+24,column.widthProperty(),content.textProperty(),content.fontProperty()));
            }
            @Override protected void updateItem(String text,boolean empty){
                super.updateItem(text,empty);setText(null);content.setText(empty?"":text);setGraphic(empty?null:content);
                setTooltip(empty?null:new Tooltip(text));
            }
        });
        double headingMinimum=Boolean.TRUE.equals(table.getProperties().get("fullHeadings"))?headerWidth(name):textWidth(name)+20;
        column.setMinWidth(name.equals("User")?100:Math.max(55,headingMinimum));
        column.setPrefWidth(width);column.setSortable(false);table.getColumns().add(column);
    }
    // Calculated output only: conventional half-up rounding, at most two fractional digits.
    static String decimal(BigDecimal value){return exact(value.setScale(2,java.math.RoundingMode.HALF_UP));}
    // Entered order limits must remain exact through display, review and editing.
    static String exact(BigDecimal value){return value.stripTrailingZeros().toPlainString();}
    static String optional(Optional<BigDecimal> value){return value.map(OrderBookView::decimal).orElse("Unavailable");}
    static VBox books(EventSnapshot event){
        var book=event.orderBook().orElseThrow();
        FlowPane panels=new FlowPane(18,18);panels.setId("orderBookPanels");panels.setMinWidth(0);
        for(int option=1;option<=2;option++){
            final int selected=option;
            var quote=book.quotes().get(option-1);
            var table=new TableView<OrderBookOrder>();table.setId("orderBook"+option);
            table(table,460);
            column(table,"Side",o->o.side().name(),65);
            column(table,"User",OrderBookOrder::userName,180);
            column(table,"Remaining",o->Integer.toString(o.quantity()),100);
            column(table,"Limit",o->exact(o.limitPrice()),90);
            table.getItems().setAll(book.orders().stream().filter(o->o.optionNumber()==selected).sorted((a,b)->{
                if(a.side()!=b.side())return a.side()==OrderSide.BUY?-1:1;
                int price=a.limitPrice().compareTo(b.limitPrice());if(a.side()==OrderSide.BUY)price=-price;
                return price!=0?price:Long.compare(a.sequence(),b.sequence());
            }).toList());
            sizeColumns(table);
            table.setPlaceholder(label("No waiting orders.","muted"));
            Label priority=label("Select an order to inspect its original priority.","muted");
            table.getSelectionModel().selectedItemProperty().addListener((o,b,a)->priority.setText(a==null?
                    "Select an order to inspect its original priority.":"Selected order priority: #"+a.sequence()+" / "+a.userName()));
            FlowPane quotes=new FlowPane(16,8,small("BID",optional(quote.bid())),small("ASK",optional(quote.ask())),
                    small("LAST",optional(quote.last())),small("MID",optional(quote.mid())),small("SPREAD",optional(quote.spread())));
            VBox panel=new VBox(12,label(event.optionLabels().get(option-1),"subheading"),quotes,
                    label(table.getItems().size()+" waiting orders / best price, then oldest priority","muted"),table,priority);
            panel.setId("orderPanel"+option);panel.getStyleClass().add("order-panel");panel.setMinWidth(0);
            panel.prefWidthProperty().bind(Bindings.createDoubleBinding(()->{
                double width=Math.max(0,panels.getWidth());return Math.max(0,Math.floor(width>=900?(width-22)/2:width-2));
            },panels.widthProperty()));
            panel.maxWidthProperty().bind(panel.prefWidthProperty());
            panels.getChildren().add(panel);
        }
        VBox box=new VBox(16,label("Waiting orders","subheading"),label("BUY is a maximum price; SELL is a minimum. Quantity is the unfilled remainder. Priority numbers keep their original order after partial fills.","muted"),panels,
                label("Participants: "+(book.participants().isEmpty()?"None yet":String.join(", ",book.participants())),"description"));
        box.setMinWidth(0);return box;
    }
    static VBox participants(EventSnapshot event){
        var book=event.orderBook().orElseThrow();
        var table=new TableView<OrderBookPosition>();table.setId("participantsTable");table(table,420);
        table.getProperties().put("fullHeadings",true);
        String one=event.optionLabels().getFirst(),two=event.optionLabels().get(1);
        column(table,"Participant",OrderBookPosition::userName,220);
        column(table,one+" qty",p->Integer.toString(p.optionOneShares()),80,true);
        column(table,one+" value",p->optional(p.optionOneValue()),100,true);
        column(table,one+" basis",p->book.quotes().getFirst().valuationBasis(),105,false);
        column(table,two+" qty",p->Integer.toString(p.optionTwoShares()),80,true);
        column(table,two+" value",p->optional(p.optionTwoValue()),100,true);
        column(table,two+" basis",p->book.quotes().get(1).valuationBasis(),105,false);
        column(table,"Total value",p->optional(p.estimatedValue()),110,true);
        table.getItems().setAll(book.positions());sizeColumns(table);
        table.setPlaceholder(label("No participants yet.","muted"));
        VBox box=new VBox(14,label("Participant holdings and values","subheading"),
                label("Quantities are current holdings. Waiting and executed order quantities are shown elsewhere.","muted"),table,
                label("Values use each outcome's MID when both sides are quoted, otherwise LAST. Unavailable means no market estimate. Closed events use the settlement value.","muted"));
        box.setMinWidth(0);return box;
    }
    private static VBox small(String caption,String value){return new VBox(3,label(caption,"muted"),label(value,"description"));}
    static VBox position(EventSnapshot event,UserSnapshot user){
        var book=event.orderBook().orElseThrow();
        var found=book.positions().stream().filter(p->p.userName().equals(user.name())).findFirst();
        VBox box=new VBox(16,label(user.name()+" / Your position","subheading"));
        if(found.isEmpty()){box.getChildren().add(label("No holdings or orders yet. Available shares: 0 for both outcomes.","muted"));return box;}
        var p=found.orElseThrow();
        Label summary=label("Your shares: "+event.optionLabels().getFirst()+" = "+p.optionOneShares()+" | "+event.optionLabels().get(1)+" = "+p.optionTwoShares(),"subheading");
        summary.setId("positionSummary");box.getChildren().add(summary);
        for(int option=1;option<=2;option++){
            var q=book.quotes().get(option-1);
            box.getChildren().add(new VBox(7,label(event.optionLabels().get(option-1),"subheading"),
                    label("Held: "+(option==1?p.optionOneShares():p.optionTwoShares())+" / SELL reserved: "+(option==1?p.reservedOne():p.reservedTwo())
                            +" / Available: "+(option==1?p.availableOne():p.availableTwo()),"description"),
                    label("Cumulative amount paid: "+decimal(option==1?p.optionOnePaid():p.optionTwoPaid())+" / Value per share: "+optional(q.estimate())+" / Basis: "+q.valuationBasis(),"muted")));
        }
        box.getChildren().addAll(PurchaseDialog.field("Cumulative purchase principal",decimal(p.purchases()),"obPurchases"),
                PurchaseDialog.field("Sale receipts",decimal(p.saleReceipts()),"obSales"),
                PurchaseDialog.field("Commissions paid",decimal(p.commissionsPaid()),"obFees"),
                PurchaseDialog.field("Market-maker commission income",decimal(p.commissionIncome()),"obFeeIncome"),
                PurchaseDialog.field("Initial owner funding",decimal(p.funding()),"obFunding"),
                PurchaseDialog.field("Settlement receipts, before closing fees",decimal(p.settlementReceipts()),"obPayouts"));
        if(event.status()==WorldEventStatus.CLOSED)box.getChildren().addAll(
                PurchaseDialog.field("Final event profit / loss",decimal(p.profitLoss()),"obProfitLoss"),
                label("All event receipts minus outflows, including funding and fees. Settled holdings are historical and are not added again.","muted"));
        else box.getChildren().addAll(PurchaseDialog.field("Estimated holdings value",optional(p.estimatedValue()),"obValue"),
                label("Estimate uses MID when both quotes exist, otherwise LAST, otherwise unavailable. This is not cash or a guaranteed sale value.","muted"));
        return box;
    }
    static VBox history(EventSnapshot event,UserSnapshot user){
        var all=event.orderBook().orElseThrow().trades().reversed();
        var table=tradeTable(event);table.setId("tradeHistory");
        VBox box=new VBox(14,label("Trade history, newest first","subheading"));
        if(user!=null){
            ComboBox<String> scope=new ComboBox<>();scope.setId("historyScope");scope.getItems().setAll("Your trades","All event trades");
            scope.getSelectionModel().selectFirst();
            scope.valueProperty().addListener((o,before,value)->table.getItems().setAll("Your trades".equals(value)?
                    all.stream().filter(t->t.buyer().equals(user.name())||(!t.mint()&&t.seller().equals(user.name()))).toList():all));
            table.getItems().setAll(all.stream().filter(t->t.buyer().equals(user.name())||(!t.mint()&&t.seller().equals(user.name()))).toList());
            box.getChildren().addAll(label("History scope","muted"),scope);
        }else table.getItems().setAll(all);
        box.getChildren().addAll(label("Mint has one purchase row per buyer. Its counterpart is the opposite buyer, not a seller.","muted"),table);return box;
    }
    static TableView<OrderBookTrade> tradeTable(EventSnapshot event){
        var table=new TableView<OrderBookTrade>();table(table,330);
        column(table,"Route",t->"#"+t.sequence()+" / "+(t.mint()?"Mint":"Ordinary"),120);
        column(table,"Buyer",OrderBookTrade::buyer,150);
        column(table,"Counterpart",OrderBookTrade::seller,150);
        column(table,"Outcome",t->event.optionLabels().get(t.optionNumber()-1),100);
        column(table,"Qty",t->Integer.toString(t.quantity()),65);
        column(table,"Price",t->decimal(t.price()),90);
        column(table,"Principal",t->decimal(t.principal()),100);
        column(table,"Buyer fee",t->decimal(t.commission()),100);
        table.setPlaceholder(label("No executed trades.","muted"));
        return table;
    }
    static void table(TableView<?> table,double height){
        table.setMinWidth(0);table.setMaxWidth(Double.MAX_VALUE);table.setPrefHeight(height);
        table.setMinHeight(height);table.setMaxHeight(height);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.widthProperty().addListener((o,b,a)->sizeColumns(table));
    }
    private static double textWidth(String value){
        var text=new javafx.scene.text.Text(value);text.setFont(javafx.scene.text.Font.font("Segoe UI",16));return text.getLayoutBounds().getWidth();
    }
    private static double headerWidth(String value){
        var text=new javafx.scene.text.Text(value);text.setFont(javafx.scene.text.Font.font("Segoe UI",javafx.scene.text.FontWeight.BOLD,16));
        return Math.ceil(text.getLayoutBounds().getWidth())+32;
    }
    private static void sizeColumns(TableView<?> table){
        double minimum=0,weight=0;
        for(var column:table.getColumns()){
            if(Boolean.TRUE.equals(column.getProperties().get("numeric"))){
                double min=Math.max(55,Boolean.TRUE.equals(table.getProperties().get("fullHeadings"))?headerWidth(column.getText()):textWidth(column.getText())+20);
                for(int i=0;i<table.getItems().size();i++)min=Math.max(min,textWidth(String.valueOf(column.getCellData(i)))+20);
                column.setMinWidth(min);
            }
            minimum+=column.getMinWidth();weight+=(Double)column.getUserData();
        }
        double extra=Math.max(0,table.getWidth()-26-minimum);
        for(var column:table.getColumns())column.setPrefWidth(Math.floor(column.getMinWidth()+extra*(Double)column.getUserData()/Math.max(1,weight)));
    }
}
