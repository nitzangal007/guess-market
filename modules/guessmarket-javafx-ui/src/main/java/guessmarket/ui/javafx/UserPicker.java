package guessmarket.ui.javafx;

import guessmarket.dto.world.UserSnapshot;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

final class UserPicker {
    private UserPicker() { }
    static void show(Window owner,List<UserSnapshot> users,Consumer<UserSnapshot> chosen){
        Dialog<UserSnapshot> dialog=new Dialog<>();
        PurchaseDialog.setup(dialog,owner,"Change user","Choose the account to inspect or act as","userPicker");
        TextField search=new TextField();search.setId("userSearch");search.setPromptText("Type part of a name");
        Label count=EventDetailsView.label("","muted");count.setId("userResultCount");
        ListView<UserSnapshot> results=new ListView<>();results.setId("userList");results.setPrefHeight(280);
        results.setCellFactory(view->new ListCell<>(){
            @Override protected void updateItem(UserSnapshot user,boolean empty){
                super.updateItem(user,empty);setText(null);
                if(empty||user==null)setGraphic(null);
                else {Label label=EventDetailsView.label(user.name(),"description");label.maxWidthProperty().bind(widthProperty().subtract(32));setGraphic(label);}
            }
        });
        results.setPlaceholder(EventDetailsView.label("No matching users.","muted"));
        Runnable filter=()->{
            String term=search.getText().toLowerCase(Locale.ROOT);
            results.getItems().setAll(users.stream().filter(u->u.name().toLowerCase(Locale.ROOT).contains(term)).toList());
            count.setText(results.getItems().size()+" matching users");
        };
        search.textProperty().addListener((o,before,value)->filter.run());filter.run();
        dialog.getDialogPane().setContent(new VBox(12,EventDetailsView.label("Find a user","muted"),search,count,results));
        ButtonType select=new ButtonType("Use selected user",ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(select,ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(select).disableProperty().bind(results.getSelectionModel().selectedItemProperty().isNull());
        dialog.setResultConverter(button->button==select?results.getSelectionModel().getSelectedItem():null);
        dialog.setOnHidden(action->{if(dialog.getResult()!=null)chosen.accept(dialog.getResult());});
        dialog.setOnShown(action->search.requestFocus());dialog.show();
    }
}
