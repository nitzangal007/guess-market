package guessmarket.client.ui;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.beans.property.SimpleStringProperty;
import java.util.*;
import java.util.function.Function;
final class UiTables {
    record Column<T>(String title,Function<T,String> value,boolean numeric,boolean money,Function<T,String> display) {
        Column(String title,Function<T,String> value,boolean numeric,boolean money){this(title,value,numeric,money,null);}
    }
    private UiTables() {}
    static Label label(String text,String style){var label=new Label(text);label.setWrapText(true);label.setMinWidth(0);label.setMaxWidth(Double.MAX_VALUE);if(style!=null)label.getStyleClass().add(style);return label;}
    static <T> VBox table(String heading,List<T> rows,List<Column<T>> columns){
        var table=new TableView<T>();table.setAccessibleText(heading);table.setFocusTraversable(true);table.setPlaceholder(label("No rows yet.","muted"));table.getItems().setAll(rows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);table.setPrefHeight(Math.min(360,Math.max(150,rows.size()*42+50)));table.setMinHeight(140);
        for(var spec:columns){var column=new TableColumn<T,String>(spec.title());column.setPrefWidth(spec.numeric()?115:180);column.setMinWidth(spec.numeric()?90:115);
            column.setCellValueFactory(value->new SimpleStringProperty(spec.value().apply(value.getValue())));if(spec.numeric())column.getStyleClass().add("numeric");
            column.setCellFactory(c->new TableCell<>(){
                @Override protected void updateItem(String raw,boolean empty){super.updateItem(raw,empty);if(empty){setGraphic(null);setText(null);setTooltip(null);return;}
                    T row=getIndex()>=0&&getIndex()<getTableView().getItems().size()?getTableView().getItems().get(getIndex()):null;
                    String text=spec.display()!=null&&row!=null?spec.display().apply(row):spec.money()?WireViewMapper.money(raw):Objects.toString(raw,"Not applicable");var label=UiTables.label(text,null);label.setAlignment(spec.numeric()?Pos.TOP_RIGHT:Pos.TOP_LEFT);label.prefWidthProperty().bind(widthProperty().subtract(16));setGraphic(label);setText(null);setAlignment(spec.numeric()?Pos.TOP_RIGHT:Pos.TOP_LEFT);setTooltip(new Tooltip(Objects.toString(raw,"Unavailable")));}
            });table.getColumns().add(column);
        }
        var full=new StringBuilder();for(T row:rows){for(var column:columns)full.append(column.title()).append(": ").append(Objects.toString(column.value().apply(row),"Unavailable")).append("   ");full.append('\n');}
        var exact=new TextArea(full.toString());exact.setEditable(false);exact.setWrapText(true);exact.setPrefRowCount(4);exact.setAccessibleText(heading+" exact values");var disclosure=new TitledPane("Exact values",exact);disclosure.setExpanded(false);
        var box=new VBox(8,label(heading,"subheading"),table,disclosure);box.setMaxWidth(Double.MAX_VALUE);return box;
    }
    static <T> Column<T> text(String name,Function<T,String> value){return new Column<>(name,value,false,false);}
    static <T> Column<T> number(String name,Function<T,String> value){return new Column<>(name,value,true,false);}
    static <T> Column<T> money(String name,Function<T,String> value){return new Column<>(name,value,true,true);}
    static <T> Column<T> compact(String name,Function<T,String> raw,Function<T,String> display){return new Column<>(name,raw,true,false,display);}
}
