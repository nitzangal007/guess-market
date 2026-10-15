package guessmarket.engine.xml.ex3;
import java.util.List;
public record ParsedEventBatch(List<EventDefinition> events){public ParsedEventBatch{events=List.copyOf(events);}}
