package guessmarket.client.ui;
import javafx.scene.layout.VBox;
import javafx.scene.layout.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import java.util.*;
import java.math.BigDecimal;
import guessmarket.dto.api.*;
import guessmarket.dto.world.OrderSide;
import guessmarket.client.session.RemoteMarketSession;
import guessmarket.dto.api.EventDetailDto;
public final class EventDetailView extends VBox {
    private final RemoteMarketSession session;
    private final EventNavigationState state;
    private final TabPane tabs=new TabPane();
    private final VBox body=new VBox(16),forms=new VBox(12);
    private final Label heading=UiTables.label("Select an event", "section-title"),error=UiTables.label("","error");
    private final ComboBox<Integer> option=new ComboBox<>(),winner=new ComboBox<>();
    private final ComboBox<OrderSide> side=new ComboBox<>();
    private final TextField quantity=new TextField("1"),limit=new TextField("0.50");
    private final Button open=new Button("Review opening"),trade=new Button("Review trade"),close=new Button("Review closing");
    private EventDetailDto detail;
    private String actor="";
    private boolean rendering;
    public EventDetailView(RemoteMarketSession session,EventNavigationState state){
        this.session=session;this.state=state;setSpacing(16);getStyleClass().add("detail-section");heading.setFocusTraversable(true);heading.setId("selected-event-heading");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);tabs.setMinWidth(0);setMinWidth(0);option.getItems().setAll(1,2);winner.getItems().setAll(1,2);side.getItems().setAll(OrderSide.values());
        option.setId("option");winner.setId("winner");side.setId("side");quantity.setId("quantity");limit.setId("limit-price");quantity.setAccessibleText("Share quantity");limit.setAccessibleText("Exact limit price");
        var converter=new StringConverter<Integer>(){public String toString(Integer value){return value==null||detail==null?"":WireViewMapper.option(value,detail.optionLabels());}public Integer fromString(String value){return null;}};option.setConverter(converter);winner.setConverter(converter);
        option.valueProperty().addListener((o,a,b)->save());winner.valueProperty().addListener((o,a,b)->save());side.valueProperty().addListener((o,a,b)->save());quantity.textProperty().addListener((o,a,b)->save());limit.textProperty().addListener((o,a,b)->save());tabs.getSelectionModel().selectedItemProperty().addListener((o,a,b)->save());
        open.setOnAction(e->review(new ActionRequest(ActionKind.OPEN,null,null,null,null)));close.setOnAction(e->review(new ActionRequest(ActionKind.CLOSE,winner.getValue(),null,null,null)));trade.setOnAction(e->trade());
        session.connectedProperty().addListener((o,a,b)->availability());session.busyProperty().addListener((o,a,b)->availability());session.unknownProperty().addListener((o,a,b)->availability());session.loggedInProperty().addListener((o,a,b)->availability());
        error.setId("trade-error");error.setVisible(false);error.setManaged(false);getChildren().addAll(heading,body,tabs,forms,error);
    }
    public void showDetail(EventDetailDto next,String actor){
        if(next.equals(detail)&&this.actor.equals(actor)){restoreDraft(state.draft(actor,next.summary().id()));availability();return;}
        detail=next;this.actor=actor;
        var draft=state.draft(actor,next.summary().id());rendering=true;
        try{
            heading.setText(WireViewMapper.eventName(next.summary()));body.getChildren().setAll(UiTables.label(next.summary().description(),"description"),UiTables.label(WireViewMapper.words(next.summary().method())+"   "+WireViewMapper.words(next.summary().status())+"\nOwner: "+next.summary().owner()+"   Commission: "+next.summary().commissionPercentage()+"% "+WireViewMapper.words(next.summary().commissionMode())+"\nContract balance: "+WireViewMapper.money(next.summary().contractBalance()),"muted"));
            option.setValue(draft.option());quantity.setText(draft.quantity());side.setValue(draft.side());limit.setText(draft.rawLimit());winner.setValue(draft.winner());
            var overview=new Tab("Overview",scroll(overview()));var position=new Tab("Your position",scroll(position()));var history=new Tab("Trade history",scroll(history()));var settlement=new Tab("Settlement",scroll(settlement()));tabs.getTabs().setAll(overview,position,history,settlement);
            tabs.getSelectionModel().select(tabs.getTabs().stream().filter(t->t.getText().equals(draft.tab())).findFirst().orElse(overview));
            forms.getChildren().clear();
            if(next.summary().status().equals("NOT_STARTED")){forms.getChildren().add(open);}
            if(next.summary().status().equals("ACTIVE")){
                var row=new FlowPane(10,10);row.getChildren().addAll(field("Option",option),field("Quantity",quantity));
                if(next.book()!=null)row.getChildren().addAll(field("Side",side),field("Exact limit (0 to "+BigDecimal.valueOf(next.book().d()).subtract(new BigDecimal(".01")).toPlainString()+")",limit));
                forms.getChildren().addAll(UiTables.label(next.book()==null?"Buy LMSR shares":"Submit an Order Book order","subheading"),row,trade);
                if(next.summary().owner().equals(actor)){var closing=new FlowPane(10,10);closing.getChildren().addAll(field("Winning option",winner),close);forms.getChildren().add(closing);}
            }
            availability();showError("");
        }finally{rendering=false;}
    }
    private void restoreDraft(EventNavigationState.Draft draft){rendering=true;try{option.setValue(draft.option());quantity.setText(draft.quantity());side.setValue(draft.side());limit.setText(draft.rawLimit());winner.setValue(draft.winner());tabs.getTabs().stream().filter(tab->tab.getText().equals(draft.tab())).findFirst().ifPresent(tab->tabs.getSelectionModel().select(tab));}finally{rendering=false;}}
    public void clearError(){showError("");}
    public void clear(){rendering=true;try{detail=null;actor="";heading.setText("Select an event");body.getChildren().clear();tabs.getTabs().clear();forms.getChildren().clear();quantity.setText("1");limit.setText("0.50");option.setValue(1);winner.setValue(1);side.setValue(OrderSide.BUY);showError("");}finally{rendering=false;}}
    private static VBox field(String name,Node control){if(control instanceof Region region){region.setPrefWidth(210);region.setMaxWidth(280);}return new VBox(5,UiTables.label(name,"muted"),control);}
    private static ScrollPane scroll(Node node){var scroll=new ScrollPane(node);scroll.setFitToWidth(true);scroll.setPrefViewportHeight(330);scroll.setMinHeight(200);UiFocus.keepVisible(scroll);return scroll;}
    private void save(){if(rendering||detail==null||option.getValue()==null||winner.getValue()==null||side.getValue()==null)return;String tab=tabs.getSelectionModel().getSelectedItem()==null?"Overview":tabs.getSelectionModel().getSelectedItem().getText();state.save(actor,detail.summary().id(),new EventNavigationState.Draft(option.getValue(),quantity.getText(),side.getValue(),limit.getText(),winner.getValue(),tab));}
    private void availability(){boolean unavailable=detail==null||!session.mutationAllowed();open.setDisable(unavailable||!detail.canOpen());trade.setDisable(unavailable||!detail.canTrade());close.setDisable(unavailable||!detail.canClose());}
    private void showError(String message){error.setText(message);error.setVisible(!message.isEmpty());error.setManaged(!message.isEmpty());}
    private void trade(){
        if(detail==null||!session.mutationAllowed()||!detail.canTrade())return;
        int shares;try{shares=Integer.parseInt(quantity.getText().trim());if(shares<=0)throw new NumberFormatException();}catch(NumberFormatException problem){showError("Quantity must be a positive whole number.");quantity.requestFocus();return;}
        if(detail.book()!=null){try{var price=new BigDecimal(limit.getText());var maximum=BigDecimal.valueOf(detail.book().d()).subtract(new BigDecimal(".01"));if(price.signum()<0||price.compareTo(maximum)>0)throw new NumberFormatException();}catch(NumberFormatException problem){showError("Enter an exact limit from 0 through "+BigDecimal.valueOf(detail.book().d()).subtract(new BigDecimal(".01"))+".");limit.requestFocus();return;}}
        review(new ActionRequest(detail.book()==null?ActionKind.LMSR_BUY:ActionKind.ORDER,option.getValue(),shares,detail.book()==null?null:side.getValue(),detail.book()==null?null:limit.getText()));
    }
    private void review(ActionRequest action){showError("");var origin=action.action()==ActionKind.OPEN?open:action.action()==ActionKind.CLOSE?close:trade;session.preview(detail.summary().id(),action,preview->{var dialog=new TradeReviewDialog(session,preview);if(getScene()!=null)dialog.initOwner(getScene().getWindow());if(Boolean.TRUE.equals(dialog.showAndWait().orElse(false)))session.execute(preview);origin.requestFocus();},this::showError);}
    private Node overview(){
        var box=new VBox(14);
        if(detail.lmsr()!=null){box.getChildren().add(UiTables.label("LMSR b: "+detail.lmsr().b()+"   Purchase commission total: "+WireViewMapper.money(detail.lmsr().commissionTotal()),null));box.getChildren().add(UiTables.table("Outcome prices",detail.lmsr().options(),List.of(UiTables.text("Option",p->WireViewMapper.option(p.option(),detail.optionLabels())),UiTables.money("Price",LmsrOptionDto::price),UiTables.number("Shares bought",p->Integer.toString(p.sharesBought())))));}
        if(detail.book()!=null){var book=detail.book();box.getChildren().add(UiTables.label("Payout d: "+book.d()+"   Initial funding: "+book.initial()+"   Mint: "+book.allowMint()+"   Issued pairs: "+book.issuedPairs(),null));
            var books=new FlowPane(14,14);for(int i=1;i<=2;i++){final int selected=i;var orders=book.orders().stream().filter(o->o.option()==selected).toList();var one=new VBox(10,UiTables.label(WireViewMapper.option(i,detail.optionLabels()),"subheading"));
                book.quotes().stream().filter(q->q.option()==selected).findFirst().ifPresent(q->one.getChildren().add(UiTables.label("BID "+WireViewMapper.money(q.bid())+"   ASK "+WireViewMapper.money(q.ask())+"\nLAST "+WireViewMapper.money(q.last())+"   MID "+WireViewMapper.money(q.mid())+"\nSPREAD "+WireViewMapper.money(q.spread())+"   Basis: "+q.valuationBasis(),"muted")));
                one.getChildren().add(UiTables.table("Orders",orders,List.of(UiTables.text("Side",o->o.side().name()),UiTables.text("User",BookOrderDto::user),UiTables.number("Remaining",o->Integer.toString(o.remainingQuantity())),UiTables.money("Limit",BookOrderDto::limitPrice))));one.setMinWidth(0);one.prefWidthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(()->books.getWidth()>=800?(books.getWidth()-14)/2:books.getWidth(),books.widthProperty()));books.getChildren().add(one);}
            box.getChildren().add(books);
            box.getChildren().add(UiTables.table("Participants",book.participants(),List.of(UiTables.text("User",ParticipantDto::username),UiTables.number(WireViewMapper.option(1,detail.optionLabels())+" shares",p->Integer.toString(p.optionOneShares())),UiTables.number(WireViewMapper.option(2,detail.optionLabels())+" shares",p->Integer.toString(p.optionTwoShares())),UiTables.money("Option 1 value",ParticipantDto::optionOneValue),UiTables.money("Option 2 value",ParticipantDto::optionTwoValue))));
        }
        return box;
    }
    private Node position(){var p=detail.ownPosition();if(p==null)return UiTables.label("No personal position in this event.","muted");var box=new VBox(12);
        box.getChildren().add(UiTables.table("Your shares",List.of(1,2),List.of(UiTables.text("Option",n->WireViewMapper.option(n,detail.optionLabels())),UiTables.number("Held",n->Integer.toString(n==1?p.optionOneShares():p.optionTwoShares())),UiTables.number("Reserved",n->Integer.toString(n==1?p.reservedOne():p.reservedTwo())),UiTables.number("Available",n->Integer.toString(n==1?p.availableOne():p.availableTwo())),UiTables.money("Paid",n->n==1?p.paidOne():p.paidTwo()),UiTables.money("Value",n->n==1?p.valueOne():p.valueTwo()))));
        var amounts=new LinkedHashMap<String,String>();amounts.put("Funding",p.funding());amounts.put("Purchases",p.purchases());amounts.put("Sale receipts",p.saleReceipts());amounts.put("Fees paid",p.feesPaid());amounts.put("Fees received",p.feesReceived());amounts.put("Payout",p.payout());amounts.put("Profit/loss",p.profitLoss());box.getChildren().add(UiTables.table("Your event cash",new ArrayList<>(amounts.entrySet()),List.of(UiTables.text("Kind",Map.Entry::getKey),UiTables.money("Amount",Map.Entry::getValue))));return box;
    }
    private Node history(){if(detail.lmsr()!=null)return UiTables.table("LMSR trades",detail.lmsr().history(),List.of(UiTables.number("Sequence",p->Long.toString(p.sequence())),UiTables.text("User",LmsrTradeDto::user),UiTables.number("Option",p->Integer.toString(p.option())),UiTables.number("Quantity",p->Integer.toString(p.quantity())),UiTables.money("Principal",LmsrTradeDto::shareCost),UiTables.money("Commission",LmsrTradeDto::commission),UiTables.money("Total paid",LmsrTradeDto::totalPaid)));
        return executions(detail.book().trades().reversed());}
    private Node settlement(){var s=detail.settlement();if(s==null)return UiTables.label("This event has not been settled.","muted");return new VBox(12,UiTables.label("Winner: "+s.winningOption()+": "+(s.winningLabel().isEmpty()?"(empty label)":s.winningLabel())+"\nFinal contract balance: "+WireViewMapper.money(s.finalContractBalance())+"\nLifetime commission: "+WireViewMapper.money(s.commissionTotal())+"\nGross payout: "+WireViewMapper.money(s.totalGrossPayout())+"   Closing commission: "+WireViewMapper.money(s.totalCommission())+"\nSubsidy refund: "+WireViewMapper.money(s.subsidyRefund()),"subheading"),payouts(s.payouts()));}
    static VBox executions(List<ExecutionDto> rows){return UiTables.table("Executions",rows,List.of(UiTables.number("Sequence",p->Long.toString(p.sequence())),UiTables.number("Option",p->Integer.toString(p.option())),UiTables.text("Buyer",ExecutionDto::buyer),UiTables.text("Counterparty",ExecutionDto::counterparty),UiTables.text("Role",p->p.counterpartyRole()==CounterpartyRole.OPPOSITE_BUYER?"Opposite buyer":"Seller"),UiTables.number("Quantity",p->Integer.toString(p.quantity())),UiTables.money("Price",ExecutionDto::price),UiTables.money("Principal",ExecutionDto::principal),UiTables.money("Commission",ExecutionDto::commission)));}
    static VBox payouts(List<PayoutDto> rows){return UiTables.table("Payouts",rows,List.of(UiTables.text("User",PayoutDto::username),UiTables.number("Winning shares",p->Integer.toString(p.winningShares())),UiTables.money("Gross",PayoutDto::grossPayout),UiTables.money("Commission",PayoutDto::commission),UiTables.money("Net",PayoutDto::netPayout)));}
}
