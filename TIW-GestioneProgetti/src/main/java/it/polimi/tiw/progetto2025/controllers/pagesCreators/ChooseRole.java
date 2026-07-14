package it.polimi.tiw.progetto2025.controllers.pagesCreators;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import it.polimi.tiw.progetto2025.beans.User;
import it.polimi.tiw.progetto2025.daos.MyDAO;

public class ChooseRole extends HttpServlet 
{
	private static final long serialVersionUID=1L;
    private Connection connection=null;
    private TemplateEngine templateEngine;
    private JakartaServletWebApplication webApplication;

    @Override
    public void init() throws ServletException 
    {
        ServletContext servletContext=getServletContext();
        this.webApplication=JakartaServletWebApplication.buildApplication(servletContext);
        WebApplicationTemplateResolver templateResolver=new WebApplicationTemplateResolver(this.webApplication);
        
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setPrefix("/WEB-INF/pages/login/");
        templateResolver.setSuffix(".html");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);
        
        this.templateEngine=new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);

        try 
        {
            new com.mysql.cj.jdbc.Driver();
            this.connection=DriverManager.getConnection(MyDAO.DB_URL, MyDAO.DB_USER, MyDAO.DB_PASS);
        } 
        catch (SQLException e) 
        {
            throw new ServletException("Impossibile connettersi al DB nella Servlet ChoseRole", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
    {
    	HttpSession session=request.getSession(false);
    	
    	response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); 
        response.setHeader("Pragma", "no-cache"); 
        response.setDateHeader("Expires", 0);
        
        IWebExchange exchange=this.webApplication.buildExchange(request, response);
        WebContext ctx=new WebContext(exchange, request.getLocale());
        
        if(session==null || session.getAttribute("user")==null) 
        {
            response.sendRedirect(getServletContext().getContextPath()+"/Login");
            return;
        }
        
        User user=(User)session.getAttribute("user");
        
        if(!user.isAdmin() && (user.isManager() && user.isCollaborator())) 
        {          
            ctx.setVariable("user", user);
            
            templateEngine.process("chooseRole", ctx, response.getWriter());  
        }
        else
        {
        	if(user.isAdmin()) response.sendRedirect(getServletContext().getContextPath()+"/Admin");
            else if(user.isManager() && !user.isCollaborator()) response.sendRedirect(getServletContext().getContextPath()+"/Manager");
            else if(!user.isManager() && user.isCollaborator()) response.sendRedirect(getServletContext().getContextPath()+"/Collaborator");
            else response.sendRedirect(getServletContext().getContextPath()+"/Logout");
            
            return;
        }        
    }
    
    @Override
    public void destroy()
    {
    	if(connection!=null)
    		try {connection.close();}
    		catch(SQLException ignored) {}
    }
}