package guessmarket.server;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import guessmarket.dto.api.FundsRequest;
@WebServlet("/api/v1/account/funds")
public final class AccountServlet extends ApiServlet {
    private static final long serialVersionUID=1L;
    @Override void dispatch(HttpServletRequest request,HttpServletResponse response)throws Exception {method(request,"POST");String session=session(request);app().actor(session);send(response,200,app().addFunds(session,body(request,FundsRequest.class)));}
}
