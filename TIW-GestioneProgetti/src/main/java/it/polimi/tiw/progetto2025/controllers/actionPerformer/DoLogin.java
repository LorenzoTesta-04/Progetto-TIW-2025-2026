package it.polimi.tiw.progetto2025.controllers.actionPerformer;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
    {   	
		String username=request.getParameter("username");
		String password=request.getParameter("password");
		
		String contextPath=getServletContext().getContextPath();

		//Verifica che nome e password non siano vuoti
		if(username==null || username.trim().isEmpty() || password==null || password.trim().isEmpty())
		{
			response.sendRedirect(contextPath+"/Login?error_msg="+URLEncoder.encode("Username e password obbligatori", "UTF-8"));
	        return;
		}
		
		UserDAO userDAO=new UserDAO(connection);
		
		//Login
		try
		{
			User user=userDAO.checkAuth(username, password);
			
			//Salva info dell'utente nella sessione
			HttpSession session=request.getSession(true);
			session.setAttribute("user", user);
			session.setAttribute("name", user.getNome());
			session.setAttribute("surname", user.getCognome());
			session.setAttribute("photo", user.getProfilePicturePath());
			
			if(user.isAdmin()) response.sendRedirect(contextPath+"/Admin");
			else if(user.isManager() && user.isCollaborator()) response.sendRedirect(contextPath+"/ChooseRole");
			else if(user.isCollaborator()) response.sendRedirect(contextPath+"/Collaborator");
			else response.sendRedirect(contextPath+"/Manager");
		}
		catch(CheckAuthException e)
		{
			response.sendRedirect(contextPath+"/Login?error_msg="+URLEncoder.encode("Credenziali non valide", "UTF-8"));
		} 
		catch(DBException e) 
		{
			response.sendRedirect(contextPath+"/Login?error_msg="+URLEncoder.encode(e.getMessage(), "UTF-8"));
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