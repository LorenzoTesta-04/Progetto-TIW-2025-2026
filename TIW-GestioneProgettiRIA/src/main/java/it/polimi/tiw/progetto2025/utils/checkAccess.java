package it.polimi.tiw.progetto2025.utils;

import java.io.IOException;

import it.polimi.tiw.progetto2025.beans.User;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class checkAccess
{
	public static boolean checkAdmin(HttpSession session, HttpServletResponse response, ServletContext context) throws IOException
	{
        if(session==null || session.getAttribute("user")==null) 
            return false;
        
        User user=(User)session.getAttribute("user");
        if(!user.isAdmin()) 
        {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accesso non autorizzato.");
            return false;
        }
        
        return true;
	}
	
	public static boolean checkManager(HttpSession session, HttpServletResponse response, ServletContext context) throws IOException
	{
		if(session==null || session.getAttribute("user")==null) 
            return false;
        
        User user=(User)session.getAttribute("user");
        if(!user.isManager()) 
        {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accesso non autorizzato.");
            return false;
        }
        
        return true;
	}
	
	public static boolean checkCollaborator(HttpSession session, HttpServletResponse response, ServletContext context) throws IOException
	{
		if(session==null || session.getAttribute("user")==null) 
            return false;
        
        User user=(User)session.getAttribute("user");
        if(!user.isCollaborator()) 
        {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accesso non autorizzato.");
            return false;
        }
        
        return true;
	}
}