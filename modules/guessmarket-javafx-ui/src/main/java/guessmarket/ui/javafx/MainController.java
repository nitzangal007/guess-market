package guessmarket.ui.javafx;

import guessmarket.dto.world.EventSnapshot;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

final class MainController {
    private final WorldSession session;
    @FXML private Button loadButton,fileDetails;
    @FXML private Label loadedPath,loadError,eventCount,emptyMessage;
    @FXML private ProgressBar loadProgress;
    @FXML private FlowPane eventCards;
    @FXML private VBox details,eventCatalogue;
    @FXML private ScrollPane eventsScroll;
    @FXML private Tab usersTab;
    @FXML private ComboBox<String> methodFilter,statusFilter,commissionFilter;
    private final ToggleGroup selection=new ToggleGroup();
    private Integer displayedEventId;
    private double listScroll;

    MainController(WorldSession session){this.session=session;}
    @FXML private void initialize(){
        methodFilter.getItems().setAll("All","LMSR","Order Book");
        statusFilter.getItems().setAll("All","Not started","Active","Closed");
        commissionFilter.getItems().setAll("All","On purchase","On close");
        for(var filter:List.of(methodFilter,statusFilter,commissionFilter))filter.getSelectionModel().selectFirst();
        for(var filter:List.of(methodFilter,statusFilter,commissionFilter)){
            filter.disableProperty().bind(session.busyProperty());
            filter.valueProperty().addListener((o,before,value)->renderWorld(false));
        }
        loadButton.disableProperty().bind(session.busyProperty());
        eventCards.disableProperty().bind(session.busyProperty());
        usersTab.disableProperty().bind(session.worldProperty().map(world->world.users().isEmpty()));
        loadedPath.textProperty().bind(session.loadedPathProperty().map(path->path.isEmpty()?"No file loaded":Path.of(path).getFileName().toString()));
        fileDetails.disableProperty().bind(session.busyProperty().or(session.loadedPathProperty().isEmpty()));
        loadError.textProperty().bind(session.errorProperty());
        loadError.visibleProperty().bind(session.errorProperty().isNotEmpty());
        loadError.managedProperty().bind(loadError.visibleProperty());
        loadProgress.progressProperty().bind(session.progressProperty());
        loadProgress.visibleProperty().bind(session.busyProperty());
        loadProgress.managedProperty().bind(loadProgress.visibleProperty());
        session.worldProperty().addListener((o,before,world)->renderWorld(session.preserveSelections()));
        renderWorld(false);
    }
    @FXML private void chooseFile(){
        FileChooser chooser=new FileChooser();chooser.setTitle("Load Guess Market EX2 XML");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML files","*.xml"));
        File file=chooser.showOpenDialog(loadButton.getScene().getWindow());
        session.load(file==null?null:file.toPath());
    }
    @FXML private void showFileDetails(){
        Alert dialog=new Alert(Alert.AlertType.INFORMATION);
        dialog.initOwner(fileDetails.getScene().getWindow());dialog.setTitle("Loaded file");
        dialog.setHeaderText("Current world source");
        dialog.getDialogPane().setContent(EventDetailsView.label(session.loadedPathProperty().get(),"description"));
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("main.css").toExternalForm());
        dialog.setResizable(true);dialog.show();
    }
    private void renderWorld(boolean preserve){
        Integer keep=preserve?displayedEventId:null;
        Integer selected=selection.getSelectedToggle()==null?null:Integer.valueOf(((ToggleButton)selection.getSelectedToggle()).getId().substring("event-card-".length()));
        var all=session.worldProperty().get().events();
        var filtered=all.stream().filter(event->
                ("All".equals(methodFilter.getValue())||EventDetailsView.method(event).equals(methodFilter.getValue()))
                &&("All".equals(statusFilter.getValue())||EventDetailsView.status(event).equals(statusFilter.getValue()))
                &&("All".equals(commissionFilter.getValue())||
                    (event.commissionMode()==guessmarket.dto.CommissionMode.ON_PURCHASE?"On purchase":"On close").equals(commissionFilter.getValue()))).toList();
        selection.getToggles().clear();eventCards.getChildren().clear();
        eventCount.setText(filtered.size()+" / "+all.size()+" events");
        emptyMessage.setVisible(filtered.isEmpty());emptyMessage.setManaged(filtered.isEmpty());
        emptyMessage.setText(all.isEmpty()?"Load an EX2 XML file to explore its events.":"No events match these filters. Choose All to widen the results.");
        for(var event:filtered)eventCards.getChildren().add(createCard(event,eventCards,selection,this::showDetails,"event-card-"));
        if(!filtered.isEmpty()){
            var chosen=filtered.stream().filter(e->Integer.valueOf(e.id()).equals(selected)).findFirst().orElse(filtered.getFirst());
            selectRow(chosen.id());
        }
        var focused=all.stream().filter(e->Integer.valueOf(e.id()).equals(keep)).findFirst();
        if(focused.isPresent())showDetails(focused.get());
        else showCatalogue(false);
    }
    private void selectRow(int id){
        for(var toggle:selection.getToggles())if(("event-card-"+id).equals(((ToggleButton)toggle).getId()))selection.selectToggle(toggle);
    }
    static ToggleButton createCard(EventSnapshot event,FlowPane rows,ToggleGroup group,Consumer<EventSnapshot> view,String prefix){
        return new EventRowView(event,rows,group,view,prefix);
    }
    private void showDetails(EventSnapshot event){
        if(displayedEventId==null)listScroll=eventsScroll.getVvalue();
        displayedEventId=event.id();
        Button back=new Button("Back to Events");back.setId("eventsBack");
        back.disableProperty().bind(session.busyProperty());back.setOnAction(action->showCatalogue(true));
        details.getChildren().setAll(back,new EventDetailsView(event));
        eventCatalogue.setVisible(false);eventCatalogue.setManaged(false);
        details.setVisible(true);details.setManaged(true);
        eventsScroll.setVvalue(0);back.requestFocus();
    }
    private void showCatalogue(boolean restore){
        Integer previous=displayedEventId;displayedEventId=null;
        details.getChildren().clear();details.setVisible(false);details.setManaged(false);
        eventCatalogue.setVisible(true);eventCatalogue.setManaged(true);
        if(restore){
            if(previous!=null)selectRow(previous);
            eventsScroll.setVvalue(listScroll);
            if(selection.getSelectedToggle()!=null)((ToggleButton)selection.getSelectedToggle()).requestFocus();
            else emptyMessage.requestFocus();
        }
    }
}

