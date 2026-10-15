package guessmarket.client.session;
import guessmarket.dto.api.DashboardResponse;
public final class DashboardGate {
    private long generation,selection,revision=-1,appliedSelection=-1;
    private String instance;
    private Integer selected;
    public void reset(long generation,String instance){this.generation=generation;this.instance=instance;revision=-1;selection=0;appliedSelection=-1;selected=null;}
    public long select(Integer event){if(!java.util.Objects.equals(selected,event)){selected=event;selection++;}return selection;}
    public boolean accept(long generation,long selection,DashboardResponse response){
        if(this.generation!=generation||!java.util.Objects.equals(instance,response.serverInstanceId())||this.selection!=selection||response.revision()<revision)return false;
        if(selected!=null&&(response.selectedEvent()==null||response.selectedEvent().summary().id()!=selected))return false;
        if(response.revision()==revision&&appliedSelection==selection)return false;
        revision=response.revision();appliedSelection=selection;return true;
    }
    public long selection(){return selection;}
    public long revision(){return revision;}
}
