package guessmarket.server;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import guessmarket.dto.api.LoginRequest;
@WebServlet("/api/v1/session/*")
public final class SessionServlet extends ApiServlet {
    private static final long serialVersionUID=1L;
    @Override void dispatch(HttpServletRequest request,HttpServletResponse response)throws Exception {
        method(request,"POST");
        if("/logout".equals(request.getPathInfo())){var session=request.getSession(false);if(session!=null){app().logout(session.getId());session.invalidate();}response.setStatus(204);return;}
        if(!"/login".equals(request.getPathInfo()))throw app().failure(404,"NOT_FOUND","Unknown session endpoint.",null);
        var input=body(request,LoginRequest.class);var session=request.getSession(true);var result=app().login(session.getId(),input.username());
        try{session.getCreationTime();app().actor(session.getId());}catch(IllegalStateException failure){app().logout(session.getId());throw failure;}
        send(response,result.createdAccount()?201:200,result);
    }
}
