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

import it.polimi.tiw.progetto2025.beans.User;
import it.polimi.tiw.progetto2025.daos.MyDAO;

public class DoChooseRole extends HttpServlet 
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

        HttpSession session=request.getSession(false);
        
        if(session==null || session.getAttribute("user")==null) 
        {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Sessione scaduta. Effettua nuovamente il login.\"}");
            return;
        }
        
        User user=(User)session.getAttribute("user");
        String chosenRole=request.getParameter("chosenRole");

        String redirectUrl="";

        if("MANAGER".equals(chosenRole)) 
        {
            user.setCollaborator(false);
            redirectUrl="manager.html";
        }
        else if("COLLABORATORE".equals(chosenRole)) 
        {
            user.setManager(false);
            redirectUrl="collaborator.html";
        } 
        else 
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Ruolo selezionato non valido.\"}");
            return;
        }

        session.setAttribute("user", user);
        String jsonResponse=String.format("{\"success\": true, \"redirectUrl\": \"%s\"}", redirectUrl);
        response.getWriter().write(jsonResponse);
    }
    
    @Override
    public void destroy()
    {
    	if(connection!=null)
    		try {connection.close();}
    		catch(SQLException ignored) {}
    }
}