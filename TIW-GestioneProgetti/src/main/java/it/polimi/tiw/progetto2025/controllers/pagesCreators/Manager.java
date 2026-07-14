package it.polimi.tiw.progetto2025.controllers.pagesCreators;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

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
import it.polimi.tiw.progetto2025.daos.UserDAO;
import it.polimi.tiw.progetto2025.daos.WorkPackageDAO;
import it.polimi.tiw.progetto2025.utils.checkAccess;

public class Manager extends HttpServlet 
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
            throw new ServletException("Impossibile connettersi al DB nella Servlet Manager", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
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

        User user=(User)session.getAttribute("user");
        ProjectDAO projectDAO=new ProjectDAO(connection);
        WorkPackageDAO workPackageDAO=new WorkPackageDAO(connection);
        TaskDAO taskDAO=new TaskDAO(connection);
        UserDAO userDAO=new UserDAO(connection);

        String idProgettoStr=request.getParameter("idProgetto");
        String codiceWPStr=request.getParameter("codiceWP");
        
        String errorMsg="";
        String msg="";
        String submittedIdTask="";
        List<String> submittedCollaboratori=new ArrayList<>();
        Map<String, String> submittedHoursMap=new HashMap<>();

        //Recupero dati se si è verificato errore
        if(session != null) 
        {
            if (session.getAttribute("errorMsg")!=null) 
            {
                errorMsg=(String) session.getAttribute("errorMsg");
                String originForm=(String)session.getAttribute("originForm");
                
                if("ALLOCATION".equals(originForm)) 
                {
                    submittedIdTask=(String) session.getAttribute("submittedIdTask");
                    submittedCollaboratori=(List<String>) session.getAttribute("submittedCollaboratori");
                    submittedHoursMap=(Map<String, String>) session.getAttribute("submittedHoursMap");
                }
                
                session.removeAttribute("errorMsg");
                session.removeAttribute("originForm");
                session.removeAttribute("submittedIdTask");
                session.removeAttribute("submittedCollaboratori");
                session.removeAttribute("submittedHoursMap");
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
            WorkPackage selectedWP=null;
            List<WorkPackage> wps=null;
            List<Task> tasks=null;
            List<User> collaborators=null;
            boolean isAssignable=false;

            if(idProgettoStr!=null && !idProgettoStr.isEmpty()) 
            {
                try 
                {
                    int idProgetto=Integer.parseInt(idProgettoStr);
                    selectedProject=projectDAO.findProjectById(idProgetto);

                    if(selectedProject!=null && selectedProject.getIdResponsabile()==user.getID()) 
                    {
                        wps=workPackageDAO.findWPsByProject(idProgetto);
                        isAssignable="CREATO".equals(selectedProject.getStato()) && projectDAO.isAssignable(idProgetto);
                        
                        if(codiceWPStr!=null && !codiceWPStr.isEmpty()) 
                        {
                            try 
                            {
                                int codiceWP=Integer.parseInt(codiceWPStr);
                                tasks=taskDAO.findTasksByWP(codiceWP);
                                collaborators=userDAO.findAllCollaborators().stream().filter(u -> u.getID()!=user.getID()).toList();
                                
                                if(tasks!=null)
                                    for(Task t:tasks)
                                        t.setCollaboratori(taskDAO.findCollaboratorsByTask(t.getId()));

                                if(wps!=null)
                                    for(WorkPackage wp:wps)
                                        if(wp.getCodiceWP()==codiceWP) 
                                        {
                                            selectedWP=wp;
                                            break;
                                        }
                            } 
                            catch(NumberFormatException ignored) {}
                        }
                    }
                } 
                catch(NumberFormatException ignored) {}
            }
            
            ctx.setVariable("user", user);
            ctx.setVariable("selectedProject", selectedProject);
            ctx.setVariable("wps", wps);
            ctx.setVariable("selectedWP", selectedWP);
            ctx.setVariable("tasks", tasks);
            ctx.setVariable("collaborators", collaborators);
            ctx.setVariable("isAssignable", isAssignable);
            
            ctx.setVariable("submittedIdTask", submittedIdTask);
            ctx.setVariable("submittedCollaboratori", submittedCollaboratori);
            ctx.setVariable("submittedHoursMap", submittedHoursMap);
            
            ctx.setVariable("errorMsg", errorMsg);
            ctx.setVariable("msg", msg);

            templateEngine.process("manager", ctx, response.getWriter());
            
        } 
        catch(SQLException e) 
        {
            ctx.setVariable("errorMsg", "Servizio temporaneamente non disponibile: errore di connessione al database.");
            ctx.setVariable("user", user);
            templateEngine.process("manager", ctx, response.getWriter());
        }
    }
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
    {
        HttpSession session=request.getSession(false);
        
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); 
        
        if(!checkAccess.checkManager(session, response, getServletContext())) return;

        User user=(User) session.getAttribute("user");
        ProjectDAO projectDAO=new ProjectDAO(connection);
        
        String idProgettoStr=request.getParameter("idProgetto");
        if(idProgettoStr==null || idProgettoStr.isEmpty())
        {
            if(session!=null) 
                session.setAttribute("errorMsg", "Impossibile assegnare: ID Progetto mancante.");
  
            response.sendRedirect(request.getContextPath()+"/Manager");
            return;
        }
        
        int idProgetto;
        try
        {
            idProgetto=Integer.parseInt(idProgettoStr);
        }
        catch(NumberFormatException e)
        {
            if(session!=null) session.setAttribute("errorMsg", "Formato ID Progetto non valido.");
            response.sendRedirect(request.getContextPath()+"/Manager");
            return;
        }
        
        try 
        {
            Project p=projectDAO.findProjectById(idProgetto);
            if(p==null)
            {
                if(session!=null) session.setAttribute("errorMsg", "Impossibile assegnare: Progetto inesistente.");

                response.sendRedirect(request.getContextPath()+"/Manager");
                return;
            }
            
            if(p.getIdResponsabile()!=user.getID())
            {
            	if(session!=null) session.setAttribute("errorMsg", "Non hai le autorizzazioni per amministrare questo progetto.");
            	
                response.sendRedirect(request.getContextPath()+"/Manager");
                return;
            }
            
            if(!"CREATO".equals(p.getStato().toString())) 
            {
            	if(session!=null) session.setAttribute("errorMsg", "Il progetto è già stato assegnato o risulta concluso.");
            	
                response.sendRedirect(request.getContextPath()+"/Manager?idProgetto="+idProgettoStr);
                return;
            }
            
            if(!projectDAO.isAssignable(idProgetto)) 
            {
            	if(session!=null) session.setAttribute("errorMsg", "Impossibile procedere: il progetto non rispetta i criteri minimi di assegnazione.");

                response.sendRedirect(request.getContextPath()+"/Manager?idProgetto="+idProgettoStr);
                return;
            }
            
            projectDAO.updateProjectState(idProgetto, Project.projectState.ASSEGNATO);
            
            if(session!=null) session.setAttribute("successMsg", "Progetto assegnato con successo!");
            response.sendRedirect(request.getContextPath()+"/Manager?idProgetto="+idProgettoStr);
        } 
        catch(SQLException e) 
        {
            if(session!=null) session.setAttribute("errorMsg", "Errore del database durante il tentativo di assegnazione del progetto.");

            response.sendRedirect(request.getContextPath()+"/Manager?idProgetto="+idProgettoStr);
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