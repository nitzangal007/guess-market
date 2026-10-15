package guessmarket.server;
import jakarta.servlet.http.*;
import java.io.*;
import java.lang.reflect.RecordComponent;
import java.util.*;
import com.google.gson.*;
import com.google.gson.stream.*;
abstract class ApiServlet extends HttpServlet {
    private static final long serialVersionUID=1L;
    final MarketApplication app(){return (MarketApplication)getServletContext().getAttribute("market");}
    final String session(HttpServletRequest request){var session=request.getSession(false);if(session==null)throw app().failure(401,"UNAUTHENTICATED","Log in to use the market.",null);return session.getId();}
    @Override protected final void service(HttpServletRequest request,HttpServletResponse response)throws IOException {
        response.setCharacterEncoding("UTF-8");response.setContentType("application/json");response.setHeader("Cache-Control","no-store");
        try{dispatch(request,response);}catch(ApiFailure failure){response.setStatus(failure.status());response.getWriter().write(JsonCodec.gson().toJson(failure.error()));}
        catch(IllegalStateException failure){var error=app().failure(401,"SESSION_EXPIRED","The session expired. Log in again.",null);response.setStatus(error.status());response.getWriter().write(JsonCodec.gson().toJson(error.error()));}
        catch(Exception failure){getServletContext().log("API request failed",failure);var error=app().failure(500,"SERVER_ERROR","The server could not complete this request. Check its outcome before retrying.",request.getHeader("X-Request-Id"));response.setStatus(500);response.getWriter().write(JsonCodec.gson().toJson(error.error()));}
    }
    abstract void dispatch(HttpServletRequest request,HttpServletResponse response)throws Exception;
    final void send(HttpServletResponse response,int status,Object value)throws IOException {response.setStatus(status);response.getWriter().write(JsonCodec.gson().toJson(value));}
    final void method(HttpServletRequest request,String expected){if(!request.getMethod().equals(expected))throw app().failure(405,"METHOD_NOT_ALLOWED","Use "+expected+" for this endpoint.",null);}
    final <T> T body(HttpServletRequest request,Class<T> type)throws IOException {
        if(request.getContentType()==null||!request.getContentType().split(";")[0].trim().equalsIgnoreCase("application/json"))throw app().failure(415,"UNSUPPORTED_CONTENT_TYPE","Send UTF-8 application/json.",null);
        byte[] bytes=request.getInputStream().readNBytes(65537);if(bytes.length>65536)throw app().failure(413,"REQUEST_TOO_LARGE","JSON input exceeds 64 KiB.",null);
        try(var reader=new JsonReader(new StringReader(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)))){
            reader.setStrictness(Strictness.STRICT);JsonElement json=read(reader,0);
            if(reader.peek()!=JsonToken.END_DOCUMENT)throw new JsonSyntaxException("Trailing JSON");
            validate(json,type);return JsonCodec.gson().fromJson(json,type);
        }catch(JsonParseException|IllegalArgumentException|MalformedJsonException|EOFException|ArithmeticException problem){throw app().failure(400,"INVALID_JSON","Malformed JSON, unknown fields, or invalid field types.",null);}
    }
    private JsonElement read(JsonReader reader,int depth)throws IOException {
        if(depth>32)throw new JsonSyntaxException("Too deep");
        return switch(reader.peek()){
            case BEGIN_OBJECT->{reader.beginObject();var object=new JsonObject();while(reader.hasNext()){String key=reader.nextName();if(object.has(key))throw new JsonSyntaxException("Duplicate field");object.add(key,read(reader,depth+1));}reader.endObject();yield object;}
            case BEGIN_ARRAY->{reader.beginArray();var array=new JsonArray();while(reader.hasNext())array.add(read(reader,depth+1));reader.endArray();yield array;}
            case STRING->new JsonPrimitive(reader.nextString());
            case NUMBER->new JsonPrimitive(new java.math.BigDecimal(reader.nextString()));
            case BOOLEAN->new JsonPrimitive(reader.nextBoolean());
            case NULL->{reader.nextNull();yield JsonNull.INSTANCE;}
            default->throw new JsonSyntaxException("Unexpected token");
        };
    }
    private void validate(JsonElement json,Class<?> type){
        if(!json.isJsonObject())throw new JsonSyntaxException("Object required");
        var object=json.getAsJsonObject();var allowed=new HashMap<String,RecordComponent>();for(var component:type.getRecordComponents())allowed.put(component.getName(),component);
        for(var field:object.entrySet()){
            var component=allowed.get(field.getKey());if(component==null)throw new JsonSyntaxException("Unknown field");
            var value=field.getValue();Class<?> fieldType=component.getType();if(value.isJsonNull()){if(fieldType.isPrimitive())throw new JsonSyntaxException("Required primitive");continue;}
            if(fieldType.isRecord()){validate(value,fieldType);continue;}
            if(!value.isJsonPrimitive())throw new JsonSyntaxException("Scalar required");var primitive=value.getAsJsonPrimitive();
            if(fieldType==String.class||fieldType.isEnum()){if(!primitive.isString())throw new JsonSyntaxException("String required");}
            if(fieldType.isEnum()&&Arrays.stream(fieldType.getEnumConstants()).noneMatch(constant->((Enum<?>)constant).name().equals(primitive.getAsString())))throw new JsonSyntaxException("Unknown enum value");
            if(fieldType==int.class||fieldType==long.class||fieldType==Integer.class){if(!primitive.isNumber())throw new JsonSyntaxException("Integer required");var number=primitive.getAsBigDecimal();if(fieldType==long.class)number.longValueExact();else number.intValueExact();}
        }
        for(var component:type.getRecordComponents())if(component.getType().isPrimitive()&&!object.has(component.getName()))throw new JsonSyntaxException("Missing primitive");
    }
}
