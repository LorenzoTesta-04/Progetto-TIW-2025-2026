package it.polimi.tiw.progetto2025.controllers.actionPerformer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import it.polimi.tiw.progetto2025.beans.WorkPackage;
import it.polimi.tiw.progetto2025.daos.MyDAO;
import it.polimi.tiw.progetto2025.daos.TaskDAO;
import it.polimi.tiw.progetto2025.daos.WorkPackageDAO;
import it.polimi.tiw.progetto2025.utils.checkAccess;

public class DoCreateTask extends HttpServlet 
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
			throw new ServletException("Impossibile connettersi al DB nella Servlet DoCreateTask", e);
		}
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		HttpSession session=request.getSession(false);
		
		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); 
		
		if(!checkAccess.checkAdmin(session, response, getServletContext())) return;

		String idProgettoStr=request.getParameter("idProgetto");
		String idWPStr=request.getParameter("idWP");
		String nomeTask=request.getParameter("nomeTask");
		String descrizione=request.getParameter("descrizione");
		String meseInizioStr=request.getParameter("meseInizio");
		String meseFineStr=request.getParameter("meseFine");

		String errorMsg=null;
		int idWP=-1;
		int meseInizio=-1;
		int meseFine=-1;
		WorkPackage chosenWP=null;
		
		WorkPackageDAO workPackageDAO=new WorkPackageDAO(connection);

		try 
		{
			if(nomeTask==null || nomeTask.trim().isEmpty()) 
				errorMsg="Il titolo del task non può essere vuoto.";
			else if(idWPStr==null || meseInizioStr==null || meseFineStr==null) 
				errorMsg="Tutti i campi temporali e di selezione sono obbligatori.";
			else 
			{
				idWP=Integer.parseInt(idWPStr);
				meseInizio=Integer.parseInt(meseInizioStr);
				meseFine=Integer.parseInt(meseFineStr);
				
				if(meseInizio<=0 || meseFine<=0) errorMsg="I valori numerici dei mesi devono essere maggiori di zero.";
				else if(meseInizio>meseFine) errorMsg="Il mese di inizio non può essere successivo al mese di fine.";
				else  
				{
					chosenWP=workPackageDAO.findWPById(idWP);
					
					if(chosenWP==null) errorMsg="Work Package di riferimento non trovato.";
					else if(meseInizio<chosenWP.getMeseInizio() || meseFine>chosenWP.getMeseFine()) 
						errorMsg="I mesi del task devono essere inclusi nel range del WP ("+chosenWP.getMeseInizio()+" - "+chosenWP.getMeseFine()+")."; 
				}
			}
		} 
		catch(NumberFormatException e) 
		{
			errorMsg="Dati inseriti non validi. Controlla il formato dei numeri.";
		}
		catch(SQLException e)
		{
			errorMsg="Errore di comunicazione con il database durante la verifica dei dati.";
		}

		if(errorMsg==null) 
		{
			TaskDAO taskDAO=new TaskDAO(connection);
			try 
			{
				taskDAO.createTask(idWP, nomeTask, descrizione, meseInizio, meseFine);
				response.sendRedirect(getServletContext().getContextPath()+"/Admin");
				return;
			} 
			catch(SQLException e) 
			{
				errorMsg="Impossibile completare il salvataggio del task nel Database a causa di un vincolo o un errore di sistema.";
			}
		}

		//Per ripristino
		if(session!=null) 
		{
			session.setAttribute("errorMsg", errorMsg);
			session.setAttribute("originForm", "TASK");
			session.setAttribute("selectedProgettoId", idProgettoStr);
			session.setAttribute("submittedWP", idWPStr);
			session.setAttribute("submittedNomeTask", nomeTask);
			session.setAttribute("submittedDescrizione", descrizione);
			session.setAttribute("submittedMeseInizioTask", meseInizioStr);
			session.setAttribute("submittedMeseFineTask", meseFineStr);
		}

		response.sendRedirect(getServletContext().getContextPath()+"/Admin");
	}
	
	@Override
	public void destroy()
	{
		if(connection!=null)
			try {connection.close();}
			catch(SQLException ignored) {}
	}
}