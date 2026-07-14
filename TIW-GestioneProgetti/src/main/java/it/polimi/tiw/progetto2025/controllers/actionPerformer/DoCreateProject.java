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
import it.polimi.tiw.progetto2025.daos.ProjectDAO;
import it.polimi.tiw.progetto2025.utils.checkAccess;

public class DoCreateProject extends HttpServlet 
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
            throw new ServletException("Impossibile connettersi al DB nella Servlet DoCreateProject", e);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
    {
        HttpSession session=request.getSession(false);
        
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); 
        
        if(!checkAccess.checkAdmin(session, response, getServletContext())) return;
        
        User user=(User)session.getAttribute("user");

        String nomeProgetto=request.getParameter("nomeProgetto");
        String durataStr=request.getParameter("durata");
        String idResponsabileStr=request.getParameter("idResponsabile");

        String errorMsg=null;
        int durata=0;
        int idResponsabile=0;

        if(nomeProgetto==null || nomeProgetto.trim().isEmpty()) errorMsg="Il titolo del progetto non può essere vuoto.";
        else if(durataStr==null || durataStr.trim().isEmpty()) errorMsg="La durata del progetto è obbligatoria.";
        else if(idResponsabileStr==null || idResponsabileStr.trim().isEmpty()) errorMsg="È obbligatorio selezionare un tecnico responsabile.";
        else 
        {
            try 
            {
                durata=Integer.parseInt(durataStr);
                idResponsabile=Integer.parseInt(idResponsabileStr);
                if(durata<=0) errorMsg="La durata in mesi deve essere un intero positivo.";
            } 
            catch(NumberFormatException e) 
            {
                errorMsg="Formato numerico dei parametri non valido.";
            }
        }

        if(errorMsg==null) 
        {
            ProjectDAO projectDAO=new ProjectDAO(connection);
            try 
            {
                projectDAO.createProject(nomeProgetto, durata, idResponsabile, user.getID());
                response.sendRedirect(getServletContext().getContextPath()+"/Admin");
                return;
            } 
            catch (SQLException e) 
            {
                errorMsg="Errore nel salvataggio: esiste già un progetto con questo nome o i dati non sono coerenti.";
            }
        }

        //Per ripristino
        if(session!=null) 
        {
            session.setAttribute("errorMsg", errorMsg);
            session.setAttribute("originForm", "PROJECT");
            session.setAttribute("submittedNome", nomeProgetto);
            session.setAttribute("submittedDurata", durataStr);
            session.setAttribute("submittedResponsabile", idResponsabileStr);
        }

        response.sendRedirect(getServletContext().getContextPath()+"/Admin");
    }
    
    @Override
    public void destroy()
    {
        if(connection!=null)
            try {connection.close();}
            catch(SQLException ignored) {}
    }
}