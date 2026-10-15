package guessmarket.server;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;
@WebListener
public final class MarketContextListener implements ServletContextListener {
    @Override public void contextInitialized(ServletContextEvent event){event.getServletContext().setAttribute("market",new MarketApplication());}
    @Override public void contextDestroyed(ServletContextEvent event){event.getServletContext().removeAttribute("market");}
}
