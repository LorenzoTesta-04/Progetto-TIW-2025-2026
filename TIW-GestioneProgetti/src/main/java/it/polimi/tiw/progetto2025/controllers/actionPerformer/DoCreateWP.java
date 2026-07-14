package it.polimi.tiw.progetto2025.controllers.actionPerformer;

import java.io.IOException;
import java.sql.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import it.polimi.tiw.progetto2025.beans.User;
import it.polimi.tiw.progetto2025.beans.Project;
import it.polimi.tiw.progetto2025.daos.ProjectDAO;
import it.polimi.tiw.progetto2025.daos.WorkPackageDAO;
import it.polimi.tiw.progetto2025.utils.checkAccess;
import it.polimi.tiw.progetto2025.daos.MyDAO;

public class DoCreateWP extends HttpServlet 
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
            throw new ServletException("Impossibile connettersi al DB nella Servlet DoCreateWP", e);
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

        String idProgettoStr=request.getParameter("idProgetto");
        String nomeWP=request.getParameter("nomeWP");
        String meseInizioStr=request.getParameter("meseInizio");
        String meseFineStr=request.getParameter("meseFine");

        String errorMsg=null;
        int idProgetto=-1;
		int meseInizio=0, meseFine=0;

        if(idProgettoStr==null || nomeWP==null || nomeWP.trim().isEmpty() || meseInizioStr==null || meseFineStr==null) 
        {
            errorMsg="Tutti i campi sono obbligatori.";
        } 
        else 
        {
            try 
            {
                idProgetto=Integer.parseInt(idProgettoStr);
                meseInizio=Integer.parseInt(meseInizioStr);
                meseFine=Integer.parseInt(meseFineStr);

                if(meseInizio<=0 || meseFine<=0) errorMsg="I mesi devono essere interi positivi.";
                else if(meseInizio>meseFine) errorMsg="Il mese di inizio deve precedere o uguagliare il mese di fine.";
            } 
            catch(NumberFormatException e) 
            {
                errorMsg="Parametri numerici non validi.";
            }
        }

        //Controllo coerenza temporale
        if(errorMsg==null) 
        {
            ProjectDAO projectDAO=new ProjectDAO(connection);
            try 
            {
            	int finalIdProgetto=idProgetto;
                Project proj=projectDAO.findAllProjects(user.getID()).stream()
                        .filter(p -> p.getId()==finalIdProgetto)
                        .findFirst()
                        .orElse(null);

                if(proj==null) errorMsg="Progetto non valido o inesistente.";
                else if(meseFine>proj.getDurata()) errorMsg="Il termine del WP eccede la durata massima del progetto ("+proj.getDurata()+" mesi).";
            } 
            catch(SQLException e) 
            {
                errorMsg="Errore durante la verifica a database.";
            }
        }

        if(errorMsg==null) 
        {
            WorkPackageDAO WpDAO=new WorkPackageDAO(connection);
            try 
            {
                WpDAO.createWorkPackage(idProgetto, nomeWP, meseInizio, meseFine);
                response.sendRedirect(getServletContext().getContextPath()+"/Admin");
                return;
            } 
            catch(Exception e) 
            {
                errorMsg="Salvataggio del Work Package fallito a causa di un errore di sistema.";
            }
        }

        //Per ripristino
        if(session!=null) 
        {
            session.setAttribute("errorMsg", errorMsg);
            session.setAttribute("originForm", "WP");
            session.setAttribute("submittedProgetto", idProgettoStr);
            session.setAttribute("submittedNomeWP", nomeWP);
            session.setAttribute("submittedMeseInizioWP", meseInizioStr);
            session.setAttribute("submittedMeseFineWP", meseFineStr);
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