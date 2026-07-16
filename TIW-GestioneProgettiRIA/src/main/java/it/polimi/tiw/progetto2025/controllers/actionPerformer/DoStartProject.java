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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.polimi.tiw.progetto2025.beans.Project;
import it.polimi.tiw.progetto2025.beans.Task;
import it.polimi.tiw.progetto2025.beans.User;
import it.polimi.tiw.progetto2025.beans.WorkPackage;
import it.polimi.tiw.progetto2025.daos.MyDAO;
import it.polimi.tiw.progetto2025.daos.ProjectDAO;
import it.polimi.tiw.progetto2025.daos.TaskDAO;
import it.polimi.tiw.progetto2025.daos.WorkPackageDAO;
import it.polimi.tiw.progetto2025.utils.checkAccess;

public class DoStartProject extends HttpServlet 
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
			throw new ServletException("Impossibile connettersi al DB nella Servlet DoLogin", e);
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
        ProjectDAO projectDAO=new ProjectDAO(connection);

        JsonElement root=JsonParser.parseReader(request.getReader());
        JsonObject json=root.getAsJsonObject();

        if (!json.has("idProgetto")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Parametro idProgetto obbligatorio assente.\"}");
            return;
        }

        int idProgetto=json.get("idProgetto").getAsInt();

        try 
        {
            Project p=projectDAO.findProjectById(idProgetto);
            if(p==null) 
			{
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\": \"Progetto non trovato.\"}");
                return;
            }

            if(p.getIdResponsabile()!=user.getID()) 
			{
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"error\": \"Non autorizzato ad avviare questo progetto.\"}");
                return;
            }

            if(!"CREATO".equals(p.getStato().toString())) 
			{
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"Il progetto non è in uno stato valido per l'avvio.\"}");
                return;
            }

            if(!projectDAO.isAssignable(idProgetto)) 
			{
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"Impossibile procedere: requisiti minimi di allocazione non soddisfatti.\"}");
                return;
            }

            projectDAO.updateProjectState(idProgetto, Project.projectState.ASSEGNATO);
            response.getWriter().write("{\"success\": true}");
        } 
        catch (SQLException e) 
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Impossibile completare l'assegnazione per errore del database.\"}");
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