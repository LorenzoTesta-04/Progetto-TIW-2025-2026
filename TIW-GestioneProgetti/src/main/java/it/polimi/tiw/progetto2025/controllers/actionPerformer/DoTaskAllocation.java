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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.polimi.tiw.progetto2025.beans.Project;
import it.polimi.tiw.progetto2025.beans.Task;
import it.polimi.tiw.progetto2025.beans.User;
import it.polimi.tiw.progetto2025.daos.MyDAO;
import it.polimi.tiw.progetto2025.daos.ProjectDAO;
import it.polimi.tiw.progetto2025.daos.TaskDAO;
import it.polimi.tiw.progetto2025.utils.checkAccess;

public class DoTaskAllocation extends HttpServlet 
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
			throw new ServletException("Impossibile connettersi al DB nella Servlet DoTaskAllocation", e);
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
		
		User user=(User)session.getAttribute("user");

		//Recupero parametri dal form
		String idProgettoStr=request.getParameter("idProgetto");
		String codiceWPStr=request.getParameter("codiceWP");
		String idTaskStr=request.getParameter("idTask");
		String[] idCollaboratoriArr=request.getParameterValues("idCollaboratori");

		//Parametri strutturali
		if(idProgettoStr==null || codiceWPStr==null || idTaskStr==null) 
		{
			if(session!=null) session.setAttribute("errorMsg", "Parametri identificativi dell'allocazione obbligatori mancanti.");
			response.sendRedirect(getServletContext().getContextPath()+"/Manager");
			return;
		}

		String redirectUrlBase=getServletContext().getContextPath()+"/Manager?idProgetto="+idProgettoStr+"&codiceWP="+codiceWPStr;

		String errorMsg=null;
		List<Integer> idCollaboratori=new ArrayList<>();
		Map<Integer, Integer> meseOreMap=new HashMap<>();
		Map<String, String> rawHoursInput=new HashMap<>(); 

		try 
		{
			int idProgetto=Integer.parseInt(idProgettoStr);
			int idTask=Integer.parseInt(idTaskStr);

			ProjectDAO projectDAO=new ProjectDAO(connection);
			TaskDAO taskDAO=new TaskDAO(connection);

			Project project=projectDAO.findProjectById(idProgetto);
			if(project==null || project.getIdResponsabile()!=user.getID()) errorMsg="Non sei autorizzato a modificare questo progetto o il progetto non esiste.";
			else if(!"CREATO".equalsIgnoreCase(project.getStato())) errorMsg="Impossibile allocare risorse in un progetto già assegnato o concluso.";

			Task task=taskDAO.findTaskById(idTask);
			if(task!=null) 
			{
				for(int m=task.getMeseInizio(); m<=task.getMeseFine(); m++) 
				{
					String fieldName="ore_task_"+idTask+"_mese_"+m;
					rawHoursInput.put(fieldName, request.getParameter(fieldName));
				}
			}
			else if(errorMsg==null) 
			{
				errorMsg="Task non trovato.";
			}

			//Validazione Collaboratori
			if(errorMsg==null) 
			{
				if(idCollaboratoriArr==null || idCollaboratoriArr.length==0) errorMsg="Devi selezionare almeno un collaboratore per il task.";
				else 
					for(String idCollStr:idCollaboratoriArr) 
					{
						int idColl=Integer.parseInt(idCollStr);
						if(idColl==user.getID()) 
						{
							errorMsg="Il responsabile non può autoassegnarsi ai task.";
							break;
						}
						idCollaboratori.add(idColl);
					}
			}

			//Validazione Mesi e Ore
			if(errorMsg==null && task!=null) 
			{
				for(int m=task.getMeseInizio(); m<=task.getMeseFine(); m++) 
				{
					String fieldName="ore_task_"+idTask+"_mese_"+m;
					String oreStr=rawHoursInput.get(fieldName);

					if(oreStr==null || oreStr.isEmpty()) 
					{
						errorMsg="Inserire le ore previste per il Mese "+m;
						break;
					}

					try 
					{
						int ore=Integer.parseInt(oreStr);
						if(ore<=0) 
						{
							errorMsg="Il quantitativo orario non può essere inferiore a 0 (Mese "+m+").";
							break;
						}
						meseOreMap.put(m, ore);
					} 
					catch(NumberFormatException e) 
					{
						errorMsg="Il formato orario per il Mese "+m+" deve essere un numero intero.";
						break;
					}
				}
			}

			//Salvataggio in db
			if(errorMsg==null) 
			{
				taskDAO.updateTaskAllocation(idTask, idCollaboratori, meseOreMap);
				if(session!=null) 
					session.setAttribute("successMsg", "Allocazione salvata con successo!");

				response.sendRedirect(redirectUrlBase);
				return;
			}
		} 
		catch(NumberFormatException e) 
		{
			errorMsg="Formato dei parametri numerici non valido.";
		} 
		catch(SQLException e) 
		{
			errorMsg="Errore nel salvataggio dell'allocazione sul database. Riprovare.";
		}

		//Per ripristino
		if(session!=null) 
		{
			session.setAttribute("errorMsg", errorMsg);
			session.setAttribute("originForm", "ALLOCATION");
			session.setAttribute("submittedIdTask", idTaskStr);
			session.setAttribute("submittedCollaboratori", idCollaboratoriArr!=null?List.of(idCollaboratoriArr):new ArrayList<String>());
			session.setAttribute("submittedHoursMap", rawHoursInput);
		}

		response.sendRedirect(redirectUrlBase);
	}
	
	@Override
	public void destroy()
	{
		if(connection!=null)
			try {connection.close();}
			catch(SQLException ignored) {}
	}
}