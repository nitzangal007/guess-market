package guessmarket.server;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
@WebServlet("/api/v1/dashboard")
public final class DashboardServlet extends ApiServlet {
    private static final long serialVersionUID=1L;
    @Override void dispatch(HttpServletRequest request,HttpServletResponse response)throws Exception {
        method(request,"GET");String session=session(request);app().actor(session);Integer event=null;
        if(request.getParameter("eventId")!=null){try{event=Integer.valueOf(request.getParameter("eventId"));if(event<=0)throw new NumberFormatException();}catch(NumberFormatException problem){throw app().failure(400,"INVALID_EVENT_ID","Select a positive event ID.",null);}}
        send(response,200,app().dashboard(session,event));
    }
}
