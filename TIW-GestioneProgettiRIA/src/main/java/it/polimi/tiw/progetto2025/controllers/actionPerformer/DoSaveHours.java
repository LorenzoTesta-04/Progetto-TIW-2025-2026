package it.polimi.tiw.progetto2025.controllers.actionPerformer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
    {
    	HttpSession session=request.getSession(false);

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

		if(!checkAccess.checkCollaborator(session, response, getServletContext()))
		{
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		User user=(User) session.getAttribute("user");
        TaskDAO taskDAO=new TaskDAO(connection);
        
        // Parsing asincrono del payload JSON tramite JsonParser
        JsonElement root=JsonParser.parseReader(request.getReader());
        JsonObject json=root.getAsJsonObject();

        if(!json.has("idTask") || !json.has("mese") || !json.has("oreLavorate")) 
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Parametri obbligatori omessi nel corpo JSON.\"}");
            return;
        }

        try 
        {
            int idTask=json.get("idTask").getAsInt();
            int mese=json.get("mese").getAsInt();
            int oreLavorate=json.get("oreLavorate").getAsInt();
            
            // Validazione a Specifica Server-Side: Ore intere non negative
            if(oreLavorate<0) 
            {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"Il numero di ore inserite deve essere maggiore o uguale a zero.\"}");
                return;
            }

            // Aggiornamento atomico nel DB
            taskDAO.updateHours(idTask, user.getID(), mese, oreLavorate);
            response.getWriter().write("{\"success\": true}");
        } 
        catch(NumberFormatException e) 
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Formato numerico dei parametri non valido.\"}");
        } 
        catch(SQLException e) 
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Salvataggio fallito: violazione dei vincoli o errore nel DB.\"}");
        }
    }

    @Override
    public void destroy() 
    {
        if(connection!=null)
            try { connection.close(); } 
            catch (SQLException ignored) {}
    }
}