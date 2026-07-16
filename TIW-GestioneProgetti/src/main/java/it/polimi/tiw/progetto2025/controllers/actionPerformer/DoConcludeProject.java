package it.polimi.tiw.progetto2025.controllers.actionPerformer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import it.polimi.tiw.progetto2025.beans.Project;
import it.polimi.tiw.progetto2025.beans.User;
import it.polimi.tiw.progetto2025.daos.MyDAO;
import it.polimi.tiw.progetto2025.daos.ProjectDAO;
import it.polimi.tiw.progetto2025.utils.checkAccess;

public class DoConcludeProject extends HttpServlet 
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
            throw new ServletException("Impossibile connettersi al DB nella Servlet DoConcludeProject", e);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
    {
        HttpSession session=request.getSession(false);
        
        if(!checkAccess.checkManager(session, response, getServletContext())) return;

        User user=(User)session.getAttribute("user");
        String idProgettoStr=request.getParameter("idProgetto");

        String redirectUrl=getServletContext().getContextPath()+"/MonitorProjects";
        if(idProgettoStr!=null && !idProgettoStr.isEmpty()) 
        	redirectUrl+="?idProgetto="+idProgettoStr;

        if(idProgettoStr==null || idProgettoStr.isEmpty()) 
        {
            if(session!=null) session.setAttribute("errorMsg", "Identificativo del progetto mancante.");
            response.sendRedirect(getServletContext().getContextPath()+"/MonitorProjects");
            return;
        }

        String errorMsg=null;

        try 
        {
            int idProgetto=Integer.parseInt(idProgettoStr);
            ProjectDAO projectDAO=new ProjectDAO(connection);
            
            Project project=projectDAO.findProjectById(idProgetto);
            
            if(project!=null && project.getIdResponsabile()==user.getID()) 
            {
                if("ASSEGNATO".equals(project.getStato())) 
                {
                    projectDAO.updateProjectState(idProgetto, Project.projectState.CONCLUSO);
                    if(session!=null) session.setAttribute("successMsg", "Progetto concluso con successo!");
                    response.sendRedirect(redirectUrl);
                    return;
                }
                else
                    errorMsg="Impossibile concludere il progetto.";
            }
            else 
                errorMsg="Non sei autorizzato a modificare questo progetto o il progetto non esiste.";
            
        } 
        catch(NumberFormatException e) 
        {
            errorMsg="Formato dell'ID progetto non valido.";
        } 
        catch(SQLException e) 
        {
            errorMsg="Errore nel database durante il salvataggio dello stato. Riprovare.";
        }

        if(session!=null && errorMsg!=null) session.setAttribute("errorMsg", errorMsg);
        
        response.sendRedirect(redirectUrl);
    }

    @Override
    public void destroy()
    {
        if(connection!=null)
            try {connection.close();}
            catch(SQLException ignored) {}
    }
}