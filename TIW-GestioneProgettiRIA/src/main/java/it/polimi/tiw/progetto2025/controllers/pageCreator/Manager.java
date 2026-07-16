package it.polimi.tiw.progetto2025.controllers.pageCreator;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import it.polimi.tiw.progetto2025.beans.Project;
import it.polimi.tiw.progetto2025.beans.Task;
import it.polimi.tiw.progetto2025.beans.User;
import it.polimi.tiw.progetto2025.beans.WorkPackage;
import it.polimi.tiw.progetto2025.daos.MyDAO;
import it.polimi.tiw.progetto2025.daos.ProjectDAO;
import it.polimi.tiw.progetto2025.daos.TaskDAO;
import it.polimi.tiw.progetto2025.daos.UserDAO;
import it.polimi.tiw.progetto2025.daos.WorkPackageDAO;
import it.polimi.tiw.progetto2025.utils.checkAccess;

public class Manager extends HttpServlet 
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
            throw new ServletException("Impossibile connettersi al DB nella Servlet Manager", e);
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
         
        // Controllo di sicurezza centralizzato (Manager loggato)
        if(!checkAccess.checkManager(session, response, getServletContext()))
        {
        	response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        	return;
        }

        User user=(User) session.getAttribute("user");

        ProjectDAO projectDAO=new ProjectDAO(connection);
        WorkPackageDAO workPackageDAO=new WorkPackageDAO(connection);
        TaskDAO taskDAO=new TaskDAO(connection);
        UserDAO userDAO=new UserDAO(connection);

        JsonObject rootResponse=new JsonObject();
        
        try
        {
        	//Collaboratori
        	JsonArray jsonCollabList=new JsonArray();
        	List<User> collabList=userDAO.findAllCollaborators().stream().filter(c -> c.getID()!=user.getID()).toList();
        	
        	if(collabList!=null)
        	{
        		for(User collab:collabList)
        		{
        			JsonObject cObj=new JsonObject();
        			cObj.addProperty("id", collab.getID());
        			cObj.addProperty("nome", collab.getNome());
        			cObj.addProperty("cognome", collab.getCognome());
        			
        			jsonCollabList.add(cObj);
        		}
        	
        		rootResponse.add("collaborators", jsonCollabList);
        	}
        	
        	//Collaboratori managed
        	List<User> managedCollabList=userDAO.findManagedCollaborators(user.getID());
        	
        	if(managedCollabList!=null) 
        	{
        	    JsonArray jsonManagedCollabList=managedCollabList.stream()
        	        .map(User::getID)
        	        .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);

        	    rootResponse.add("managedCollaborators", jsonManagedCollabList);
        	}
        	
        	//Progetti
        	JsonArray jsonManagedProjects=new JsonArray();
        	List<Project> projects=projectDAO.findProjectsByManager(user.getID()); // Recupera i progetti assegnati al manager
        	
        	for(Project proj:projects) 
        	{
        	    JsonObject projObj=new JsonObject();
        	    projObj.addProperty("idProgetto", proj.getId());
        	    projObj.addProperty("nome", proj.getNomeProgetto());
        	    projObj.addProperty("durata", proj.getDurata());
        	    projObj.addProperty("stato", proj.getStato().toString());
        	    projObj.addProperty("isAssignable", projectDAO.isAssignable(proj.getId()));
        	    
        	    //WP
        	    JsonArray jsonWpList=new JsonArray();
        	    List<WorkPackage> wps=workPackageDAO.findWPsByProject(proj.getId());
        	    
        	    for(WorkPackage wp:wps) 
        	    {
        	        JsonObject wpObj=new JsonObject();
        	        wpObj.addProperty("idWp", wp.getCodiceWP());
        	        wpObj.addProperty("TitoloWP", wp.getTitolo());
        	        wpObj.addProperty("MeseInizio", wp.getMeseInizio());
        	        wpObj.addProperty("MeseFine", wp.getMeseFine());
        	        wpObj.addProperty("NumeroOrdine", wp.getNumeroOrdine());
        	        
        	        //Task
        	        JsonArray jsonTaskList=new JsonArray();
        	        List<Task> tasks=taskDAO.findTasksByWP(wp.getCodiceWP());
        	        
        	        for(Task task:tasks) 
        	        {
        	            JsonObject taskObj=new JsonObject();
        	            taskObj.addProperty("idTask", task.getId());
        	            taskObj.addProperty("TitoloTask", task.getNomeTask());
        	            taskObj.addProperty("Descrizione", task.getDescrizione()!=null?task.getDescrizione():"");
        	            taskObj.addProperty("NumeroOrdine", task.getNumeroOrdine());
        	            taskObj.addProperty("MeseInizio", task.getMeseInizio());
        	            taskObj.addProperty("MeseFine", task.getMeseFine());
        	            
        	            //Ore previste
        	            JsonArray jsonOreCoppie = new JsonArray();

						IntStream.rangeClosed(task.getMeseInizio(), task.getMeseFine())
						     .forEach(m -> {
						         JsonObject coppiaObj=new JsonObject();
						         
							     int prev = task.getOrePreviste().getOrDefault(m, 0);
							     coppiaObj.addProperty("prevista", prev);
							     
							     int lav = task.getOreLavorate().getOrDefault(m, 0);
							     coppiaObj.addProperty("lavorata", lav);
						     
						     jsonOreCoppie.add(coppiaObj);
						 });
	
	        	         taskObj.add("ore", jsonOreCoppie);
        	            
        	            //Collaboratori assegnati ai task
        	            JsonArray jsonCollabTask=new JsonArray();
        	            List<Integer> collabIds=taskDAO.findCollaboratorsByTask(task.getId());
        	            
        	            for(Integer collabId:collabIds) 
        	            {
        	                JsonObject collabTaskObj=new JsonObject();
        	                collabTaskObj.addProperty("id", collabId);
        	                
        	                //Ore lavorate dal collaboratore       	                
        	                Task taskCollabDetails=taskDAO.findTaskByIdWithCollaborator(task.getId(), collabId);
        	                JsonArray jsonOreCollab=new JsonArray();
        	                
        	                if(taskCollabDetails!=null) 
        	                {
        	                	jsonOreCollab=taskCollabDetails.getOreLavorate().entrySet().stream()
        	            	        .map(e -> e.getValue())
        	            	        .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
        	                
        	                	collabTaskObj.add("ore", jsonOreCollab);
        	                }
        	                
        	                jsonCollabTask.add(collabTaskObj);
        	            }
        	            
        	            taskObj.add("Collab", jsonCollabTask);
        	            
        	            jsonTaskList.add(taskObj);
        	        }
        	        
        	        wpObj.add("Task", jsonTaskList);
        	        jsonWpList.add(wpObj);
        	    }
        	    
        	    projObj.add("WP", jsonWpList);
        	    
        	    jsonManagedProjects.add(projObj);
        	}
        	
        	rootResponse.add("managedProjects", jsonManagedProjects);
        	
        	response.getWriter().write(rootResponse.toString());
        }
        catch(SQLException e) 
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