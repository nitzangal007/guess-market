package guessmarket.ui.javafx;

import guessmarket.dto.world.OpeningPreview;
import java.util.function.Consumer;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** Confirmation displays Engine values and returns only the user's decision. */
final class OpeningDialog {
    private OpeningDialog() { }
    static void show(Window owner,OpeningPreview preview,Consumer<Boolean> decision){
        Dialog<Boolean> dialog=new Dialog<>();
        PurchaseDialog.setup(dialog,owner,"Review opening",preview.eventName(),"openingDialog");
        VBox content=new VBox(12,
                PurchaseDialog.field("Acting user",preview.actingUser(),"openingUser"),
                PurchaseDialog.field("Current balance",EventDetailsView.money(preview.currentBalance()),"openingBefore"),
                PurchaseDialog.field("Required funding",EventDetailsView.money(preview.requiredFunding()),"openingFunding"),
                PurchaseDialog.field("Balance after opening",EventDetailsView.money(preview.balanceAfterOpening()),"openingAfter"),
                EventDetailsView.label("This transfers the required opening funding to the event. No purchase commission is charged.","muted"));
        ScrollPane scroll=new ScrollPane(content);scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);scroll.setPrefViewportHeight(300);
        dialog.getDialogPane().setContent(scroll);
        PurchaseDialog.confirm(dialog,"Confirm opening",decision);
    }
}
