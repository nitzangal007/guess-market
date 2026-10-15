package guessmarket.client.session;
public final class OperationGate {
    private long generation;
    private String instance,id;
    public void reset(long generation,String instance){this.generation=generation;this.instance=instance;id=null;}
    public void begin(String id){if(this.id!=null)throw new IllegalStateException("One mutation at a time");this.id=java.util.Objects.requireNonNull(id);}
    public boolean resolve(long generation,String instance,String id){if(this.generation!=generation||!java.util.Objects.equals(this.instance,instance)||this.id==null||!this.id.equals(id))return false;this.id=null;return true;}
    public boolean pending(){return id!=null;}
}
