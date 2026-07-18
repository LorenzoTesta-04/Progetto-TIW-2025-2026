package it.polimi.tiw.progetto2025.controllers.pageCreator;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import it.polimi.tiw.progetto2025.beans.User;
import it.polimi.tiw.progetto2025.beans.Project;
import it.polimi.tiw.progetto2025.beans.Task;
import it.polimi.tiw.progetto2025.beans.WorkPackage;
import it.polimi.tiw.progetto2025.daos.ProjectDAO;
import it.polimi.tiw.progetto2025.daos.TaskDAO;
import it.polimi.tiw.progetto2025.daos.UserDAO;
import it.polimi.tiw.progetto2025.daos.WorkPackageDAO;
import it.polimi.tiw.progetto2025.utils.checkAccess;
import it.polimi.tiw.progetto2025.daos.MyDAO;

public class Admin extends HttpServlet 
{
    private static final long serialVersionUID=1L;
    private Connection connection=null;

    @Override
    public void init()throws ServletException 
    {        
        try 
        {
            new com.mysql.cj.jdbc.Driver();
            this.connection=DriverManager.getConnection(MyDAO.DB_URL, MyDAO.DB_USER, MyDAO.DB_PASS);
        } 
        catch(SQLException e)
        {
            throw new ServletException("Impossibile connettersi al DB nella Servlet Admin", e);
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

        //Controllo di sicurezza centralizzato(Admin loggato)
        if(!checkAccess.checkAdmin(session, response, getServletContext()))
        {
        	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        	return;
        }
        
        User user=(User) session.getAttribute("user");
        
        UserDAO userDAO=new UserDAO(connection);
        ProjectDAO projectDAO=new ProjectDAO(connection);
        WorkPackageDAO workPackageDAO=new WorkPackageDAO(connection);
        TaskDAO taskDAO=new TaskDAO(connection);

        try 
        {
            JsonObject jsonResponse=new JsonObject();
            
            List<User> technicians=userDAO.findAllCollaborators();
            JsonArray jsonTechnicians=new JsonArray();
            if(technicians!=null) 
                for(User t:technicians) 
                {
                    JsonObject tObj=new JsonObject();
                    tObj.addProperty("id", t.getID());
                    tObj.addProperty("nomeCompleto", t.getCognome()+" "+t.getNome());
                    jsonTechnicians.add(tObj);
                }

            jsonResponse.add("technicians", jsonTechnicians);

            List<Project> myProjects=projectDAO.findAllProjects(user.getID());
            JsonArray jsonProjectsArray=new JsonArray();
            
            Integer defaultProjectId=null;

            if(myProjects!=null && !myProjects.isEmpty()) 
            {
                for(Project p:myProjects.reversed()) 
                    if(p.getStato()!=null && p.getStato().equalsIgnoreCase("CREATO")) 
                    {
                        defaultProjectId=p.getId();
                        break;
                    }

                //Fallback sul primo se nessuno è in stato CREATO
                if(defaultProjectId==null && myProjects.getFirst()!=null) 
                    defaultProjectId=myProjects.getFirst().getId();

                for(Project p:myProjects) 
                {
                    JsonObject pJson=new JsonObject();
                    pJson.addProperty("id", p.getId());
                    pJson.addProperty("nomeProgetto", p.getNomeProgetto());
                    pJson.addProperty("stato", p.getStato());
                    pJson.addProperty("responsabile", p.getResponsabile());

                    int projTotalHours=0;
                    int projWorkedHours=0;

                    // Recupero dei Work Package associati al progetto corrente
                    JsonArray jsonWpsArray=new JsonArray();
                    List<WorkPackage> wps=workPackageDAO.findWPsByProject(p.getId());
                    
                    if(wps!=null) 
                    {
                        for(WorkPackage wp:wps) 
                        {
                            JsonObject wpJson=new JsonObject();
                            wpJson.addProperty("codiceWP", wp.getCodiceWP());
                            wpJson.addProperty("titolo", wp.getTitolo());
                            wpJson.addProperty("meseInizio", wp.getMeseInizio());
                            wpJson.addProperty("meseFine", wp.getMeseFine());
                            wpJson.addProperty("numeroOrdine", wp.getNumeroOrdine());

                            // Recupero dei Task associati al Work Package corrente
                            JsonArray jsonTasksArray=new JsonArray();
                            List<Task> tasks=taskDAO.findTasksByWP(wp.getCodiceWP());
                            
                            if(tasks!=null) 
                                for(Task t:tasks) 
                                {
                                    projTotalHours+=t.getOrePrevisteTotali();
                                    projWorkedHours+=t.getOreLavorateTotali();

                                    JsonObject tJson=new JsonObject();
                                    tJson.addProperty("idTask", t.getId());
                                    tJson.addProperty("nomeTask", t.getNomeTask());
                                    tJson.addProperty("descrizione", t.getDescrizione());
                                    tJson.addProperty("meseInizio", t.getMeseInizio());
                                    tJson.addProperty("meseFine", t.getMeseFine());
                                    tJson.addProperty("orePreviste", t.getOrePrevisteTotali());
                                    tJson.addProperty("oreLavorate", t.getOreLavorateTotali());
                                    tJson.addProperty("numeroOrdine", t.getNumeroOrdine());
                                    
                                    jsonTasksArray.add(tJson);
                                }
                            
                            wpJson.add("tasks", jsonTasksArray);
                            jsonWpsArray.add(wpJson);
                        }
                    }
                    
                    pJson.add("wps", jsonWpsArray);
                    pJson.addProperty("totalProjectHours", projTotalHours);
                    pJson.addProperty("totalWorkedHours", projWorkedHours);
                    
                    jsonProjectsArray.add(pJson);
                }
            }
            
            jsonResponse.add("myProjects", jsonProjectsArray);
            jsonResponse.addProperty("defaultProjectId", defaultProjectId);

            response.getWriter().write(jsonResponse.toString());
        }
        catch(SQLException e) 
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Errore SQL nel caricamento della struttura dati.\"}");
        }
    }
    
    @Override
    public void destroy()
    {
        if(connection!=null)
            try { connection.close(); }
            catch(SQLException ignored) {}
    }
}