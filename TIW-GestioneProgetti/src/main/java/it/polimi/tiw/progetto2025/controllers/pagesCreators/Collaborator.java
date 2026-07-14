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

import it.polimi.tiw.progetto2025.beans.User;
import it.polimi.tiw.progetto2025.beans.WorkPackage;
import it.polimi.tiw.progetto2025.beans.Project;
import it.polimi.tiw.progetto2025.beans.Task;
import it.polimi.tiw.progetto2025.daos.MyDAO;
import it.polimi.tiw.progetto2025.daos.ProjectDAO;
import it.polimi.tiw.progetto2025.daos.TaskDAO;
import it.polimi.tiw.progetto2025.daos.WorkPackageDAO;
import it.polimi.tiw.progetto2025.utils.checkAccess;

public class Collaborator extends HttpServlet 
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
		templateResolver.setPrefix("/WEB-INF/pages/collaborator/");
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
			throw new ServletException("Impossibile connettersi al DB nella Servlet Collaborator", e);
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
	 
		 if(!checkAccess.checkCollaborator(session, response, getServletContext())) return;

		User user=(User) session.getAttribute("user");
		ProjectDAO projectDAO=new ProjectDAO(connection);
		WorkPackageDAO wpDAO=new WorkPackageDAO(connection);
		TaskDAO taskDAO=new TaskDAO(connection);
		
		//Caso base: lettura da URL 
		String idProgettoStr=request.getParameter("idProgetto");
		String idTaskStr=request.getParameter("idTask");
		String meseStr=request.getParameter("mese");
		
		String errorMsg=null;
		String oreLavorateInSessione=null;

		//Errori da salvataggio
		if(session!=null && session.getAttribute("errorMsg")!=null) 
		{
			errorMsg=(String) session.getAttribute("errorMsg");
			oreLavorateInSessione=(String) session.getAttribute("oldOreLavorate");
			meseStr=(String) session.getAttribute("oldMese");
			idProgettoStr=(String) session.getAttribute("oldIdProgetto");
			idTaskStr=(String) session.getAttribute("oldIdTask");
			
			session.removeAttribute("errorMsg");
			session.removeAttribute("oldOreLavorate");
			session.removeAttribute("oldMese");
			session.removeAttribute("oldIdProgetto");
			session.removeAttribute("oldIdTask");
		}

		try 
		{        	
			//Progetti in cui il collaboratore è assegnato
			List<Project> projects=projectDAO.findProjectByCollaborator(user.getID())
											   .stream()
											   .filter(p -> p.getStato().equalsIgnoreCase("ASSEGNATO"))
											   .toList();

			Project selectedProject=null;        
			Task selectedTask=null;
			Integer selectedMese=null;
			Object oreLavorateNelMese=0;

			if(idProgettoStr!=null && !idProgettoStr.isEmpty()) 
			{

				int idProgetto=Integer.parseInt(idProgettoStr);
				
				//Verifica che il progetto sia corretto
				boolean isAuthorizedProject=projects.stream().anyMatch(p -> p.getId()==idProgetto);
				
				if(isAuthorizedProject) 
				{
					selectedProject=projectDAO.findProjectById(idProgetto);
					
					if(selectedProject!=null) 
					{
						//Carichiamo WP e Task
						List<WorkPackage> wps=wpDAO.findWPsWithCollaboratorInProject(idProgetto, user.getID());
						for(WorkPackage wp:wps)
							wp.setTasks(taskDAO.findTaskWithCollaboratorInWp(wp.getCodiceWP(), user.getID()));
						selectedProject.setWps(wps);
						
						if(idTaskStr!=null && !idTaskStr.isEmpty()) 
						{
							int idTask=Integer.parseInt(idTaskStr);
							
							boolean isTaskDelProgetto=wps.stream().flatMap(wp -> wp.getTasks().stream()).anyMatch(t -> t.getId()==idTask);
							
							if(isTaskDelProgetto) 
							{
								selectedTask=taskDAO.findTaskByIdWithCollaborator(idTask, user.getID()); 
								
								if(selectedTask!=null && meseStr!=null && !meseStr.isEmpty()) 
								{
									selectedMese=Integer.parseInt(meseStr);
									
									if(errorMsg!=null) oreLavorateNelMese=oreLavorateInSessione;
									else
										if(selectedTask.getOreLavorate().containsKey(selectedMese)) oreLavorateNelMese=selectedTask.getOreLavorate().get(selectedMese);
										else errorMsg="Accesso non autorizzato al mese specificato per questo task.";
								}
							}
							else 
								errorMsg="Accesso non autorizzato al task specificato per questo progetto.";
						}
					}
				} 
				else 
					errorMsg="Accesso non autorizzato al progetto specificato.";
			}
			
			ctx.setVariable("user", user);
			ctx.setVariable("availableProjects", projects);
			ctx.setVariable("selectedProject", selectedProject);
			ctx.setVariable("selectedTask", selectedTask);
			ctx.setVariable("selectedMese", selectedMese);
			ctx.setVariable("oreLavorateNelMese", oreLavorateNelMese);
			ctx.setVariable("errorMsg", errorMsg);

			templateEngine.process("collaborator", ctx, response.getWriter());

		} 
		catch(SQLException | NumberFormatException e)
        {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento della dashboard amministratore");
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