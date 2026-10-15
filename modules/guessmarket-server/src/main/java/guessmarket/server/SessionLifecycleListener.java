package guessmarket.server;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.*;
@WebListener
public final class SessionLifecycleListener implements HttpSessionListener {
    @Override public void sessionDestroyed(HttpSessionEvent event){var app=(MarketApplication)event.getSession().getServletContext().getAttribute("market");if(app!=null)app.logout(event.getSession().getId());}
}
