package it.polimi.tiw.progetto2025.controllers.actionPerformer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import it.polimi.tiw.progetto2025.beans.User;
import it.polimi.tiw.progetto2025.daos.MyDAO;
import it.polimi.tiw.progetto2025.daos.TaskDAO;
import it.polimi.tiw.progetto2025.utils.checkAccess;

public class DoSaveHours extends HttpServlet 
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
			throw new ServletException("Impossibile connettersi al DB nella Servlet DoSaveHours", e);
		}
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		HttpSession session=request.getSession(false);
		
		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); 
	
		if(!checkAccess.checkCollaborator(session, response, getServletContext())) return;
		
		User user=(User)session.getAttribute("user");
		
		String idProgettoStr=request.getParameter("idProgetto");
		String idTaskStr=request.getParameter("idTask");
		String meseStr=request.getParameter("mese");
		String oreLavorateStr=request.getParameter("oreLavorate");
		
		String errorMsg=null;
		int idProgetto=0, idTask=0, mese=0, oreLavorate=0;      	
			
		//Validazione input
		if(idTaskStr==null || idTaskStr.trim().isEmpty()) errorMsg="Task non specificato.";
		else if(meseStr==null || meseStr.trim().isEmpty()) errorMsg="Mese di riferimento non specificato.";
		else if(oreLavorateStr==null || oreLavorateStr.trim().isEmpty()) errorMsg="Il campo delle ore lavorate è obbligatorio.";
		else 
		{
			try 
			{
				idTask=Integer.parseInt(idTaskStr);
				mese=Integer.parseInt(meseStr);
				oreLavorate=Integer.parseInt(oreLavorateStr);
				
				if(oreLavorate<0) errorMsg="Il numero di ore inserite deve essere maggiore o uguale a zero.";
			} 
			catch(NumberFormatException e) 
			{
				errorMsg="Formato numerico dei parametri non valido.";
			}
		}
		
		//Salvataggio in DB
		if(errorMsg==null) 
		{
			TaskDAO TaskDAO=new TaskDAO(connection);
			try 
			{
				TaskDAO.updateHours(idTask, user.getID(), mese, oreLavorate); 
				response.sendRedirect(getServletContext().getContextPath()+"/Collaborator?idProgetto="+idProgetto+"&idTask="+idTask+"&mese="+mese+"&success=true");
				return;
			} 
			catch(SQLException e) 
			{
				errorMsg="Salvataggio delle ore fallito: errore di coerenza o connessione al database.";
			}
		}
		
		if(session!=null) 
		{      
			session.setAttribute("errorMsg", errorMsg);
			session.setAttribute("oldOreLavorate", oreLavorateStr);
			session.setAttribute("oldMese", meseStr);
			session.setAttribute("oldIdProgetto", idProgettoStr);
			session.setAttribute("oldIdTask", idTaskStr);
		}

		response.sendRedirect(getServletContext().getContextPath()+"/Collaborator");
	}
	
	@Override
	public void destroy()
	{
		if(connection!=null)
			try {connection.close();}
			catch(SQLException ignored) {}
	}

}
