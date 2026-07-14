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
            throw new ServletException("Impossibile connettersi al DB nella Servlet DoCreateTask", e);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
    {
        HttpSession session=request.getSession(false);
        if(session==null || session.getAttribute("user")==null) 
        {
            response.sendRedirect(getServletContext().getContextPath()+"/login.html");
            return;
        }
        
        User user=(User)session.getAttribute("user");

        String chosenRole=request.getParameter("chosenRole");
        String context=getServletContext().getContextPath();

        if("MANAGER".equals(chosenRole)) 
        {
        	user.setCollaborator(false);
            response.sendRedirect(context+"/Manager");
        }
        else if("COLLABORATORE".equals(chosenRole)) 
        {
        	user.setManager(false);
            response.sendRedirect(context+"/Collaborator");
        } 
        else
            response.sendRedirect(context+"/ChooseRole");
    }
    
    @Override
    public void destroy()
    {
    	if(connection!=null)
    		try {connection.close();}
    		catch(SQLException ignored) {}
    }
}