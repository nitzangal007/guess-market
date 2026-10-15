package guessmarket.client.http;
import guessmarket.dto.api.*;
import okhttp3.*;
import com.google.gson.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
public final class MarketHttpClient implements AutoCloseable {
    public static final String DEFAULT_URL="http://localhost:8080/guessmarket/api/v1/";
    private static final MediaType JSON=MediaType.get("application/json; charset=utf-8");
    private final Gson gson=new GsonBuilder().serializeNulls().setStrictness(Strictness.STRICT).addReflectionAccessFilter(ReflectionAccessFilter.BLOCK_ALL_PLATFORM).create();
    private final HttpUrl base;
    final OkHttpClient transport;
    private final SessionCookieJar cookies=new SessionCookieJar();
    private final Object callGate=new Object();
    private final Set<Call> activeCalls=new HashSet<>();
    private boolean closed;
    private volatile String expectedAccount;
    public MarketHttpClient(){this(DEFAULT_URL);}
    public MarketHttpClient(String url){base=HttpUrl.get(url);transport=new OkHttpClient.Builder().cookieJar(cookies).connectTimeout(5,TimeUnit.SECONDS).readTimeout(10,TimeUnit.SECONDS).writeTimeout(10,TimeUnit.SECONDS).callTimeout(15,TimeUnit.SECONDS).retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false)
            .addNetworkInterceptor(chain->{var response=chain.proceed(chain.request());
                // OkHttp 4.12 treats Retry-After: 0 on 503 as a follow-up independently of its retry flag.
                // Ignore that automatic-retry directive while preserving the actual status and error body.
                return response.code()==503?response.newBuilder().removeHeader("Retry-After").build():response;}).build();}
    private Request.Builder request(String path){return new Request.Builder().url(base.newBuilder().addPathSegments(path).build());}
    private Request json(String path,Object value){var builder=request(path).post(oneInvocation(RequestBody.create(gson.toJson(value),JSON)));WireResponseValidator.Expected expected=null;
        if(value instanceof FundsRequest r)expected=new WireResponseValidator.Expected(r.requestId(),OperationKind.FUND_LOAD,null,null,null);
        if(value instanceof CommandRequest r)expected=new WireResponseValidator.Expected(r.requestId(),OperationKind.valueOf(r.action().action().name()),Integer.valueOf(path.split("/")[1]),r.action(),r.expectedRevision());
        if(value instanceof ActionRequest r)expected=new WireResponseValidator.Expected(null,null,Integer.valueOf(path.split("/")[1]),r,null);
        if(expected!=null)builder.tag(WireResponseValidator.Expected.class,expected);return builder.build();}
    private static RequestBody oneInvocation(RequestBody body){return new RequestBody(){
        @Override public MediaType contentType(){return body.contentType();}
        @Override public long contentLength()throws IOException{return body.contentLength();}
        @Override public void writeTo(okio.BufferedSink sink)throws IOException{body.writeTo(sink);}
        @Override public boolean isOneShot(){return true;}
    };}
    private <T> T execute(Request request,Class<T> type)throws IOException,ApiCallException{
        Call call;synchronized(callGate){if(closed)throw new IOException("This client is closed.");call=transport.newCall(request);activeCalls.add(call);}
        try(Response response=call.execute()){
            String body=response.body()==null?"":response.body().string();
            checkActive(call);
            if(!response.isSuccessful()){
                ApiError error=null;try{error=gson.fromJson(body,ApiError.class);}catch(RuntimeException ignored){/* Container errors may be HTML or have an incompatible error envelope. */}
                if(error==null||error.code()==null||error.message()==null)error=new ApiError("HTTP_"+response.code(),"Server returned HTTP "+response.code()+". Check the server address or log in again.",null,null,null,null,null);
                checkActive(call);throw new ApiCallException(response.code(),error);
            }
            if(type==Void.class)return null;
            try{var tree=gson.fromJson(body,JsonElement.class);if(tree==null||tree.isJsonNull())throw new JsonSyntaxException("Empty response");WireResponseValidator.shape(tree,type);T value=gson.fromJson(tree,type);WireResponseValidator.metadata(value,request.tag(WireResponseValidator.Expected.class));checkActive(call);return value;}catch(RuntimeException problem){throw new IOException("The server returned an unreadable response. Check the operation outcome before retrying.",problem);}
        }finally{synchronized(callGate){activeCalls.remove(call);}}
    }
    private void checkActive(Call call)throws IOException{synchronized(callGate){if(closed||call.isCanceled())throw new IOException("The local request was canceled. Its server outcome may still be unknown.");}}
    public LoginResponse login(String username)throws IOException,ApiCallException{String prior=expectedAccount;if(prior==null&&username!=null)expectedAccount=username.trim();try{var value=execute(json("session/login",new LoginRequest(username)),LoginResponse.class);if(username==null||!value.username().equalsIgnoreCase(username.trim()))throw new IOException("The login response does not match the requested account.");expectedAccount=value.username();return value;}catch(ApiCallException failure){if(failure.status()>=400&&failure.status()<500)expectedAccount=prior;throw failure;}}
    public DashboardResponse dashboard(Integer eventId)throws IOException,ApiCallException{var url=base.newBuilder().addPathSegment("dashboard");if(eventId!=null)url.addQueryParameter("eventId",eventId.toString());var value=execute(new Request.Builder().url(url.build()).build(),DashboardResponse.class);if(expectedAccount!=null&&!expectedAccount.equalsIgnoreCase(value.account().username()))throw new IOException("The dashboard response does not match this account.");if(eventId!=null&&(value.selectedEvent()==null||value.selectedEvent().summary().id()!=eventId))throw new IOException("The dashboard response does not contain the requested event.");return value;}
    public CommandResponse addFunds(FundsRequest request)throws IOException,ApiCallException{return execute(json("account/funds",request),CommandResponse.class);}
    public ActionPreviewDto preview(int event,ActionRequest action)throws IOException,ApiCallException{return execute(json("events/"+event+"/preview",action),ActionPreviewDto.class);}
    public CommandResponse execute(int event,CommandRequest request)throws IOException,ApiCallException{return execute(json("events/"+event+"/commands",request),CommandResponse.class);}
    public CommandStatusDto commandStatus(String id)throws IOException,ApiCallException{return execute(request("commands/"+id).tag(WireResponseValidator.Expected.class,new WireResponseValidator.Expected(id,null,null,null,null)).build(),CommandStatusDto.class);}
    public UploadResponse upload(UploadAttempt attempt)throws IOException,ApiCallException{
        var multipart=new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("file",attempt.filename(),RequestBody.create(attempt.bytes(),MediaType.get("application/xml"))).build();
        return execute(request("events/upload").header("X-Request-Id",attempt.requestId()).tag(WireResponseValidator.Expected.class,new WireResponseValidator.Expected(attempt.requestId(),OperationKind.UPLOAD,null,null,null)).post(oneInvocation(multipart)).build(),UploadResponse.class);
    }
    public void logout()throws IOException,ApiCallException{execute(request("session/logout").post(oneInvocation(RequestBody.create(new byte[0],null))).build(),Void.class);cookies.clear();expectedAccount=null;}
    @Override public void close(){List<Call> calls;synchronized(callGate){if(closed)return;closed=true;calls=List.copyOf(activeCalls);}calls.forEach(Call::cancel);transport.dispatcher().cancelAll();transport.connectionPool().evictAll();transport.dispatcher().executorService().shutdown();cookies.close();}
}
