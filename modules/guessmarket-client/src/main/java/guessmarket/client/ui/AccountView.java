package guessmarket.client.ui;
import guessmarket.client.session.RemoteMarketSession;
import guessmarket.dto.api.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;
public final class AccountView extends ScrollPane {
    private final EventsView events;
    private final AccountActivityView activity;
    private final VBox content=new VBox(16),users=new VBox(12);
    private final Label hint=UiTables.label("Active events you can join, including owned events.","muted");
    private String section="Available";
    private DashboardResponse current;
    private List<PublicUserDto> oldUsers=List.of();
    public AccountView(RemoteMarketSession session,EventNavigationState navigation){
        setFitToWidth(true);events=new EventsView(session,navigation,false);activity=new AccountActivityView(events::open);
        var group=new ToggleGroup();var sections=new FlowPane(10,10);for(String name:List.of("Available","Owned","Participations")){var button=new ToggleButton(name);button.setToggleGroup(group);button.setSelected(name.equals(section));button.setOnAction(e->{section=name;hint.setText(name.equals("Available")?"Active events you can join, including owned events.":name.equals("Owned")?"Your events, including not-started events without a position.":"Your active and closed participation history.");membership();});sections.getChildren().add(button);}
        var publicUsers=new TitledPane("Public users (read only)",users);publicUsers.setExpanded(false);var journal=new TitledPane("Your account activity",activity);journal.setExpanded(true);
        content.getChildren().addAll(UiTables.label("Your account","section-title"),sections,hint,events,journal,publicUsers);setContent(content);
    }
    public void update(DashboardResponse snapshot){current=snapshot;membership();events.update(snapshot);activity.update(snapshot.account());if(!oldUsers.equals(snapshot.users())){oldUsers=snapshot.users();users.getChildren().setAll(UiTables.table("Public users",snapshot.users(),List.of(UiTables.text("Name",PublicUserDto::name),UiTables.money("Balance",PublicUserDto::balance),UiTables.text("Market maker",u->u.marketMaker()?"Yes":"No"))));}}
    private void membership(){if(current==null)return;var account=current.account();events.membership(e->section.equals("Available")?e.status().equals("ACTIVE"):section.equals("Owned")?account.ownedEventIds().contains(e.id()):account.participatingEventIds().contains(e.id()));}
    public void open(int id){events.open(id);}
    public void activate(){events.activate();}
    public void clear(){current=null;oldUsers=List.of();events.clear();activity.clear();users.getChildren().clear();}
}
