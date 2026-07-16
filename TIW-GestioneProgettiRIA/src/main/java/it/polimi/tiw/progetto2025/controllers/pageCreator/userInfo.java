package it.polimi.tiw.progetto2025.controllers.pageCreator;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import it.polimi.tiw.progetto2025.beans.User;

public class userInfo extends HttpServlet 
{
	private static final long serialVersionUID = 1L;
	private final Gson gson = new Gson();
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
    {
        HttpSession session=request.getSession(false);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        // Controllo di sicurezza sulla sessione
        if(session==null || session.getAttribute("user")==null) 
        {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Sessione assente o scaduta.\"}");
            return;
        }

        // Estrazione del Bean utente completo
        User sessionUser=(User)session.getAttribute("user");

        try 
        {
            JsonObject customJson=new JsonObject();
            
            customJson.addProperty("nome", sessionUser.getNome());
            customJson.addProperty("cognome", sessionUser.getCognome());
            customJson.addProperty("profilePicturePath", sessionUser.getProfilePicturePath());

            String jsonResponse=this.gson.toJson(customJson);
            response.getWriter().write(jsonResponse);
        } 
        catch (Exception e) 
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Errore durante la creazione del JSON custom.\"}");
        }
    }
}