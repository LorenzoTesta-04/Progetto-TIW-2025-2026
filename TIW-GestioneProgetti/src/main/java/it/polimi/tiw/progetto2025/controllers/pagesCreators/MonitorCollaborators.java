package it.polimi.tiw.progetto2025.controllers.pagesCreators;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import it.polimi.tiw.progetto2025.beans.User;
import it.polimi.tiw.progetto2025.beans.WorkPackage;
import it.polimi.tiw.progetto2025.beans.Project;
import it.polimi.tiw.progetto2025.daos.MyDAO;
import it.polimi.tiw.progetto2025.daos.ProjectDAO;
import it.polimi.tiw.progetto2025.daos.TaskDAO;
import it.polimi.tiw.progetto2025.daos.UserDAO;
import it.polimi.tiw.progetto2025.daos.WorkPackageDAO;
import it.polimi.tiw.progetto2025.utils.checkAccess;

public class MonitorCollaborators extends HttpServlet 
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
        templateResolver.setPrefix("/WEB-INF/pages/manager/");
        templateResolver.setSuffix(".html");
        templateResolver.setCharacterEncoding("UTF-8");
        
        this.templateEngine=new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
        
        try 
        {
            new com.mysql.cj.jdbc.Driver();
            this.connection=DriverManager.getConnection(MyDAO.DB_URL, MyDAO.DB_USER, MyDAO.DB_PASS);
        } 
        catch(SQLException e) 
        {
            throw new ServletException("Impossibile connettersi al DB nella Servlet MonitorCollaborators", e);
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
    {
        HttpSession session=request.getSession(false);
            
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); 
        response.setHeader("Pragma", "no-cache"); 
        response.setDateHeader("Expires", 0);
        
        IWebExchange exchange=this.webApplication.buildExchange(request, response);
        WebContext ctx=new WebContext(exchange, request.getLocale());
    
        // Controllo accesso: deve essere loggato e deve essere MANAGER
        if(!checkAccess.checkManager(session, response, getServletContext())) return;

        User user=(User)session.getAttribute("user");
            
        ProjectDAO projectDAO=new ProjectDAO(connection);
        WorkPackageDAO wpDAO=new WorkPackageDAO(connection);
        TaskDAO taskDAO=new TaskDAO(connection);
        UserDAO userDAO=new UserDAO(connection);
        
        String idCollaboratoreStr=request.getParameter("idCollaboratore");

        try 
        {    
        	List<User> managedCollaborators=userDAO.findManagedCollaborators(user.getID());
        	
            List<Project> projects=new ArrayList<>();
            User selectedCollaborator=null;        
            
            if(idCollaboratoreStr!=null && !idCollaboratoreStr.isEmpty())
            {
                int idCollaboratore=Integer.parseInt(idCollaboratoreStr);
                selectedCollaborator=userDAO.getUser(idCollaboratore);
                
                projects=projectDAO.findProjectByCollaborator(idCollaboratore).stream().filter(p -> p.getIdResponsabile()==user.getID()).toList();

                for(Project p:projects) 
                {                   
                    if(p!=null)
                    {
                        List<WorkPackage> wps=wpDAO.findWPsWithCollaboratorInProject(p.getId(), idCollaboratore);
                        
                        for(WorkPackage wp:wps)
                            wp.setTasks(taskDAO.findTaskWithCollaboratorInWp(wp.getCodiceWP(), idCollaboratore));

                        p.setWps(wps);
                    }
                }
            }
            
            ctx.setVariable("user", user);
            ctx.setVariable("collaborators", managedCollaborators);
            ctx.setVariable("projects", projects);
            ctx.setVariable("selectedCollaborator", selectedCollaborator);

            templateEngine.process("monitorCollaborators", ctx, response.getWriter());

        } 
        catch(SQLException | NumberFormatException e) 
        {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento del monitoraggio collaboratore.");
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