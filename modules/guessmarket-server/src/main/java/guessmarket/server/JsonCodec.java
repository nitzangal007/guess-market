package guessmarket.server;
import com.google.gson.*;
public final class JsonCodec {
    private static final Gson GSON=new GsonBuilder().serializeNulls().setStrictness(Strictness.STRICT)
            .addReflectionAccessFilter(ReflectionAccessFilter.BLOCK_ALL_PLATFORM).create();
    private JsonCodec() {}
    public static Gson gson(){return GSON;}
}
