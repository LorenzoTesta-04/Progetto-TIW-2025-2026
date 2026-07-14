package it.polimi.tiw.progetto2025.controllers.pagesCreators;

import java.io.IOException;
import java.sql.*;
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
import it.polimi.tiw.progetto2025.beans.Task;
import it.polimi.tiw.progetto2025.daos.ProjectDAO;
import it.polimi.tiw.progetto2025.daos.TaskDAO;
import it.polimi.tiw.progetto2025.daos.WorkPackageDAO;
import it.polimi.tiw.progetto2025.utils.checkAccess;
import it.polimi.tiw.progetto2025.daos.MyDAO;

public class VerifyProjects extends HttpServlet 
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
        templateResolver.setPrefix("/WEB-INF/pages/admin/");
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
            throw new ServletException("Impossibile connettersi al DB nella Servlet Admin", e);
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

        // Controllo accesso: deve essere loggato e deve essere ADMIN
        if(!checkAccess.checkAdmin(session, response, getServletContext())) return;
        
        User user=(User)session.getAttribute("user");
        String idProgettoStr=request.getParameter("idProgetto");
        ProjectDAO projectDAO=new ProjectDAO(connection);
        WorkPackageDAO workPackageDAO=new WorkPackageDAO(connection);
        TaskDAO taskDAO=new TaskDAO(connection);

        try 
        {
            List<Project> myProjects=projectDAO.findAllProjects(user.getID());
            Project selectedProject=null;
            int totalProjectHours=0;
            int totalWorkedHours=0;
            
            //Default: ultimo progetto creato
            if(idProgettoStr==null || idProgettoStr.isEmpty()) 
            {
                if 
                (myProjects!=null && !myProjects.isEmpty()) 
                {
                    //Ultimo progetto in costruzione (più recente)
                    Project defaultProject=null;
                    for (Project p:myProjects.reversed())
                        if(p.getStato()!=null && p.getStato().equalsIgnoreCase("CREATO")) 
                        {
                            defaultProject=p;
                            break;
                        }
                    
                    
                    //Fallback: se nessuno è in stato CREATO, prendiamo il primo della lista
                    if(defaultProject==null && myProjects.getFirst()!=null) defaultProject=myProjects.getFirst();
                        
                    idProgettoStr=String.valueOf(defaultProject.getId());
                }
            }
            
            //Caricamento dell'albero gerarchico del progetto selezionato
            if(idProgettoStr!=null && !idProgettoStr.isEmpty()) 
            {
                int idProgetto=Integer.parseInt(idProgettoStr);
                selectedProject=projectDAO.findProjectById(idProgetto);
                
                if(selectedProject!=null) 
                {
                    List<WorkPackage> wps=workPackageDAO.findWPsByProject(idProgetto);
                    
                    if(wps!=null)
                    {
                    	for(WorkPackage wp:wps)
                    	{
                    		List<Task> tasks=taskDAO.findTasksByWP(wp.getCodiceWP());
                    		wp.setTasks(tasks);
                    		
                    		 if(tasks!=null) 
                                 for(Task t:tasks) 
                                 {
                                	 totalProjectHours+=t.getOrePrevisteTotali();
                                	 totalWorkedHours+=t.getOreLavorateTotali();
                                 }
                    	}
                    }
                    
                    selectedProject.setWps(wps);
                }
            }
            
            ctx.setVariable("user", user);
            ctx.setVariable("myProjects", myProjects);
            ctx.setVariable("selectedProject", selectedProject);
            ctx.setVariable("totalProjectHours", totalProjectHours);
            ctx.setVariable("totalWorkedHours", totalWorkedHours);

            templateEngine.process("verifyProjects", ctx, response.getWriter());
            
        }
        catch (SQLException e) 
        {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore SQL nel recupero dell'albero di progetto.");
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