package guessmarket.server;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
@WebServlet("/api/v1/commands/*")
public final class CommandStatusServlet extends ApiServlet {
    private static final long serialVersionUID=1L;
    @Override void dispatch(HttpServletRequest request,HttpServletResponse response)throws Exception {method(request,"GET");String path=request.getPathInfo();send(response,200,app().commandStatus(session(request),path==null?null:path.substring(1)));}
}
