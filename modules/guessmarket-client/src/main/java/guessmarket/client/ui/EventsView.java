package guessmarket.client.ui;
import guessmarket.client.session.RemoteMarketSession;
import guessmarket.dto.api.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.KeyCode;
import java.util.*;
import java.util.function.Predicate;
public final class EventsView extends BorderPane {
    private final RemoteMarketSession session;
    private final ListView<EventSummaryDto> list=new ListView<>();
    private final VBox listPanel=new VBox(12),detailPanel=new VBox(10);
    private final EventDetailView detail;
    private final ScrollPane detailScroll;
    private final Label loading=UiTables.label("Loading event details...","muted");
    private final ComboBox<String> method=filter("Method","All","LMSR","ORDER_BOOK"),status=filter("Status","All","NOT_STARTED","ACTIVE","CLOSED"),commission=filter("Commission","All","ON_PURCHASE","ON_CLOSE");
    private Predicate<EventSummaryDto> membership=e->true;
    private DashboardResponse current;
    private Integer selected;
    private boolean detailActive,updating;
    public EventsView(RemoteMarketSession session,EventNavigationState navigation,boolean global){
        this.session=session;detail=new EventDetailView(session,navigation);detailScroll=new ScrollPane(detail);detailScroll.setFitToWidth(true);detail.setMinWidth(0);detail.prefWidthProperty().bind(detailScroll.widthProperty().subtract(18));UiFocus.keepVisible(detailScroll);VBox.setVgrow(detailScroll,Priority.ALWAYS);setMinHeight(420);
        if(global){var filters=new FlowPane(10,10,filterField("Method",method),filterField("Status",status),filterField("Commission",commission));listPanel.getChildren().add(filters);for(var combo:List.of(method,status,commission))combo.valueProperty().addListener((o,a,b)->updateList());}
        list.setId(global?"events-list":"account-events-list");list.setAccessibleText(global?"All market events":"Account events");list.setPlaceholder(UiTables.label("No matching events. Upload XML to add events.","muted"));VBox.setVgrow(list,Priority.ALWAYS);listPanel.getChildren().add(list);listPanel.setMinWidth(0);
        list.setCellFactory(v->new ListCell<>(){@Override protected void updateItem(EventSummaryDto event,boolean empty){super.updateItem(event,empty);if(empty||event==null){setGraphic(null);setText(null);return;}var name=UiTables.label(WireViewMapper.eventName(event),"card-title");var metadata=UiTables.label(WireViewMapper.words(event.method())+"   "+WireViewMapper.words(event.status())+"\nOwner: "+event.owner()+"   Contract: "+WireViewMapper.money(event.contractBalance())+"\nFee: "+event.commissionPercentage()+"% "+WireViewMapper.words(event.commissionMode()),"muted");var row=new VBox(8,name,metadata);row.prefWidthProperty().bind(list.widthProperty().subtract(60));setGraphic(row);setText(null);setTooltip(new Tooltip(event.name()+"\n"+event.description()));}});
        list.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{if(!updating&&b!=null)open(b.id());});list.setOnMouseClicked(e->{if(e.getClickCount()==1&&list.getSelectionModel().getSelectedItem()!=null)open(list.getSelectionModel().getSelectedItem().id());});list.setOnKeyPressed(e->{if(e.getCode()==KeyCode.ENTER&&list.getSelectionModel().getSelectedItem()!=null){open(list.getSelectionModel().getSelectedItem().id());e.consume();}});
        var back=new Button("Back to event list");back.setId("detail-back");back.setOnAction(e->{detailActive=false;session.selectEvent(null);layoutPanes();list.requestFocus();});loading.setManaged(false);loading.setVisible(false);detailPanel.getChildren().addAll(back,loading,detailScroll);
        widthProperty().addListener((o,a,b)->layoutPanes());layoutPanes();
    }
    private static ComboBox<String> filter(String name,String...values){var combo=new ComboBox<String>();combo.getItems().setAll(values);combo.setValue("All");combo.setAccessibleText(name+" filter");return combo;}
    private static VBox filterField(String name,ComboBox<String> control){control.setPrefWidth(145);return new VBox(5,UiTables.label(name,"muted"),control);}
    public void membership(Predicate<EventSummaryDto> filter){membership=filter;updateList();}
    public void update(DashboardResponse snapshot){current=snapshot;updateList();if(detailActive&&selected!=null&&snapshot.selectedEvent()!=null&&snapshot.selectedEvent().summary().id()==selected){detail.showDetail(snapshot.selectedEvent(),snapshot.account().username());loading(false);}}
    private void updateList(){if(current==null)return;var filtered=current.events().stream().filter(membership).filter(e->method.getValue().equals("All")||method.getValue().equals(e.method())).filter(e->status.getValue().equals("All")||status.getValue().equals(e.status())).filter(e->commission.getValue().equals("All")||commission.getValue().equals(e.commissionMode())).toList();
        if(!list.getItems().equals(filtered)){updating=true;try{list.getItems().setAll(filtered);if(selected!=null)filtered.stream().filter(e->e.id()==selected).findFirst().ifPresent(e->list.getSelectionModel().select(e));}finally{updating=false;}}
    }
    public void open(int id){selected=id;detailActive=true;session.selectEvent(id);if(current!=null&&current.selectedEvent()!=null&&current.selectedEvent().summary().id()==id){detail.showDetail(current.selectedEvent(),current.account().username());loading(false);}else loading(true);layoutPanes();}
    public void activate(){detail.clearError();if(detailActive&&selected!=null)session.selectEvent(selected);else session.selectEvent(null);}
    public void clear(){updating=true;try{current=null;selected=null;detailActive=false;list.getItems().clear();detail.clear();loading(false);layoutPanes();}finally{updating=false;}}
    private void loading(boolean value){loading.setVisible(value);loading.setManaged(value);detailScroll.setVisible(!value);detailScroll.setManaged(!value);}
    private void layoutPanes(){if(getWidth()<900){listPanel.setMinWidth(0);listPanel.setMaxWidth(Double.MAX_VALUE);setCenter(detailActive?detailPanel:listPanel);}else{var row=new HBox(20,listPanel,detailPanel);listPanel.setMinWidth(300);listPanel.setPrefWidth(320);listPanel.setMaxWidth(320);detailPanel.setPrefWidth(Math.max(200,getWidth()-340));HBox.setHgrow(detailPanel,Priority.ALWAYS);detailPanel.setMinWidth(0);setCenter(row);}}
}
