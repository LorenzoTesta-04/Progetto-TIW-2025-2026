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

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import it.polimi.tiw.progetto2025.beans.User;
import it.polimi.tiw.progetto2025.beans.exceptions.CheckAuthException;
import it.polimi.tiw.progetto2025.beans.exceptions.DBException;
import it.polimi.tiw.progetto2025.daos.MyDAO;
import it.polimi.tiw.progetto2025.daos.UserDAO;

public class DoLogin extends HttpServlet 
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
			throw new ServletException("Impossibile connettersi al DB nella Servlet DoLogin", e);
		}
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		
		String username=request.getParameter("username");
		String password=request.getParameter("password");
		
		if(username==null || username.trim().isEmpty() || password==null || password.trim().isEmpty())
		{
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("{\"error\": \"Username e password non possono essere vuote.\"}");
			return;
		}
		
		UserDAO userDAO=new UserDAO(connection);
		
		//Login
		try
		{
			User user=userDAO.checkAuth(username, password);
			
			HttpSession session=request.getSession(true);
			session.setAttribute("user", user);
			
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");    
			
			boolean requiresChoice=user.isManager() && user.isCollaborator();
			
			String redirectUrl="";
			if(user.isAdmin()) redirectUrl="admin.html";
			else if(requiresChoice) redirectUrl="ChooseRole"; 
			else if(user.isManager()) redirectUrl="manager.html";
			else if(user.isCollaborator()) redirectUrl="collaborator.html";
			else
			{
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.setContentType("application/json");
				response.setCharacterEncoding("UTF-8");
				response.getWriter().write("{\"error\": \"Nessun ruolo assegnato. Contattare l'amministratore.\"}");
				return;
			}

		   	JsonObject jsonResponse=new JsonObject();
			jsonResponse.addProperty("redirectUrl", redirectUrl);
			jsonResponse.addProperty("requiresChoice", requiresChoice);
			jsonResponse.addProperty("nome", user.getNome());
			jsonResponse.addProperty("cognome", user.getCognome());
			jsonResponse.addProperty("profilePicturePath", user.getProfilePicturePath());
			
			response.getWriter().write(jsonResponse.toString());
		}
		catch(CheckAuthException e)
		{
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write("{\"error\": \"Credenziali non valide.\"}");
		} 
		catch(DBException e) 
		{
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write("{\"error\": \"Errore nel database.\"}");
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