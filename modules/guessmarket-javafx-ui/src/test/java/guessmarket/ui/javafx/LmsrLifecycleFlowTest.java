package guessmarket.ui.javafx;

import guessmarket.engine.GuessMarketWorldEngineImpl;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.stage.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class LmsrLifecycleFlowTest {
    @Test void positiveCashRecoveredOwnerWithoutPositionCanInspectAndClose(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        Path fixture=Path.of("modules/guessmarket-engine/src/test/resources/guessmarket/engine/xml/ex2/fixtures/multiple.xml");
        Path input=directory.resolve("blocked owner.xml");
        java.nio.file.Files.writeString(input,java.nio.file.Files.readString(fixture)
                .replace("<initial-cash>10000</initial-cash>","<initial-cash>210</initial-cash>")
                .replace("<commission type=\"on-purchase\">5</commission>","<commission type=\"on-purchase\">90</commission>"));
        WorldSession session=FxTestSupport.fx(()->new WorldSession(new GuessMarketWorldEngineImpl()));
        Stage[] stage=new Stage[1];
        try {
            Parent root=FxTestSupport.fx(()->{
                var loader=new FXMLLoader(getClass().getResource("/guessmarket/ui/javafx/main.fxml"));
                loader.setControllerFactory(type->{try{return type.getDeclaredConstructor(WorldSession.class).newInstance(session);}
                    catch(ReflectiveOperationException e){throw new IllegalStateException(e);}});
                Parent view=loader.load();stage[0]=new Stage();stage[0].setScene(new Scene(view,1280,800));stage[0].show();return view;
            });
            FxTestSupport.fx(()->{session.load(input);return null;});FxTestSupport.awaitIdle(session);
            for(int id:new int[]{1,4}){
                FxTestSupport.fx(()->{session.previewOpening("Tikva",id,p->session.completeOpening(true));return null;});FxTestSupport.awaitIdle(session);
            }
            FxTestSupport.fx(()->{session.previewPurchase("Tikva",1,1,3,p->session.completePurchase(true));return null;});FxTestSupport.awaitIdle(session);
            FxTestSupport.fx(()->{
                ((TabPane)root.lookup("#navigation")).getSelectionModel().select(1);root.applyCss();root.layout();
                FxTestSupport.chooseUser(root,1);FxTestSupport.viewUser(root,"ownedSection",4);
                assertFalse(root.lookup("#blockedStatus").isVisible());
                assertTrue(Double.parseDouble(label(root,"userBalance"))>0);
                assertFalse(root.lookup("#buyShares").isDisabled());assertFalse(root.lookup("#closeEvent").isDisabled());
                FxTestSupport.userDetail(root,"Your position");assertTrue(label(root,"positionSummary").contains("= 0"));
                FxTestSupport.userDetail(root,"Trade history");
                ((ComboBox<?>)root.lookup("#historyScope")).getSelectionModel().select(1);
                assertNotNull(root.lookup("#tradeHistory"));save(root,"blocked-owner-no-position.png");
                ((Button)root.lookup("#closeEvent")).fire();return null;
            });
            var winner=awaitDialog("closeInput");
            FxTestSupport.fx(()->{((ComboBox<?>)winner.lookup("#winningOption")).getSelectionModel().select(0);confirm(winner);return null;});
            var review=awaitDialog("closeDialog");
            FxTestSupport.fx(()->{confirm(review);return null;});FxTestSupport.awaitIdle(session);
            FxTestSupport.fx(()->{
                assertNull(root.lookup("#buyShares"));assertNull(root.lookup("#closeEvent"));
                assertFalse(root.lookup("#blockedStatus").isVisible());
                FxTestSupport.userDetail(root,"Settlement");assertEquals(0,((TableView<?>)root.lookup("#settlementPayments")).getItems().size());
                save(root,"closed-owner-no-position.png");
                ((Button)root.lookup("#usersBack")).fire();assertTrue(((ToggleButton)root.lookup("#user-event-4")).isSelected());
                FxTestSupport.chooseUser(root,2);FxTestSupport.viewUser(root,"availableSection",1);
                assertNull(root.lookup("#closeEvent"));assertFalse(root.lookup("#buyShares").isDisabled());
                return null;
            });
        } finally {FxTestSupport.fx(()->{session.close();if(stage[0]!=null)stage[0].close();return null;});}
    }
    @Test void selfMakerPreviewExplainsRecoveryWithPositiveFinalCash(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        var source=Path.of("modules/guessmarket-engine/src/test/resources/guessmarket/engine/xml/ex2/fixtures/small.xml");
        var input=directory.resolve("self maker.xml");
        java.nio.file.Files.writeString(input,java.nio.file.Files.readString(source)
                .replace("<initial-cash>10000</initial-cash>","<initial-cash>70</initial-cash>")
                .replace("<commission type=\"on-purchase\">5</commission>","<commission type=\"on-purchase\">90</commission>"));
        var engine=new GuessMarketWorldEngineImpl();
        engine.loadWorldFromXml(input);
        var opening=engine.previewLmsrOpening("Tikva",1);
        engine.openLmsrEvent("Tikva",1,opening.worldRevision());
        var before=engine.getWorldSnapshot();
        var preview=engine.previewLmsrPurchase("Tikva",1,1,1);
        assertFalse(preview.becomesBlocked());assertTrue(preview.overdraftBeforeReceipts());assertTrue(preview.balanceAfter()>0);
        AtomicReference<Stage> owner=new AtomicReference<>();
        var cancelled=new java.util.concurrent.atomic.AtomicBoolean(false);
        try {
            FxTestSupport.fx(()->{
                Stage window=new Stage();owner.set(window);window.setScene(new Scene(new javafx.scene.layout.VBox(),500,500));window.show();
                PurchaseDialog.show(window,preview,confirmed->cancelled.set(!confirmed));return null;
            });
            var pane=awaitDialog("purchaseDialog");
            FxTestSupport.fx(()->{
                assertTrue(Double.parseDouble(label(pane,"purchaseAfter"))>0);
                assertTrue(label(pane,"purchaseWarning").contains("positive cash"));
                assertFalse(label(pane,"purchaseFeeReceipt").isEmpty());
                assertBlockingWarningVisible(pane);
                save(pane,"self-mm-positive-but-blocked.png");
                ((Button)pane.lookupButton(ButtonType.CANCEL)).fire();return null;
            });
            assertTrue(cancelled.get());assertEquals(before,engine.getWorldSnapshot());
        } finally {FxTestSupport.fx(()->{if(owner.get()!=null)owner.get().close();return null;});}
    }
    @BeforeAll static void startFx() throws Exception { FxTestSupport.start(); }
    private static DialogPane awaitDialog(String id) throws Exception {
        long until=System.nanoTime()+5_000_000_000L;
        while(System.nanoTime()<until) {
            DialogPane found=FxTestSupport.fx(()-> {
                for(var window:Window.getWindows()) if(window.isShowing() && window.getScene()!=null
                        && window.getScene().getRoot() instanceof DialogPane pane && id.equals(pane.getId())) return pane;
                return null;
            });
            if(found!=null)return found;
            Thread.sleep(20);
        }
        throw new AssertionError("Dialog missing: "+id);
    }
    private static void confirm(DialogPane pane) {
        ButtonType type=pane.getButtonTypes().stream().filter(b->b!=ButtonType.CANCEL).findFirst().orElseThrow();
        ((Button)pane.lookupButton(type)).fire();
    }
    private static String label(Parent root,String id) {return ((Label)root.lookup("#"+id)).getText();}
    private static void assertBlockingWarningVisible(DialogPane pane) {
        pane.setPrefSize(680,592);((Stage)pane.getScene().getWindow()).sizeToScene();pane.applyCss();pane.layout();
        Label warning=(Label)pane.lookup("#purchaseWarning");
        for(Node node=warning.getParent();node!=null;node=node.getParent())
            assertFalse(node instanceof ScrollPane,"Critical consequences must not scroll out of view");
        assertTrue(warning.getHeight()+1>=warning.prefHeight(warning.getWidth()),"Complete warning must fit its label");
        var bounds=warning.localToScene(warning.getBoundsInLocal());
        var confirmType=pane.getButtonTypes().stream().filter(t->t!=ButtonType.CANCEL).findFirst().orElseThrow();
        Node confirm=pane.lookupButton(confirmType);
        assertTrue(bounds.getMaxY()<=confirm.localToScene(confirm.getBoundsInLocal()).getMinY());
        assertTrue(bounds.getMinY()>=0&&bounds.getMaxY()<=pane.getHeight());
        assertEquals(680,pane.getWidth(),1);assertEquals(592,pane.getHeight(),1);
    }
    private static void save(Parent root,String name) throws Exception {
        root.applyCss();root.layout();var image=root.snapshot(null,null);
        var png=new java.awt.image.BufferedImage((int)image.getWidth(),(int)image.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<png.getHeight();y++)for(int x=0;x<png.getWidth();x++)png.setRGB(x,y,image.getPixelReader().getArgb(x,y));
        var file=Path.of(System.getProperty("guessmarket.uiEvidenceDirectory",
                "private/coordination/assignment-2/ui-evidence-terminal-v01"),name);
        java.nio.file.Files.createDirectories(file.getParent());assertTrue(javax.imageio.ImageIO.write(png,"png",file.toFile()));
    }
    @Test void firstPurchaseOverdraftThenOwnerSettlementRecoversAccessAndKeepsHistoryInspectable() throws Exception {
        var engine=new GuessMarketWorldEngineImpl();
        WorldSession session=FxTestSupport.fx(()->new WorldSession(engine));
        AtomicReference<Stage> stage=new AtomicReference<>();
        try {
            Parent root=FxTestSupport.fx(()->{
                var loader=new FXMLLoader(getClass().getResource("/guessmarket/ui/javafx/main.fxml"));
                loader.setControllerFactory(type->{
                    try{return type.getDeclaredConstructor(WorldSession.class).newInstance(session);}
                    catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
                });
                Parent view=loader.load(); Stage window=new Stage(); stage.set(window);
                window.setScene(new Scene(view,1120,850)); window.show(); return view;
            });
            FxTestSupport.fx(()->{session.load(Path.of("modules/guessmarket-engine/src/test/resources/guessmarket/engine/xml/ex2/fixtures/small.xml"));return null;});
            FxTestSupport.awaitIdle(session);
            FxTestSupport.fx(()->{
                ((TabPane)root.lookup("#navigation")).getSelectionModel().select(1);root.applyCss();root.layout();
                FxTestSupport.chooseUser(root,1);
                FxTestSupport.viewUser(root,"ownedSection",1);
                ((Button)root.lookup("#openEvent")).fire();return null;
            });
            var opening=awaitDialog("openingDialog");
            FxTestSupport.fx(()->{confirm(opening);return null;});
            FxTestSupport.awaitIdle(session);
            FxTestSupport.fx(()->{
                FxTestSupport.chooseUser(root,2);
                FxTestSupport.viewUser(root,"availableSection",1);
                ((Button)root.lookup("#buyShares")).fire();return null;
            });
            var input=awaitDialog("purchaseInput");
            FxTestSupport.fx(()->{
                ((ComboBox<?>)input.lookup("#purchaseOption")).getSelectionModel().select(0);
                ((TextField)input.lookup("#purchaseQuantity")).setText("500");
                confirm(input);return null;
            });
            var quote=awaitDialog("purchaseDialog");
            var before=engine.getWorldSnapshot();
            FxTestSupport.fx(()->{
                assertTrue(label(quote,"purchaseWarning").contains("blocked"));
                assertBlockingWarningVisible(quote);
                save(quote,"purchase-warning.png");
                ((Button)quote.lookupButton(ButtonType.CANCEL)).fire();return null;
            });
            FxTestSupport.awaitIdle(session);assertEquals(before,engine.getWorldSnapshot());
            FxTestSupport.fx(()->{((Button)root.lookup("#buyShares")).fire();return null;});
            final DialogPane nextInput=awaitDialog("purchaseInput");
            FxTestSupport.fx(()->{
                ((ComboBox<?>)nextInput.lookup("#purchaseOption")).getSelectionModel().select(0);
                ((TextField)nextInput.lookup("#purchaseQuantity")).setText("500");
                confirm(nextInput);return null;
            });
            final DialogPane finalQuote=awaitDialog("purchaseDialog");
            FxTestSupport.fx(()->{confirm(finalQuote);return null;});
            FxTestSupport.awaitIdle(session);
            FxTestSupport.fx(()->{
                assertTrue(root.lookup("#blockedStatus").isVisible());
                assertTrue(((Button)root.lookup("#buyShares")).isDisabled());
                assertTrue(label(root,"purchaseNotice").contains("Purchase completed"));
                FxTestSupport.userDetail(root,"Your position");
                assertTrue(label(root,"positionSummary").contains("500"));
                save(root,"blocked-purchase.png");
                FxTestSupport.chooseUser(root,1);
                FxTestSupport.viewUser(root,"ownedSection",1);
                ((Button)root.lookup("#closeEvent")).fire();return null;
            });
            var winner=awaitDialog("closeInput");
            FxTestSupport.fx(()->{((ComboBox<?>)winner.lookup("#winningOption")).getSelectionModel().select(0);confirm(winner);return null;});
            var payouts=awaitDialog("closeDialog");
            assertTrue(FxTestSupport.fx(()->payouts.getHeaderText().contains("Acting as Tikva")));
            var beforeClose=engine.getWorldSnapshot();
            FxTestSupport.fx(()->{save(payouts,"settlement-preview.png");((Button)payouts.lookupButton(ButtonType.CANCEL)).fire();return null;});
            FxTestSupport.awaitIdle(session);
            assertEquals(beforeClose,engine.getWorldSnapshot());
            FxTestSupport.fx(()->{((Button)root.lookup("#closeEvent")).fire();return null;});
            var winnerAgain=awaitDialog("closeInput");
            FxTestSupport.fx(()->{((ComboBox<?>)winnerAgain.lookup("#winningOption")).getSelectionModel().select(0);confirm(winnerAgain);return null;});
            var payoutAgain=awaitDialog("closeDialog");
            FxTestSupport.fx(()->{confirm(payoutAgain);return null;});
            FxTestSupport.awaitIdle(session);
            FxTestSupport.fx(()->{
                FxTestSupport.chooseUser(root,2);
                FxTestSupport.viewUser(root,"participationsSection",1);
                FxTestSupport.userDetail(root,"Your position");
                root.applyCss();root.layout();
                assertFalse(root.lookup("#blockedStatus").isVisible());
                assertTrue(label(root,"user-eventState").contains("Closed"));
                assertTrue(label(root,"positionSummary").contains("500"));
                assertTrue(Double.parseDouble(label(root,"userBalance").replace(",",""))>0);
                save(root,"blocked-after-payout.png");
                ((TabPane)root.lookup("#navigation")).getSelectionModel().select(0);
                ((ToggleButton)root.lookup("#event-card-1")).fire();
                root.applyCss();root.layout();
                assertEquals("0.00",label(root,"contractBalance"));
                assertTrue(label(root,"eventState").contains("Closed"));
                var globalTabs=(TabPane)root.lookup("#eventSections");
                globalTabs.getSelectionModel().select(2);root.applyCss();root.layout();
                assertEquals(1,((TableView<?>)root.lookup("#settlementPayments")).getItems().size());
                save(root,"closed-event-settlement.png");
                stage.get().setWidth(640);stage.get().setHeight(750);
                return null;
            });
            Thread.sleep(200);
            FxTestSupport.fx(()->{save(root,"closed-events-narrow.png");return null;});
            FxTestSupport.fx(()->{
                ((ScrollPane)root.lookup("#eventsScroll")).setVvalue(1);root.applyCss();root.layout();
                var table=(TableView<?>)((TabPane)root.lookup("#eventSections")).getSelectionModel().getSelectedItem().getContent().lookup("#settlementPayments");
                ScrollBar horizontal=table.lookupAll(".scroll-bar").stream().filter(n->n instanceof ScrollBar bar
                        &&bar.getOrientation()==javafx.geometry.Orientation.HORIZONTAL).map(n->(ScrollBar)n).findFirst().orElseThrow();
                assertTrue(horizontal.isVisible());assertTrue(horizontal.getMax()>horizontal.getMin());
                horizontal.setValue(horizontal.getMax());return null;
            });
            Thread.sleep(150);
            FxTestSupport.fx(()->{
                var table=(TableView<?>)((TabPane)root.lookup("#eventSections")).getSelectionModel().getSelectedItem().getContent().lookup("#settlementPayments");root.applyCss();root.layout();
                var net=table.lookupAll(".table-cell").stream().filter(n->n instanceof TableCell<?,?> c
                        &&!c.isEmpty()&&"Net payout".equals(c.getTableColumn().getText())).findFirst().orElseThrow();
                var outer=(ScrollPane)root.lookup("#eventsScroll");
                var initial=net.localToScene(net.getBoundsInLocal());
                var outerBounds=outer.lookup(".viewport").localToScene(outer.lookup(".viewport").getBoundsInLocal());
                double excess=outer.getContent().getLayoutBounds().getHeight()-outer.getViewportBounds().getHeight();
                if(excess>0)outer.setVvalue(Math.max(0,Math.min(1,outer.getVvalue()
                        +(initial.getCenterY()-outerBounds.getCenterY())/excess)));
                root.layout();
                var cellBounds=net.localToScene(net.getBoundsInLocal());var tableBounds=table.localToScene(table.getBoundsInLocal());
                assertTrue(cellBounds.getMinX()>=tableBounds.getMinX()&&cellBounds.getMaxX()<=tableBounds.getMaxX());
                var viewport=root.lookup("#eventsScroll").lookup(".viewport");var visible=viewport.localToScene(viewport.getBoundsInLocal());
                save(root,"closed-events-narrow-net.png");
                assertTrue(cellBounds.getMinY()>=visible.getMinY()&&cellBounds.getMaxY()<=visible.getMaxY(),
                        "cell="+cellBounds+" viewport="+visible+" excess="+excess+" scroll="+outer.getVvalue());
                return null;
            });
        } finally {FxTestSupport.fx(()->{
            for(Window window:java.util.List.copyOf(Window.getWindows()))
                if(window instanceof Stage dialog && dialog!=stage.get() && dialog.getOwner()==stage.get())dialog.close();
            session.close(); if(stage.get()!=null)stage.get().close();return null;
        });}
    }
}


