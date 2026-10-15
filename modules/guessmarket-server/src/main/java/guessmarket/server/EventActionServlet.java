package guessmarket.server;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import guessmarket.dto.api.*;
@WebServlet("/api/v1/events/*")
public final class EventActionServlet extends ApiServlet {
    private static final long serialVersionUID=1L;
    @Override void dispatch(HttpServletRequest request,HttpServletResponse response)throws Exception {
        method(request,"POST");String session=session(request);app().actor(session);String path=request.getPathInfo();String[] parts=path==null?new String[0]:path.split("/");
        if(parts.length!=3)throw app().failure(404,"NOT_FOUND","Unknown event action endpoint.",null);
        int id;try{id=Integer.parseInt(parts[1]);if(id<=0)throw new NumberFormatException();}catch(NumberFormatException problem){throw app().failure(400,"INVALID_EVENT_ID","Use a positive event ID.",null);}
        if(parts[2].equals("preview"))send(response,200,app().preview(session,id,body(request,ActionRequest.class)));
        else if(parts[2].equals("commands"))send(response,200,app().execute(session,id,body(request,CommandRequest.class)));
        else throw app().failure(404,"NOT_FOUND","Unknown event action endpoint.",null);
    }
}
