package guessmarket.engine.xml.ex3;
import java.util.List;
import guessmarket.dto.CommissionMode;
import guessmarket.dto.world.PricingConfiguration;
public record EventDefinition(String name,String description,List<String> options,int commissionPercentage,CommissionMode commissionMode,PricingConfiguration pricing){public EventDefinition{options=List.copyOf(options);}}
