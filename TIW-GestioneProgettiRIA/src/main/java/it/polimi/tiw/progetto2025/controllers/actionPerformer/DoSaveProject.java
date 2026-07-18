package it.polimi.tiw.progetto2025.controllers.actionPerformer;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.polimi.tiw.progetto2025.beans.Task;
import it.polimi.tiw.progetto2025.beans.User;
import it.polimi.tiw.progetto2025.beans.WorkPackage;
import it.polimi.tiw.progetto2025.daos.MyDAO;
import it.polimi.tiw.progetto2025.daos.ProjectDAO;
import it.polimi.tiw.progetto2025.daos.TaskDAO;
import it.polimi.tiw.progetto2025.daos.WorkPackageDAO;
import it.polimi.tiw.progetto2025.utils.checkAccess;

public class DoSaveProject extends HttpServlet 
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
		catch(SQLException e) 
		{
			throw new ServletException("Impossibile connettersi al DB nella Servlet DoSaveProject", e);
		}
	}
	
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		HttpSession session=request.getSession(false);

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

		if(!checkAccess.checkAdmin(session, response, getServletContext()))
		{
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		User user=(User) session.getAttribute("user");
		
		JsonElement root=JsonParser.parseReader(request.getReader());
		JsonObject json=root.getAsJsonObject();
		
		try 
		{
			//Validazione
			if(!json.has("nomeProgetto") || !json.has("durata") || !json.has("idResponsabile") || !json.has("workPackages")) 
			{
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().write("{\"error\": \"Struttura dati JSON non conforme o parametri obbligatori omessi.\"}");
				return;
			}

			String nomeProgetto=json.get("nomeProgetto").getAsString().trim();
			int durata=json.get("durata").getAsInt();
			int idResponsabile=json.get("idResponsabile").getAsInt();
			JsonArray wpArray=json.getAsJsonArray("workPackages");

			if(nomeProgetto.isEmpty()) 
			{
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().write("{\"error\": \"Il nome del progetto non può essere vuoto.\"}");
				return;
			}

			if(durata<=0) 
			{
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().write("{\"error\": \"La durata del progetto deve essere maggiore di zero.\"}");
				return;
			}

			if(wpArray==null || wpArray.size()==0) 
			{
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().write("{\"error\": \"Attenzione: definire almeno un Work Package prima di salvare.\"}");
				return;
			}

			for(JsonElement wpElem:wpArray) 
			{
				if(!wpElem.isJsonObject()) 
				{
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					response.getWriter().write("{\"error\": \"Formato del Work Package non valido.\"}");
					return;
				}

				JsonObject wpObj=wpElem.getAsJsonObject();
				if(!wpObj.has("nomeWP") || !wpObj.has("meseInizio") || !wpObj.has("meseFine") || !wpObj.has("tasks")) 
				{
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					response.getWriter().write("{\"error\": \"Attributi del Work Package incompleti.\"}");
					return;
				}

				String nomeWP=wpObj.get("nomeWP").getAsString().trim();
				int wpInizio=wpObj.get("meseInizio").getAsInt();
				int wpFine=wpObj.get("meseFine").getAsInt();
				JsonArray taskArray=wpObj.getAsJsonArray("tasks");

				if(nomeWP.isEmpty()) 
				{
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					response.getWriter().write("{\"error\": \"Il titolo dei Work Package non può essere vuoto.\"}");
					return;
				}

				// Coerenza interna e di progetto del WP
				if(wpInizio<1 || wpInizio>wpFine || wpFine>durata) 
				{
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					response.getWriter().write("{\"error\": \"Coerenza temporale del '"+nomeWP+"'non valida.\"}");
					return;
				}

				if(taskArray==null || taskArray.size()==0) 
				{
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					response.getWriter().write("{\"error\": \"Errore: il WP '"+nomeWP+"' non contiene alcun Task.\"}");
					return;
				}

				//Coerenza temporale dei singoli Task rispetto al WP genitore
				for(JsonElement tElem:taskArray) 
				{
					if(!tElem.isJsonObject()) 
					{
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						response.getWriter().write("{\"error\": \"Formato del Task non valido nel WP '"+nomeWP+"'.\"}");
						return;
					}

					JsonObject tObj=tElem.getAsJsonObject();
					if(!tObj.has("nomeTask") || !tObj.has("meseInizio") || !tObj.has("meseFine") || !tObj.has("descrizione")) 
					{
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						response.getWriter().write("{\"error\": \"Attributi del Task incompleti nel WP '"+nomeWP+"'.\"}");
						return;
					}

					String nomeTask=tObj.get("nomeTask").getAsString().trim();
					int tInizio=tObj.get("meseInizio").getAsInt();
					int tFine=tObj.get("meseFine").getAsInt();

					if(nomeTask.isEmpty()) 
					{
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						response.getWriter().write("{\"error\": \"Il titolo del Task nel WP '"+nomeWP+"' non può essere vuoto.\"}");
						return;
					}

					if(tInizio>tFine || tInizio<wpInizio || tFine>wpFine) 
					{
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						response.getWriter().write("{\"error\": \"Errore di coerenza temporale nel Task '"+nomeTask+"'.\"}");
						return;
					}

				}
			}

			connection.setAutoCommit(false);

			ProjectDAO pDAO=new ProjectDAO(connection);
			pDAO.createProject(nomeProgetto, durata, idResponsabile, user.getID());
			int idProgetto=pDAO.findAllProjects(user.getID()).stream()
					.filter(p -> p.getNomeProgetto().equals(nomeProgetto))
					.findFirst()
					.orElseThrow(() -> new SQLException("Errore nel recupero del progetto appena creato"))
					.getId();

			// Salvataggio WP
			WorkPackageDAO wpDAO=new WorkPackageDAO(connection);
			for(JsonElement wpElem:wpArray) 
			{
				JsonObject wpObj=wpElem.getAsJsonObject();
				String nomeWP=wpObj.get("nomeWP").getAsString();
				
				wpDAO.createWorkPackage(idProgetto, nomeWP, wpObj.get("meseInizio").getAsInt(), wpObj.get("meseFine").getAsInt());
			}
			
			Map<String, Integer> wpMap=new HashMap<>();
			List<WorkPackage> wps=wpDAO.findWPsByProject(idProgetto);
			for(WorkPackage wp:wps)
				wpMap.put(wp.getTitolo(), wp.getCodiceWP());

			// Salvataggio task
			TaskDAO tDAO=new TaskDAO(connection);
			for(JsonElement wpElem:wpArray) 
			{
				JsonObject wpObj=wpElem.getAsJsonObject();
				String nomeWP=wpObj.get("nomeWP").getAsString();
				int idWpCorrente=wpMap.get(nomeWP); 
				
				JsonArray taskArray=wpObj.getAsJsonArray("tasks");
				for(JsonElement tElem:taskArray) 
				{
					JsonObject tObj=tElem.getAsJsonObject();
					tDAO.createTask(
						idWpCorrente,
						tObj.get("nomeTask").getAsString(), 
						tObj.get("descrizione").getAsString(), 
						tObj.get("meseInizio").getAsInt(), 
						tObj.get("meseFine").getAsInt()
					);
				}
			}

			connection.commit();

			//Risposta
			JsonObject pJson=new JsonObject();
			pJson.addProperty("id", idProgetto);
			pJson.addProperty("durata", durata);
			pJson.addProperty("nomeProgetto", nomeProgetto);
			pJson.addProperty("stato", "CREATO");

			int projTotalHours=0;
			int projWorkedHours=0;

			JsonArray jsonWpsArray=new JsonArray();
			if(wps != null) 
			{
				for(WorkPackage wp:wps) 
				{
					JsonObject wpJson=new JsonObject();
					wpJson.addProperty("codiceWP", wp.getCodiceWP());
					wpJson.addProperty("titolo", wp.getTitolo());
					wpJson.addProperty("meseInizio", wp.getMeseInizio());
					wpJson.addProperty("meseFine", wp.getMeseFine());
					wpJson.addProperty("numeroOrdine", wp.getNumeroOrdine());

					JsonArray jsonTasksArray=new JsonArray();
					List<Task> tasks=tDAO.findTasksByWP(wp.getCodiceWP());
					
					if(tasks!=null) 
					{
						for(Task t:tasks) 
						{
							projTotalHours+=t.getOrePrevisteTotali();
							projWorkedHours+=t.getOreLavorateTotali();

							JsonObject tJson=new JsonObject();
							tJson.addProperty("idTask", t.getId());
							tJson.addProperty("nomeTask", t.getNomeTask());
							tJson.addProperty("descrizione", t.getDescrizione());
							tJson.addProperty("meseInizio", t.getMeseInizio()); 
							tJson.addProperty("meseFine", t.getMeseFine());
							tJson.addProperty("orePreviste", t.getOrePrevisteTotali());
							tJson.addProperty("oreLavorate", t.getOreLavorateTotali());
							tJson.addProperty("numeroOrdine", t.getNumeroOrdine());
							
							jsonTasksArray.add(tJson);
						}
					}
					wpJson.add("tasks", jsonTasksArray);
					jsonWpsArray.add(wpJson);
				}
			}

			pJson.add("wps", jsonWpsArray);
			pJson.addProperty("totalProjectHours", projTotalHours);
			pJson.addProperty("totalWorkedHours", projWorkedHours);

			response.getWriter().write(pJson.toString());
		} 
		catch(Exception e) 
		{
			try { connection.rollback(); } 
			catch(SQLException ignore) {}
			
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().write("{\"error\": \"Errore interno del server durante il salvataggio: "+e.getMessage()+"\"}");
		} 
		finally
		{
			try { connection.setAutoCommit(true); }
			catch(SQLException ignore) {}
		}
	}

	@Override
	public void destroy()
	{
		if(connection!=null)
			try { connection.close(); }
			catch(SQLException ignored) {}
	}
}