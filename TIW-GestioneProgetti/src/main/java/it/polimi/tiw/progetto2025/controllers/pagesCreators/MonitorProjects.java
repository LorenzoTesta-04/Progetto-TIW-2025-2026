package it.polimi.tiw.progetto2025.controllers.pagesCreators;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
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

import it.polimi.tiw.progetto2025.beans.Project;
import it.polimi.tiw.progetto2025.beans.Task;
import it.polimi.tiw.progetto2025.beans.User;
import it.polimi.tiw.progetto2025.beans.WorkPackage;
import it.polimi.tiw.progetto2025.daos.MyDAO;
import it.polimi.tiw.progetto2025.daos.ProjectDAO;
import it.polimi.tiw.progetto2025.daos.TaskDAO;
import it.polimi.tiw.progetto2025.daos.WorkPackageDAO;
import it.polimi.tiw.progetto2025.utils.checkAccess;

public class MonitorProjects extends HttpServlet 
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
        catch (SQLException e) 
        {
            throw new ServletException("Impossibile connettersi al DB nella Servlet MonitorProjects", e);
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
    
        if(!checkAccess.checkManager(session, response, getServletContext())) return;

        User user=(User) session.getAttribute("user");
        ProjectDAO projectDAO=new ProjectDAO(connection);
        WorkPackageDAO workPackageDAO=new WorkPackageDAO(connection);
        TaskDAO taskDAO=new TaskDAO(connection);

        String idProgettoStr=request.getParameter("idProgetto");
        
        String errorMsg="";
        String msg="";
        
        if(session!=null) 
        {
            if(session.getAttribute("errorMsg")!=null) 
            {
                errorMsg=(String) session.getAttribute("errorMsg");
                session.removeAttribute("errorMsg");
            }
            
            if(session.getAttribute("successMsg")!=null) 
            {
                msg=(String) session.getAttribute("successMsg");
                session.removeAttribute("successMsg");
            }
        }

        try 
        {
            List<Project> managedProjects=projectDAO.findProjectsByManager(user.getID());
            ctx.setVariable("managedProjects", managedProjects);
            
            Project selectedProject=null;
            int totalAllocatedHours=0;
            int totalPlannedHours=0;

            if(idProgettoStr!=null && !idProgettoStr.isEmpty()) 
            {
                try {
                    int idProgetto=Integer.parseInt(idProgettoStr);
                    selectedProject=projectDAO.findProjectById(idProgetto);

                    if(selectedProject!=null && selectedProject.getIdResponsabile()==user.getID()) 
                    {
                        List<WorkPackage> wps=workPackageDAO.findWPsByProject(idProgetto);
                        boolean canConclude="ASSEGNATO".equals(selectedProject.getStato()) && !wps.isEmpty();
                        
                        for(WorkPackage wp:wps) 
                        {
                            List<Task> tasks=taskDAO.findTasksByWP(wp.getCodiceWP());
                            
                            if(tasks.isEmpty()) canConclude=false;
                            
                            for(Task t:tasks) 
                            {
                                totalPlannedHours+=t.getOrePrevisteTotali();
                                totalAllocatedHours+=t.getOreLavorateTotali();
                                
                                if("ASSEGNATO".equals(selectedProject.getStato()) && t.getOreLavorateTotali()<=t.getOrePrevisteTotali())
                                    canConclude=false;
                            }
                            wp.setTasks(tasks);
                        }
                        selectedProject.setWps(wps);
                        ctx.setVariable("canConclude", canConclude);
                    }
                    else 
                        errorMsg="Non sei autorizzato a visionare questo progetto o il progetto non esiste.";

                } 
                catch(NumberFormatException e) 
                {
                    errorMsg="L'identificativo del progetto fornito non è valido.";
                }
            }
            
            ctx.setVariable("user", user);
            ctx.setVariable("selectedProject", selectedProject);
            ctx.setVariable("totalPlannedHours", totalPlannedHours);
            ctx.setVariable("totalAllocatedHours", totalAllocatedHours);
            ctx.setVariable("errorMsg", errorMsg);
            ctx.setVariable("msg", msg);
            
            templateEngine.process("monitorProjects", ctx, response.getWriter());

        } 
        catch (SQLException e) 
        {
            ctx.setVariable("errorMsg", "Servizio temporaneamente non disponibile: errore di connessione al database.");
            ctx.setVariable("user", user);
            templateEngine.process("monitorProjects", ctx, response.getWriter());
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