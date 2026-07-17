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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

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
import it.polimi.tiw.progetto2025.daos.UserDAO;
import it.polimi.tiw.progetto2025.daos.WorkPackageDAO;
import it.polimi.tiw.progetto2025.utils.checkAccess;

public class DoTaskAllocation extends HttpServlet 
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
            throw new ServletException("Impossibile connettersi al DB", e);
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

        // Parsing Payload JSON
        JsonElement root=JsonParser.parseReader(request.getReader());
        JsonObject json=root.getAsJsonObject();

        if(!json.has("idProgetto") || !json.has("idTask") || !json.has("idCollaboratori") || !json.has("orePreviste")) 
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Struttura dati JSON non conforme.\"}");
            return;
        }

        int idProgetto=json.get("idProgetto").getAsInt();
        int idTask=json.get("idTask").getAsInt();
        
        JsonArray colArray=json.getAsJsonArray("idCollaboratori");
        JsonObject oreObj=json.getAsJsonObject("orePreviste");

        try 
        {
            connection.setAutoCommit(false);
            
            ProjectDAO projectDAO=new ProjectDAO(connection);
            WorkPackageDAO wpDAO=new WorkPackageDAO(connection);
            TaskDAO taskDAO=new TaskDAO(connection);
            UserDAO userDAO=new UserDAO(connection);

            Project project=projectDAO.findProjectById(idProgetto);
            if(project==null || project.getIdResponsabile()!=user.getID()) 
            {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"Non autorizzato ad operare sul progetto.\"}");
                connection.rollback();
                return;
            }

            if(!"CREATO".equalsIgnoreCase(project.getStato().toString())) 
            {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"Impossibile riallocare task di un progetto già avviato o chiuso.\"}");
                connection.rollback();
                return;
            }

            Task task=taskDAO.findTaskById(idTask);
            if(task==null) 
            {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"Task inesistente.\"}");
                connection.rollback();
                return;
            }

            // 1. Validazione Collaboratori a lato Server 
            List<Integer> idCollaboratori=new ArrayList<>();
            if(colArray == null || colArray.size() == 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"Selezionare almeno un collaboratore per il task.\"}");
                connection.rollback();
                return;
            }

            for (JsonElement cElem : colArray) {
                int idColl=cElem.getAsInt();
                if(idColl == user.getID()) { // Controllo anti auto-assegnazione 
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"error\": \"Il responsabile non può auto-assegnarsi ai task.\"}");
                    connection.rollback();
                    return;
                }
                idCollaboratori.add(idColl);
            }

            // 2. Validazione Mesi e Ore a lato Server 
            Map<Integer, Integer> meseOreMap=new HashMap<>();
            for(int m=task.getMeseInizio(); m<=task.getMeseFine(); m++) 
            {
                String key=String.valueOf(m);
                if(!oreObj.has(key)) 
                {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"error\": \"Nessun valore di ore definito per il mese "+m+"\"}");
                    connection.rollback();
                    return;
                }

                int ore=oreObj.get(key).getAsInt();
                if(ore<=0) 
                {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"error\": \"Le ore inserite devono essere >0 (Mese "+m+")\"}");
                    connection.rollback();
                    return;
                }
                meseOreMap.put(m, ore);
            }

            //Aggiornamento DB
            taskDAO.updateTaskAllocation(idTask, idCollaboratori, meseOreMap);
            
            connection.commit();

            //Risposta (stato progetto aggiornato)
            JsonObject jsonResponse=new JsonObject();
            Project p=projectDAO.findProjectById(idProgetto);
            
            JsonObject projObj=new JsonObject();
            projObj.addProperty("idProgetto", p.getId());
            projObj.addProperty("nome", p.getNomeProgetto());
            projObj.addProperty("durata", p.getDurata());
            projObj.addProperty("stato", p.getStato().toString());
            projObj.addProperty("isAssignable", projectDAO.isAssignable(p.getId()));
            
            //WPs
            JsonArray jsonWpList=new JsonArray();
            List<WorkPackage> wps=wpDAO.findWPsByProject(p.getId());
            
            for(WorkPackage wp:wps) 
            {
                JsonObject wpObj=new JsonObject();
                wpObj.addProperty("idWp", wp.getCodiceWP());
                wpObj.addProperty("TitoloWP", wp.getTitolo());
                wpObj.addProperty("MeseInizio", wp.getMeseInizio());
                wpObj.addProperty("MeseFine", wp.getMeseFine());
                wpObj.addProperty("NumeroOrdine", wp.getNumeroOrdine());
                
                // Tasks del WP
                JsonArray jsonTaskList=new JsonArray();
                List<Task> tasks=taskDAO.findTasksByWP(wp.getCodiceWP());
                
                for(Task t:tasks) 
                {
                    JsonObject taskObj=new JsonObject();
                    taskObj.addProperty("idTask", t.getId());
                    taskObj.addProperty("TitoloTask", t.getNomeTask());
                    taskObj.addProperty("Descrizione", t.getDescrizione()!=null?t.getDescrizione():"");
                    taskObj.addProperty("NumeroOrdine", t.getNumeroOrdine());
                    taskObj.addProperty("MeseInizio", t.getMeseInizio());
                    taskObj.addProperty("MeseFine", t.getMeseFine());
                    
                    JsonArray jsonOreCoppie=new JsonArray();
                    IntStream.rangeClosed(t.getMeseInizio(), t.getMeseFine())
                         .forEach(m -> {
                             JsonObject coppiaObj=new JsonObject();
                             
                             int prev=t.getOrePreviste().getOrDefault(m, 0);
                             coppiaObj.addProperty("prevista", prev);
                             
                             int  lav=t.getOreLavorate().getOrDefault(m, 0);
                             coppiaObj.addProperty("lavorata", lav);
                             
                             jsonOreCoppie.add(coppiaObj);
                         });

                    taskObj.add("ore", jsonOreCoppie);
                    
                    // Collaboratori assegnati al task corrente
                    JsonArray jsonCollabTask=new JsonArray();
                    List<Integer> collabIds=taskDAO.findCollaboratorsByTask(t.getId());
                    
                    for(Integer collabId:collabIds) 
                    {
                        JsonObject collabTaskObj=new JsonObject();
                        collabTaskObj.addProperty("id", collabId);
                        
                        // Ore lavorate dal collaboratore per il task corrente       
                        Task taskCollabDetails=taskDAO.findTaskByIdWithCollaborator(t.getId(), collabId);
                        JsonArray jsonOreCollab=new JsonArray();
                        
                        if(taskCollabDetails!=null) 
                        {
                            jsonOreCollab=taskCollabDetails.getOreLavorate().entrySet().stream()
                                    .map(Map.Entry::getValue)
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
            jsonResponse.add("Project", projObj);
            
            //Collaboratori managed
        	List<User> managedCollabList=userDAO.findManagedCollaborators(user.getID());
        	
        	if(managedCollabList!=null) 
        	{
        	    JsonArray jsonManagedCollabList=managedCollabList.stream()
        	        .map(User::getID)
        	        .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);

        	    jsonResponse.add("managedCollaborators", jsonManagedCollabList);
        	}
            
            response.getWriter().write(jsonResponse.toString());
		} 
		catch(Exception e) 
		{
			try {connection.rollback();} 
			catch(SQLException ignore) {}
			
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().write("Errore: "+e.getMessage());
		} 
		finally
		{
			try {connection.setAutoCommit(true);}
			catch(SQLException ignore) {}
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