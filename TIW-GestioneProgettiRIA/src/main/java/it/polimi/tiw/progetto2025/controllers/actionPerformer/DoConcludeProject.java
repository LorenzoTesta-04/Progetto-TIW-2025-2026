package it.polimi.tiw.progetto2025.controllers.actionPerformer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.polimi.tiw.progetto2025.beans.Project;
import it.polimi.tiw.progetto2025.beans.Task;
import it.polimi.tiw.progetto2025.beans.WorkPackage;
import it.polimi.tiw.progetto2025.beans.User;
import it.polimi.tiw.progetto2025.daos.MyDAO;
import it.polimi.tiw.progetto2025.daos.ProjectDAO;
import it.polimi.tiw.progetto2025.daos.TaskDAO;
import it.polimi.tiw.progetto2025.daos.WorkPackageDAO;
import it.polimi.tiw.progetto2025.utils.checkAccess;

public class DoConcludeProject extends HttpServlet 
{
	private static final long serialVersionUID=1L;
	private Connection connection=null;

	@Override
	public void init() throws ServletException 
	{
		try 
		{
			new com.mysql.cj.jdbc.Driver();
			this.connection=DriverManager.getConnection(MyDAO.DB_URL, MyDAO.DB_USER, MyDAO.DB_PASS);
		} 
		catch (SQLException e) 
		{
			throw new ServletException("Impossibile connettersi al DB nella Servlet DoConcludeProject", e);
		}
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		HttpSession session=request.getSession(false);

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

		if(!checkAccess.checkManager(session, response, getServletContext()))
		{
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		User user=(User) session.getAttribute("user");

		JsonElement root=JsonParser.parseReader(request.getReader());
		JsonObject json=root.getAsJsonObject();

		if(!json.has("idProgetto")) 
		{
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().write("{\"error\": \"ID Progetto mancante nel corpo della richiesta.\"}");
			return;
		}

		int idProgetto=json.get("idProgetto").getAsInt();

		try 
		{
			ProjectDAO projectDAO=new ProjectDAO(connection);
			WorkPackageDAO workPackageDAO=new WorkPackageDAO(connection);
			TaskDAO taskDAO=new TaskDAO(connection);

			Project project=projectDAO.findProjectById(idProgetto);

			//Validazione
			if(project==null || project.getIdResponsabile()!=user.getID()) 
			{
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				response.getWriter().write("{\"error\": \"Non sei autorizzato a modificare questo progetto o il progetto non esiste.\"}");
				return;
			}

			if(!"ASSEGNATO".equals(project.getStato())) 
			{
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().write("{\"error\": \"Il progetto deve essere in stato ASSEGNATO per poter essere concluso.\"}");
				return;
			}

			//Verifica ore
			List<WorkPackage> wps=workPackageDAO.findWPsByProject(idProgetto);
			if(wps==null || wps.isEmpty()) 
			{
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().write("{\"error\": \"Impossibile concludere un progetto privo di Work Package.\"}");
				return;
			}

			for(WorkPackage wp:wps) 
			{
				List<Task> tasks=taskDAO.findTasksByWP(wp.getCodiceWP());
				if(tasks==null || tasks.isEmpty()) 
				{
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					response.getWriter().write("{\"error\": \"Tutti i Work Package devono includere almeno un task prima della chiusura.\"}");
					return;
				}

				for(Task t:tasks) 
					if(t.getOreLavorateTotali()<t.getOrePrevisteTotali()) 
					{
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						response.getWriter().write("{\"error\": \"Impossibile concludere: il task '"+t.getNomeTask()+"' ha ore effettive inferiori a quelle preventivate.\"}");
						return;
					}
			}

			projectDAO.updateProjectState(idProgetto, Project.projectState.CONCLUSO);
			response.getWriter().write("{\"success\": true}");
		} 
		catch (SQLException e) 
		{
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().write("{\"error\": \"Errore interno del database durante il salvataggio dello stato.\"}");
		}
	}

	@Override
	public void destroy() 
	{
		if(connection != null)
			try {connection.close();} 
			catch(SQLException ignored) {}
	}
}