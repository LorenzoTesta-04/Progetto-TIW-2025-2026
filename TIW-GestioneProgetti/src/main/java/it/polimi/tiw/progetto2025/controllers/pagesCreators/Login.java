package it.polimi.tiw.progetto2025.controllers.pagesCreators;

import java.io.IOException;
import java.sql.Connection;
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

@SuppressWarnings("unused")
public class Login extends HttpServlet
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
        
        this.templateEngine=new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
        
        this.connection=(Connection)getServletContext().getAttribute("dbContext"); 
    }
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
    {
        HttpSession session=request.getSession(false);
		
		if(session!=null)
			session.invalidate();
		
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        

        if(request.getSession().getAttribute("name")!=null) 
        {
            response.sendRedirect(getServletContext().getContextPath()+"/");
        } 
        else 
        {
            IWebExchange exchange=this.webApplication.buildExchange(request, response);
            WebContext ctx=new WebContext(exchange, request.getLocale());                           

            String errorMsg=request.getParameter("error_msg");
            
            if(errorMsg==null) errorMsg="";
            ctx.setVariable("error_msg", errorMsg);
            
            templateEngine.process("login", ctx, response.getWriter());
        }
    }
}