package guessmarket.ui.javafx;
import guessmarket.dto.world.*;
import guessmarket.engine.GuessMarketWorldEngineImpl;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicReference;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

final class OrderBookUiFlowTest {
    @BeforeAll static void start()throws Exception{FxTestSupport.start();}
    @TempDir Path directory;
    Path fixture()throws Exception{
        return Files.writeString(directory.resolve("book.xml"),"""
                <Guess-Market><GM-events><GM-event name="Order Book demo"><id>1</id><description>Two outcomes</description>
                <commission type="on-purchase">5</commission><GM-options><GM-option>Yes</GM-option><GM-option>No</GM-option></GM-options>
                <GM-method><GM-order-book initial="100" d="1" allow-mint="true"/></GM-method></GM-event></GM-events>
                <GM-users><GM-user name="Owner"><initial-cash>1000</initial-cash><GM-market-maker><event id="1"/></GM-market-maker></GM-user>
                <GM-user name="Alexandra Elizabeth Montgomery with a long account name"><initial-cash>1000</initial-cash></GM-user></GM-users></Guess-Market>
                """);
    }
    @Test void calculatedTradeDisplaysRoundWithoutChangingExactLedger()throws Exception{
        var engine=new GuessMarketWorldEngineImpl();engine.loadWorldFromXml(fixture());
        engine.openOrderBookEvent("Owner",1,engine.previewOrderBookOpening("Owner",1).worldRevision());
        var sell=new OrderRequest("Owner",1,1,OrderSide.SELL,1,new BigDecimal("0.425"));
        engine.submitOrder(sell,engine.previewOrder(sell).worldRevision());
        var buy=new OrderRequest("Alexandra Elizabeth Montgomery with a long account name",1,1,OrderSide.BUY,1,new BigDecimal("0.425"));
        var preview=engine.previewOrder(buy);
        var snapshot=engine.submitOrder(buy,preview.worldRevision()).world();
        var trade=snapshot.events().getFirst().orderBook().orElseThrow().trades().getFirst();
        assertEquals(0,new BigDecimal("0.425").compareTo(trade.principal()));
        assertEquals(0,new BigDecimal("0.02125").compareTo(trade.commission()));
        Stage[] stage=new Stage[1];
        try{FxTestSupport.fx(()->{
            var event=snapshot.events().getFirst();
            var view=new EventDetailsView(event);var shell=new ScrollPane(view);
            stage[0]=new Stage();stage[0].setScene(new Scene(shell,1100,700));stage[0].show();
            var tabs=(TabPane)view.lookup("#eventSections");tabs.getSelectionModel().select(3);shell.applyCss();shell.layout();
            var history=(TableView<?>)view.lookup("#tradeHistory");
            assertEquals("0.43",history.getColumns().get(5).getCellData(0));
            assertEquals("0.43",history.getColumns().get(6).getCellData(0));
            assertEquals("0.02",history.getColumns().get(7).getCellData(0));
            save(shell,"calculated-history.png");
            var user=snapshot.users().stream().filter(u->u.name().equals(buy.userName())).findFirst().orElseThrow();
            var position=OrderBookView.position(event,user);
            assertEquals("0.43",((Label)position.lookup("#obPurchases")).getText());
            assertEquals("0.02",((Label)position.lookup("#obFees")).getText());
            assertEquals("0.43",((Label)position.lookup("#obValue")).getText());
            var participants=(TableView<?>)OrderBookView.participants(event).lookup("#participantsTable");
            int row=event.orderBook().orElseThrow().positions().stream().map(OrderBookPosition::userName).toList().indexOf(buy.userName());
            assertEquals("0.43",participants.getColumns().get(2).getCellData(row));
            assertEquals("0.43",participants.getColumns().get(7).getCellData(row));
            OrderDialog.show(stage[0],event,preview,decision->{});
            var review=(DialogPane)Window.getWindows().stream().filter(w->w.getScene()!=null&&"orderDialog".equals(w.getScene().getRoot().getId()))
                    .map(w->w.getScene().getRoot()).findFirst().orElseThrow();
            assertEquals("0.425",((Label)review.lookup("#orderLimit")).getText());
            var fills=(TableView<?>)review.lookup("#orderFills");
            assertEquals("0.43",fills.getColumns().get(6).getCellData(0));
            assertEquals("0.02",fills.getColumns().get(7).getCellData(0));
            press(review,"Cancel");return null;
        });}finally{FxTestSupport.fx(()->{if(stage[0]!=null)stage[0].close();return null;});}
        assertEquals(snapshot,engine.getWorldSnapshot());
    }
    @Test void serializedOrderPreviewCancelCommitAndCloseAreMethodAware()throws Exception{
        var path=fixture();var session=FxTestSupport.fx(()->new WorldSession(new GuessMarketWorldEngineImpl()));
        try{
            FxTestSupport.fx(()->{session.load(path);return null;});FxTestSupport.awaitIdle(session);
            FxTestSupport.fx(()->{session.previewOpening("Owner",1,p->session.completeOpening(true));return null;});FxTestSupport.awaitIdle(session);
            assertEquals(WorldEventStatus.ACTIVE,FxTestSupport.fx(()->session.worldProperty().get().events().getFirst().status()));
            var request=new OrderRequest("Owner",1,1,OrderSide.SELL,4,new BigDecimal("0.425"));
            var before=FxTestSupport.fx(()->session.worldProperty().get());
            FxTestSupport.fx(()->{session.previewOrder(request,p->{assertTrue(session.busyProperty().get());session.completeOrder(false);});return null;});FxTestSupport.awaitIdle(session);
            assertEquals(before,FxTestSupport.fx(()->session.worldProperty().get()));
            FxTestSupport.fx(()->{session.previewOrder(request,p->session.completeOrder(true));return null;});FxTestSupport.awaitIdle(session);
            assertEquals(4,FxTestSupport.fx(()->session.worldProperty().get().events().getFirst().orderBook().orElseThrow().sellReservations().getFirst().quantity()));
            FxTestSupport.fx(()->{session.previewClose("Owner",1,2,p->session.completeClose(true));return null;});FxTestSupport.awaitIdle(session);
            assertEquals(WorldEventStatus.CLOSED,FxTestSupport.fx(()->session.worldProperty().get().events().getFirst().status()));
        }finally{FxTestSupport.fx(()->{session.close();return null;});}
    }
    @Test void fourAndLongBooksKeepColumnsAlignedAndStackAtNarrowWidth()throws Exception{
        for(int count:new int[]{8,24}){
        var engine=new GuessMarketWorldEngineImpl();engine.loadWorldFromXml(fixture());
        var open=engine.previewOrderBookOpening("Owner",1);engine.openOrderBookEvent("Owner",1,open.worldRevision());
        for(int i=0;i<count;i++){
            var request=new OrderRequest("Owner",1,i%2+1,OrderSide.SELL,1,new BigDecimal("0.75"));
            engine.submitOrder(request,engine.previewOrder(request).worldRevision());
            var buy=new OrderRequest("Alexandra Elizabeth Montgomery with a long account name",1,i%2+1,OrderSide.BUY,1,new BigDecimal("0.10"));
            engine.submitOrder(buy,engine.previewOrder(buy).worldRevision());
        }
        var snapshot=engine.getWorldSnapshot();Stage[] stage=new Stage[1];
        try{
            FxTestSupport.fx(()->{
                var view=new EventDetailsView(snapshot.events().getFirst(),"",snapshot.users().getFirst());
                var scroll=new ScrollPane(view);scroll.setFitToWidth(true);stage[0]=new Stage();
                stage[0].setScene(new Scene(scroll,1120,820));stage[0].getScene().getStylesheets().add(getClass().getResource("/guessmarket/ui/javafx/main.css").toExternalForm());stage[0].show();
                var tabs=(TabPane)view.lookup("#eventSections");tabs.getSelectionModel().select(tabs.getTabs().stream().filter(t->t.getText().equals("Order books")).findFirst().orElseThrow());
                scroll.applyCss();scroll.layout();
                var table=(TableView<?>)view.lookup("#orderBook1");assertEquals(count,table.getItems().size());
                assertTrue(table.getHeight()<=460);assertEquals(4,table.getColumns().size());
                var completeRows=table.lookupAll(".table-row-cell").stream().filter(n->n instanceof TableRow<?> row&&!row.isEmpty()
                        &&row.getBoundsInParent().getMinY()>=0&&row.getBoundsInParent().getMaxY()<=table.getHeight()-45).count();
                assertTrue(completeRows>=4,"At least four complete rows must fit within each book");
                var first=view.lookup("#orderPanel1");var second=view.lookup("#orderPanel2");
                assertEquals(first.localToScene(first.getBoundsInLocal()).getMinY(),second.localToScene(second.getBoundsInLocal()).getMinY(),2);
                for(String tableId:new String[]{"orderBook1","orderBook2"}){
                    var keyboard=(TableView<?>)view.lookup("#"+tableId);assertTrue(keyboard.isFocusTraversable());
                    keyboard.requestFocus();assertSame(keyboard,keyboard.getScene().getFocusOwner());
                    keyboard.fireEvent(new javafx.scene.input.KeyEvent(javafx.scene.input.KeyEvent.KEY_PRESSED,"","",javafx.scene.input.KeyCode.DOWN,false,false,false,false));
                    assertTrue(keyboard.getSelectionModel().getSelectedIndex()>=0);
                }
                scroll.setVvalue(0.4);scroll.layout();
                save(scroll,count+"-rows-wide.png");
                stage[0].setWidth(680);stage[0].setHeight(602);return null;
            });
            FxTestSupport.fx(()->{
                var root=stage[0].getScene().getRoot();root.applyCss();root.layout();
                var a=root.lookup("#orderPanel1");var b=root.lookup("#orderPanel2");
                assertTrue(b.localToScene(b.getBoundsInLocal()).getMinY()>a.localToScene(a.getBoundsInLocal()).getMinY()+100);
                assertTrue(a.getBoundsInParent().getWidth()<660);
                var table=(TableView<?>)root.lookup("#orderBook1");table.scrollTo(count-1);root.applyCss();root.layout();
                if(count==24)assertTrue(table.lookupAll(".scroll-bar").stream().anyMatch(n->n instanceof ScrollBar bar&&bar.getOrientation()==javafx.geometry.Orientation.VERTICAL&&bar.isVisible()));
                save(root,count+"-rows-narrow.png");
                return null;
            });
        }finally{FxTestSupport.fx(()->{if(stage[0]!=null)stage[0].close();return null;});}
        }
    }
    @Test void globalParticipantsShowOwnerAndUnmatchedHoldingsValuesAndBasisAtNarrowWidth()throws Exception{
        checkParticipants("Yes","No");
    }
    @Test void customOutcomeParticipantsPreserveLabelsNumericAlignmentAndNarrowAccess()throws Exception{
        checkParticipants("Argentina","Spain");
        checkParticipants("Red outcome with a deliberately long descriptive name","Blue outcome with a deliberately long descriptive name");
    }
    private void checkParticipants(String one,String two)throws Exception{
        Path path=fixture();Files.writeString(path,Files.readString(path).replace("<GM-option>Yes</GM-option>","<GM-option>"+one+"</GM-option>")
                .replace("<GM-option>No</GM-option>","<GM-option>"+two+"</GM-option>"));
        var engine=new GuessMarketWorldEngineImpl();engine.loadWorldFromXml(path);
        var open=engine.previewOrderBookOpening("Owner",1);engine.openOrderBookEvent("Owner",1,open.worldRevision());
        var sell=new OrderRequest("Owner",1,1,OrderSide.SELL,2,new BigDecimal("0.50"));
        engine.submitOrder(sell,engine.previewOrder(sell).worldRevision());
        String longName="Alexandra Elizabeth Montgomery with a long account name";
        var buy=new OrderRequest(longName,1,1,OrderSide.BUY,1,new BigDecimal("0.30"));
        engine.submitOrder(buy,engine.previewOrder(buy).worldRevision());
        var snapshot=engine.getWorldSnapshot();Stage[] stage=new Stage[1];
        try{
            FxTestSupport.fx(()->{
                var view=new EventDetailsView(snapshot.events().getFirst());
                var scroll=new ScrollPane(view);scroll.setFitToWidth(true);stage[0]=new Stage();
                stage[0].setScene(new Scene(scroll,650,600));stage[0].getScene().getStylesheets().add(getClass().getResource("/guessmarket/ui/javafx/main.css").toExternalForm());stage[0].show();
                var tabs=(TabPane)view.lookup("#eventSections");
                var participants=tabs.getTabs().stream().filter(t->t.getText().equals("Participants")).findFirst().orElseThrow();
                tabs.getSelectionModel().select(participants);scroll.applyCss();scroll.layout();
                @SuppressWarnings("unchecked") var table=(TableView<OrderBookPosition>)view.lookup("#participantsTable");assertNotNull(table);
                assertEquals(2,table.getItems().size());
                assertEquals(java.util.List.of("Participant",one+" qty",one+" value",one+" basis",two+" qty",two+" value",two+" basis","Total value"),
                        table.getColumns().stream().map(TableColumn::getText).toList());
                for(var node:table.lookupAll(".column-header .label")){
                    var heading=(Label)node;
                    assertEquals(heading.getText(),((javafx.scene.text.Text)heading.lookup(".text")).getText(),"Headings must remain fully readable through contained horizontal scrolling");
                }
                var owner=table.getItems().stream().filter(p->p.userName().equals("Owner")).findFirst().orElseThrow();
                var unmatched=table.getItems().stream().filter(p->p.userName().equals(longName)).findFirst().orElseThrow();
                assertEquals(100,owner.optionOneShares());assertEquals(100,owner.optionTwoShares());
                assertEquals(0,unmatched.optionOneShares());assertEquals(0,unmatched.optionTwoShares());
                assertEquals("40",table.getColumns().get(2).getCellData(owner));assertEquals("MID",table.getColumns().get(3).getCellData(owner));
                assertEquals("Unavailable",table.getColumns().get(5).getCellData(owner));assertEquals("UNAVAILABLE",table.getColumns().get(6).getCellData(owner));
                assertEquals("0",table.getColumns().get(2).getCellData(unmatched));assertEquals("0",table.getColumns().get(5).getCellData(unmatched));
                assertTrue(table.isFocusTraversable());table.requestFocus();assertSame(table,table.getScene().getFocusOwner());
                table.fireEvent(new javafx.scene.input.KeyEvent(javafx.scene.input.KeyEvent.KEY_PRESSED,"","",javafx.scene.input.KeyCode.DOWN,false,false,false,false));
                assertTrue(table.getSelectionModel().getSelectedIndex()>=0);
                for(int index:new int[]{1,2,4,5,7}){
                    @SuppressWarnings("unchecked") var column=(TableColumn<OrderBookPosition,String>)table.getColumns().get(index);
                    assertEquals(javafx.geometry.Pos.TOP_RIGHT,column.getCellFactory().call(column).getAlignment());
                }
                assertTrue(table.lookupAll(".scroll-bar").stream().anyMatch(n->n instanceof ScrollBar bar&&bar.getOrientation()==javafx.geometry.Orientation.HORIZONTAL&&bar.isVisible()));
                assertTrue(table.getWidth()<650);assertEquals(420,table.getHeight(),2);
                var rows=table.lookupAll(".table-row-cell").stream().filter(n->n instanceof TableRow<?> row&&!row.isEmpty()).toList();
                assertTrue(rows.size()>=2);
                assertTrue(rows.stream().allMatch(n->n.getBoundsInParent().getHeight()>=40));
                var sorted=rows.stream().sorted(java.util.Comparator.comparingDouble(n->n.getBoundsInParent().getMinY())).toList();
                assertTrue(sorted.getFirst().getBoundsInParent().getMaxY()<=sorted.get(1).getBoundsInParent().getMinY()+1);
                table.lookupAll(".scroll-bar").stream().filter(n->n instanceof ScrollBar bar&&bar.getOrientation()==javafx.geometry.Orientation.HORIZONTAL).forEach(n->((ScrollBar)n).setValue(0));
                save(table,one.split(" ")[0]+"-participants-narrow.png");
                return null;
            });
            FxTestSupport.fx(()->{stage[0].setWidth(1120);return null;});
            FxTestSupport.fx(()->{var root=stage[0].getScene().getRoot();root.applyCss();root.layout();save(root.lookup("#participantsTable"),one.split(" ")[0]+"-participants-wide.png");return null;});
            var close=engine.previewOrderBookClose("Owner",1,1);engine.closeOrderBookEvent("Owner",1,1,close.worldRevision());
            var closed=engine.getWorldSnapshot().events().getFirst();
            FxTestSupport.fx(()->{
                var view=new EventDetailsView(closed);stage[0].getScene().setRoot(view);view.applyCss();view.layout();
                var tabs=(TabPane)view.lookup("#eventSections");tabs.getSelectionModel().select(tabs.getTabs().stream().filter(t->t.getText().equals("Participants")).findFirst().orElseThrow());
                view.applyCss();view.layout();
                @SuppressWarnings("unchecked") var table=(TableView<OrderBookPosition>)view.lookup("#participantsTable");
                var owner=table.getItems().stream().filter(p->p.userName().equals("Owner")).findFirst().orElseThrow();
                assertEquals("100",table.getColumns().get(2).getCellData(owner));assertEquals("SETTLEMENT",table.getColumns().get(3).getCellData(owner));
                assertEquals("0",table.getColumns().get(5).getCellData(owner));assertEquals("SETTLEMENT",table.getColumns().get(6).getCellData(owner));
                save(view,one.split(" ")[0]+"-participants-settlement.png");
                return null;
            });
        }finally{FxTestSupport.fx(()->{if(stage[0]!=null)stage[0].close();return null;});}
    }
    @Test void nativeOrderInputBackConfirmAndCancelPreserveExplicitUser()throws Exception{
        var path=fixture();var session=FxTestSupport.fx(()->new WorldSession(new GuessMarketWorldEngineImpl()));
        Stage[] stage=new Stage[1];
        try{
            Parent root=FxTestSupport.fx(()->{
                var loader=new FXMLLoader(getClass().getResource("/guessmarket/ui/javafx/main.fxml"));
                loader.setControllerFactory(type->{try{return type.getDeclaredConstructor(WorldSession.class).newInstance(session);}
                    catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}});
                Parent view=loader.load();stage[0]=new Stage();stage[0].setScene(new Scene(view,1280,800));stage[0].show();return view;
            });
            FxTestSupport.fx(()->{session.load(path);return null;});FxTestSupport.awaitIdle(session);
            FxTestSupport.fx(()->{session.previewOpening("Owner",1,p->session.completeOpening(true));return null;});FxTestSupport.awaitIdle(session);
            FxTestSupport.fx(()->{
                ((TabPane)root.lookup("#navigation")).getSelectionModel().select(1);root.applyCss();root.layout();
                FxTestSupport.viewUser(root,"availableSection",1);
                assertNotNull(root.lookup("#submitOrder"));assertNull(root.lookup("#buyShares"));
                ((Button)root.lookup("#submitOrder")).fire();return null;
            });
            DialogPane input=awaitDialog("orderInput");
            FxTestSupport.fx(()->{
                ((ComboBox<?>)input.lookup("#orderSide")).getSelectionModel().select(1);
                ((TextField)input.lookup("#orderQuantity")).setText("0");press(input,"Review order");
                assertTrue(input.lookup("#orderInputError").isVisible());
                ((TextField)input.lookup("#orderQuantity")).setText("4");((TextField)input.lookup("#orderPrice")).setText("0.425");
                assertFalse(input.lookup("#orderInputError").isVisible());save(input,"order-input.png");press(input,"Review order");return null;
            });
            var review=awaitDialog("orderDialog");
            FxTestSupport.fx(()->{assertEquals("0.425",((Label)review.lookup("#orderLimit")).getText());assertEquals("4",((Label)review.lookup("#orderRemaining")).getText());save(review,"order-review.png");press(review,"Back to edit");return null;});
            final DialogPane edited=awaitDialog("orderInput");
            FxTestSupport.fx(()->{assertEquals("0.425",((TextField)edited.lookup("#orderPrice")).getText());assertEquals("4",((TextField)edited.lookup("#orderQuantity")).getText());press(edited,"Review order");return null;});
            final DialogPane accepted=awaitDialog("orderDialog");
            FxTestSupport.fx(()->{assertEquals("0.425",((Label)accepted.lookup("#orderLimit")).getText());press(accepted,"Confirm order");return null;});FxTestSupport.awaitIdle(session);
            FxTestSupport.fx(()->{
                assertEquals(0,new BigDecimal("0.425").compareTo(session.worldProperty().get().events().getFirst().orderBook().orElseThrow().orders().getFirst().limitPrice()));
                assertEquals(4,session.worldProperty().get().events().getFirst().orderBook().orElseThrow().sellReservations().getFirst().quantity());
                FxTestSupport.userDetail(root,"Order books");assertEquals(1,((TableView<?>)root.lookup("#orderBook1")).getItems().size());
                FxTestSupport.userDetail(root,"Your position");assertNotNull(root.lookup("#obPurchases"));
                ((Button)root.lookup("#submitOrder")).fire();return null;
            });
            final var cancelled=awaitDialog("orderInput");var before=FxTestSupport.fx(()->session.worldProperty().get());
            FxTestSupport.fx(()->{press(cancelled,"Cancel");return null;});
            assertEquals(before,FxTestSupport.fx(()->session.worldProperty().get()));
            FxTestSupport.fx(()->{((Button)root.lookup("#closeEvent")).fire();return null;});
            final var closeInput=awaitDialog("closeInput");
            FxTestSupport.fx(()->{((ComboBox<?>)closeInput.lookup("#winningOption")).getSelectionModel().select(1);press(closeInput,"Review payouts");return null;});
            final var closeReview=awaitDialog("closeDialog");
            FxTestSupport.fx(()->{assertNull(closeReview.lookup("#closingRefund"));save(closeReview,"ob-close-review.png");press(closeReview,"Close event and pay out");return null;});FxTestSupport.awaitIdle(session);
            FxTestSupport.fx(()->{
                assertNull(root.lookup("#submitOrder"));FxTestSupport.userDetail(root,"Settlement");
                assertNotNull(root.lookup("#settlementPayments"));save(root,"ob-closed.png");
                FxTestSupport.userDetail(root,"Your position");assertNotNull(root.lookup("#obProfitLoss"));return null;
            });
        }finally{FxTestSupport.fx(()->{for(var window:java.util.List.copyOf(Window.getWindows()))if(window!=stage[0])window.hide();session.close();if(stage[0]!=null)stage[0].close();return null;});}
    }
    @Test void blockedReviewKeepsFullConsequenceOutsideScrollingDetails()throws Exception{
        Path path=fixture();String name="Alexandra Elizabeth Montgomery with a long account name";
        Files.writeString(path,Files.readString(path).replace("<GM-user name=\""+name+"\"><initial-cash>1000","<GM-user name=\""+name+"\"><initial-cash>5"));
        var engine=new GuessMarketWorldEngineImpl();engine.loadWorldFromXml(path);
        var open=engine.previewOrderBookOpening("Owner",1);engine.openOrderBookEvent("Owner",1,open.worldRevision());
        var sell=new OrderRequest("Owner",1,1,OrderSide.SELL,20,new BigDecimal("0.60"));engine.submitOrder(sell,engine.previewOrder(sell).worldRevision());
        var waiting=new OrderRequest(name,1,2,OrderSide.BUY,2,new BigDecimal("0.20"));engine.submitOrder(waiting,engine.previewOrder(waiting).worldRevision());
        var request=new OrderRequest(name,1,1,OrderSide.BUY,20,new BigDecimal("0.60"));
        var preview=engine.previewOrder(request);var before=engine.getWorldSnapshot();assertTrue(preview.blockedAfter());assertEquals(1,preview.cancelledOrderCount());
        Stage[] stage=new Stage[1];var decision=new AtomicReference<OrderDialog.Decision>();
        try{
            FxTestSupport.fx(()->{stage[0]=new Stage();stage[0].setScene(new Scene(new VBox(),500,400));stage[0].show();
                OrderDialog.show(stage[0],before.events().getFirst(),preview,decision::set);return null;});
            var dialog=awaitDialog("orderDialog");
            FxTestSupport.fx(()->{
                dialog.applyCss();dialog.layout();var warning=(Label)dialog.lookup("#orderWarning");
                assertTrue(warning.getText().contains("ends blocked"));assertTrue(warning.getText().contains("accounts: 1"));
                assertInstanceOf(BorderPane.class,warning.getParent());assertTrue(warning.getHeight()>=warning.prefHeight(warning.getWidth())-1);
                assertEquals(1,((TableView<?>)dialog.lookup("#orderFills")).getItems().size());
                save(dialog,"order-blocked-review.png");press(dialog,"Cancel");return null;
            });
            assertEquals(OrderDialog.Decision.CANCEL,decision.get());assertEquals(before,engine.getWorldSnapshot());
        }finally{FxTestSupport.fx(()->{if(stage[0]!=null)stage[0].close();return null;});}
    }
    private static DialogPane awaitDialog(String id)throws Exception{
        for(int i=0;i<100;i++){
            var pane=FxTestSupport.fx(()->Window.getWindows().stream().filter(w->w.getScene()!=null&&id.equals(w.getScene().getRoot().getId()))
                    .map(w->(DialogPane)w.getScene().getRoot()).findFirst().orElse(null));
            if(pane!=null)return pane;Thread.sleep(20);
        }
        throw new AssertionError("Dialog not shown: "+id);
    }
    private static void press(DialogPane pane,String text){
        var type=pane.getButtonTypes().stream().filter(b->b.getText().equals(text)).findFirst().orElseThrow();
        ((Button)pane.lookupButton(type)).fire();
    }
    private static void save(Node node,String name)throws Exception{
        var image=node.snapshot(null,null);int width=(int)image.getWidth(),height=(int)image.getHeight();
        var buffered=new java.awt.image.BufferedImage(width,height,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++)buffered.setRGB(x,y,image.getPixelReader().getArgb(x,y));
        Path target=Path.of(System.getProperty("guessmarket.uiEvidenceDirectory",
                "private/coordination/assignment-2/evidence-ob-ui/screenshots"),name);
        Files.createDirectories(target.getParent());javax.imageio.ImageIO.write(buffered,"png",target.toFile());
    }
}
