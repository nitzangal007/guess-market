package guessmarket.ui.javafx;

import guessmarket.dto.world.*;
import java.util.List;
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

final class UsersController {
    private enum Section { AVAILABLE, OWNED, PARTICIPATIONS }
    private final WorldSession session;
    @FXML private Label userName,userBalance,blockedStatus,purchaseNotice,sectionTitle,userEmpty,userCount;
    @FXML private Button changeUser;
    @FXML private ToggleButton availableSection,ownedSection,participationsSection;
    @FXML private FlowPane userEventRows;
    @FXML private VBox userCatalogue,userDetails;
    @FXML private ScrollPane userScroll;
    private final ToggleGroup rows=new ToggleGroup(),sections=new ToggleGroup();
    private String selectedUser;
    private Section section=Section.AVAILABLE;
    private Integer displayedEventId;
    private double listScroll;
    private String actionContext="",receiptContext="";

    UsersController(WorldSession session){this.session=session;}
    @FXML private void initialize(){
        changeUser.disableProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(
                ()->session.busyProperty().get()||session.worldProperty().get().users().isEmpty(),session.busyProperty(),session.worldProperty()));
        userEventRows.disableProperty().bind(session.busyProperty());
        for(var button:List.of(availableSection,ownedSection,participationsSection)){
            button.setToggleGroup(sections);button.disableProperty().bind(session.busyProperty());
        }
        availableSection.setOnAction(a->chooseSection(Section.AVAILABLE));
        ownedSection.setOnAction(a->chooseSection(Section.OWNED));
        participationsSection.setOnAction(a->chooseSection(Section.PARTICIPATIONS));
        session.noticeProperty().addListener((o,before,value)->{
            if(!value.isEmpty())receiptContext=actionContext;
            purchaseNotice.setText(value.isEmpty()?"":(receiptContext.isEmpty()?"":receiptContext+"\n")+value);
        });
        purchaseNotice.visibleProperty().bind(session.noticeProperty().isNotEmpty());
        purchaseNotice.managedProperty().bind(purchaseNotice.visibleProperty());
        session.worldProperty().addListener((o,before,world)->renderWorld());
        renderWorld();
    }
    @FXML private void changeUser(){
        UserPicker.show(changeUser.getScene().getWindow(),session.worldProperty().get().users(),user->{
            if(!Objects.equals(selectedUser,user.name())){
                selectedUser=user.name();displayedEventId=null;actionContext="";receiptContext="";
                session.dismissTransientFeedback();renderUser(null,0);
            }
            changeUser.requestFocus();
        });
    }
    private UserSnapshot user(){
        return session.worldProperty().get().users().stream().filter(u->u.name().equals(selectedUser)).findFirst().orElse(null);
    }
    private EventSnapshot event(int id){
        return session.worldProperty().get().events().stream().filter(e->e.id()==id).findFirst().orElse(null);
    }
    private void renderWorld(){
        Integer keep=session.preserveSelections()?displayedEventId:null;
        int inner=0;
        if(keep!=null && userDetails.lookup("#user-eventSections") instanceof TabPane tabs)inner=tabs.getSelectionModel().getSelectedIndex();
        if(!session.preserveSelections()){
            section=Section.AVAILABLE;displayedEventId=null;listScroll=0;actionContext="";receiptContext="";
            selectedUser=session.worldProperty().get().users().isEmpty()?null:session.worldProperty().get().users().getFirst().name();
        }
        renderUser(keep,inner);
    }
    private void renderUser(Integer focused,int inner){
        var user=user();
        userName.setText(user==null?"Select a user":user.name());
        userBalance.setText(user==null?"":EventDetailsView.money(user.currentBalance()));
        boolean blocked=user!=null&&user.blocked();
        blockedStatus.setText(blocked?"Blocked after an overdraft purchase. A receipt that leaves your balance above zero restores buying and opening. You may inspect records and close an active event you own.":"");
        blockedStatus.setVisible(blocked);blockedStatus.setManaged(blocked);
        purchaseNotice.getStyleClass().removeAll("warning","notice");purchaseNotice.getStyleClass().add(blocked?"warning":"notice");
        ToggleButton active=switch(section){case AVAILABLE->availableSection;case OWNED->ownedSection;case PARTICIPATIONS->participationsSection;};
        sections.selectToggle(active);sectionTitle.setText(active.getText());
        Integer selected=rows.getSelectedToggle()==null?null:Integer.valueOf(((ToggleButton)rows.getSelectedToggle()).getId().substring("user-event-".length()));
        rows.getToggles().clear();userEventRows.getChildren().clear();
        var membership=user==null?List.<EventSnapshot>of():session.worldProperty().get().events().stream().filter(e->switch(section){
            case AVAILABLE->e.status()==WorldEventStatus.ACTIVE;
            case OWNED->user.ownedEventIds().contains(e.id());
            case PARTICIPATIONS->user.positions().stream().anyMatch(p->p.eventId()==e.id());
        }).toList();
        for(var event:membership){
            EventRowView row=new EventRowView(event,userEventRows,rows,this::showEvent,"user-event-");
            boolean owner=event.marketMakerName().equals(user.name());
            boolean participant=user.participatingEventIds().contains(event.id());
            boolean history=event.status()==WorldEventStatus.CLOSED&&user.positions().stream().anyMatch(p->p.eventId()==event.id());
            if(owner)row.relationship("Owner","owner-event-"+event.id());
            if(participant)row.relationship("Active / Participating","participation-event-"+event.id());
            if(history)row.relationship("Closed / Your history","history-event-"+event.id());
            userEventRows.getChildren().add(row);
        }
        if(!membership.isEmpty()){
            int selectedId=membership.stream().filter(e->Integer.valueOf(e.id()).equals(selected)).findFirst().orElse(membership.getFirst()).id();
            selectRow(selectedId,false);
        }
        userCount.setText(membership.size()+" events");
        userEmpty.setText(switch(section){
            case AVAILABLE->"No active events. An event's market maker can review funding from Owned events.";
            case OWNED->"No owned events for this user.";
            case PARTICIPATIONS->"No participation yet. Active participation and closed history will appear here.";
        });
        userEmpty.setVisible(membership.isEmpty());userEmpty.setManaged(membership.isEmpty());
        if(focused!=null&&event(focused)!=null){
            showEvent(event(focused));
            var tabs=(TabPane)userDetails.lookup("#user-eventSections");
            if(tabs!=null)tabs.getSelectionModel().select(Math.min(inner,tabs.getTabs().size()-1));
        }else showCatalogue(false);
    }
    private void chooseSection(Section next){
        session.dismissTransientFeedback();
        section=next;displayedEventId=null;listScroll=0;renderUser(null,0);
    }
    private void showEvent(EventSnapshot event){
        var user=user();if(user==null)return;
        if(!session.preserveSelections())session.dismissTransientFeedback();
        if(displayedEventId==null)listScroll=userScroll.getVvalue();
        displayedEventId=event.id();
        Button back=new Button("Back to "+sectionTitle.getText());back.setId("usersBack");
        back.disableProperty().bind(session.busyProperty());back.setOnAction(a->showCatalogue(true));
        var details=new EventDetailsView(event,"user-",user);
        var detailTabs=details.getChildren().stream().filter(TabPane.class::isInstance)
                .map(TabPane.class::cast).findFirst().orElseThrow();
        detailTabs.getSelectionModel().selectedItemProperty().addListener((o,before,after)->{
            if(!session.preserveSelections())session.dismissTransientFeedback();
        });
        FlowPane actions=new FlowPane(12,12);actions.setId("userActions");
        {
            if(event.status()==WorldEventStatus.NOT_STARTED&&event.marketMakerName().equals(user.name())){
                Button open=action("Open event","openEvent",user.blocked());actions.getChildren().add(open);
                open.setOnAction(a->{rememberAction(event,user);session.previewOpening(user.name(),event.id(),
                        preview->OpeningDialog.show(open.getScene().getWindow(),preview,session::completeOpening));});
            }
            if(event.status()==WorldEventStatus.ACTIVE){
                if(event.pricing() instanceof OrderBookConfiguration){
                    Button submit=action("Submit order","submitOrder",user.blocked());actions.getChildren().add(submit);
                    submit.setOnAction(a->{rememberAction(event,user);promptOrder(submit,event,user,null);});
                }else{
                Button buy=action("Buy shares","buyShares",user.blocked());actions.getChildren().add(buy);
                buy.setOnAction(a->{rememberAction(event,user);PurchaseDialog.prompt(buy.getScene().getWindow(),event,user.name(),
                        choice->session.previewPurchase(user.name(),event.id(),choice.option(),choice.quantity(),
                                preview->PurchaseDialog.show(buy.getScene().getWindow(),preview,session::completePurchase)));});
                }
                if(event.marketMakerName().equals(user.name())){
                    Button close=action("Close event","closeEvent",false);actions.getChildren().add(close);
                    close.setOnAction(a->{rememberAction(event,user);CloseDialog.prompt(close.getScene().getWindow(),event,user.name(),
                            winner->session.previewClose(user.name(),event.id(),winner,
                                    preview->CloseDialog.show(close.getScene().getWindow(),preview,event.pricing() instanceof OrderBookConfiguration,session::completeClose)));});
                }
            }
        }
        String relationship=(event.marketMakerName().equals(user.name())?"Owner":"Not owner")
                +(user.positions().stream().anyMatch(p->p.eventId()==event.id())?" / Participant":" / No participation yet");
        FlowPane toolbar=new FlowPane(16,12,back,EventDetailsView.label(relationship,"relationship"),actions);
        toolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        userDetails.getChildren().setAll(toolbar,details);
        userCatalogue.setVisible(false);userCatalogue.setManaged(false);
        userDetails.setVisible(true);userDetails.setManaged(true);userScroll.setVvalue(0);back.requestFocus();
    }
    private Button action(String caption,String id,boolean blocked){
        Button button=new Button(caption);button.setId(id);
        button.disableProperty().bind(session.busyProperty().or(new javafx.beans.property.SimpleBooleanProperty(blocked)));
        return button;
    }
    private void promptOrder(Button button,EventSnapshot event,UserSnapshot user,OrderRequest initial){
        OrderDialog.prompt(button.getScene().getWindow(),event,user,initial,request->session.previewOrder(request,
                preview->OrderDialog.show(button.getScene().getWindow(),event,preview,decision->{
                    session.completeOrder(decision==OrderDialog.Decision.CONFIRM);
                    if(decision==OrderDialog.Decision.BACK)promptOrder(button,event,user,request);
                })));
    }
    private void rememberAction(EventSnapshot event,UserSnapshot user){actionContext=user.name()+" / Event "+event.id()+" / "+event.name();}
    private void selectRow(int id,boolean focus){
        for(var toggle:rows.getToggles())if(("user-event-"+id).equals(((ToggleButton)toggle).getId())){
            rows.selectToggle(toggle);if(focus)((ToggleButton)toggle).requestFocus();return;
        }
        if(focus){if(rows.getSelectedToggle()!=null)((ToggleButton)rows.getSelectedToggle()).requestFocus();else userEmpty.requestFocus();}
    }
    private void showCatalogue(boolean restore){
        if(!session.preserveSelections())session.dismissTransientFeedback();
        Integer previous=displayedEventId;displayedEventId=null;
        userDetails.getChildren().clear();userDetails.setVisible(false);userDetails.setManaged(false);
        userCatalogue.setVisible(true);userCatalogue.setManaged(true);
        if(restore){userScroll.setVvalue(listScroll);if(previous!=null)selectRow(previous,true);}
    }
}
