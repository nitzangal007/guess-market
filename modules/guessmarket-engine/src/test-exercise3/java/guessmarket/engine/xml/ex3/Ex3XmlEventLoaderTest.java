package guessmarket.engine.xml.ex3;
import org.junit.jupiter.api.Test;
import guessmarket.engine.EngineOperationException;
import java.nio.file.*;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
class Ex3XmlEventLoaderTest {
    @Test void suppliedSamplesValidateWithoutUsersOrIds() throws Exception {
        for(String name:new String[]{"small.xml","multiple.xml"})try(var in=Files.newInputStream(Path.of("provided/assignment-3/test files ex3",name))){
            var batch=new Ex3XmlEventLoader().parse(in,name);assertFalse(batch.events().isEmpty());assertEquals(2,batch.events().getFirst().options().size());
        }
    }
    @Test void schemaAndSemanticsRejectHostileOrPartialInput() throws Exception {
        String valid=Files.readString(Path.of("provided/assignment-3/test files ex3/small.xml"));
        for(String xml:new String[]{"broken",valid.replace("<Guess-Market ","<!DOCTYPE Guess-Market [<!ENTITY external SYSTEM 'file:///C:/secret'>]><Guess-Market "),valid.replace("</GM-options>","<GM-option>third</GM-option></GM-options>"),valid.replace("<b>100</b>","<b>0</b>")}) {
            assertThrows(EngineOperationException.class,()->parse(xml));
        }
    }
    @Test void emptyAndRepeatedLabelsAreOrderedAndNamesTrimOnlyAtEnds() throws Exception {
        String xml="<Guess-Market><GM-events><GM-event name='  '><description>  a  b  </description><commission type='on-close'>0</commission><GM-options><GM-option></GM-option><GM-option></GM-option></GM-options><GM-method><GM-order-book initial='3' d='2' allow-mint='true'/></GM-method></GM-event></GM-events></Guess-Market>";
        var event=parse(xml).events().getFirst();assertEquals("",event.name());assertEquals("a  b",event.description());assertEquals(java.util.List.of("",""),event.options());
        assertThrows(EngineOperationException.class,()->parse(xml.replace("<GM-option></GM-option><GM-option></GM-option>","<GM-option></GM-option>")));
        assertThrows(EngineOperationException.class,()->parse(xml.replace("<GM-event name='  '>","<GM-event id='1' name='  '>")));
    }
    private static ParsedEventBatch parse(String xml)throws Exception{return new Ex3XmlEventLoader().parse(new ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)),"test.xml");}
}
