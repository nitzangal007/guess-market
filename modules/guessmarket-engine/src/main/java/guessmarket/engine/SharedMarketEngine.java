package guessmarket.engine;
import guessmarket.dto.api.*;
import guessmarket.engine.xml.ex3.ParsedEventBatch;
public interface SharedMarketEngine {
    void ensureAccount(String actor)throws WorldCommandException;
    DashboardResponse dashboard(String actor,Integer eventId)throws WorldCommandException;
    UploadResponse appendEvents(String actor,ParsedEventBatch batch,String requestId)throws WorldCommandException;
    CommandResponse addFunds(String actor,String amount,String requestId)throws WorldCommandException;
    ActionPreviewDto preview(String actor,int eventId,ActionRequest action)throws WorldCommandException;
    CommandResponse execute(String actor,int eventId,CommandRequest request)throws WorldCommandException;
}
