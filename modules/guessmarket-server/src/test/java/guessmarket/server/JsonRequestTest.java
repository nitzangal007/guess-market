package guessmarket.server;
import guessmarket.dto.api.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
class JsonRequestTest {
    @Test void unknownEnumsRejectBeforeTypedNullCanReachDispatch()throws Exception{
        var app=new MarketApplication();var context=(ServletContext)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{ServletContext.class},(p,m,a)->m.getName().equals("getAttribute")?app:null);
        var config=(ServletConfig)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{ServletConfig.class},(p,m,a)->m.getName().equals("getServletContext")?context:null);
        var servlet=new EventActionServlet();servlet.init(config);
        for(String json:new String[]{"{\"action\":\"OPEN\",\"side\":\"NOT_A_SIDE\"}","{\"action\":\"LMSR_BUY\",\"side\":\"NOT_A_SIDE\"}","{\"action\":\"CLOSE\",\"side\":\"NOT_A_SIDE\"}","{\"action\":\"ORDER\",\"side\":\"NOT_A_SIDE\"}","{\"action\":\"NOT_AN_ACTION\"}"})assertEquals(400,assertThrows(ApiFailure.class,()->servlet.body(request(json),ActionRequest.class)).status());
        assertNull(servlet.body(request("{\"action\":\"OPEN\",\"side\":null}"),ActionRequest.class).side());assertNull(servlet.body(request("{\"action\":\"OPEN\"}"),ActionRequest.class).side());
    }
    private HttpServletRequest request(String json){
        var bytes=new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        var input=new ServletInputStream(){@Override public int read(){return bytes.read();}@Override public boolean isFinished(){return bytes.available()==0;}@Override public boolean isReady(){return true;}@Override public void setReadListener(ReadListener listener){throw new UnsupportedOperationException();}};
        return (HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{HttpServletRequest.class},(p,m,a)->switch(m.getName()){case "getContentType"->"application/json";case "getInputStream"->input;default->null;});
    }
}
