package it.polimi.tiw.progetto2025.controllers.pageCreator;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import it.polimi.tiw.progetto2025.beans.User;
import it.polimi.tiw.progetto2025.beans.Project;
import it.polimi.tiw.progetto2025.beans.WorkPackage;
import it.polimi.tiw.progetto2025.beans.Task;
import it.polimi.tiw.progetto2025.daos.MyDAO;
import it.polimi.tiw.progetto2025.daos.ProjectDAO;
import it.polimi.tiw.progetto2025.daos.WorkPackageDAO;
import it.polimi.tiw.progetto2025.daos.TaskDAO;
import it.polimi.tiw.progetto2025.utils.checkAccess;

public class Collaborator extends HttpServlet 
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
            throw new ServletException("Impossibile connettersi al DB nella Servlet Collaborator", e);
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
    {
        HttpSession session=request.getSession(false);
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
         
        // Controllo di sicurezza centralizzato (Collaboratore loggato)
        if(!checkAccess.checkCollaborator(session, response, getServletContext()))
        {
        	response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        	return;
        }

        User user=(User)session.getAttribute("user");

        ProjectDAO projectDAO=new ProjectDAO(connection);
        WorkPackageDAO wpDAO=new WorkPackageDAO(connection);
        TaskDAO taskDAO=new TaskDAO(connection);

        try 
        {
            JsonObject rootResponse=new JsonObject();
            JsonArray jsonProjList=new JsonArray();

            // Recupera solo i progetti in stato ASSEGNATO a cui partecipa il collaboratore
            List<Project> projects=projectDAO.findProjectByCollaborator(user.getID())
                    .stream()
                    .filter(p -> p.getStato().equalsIgnoreCase("ASSEGNATO"))
                    .toList();

            if(projects!=null) 
            {
                for(Project p:projects) 
                {
                    JsonObject pObj=new JsonObject();
                    pObj.addProperty("id", p.getId());
                    pObj.addProperty("nomeProgetto", p.getNomeProgetto());
                    pObj.addProperty("stato", p.getStato());
                    pObj.addProperty("durata", p.getDurata());

                    JsonArray jsonWps=new JsonArray();
                    List<WorkPackage> wps=wpDAO.findWPsWithCollaboratorInProject(p.getId(), user.getID());

                    if(wps!=null) 
                    {
                        for(WorkPackage wp:wps) 
                        {
                            JsonObject wpObj=new JsonObject();
                            wpObj.addProperty("numeroOrdine", wp.getNumeroOrdine());
                            wpObj.addProperty("codiceWP", wp.getCodiceWP());
                            wpObj.addProperty("titolo", wp.getTitolo());

                            JsonArray jsonTasks=new JsonArray();
                            List<Task> tasks=taskDAO.findTaskWithCollaboratorInWp(wp.getCodiceWP(), user.getID());

                            if(tasks!=null) 
                            {
                                for(Task t:tasks) 
                                {
                                    JsonObject tObj=new JsonObject();
                                    tObj.addProperty("id", t.getId());
                                    tObj.addProperty("numeroOrdine", t.getNumeroOrdine());
                                    tObj.addProperty("nomeTask", t.getNomeTask());
                                    tObj.addProperty("meseInizio", t.getMeseInizio());
                                    tObj.addProperty("meseFine", t.getMeseFine());

                                    // Costruisce la mappa delle ore lavorate mensili memorizzate nel DB per l'utente corrente
                                    Task dbTaskWithHours=taskDAO.findTaskByIdWithCollaborator(t.getId(), user.getID());
                                    JsonObject jsonHoursWorked=new JsonObject();
                                    
                                    for(int m=1; m<=p.getDurata(); m++) 
                                    {
                                        int hours=0;
                                        if(dbTaskWithHours!=null && dbTaskWithHours.getOreLavorate()!=null && dbTaskWithHours.getOreLavorate().containsKey(m)) 
                                            hours=dbTaskWithHours.getOreLavorate().get(m);
                                        
                                        jsonHoursWorked.addProperty(String.valueOf(m), hours);
                                    }
                                    
                                    tObj.add("oreLavorateMese", jsonHoursWorked);
                                    jsonTasks.add(tObj);
                                }
                            }
                            
                            wpObj.add("tasks", jsonTasks);
                            jsonWps.add(wpObj);
                        }
                    }
                    
                    pObj.add("wps", jsonWps);
                    jsonProjList.add(pObj);
                }
            }
            
            rootResponse.add("assignedProjects", jsonProjList);
            response.getWriter().write(rootResponse.toString());
        } 
        catch (SQLException e) 
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Errore DB nel recupero dell'ambiente collaboratore.\"}");
        }
    }

    @Override
    public void destroy() 
    {
        if (connection != null)
            try { connection.close(); } 
            catch (SQLException ignored) {}
    }
}