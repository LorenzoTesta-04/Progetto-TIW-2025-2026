package it.polimi.tiw.progetto2025.controllers.pagesCreators;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import it.polimi.tiw.progetto2025.beans.User;
import it.polimi.tiw.progetto2025.beans.Project;
import it.polimi.tiw.progetto2025.beans.WorkPackage;
import it.polimi.tiw.progetto2025.daos.ProjectDAO;
import it.polimi.tiw.progetto2025.daos.UserDAO;
import it.polimi.tiw.progetto2025.daos.WorkPackageDAO;
import it.polimi.tiw.progetto2025.utils.checkAccess;
import it.polimi.tiw.progetto2025.daos.MyDAO;

public class Admin extends HttpServlet 
{
    private static final long serialVersionUID=1L;
    private Connection connection=null;
    private TemplateEngine templateEngine;
    private JakartaServletWebApplication webApplication;

    @Override
    public void init()throws ServletException 
    {
        ServletContext servletContext=getServletContext();
        this.webApplication=JakartaServletWebApplication.buildApplication(servletContext);
        
        WebApplicationTemplateResolver templateResolver=new WebApplicationTemplateResolver(this.webApplication);
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setPrefix("/WEB-INF/pages/admin/");
        templateResolver.setSuffix(".html");
        templateResolver.setCharacterEncoding("UTF-8");
        
        this.templateEngine=new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
        
        try 
        {
            new com.mysql.cj.jdbc.Driver();
            this.connection=DriverManager.getConnection(MyDAO.DB_URL, MyDAO.DB_USER, MyDAO.DB_PASS);
        } 
        catch (SQLException e)
        {
            throw new ServletException("Impossibile connettersi al DB nella Servlet Admin", e);
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)throws ServletException, IOException 
    {
        HttpSession session=request.getSession(false);
        
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); 
        response.setHeader("Pragma", "no-cache"); 
        response.setDateHeader("Expires", 0);
        
        IWebExchange exchange=this.webApplication.buildExchange(request, response);
        WebContext ctx=new WebContext(exchange, request.getLocale());

        if(!checkAccess.checkAdmin(session, response, getServletContext()))return;
        
        User user=(User)session.getAttribute("user");
        UserDAO userDAO=new UserDAO(connection);
        ProjectDAO projectDAO=new ProjectDAO(connection);
        WorkPackageDAO workPackageDAO=new WorkPackageDAO(connection);
        
        //default form Progetto
        String errorMsg="";
        String submittedNome="";
        String submittedDurata="";
        String submittedResponsabile=null;

        //default form WP
        String submittedProgetto=null;
        String submittedNomeWP="";
        String submittedMeseInizioWP="";
        String submittedMeseFineWP="";

        //default form Task
        String idProgettoStr=request.getParameter("idProgetto");
        String submittedWP=null;
        String submittedNomeTask="";
        String submittedDescrizione="";
        String submittedMeseInizioTask="";
        String submittedMeseFineTask="";

        //Ripristino errori inserimento
        if(session!=null && session.getAttribute("errorMsg")!=null)
        {
            errorMsg=(String)session.getAttribute("errorMsg");
            String originForm=(String)session.getAttribute("originForm");

            if("PROJECT".equals(originForm))
            {
                submittedNome=(String)session.getAttribute("submittedNome");
                submittedDurata=(String)session.getAttribute("submittedDurata");
                submittedResponsabile=(String)session.getAttribute("submittedResponsabile");
            } 
            else if("WP".equals(originForm))
            {
                submittedProgetto=(String)session.getAttribute("submittedProgetto");
                submittedNomeWP=(String)session.getAttribute("submittedNomeWP");
                submittedMeseInizioWP=(String)session.getAttribute("submittedMeseInizioWP");
                submittedMeseFineWP=(String)session.getAttribute("submittedMeseFineWP");
            } 
            else if("TASK".equals(originForm))
            {
                idProgettoStr=(String)session.getAttribute("selectedProgettoId");
                submittedWP=(String)session.getAttribute("submittedWP");
                submittedNomeTask=(String)session.getAttribute("submittedNomeTask");
                submittedDescrizione=(String)session.getAttribute("submittedDescrizione");
                submittedMeseInizioTask=(String)session.getAttribute("submittedMeseInizioTask");
                submittedMeseFineTask=(String)session.getAttribute("submittedMeseFineTask");
            }

            // PULIZIA DELLA SESSIONE
            session.removeAttribute("errorMsg");
            session.removeAttribute("originForm");
            session.removeAttribute("submittedNome");
            session.removeAttribute("submittedDurata");
            session.removeAttribute("submittedResponsabile");
            session.removeAttribute("submittedProgetto");
            session.removeAttribute("submittedNomeWP");
            session.removeAttribute("submittedMeseInizioWP");
            session.removeAttribute("submittedMeseFineWP");
            session.removeAttribute("selectedProgettoId");
            session.removeAttribute("submittedWP");
            session.removeAttribute("submittedNomeTask");
            session.removeAttribute("submittedDescrizione");
            session.removeAttribute("submittedMeseInizioTask");
            session.removeAttribute("submittedMeseFineTask");
        }
        
        try 
        {         
            // 1. Dati per il Form Progetto
            List<User> technicians=userDAO.findAllTechnicians();
            ctx.setVariable("technicians", technicians);
            ctx.setVariable("submittedNome", submittedNome);
            ctx.setVariable("submittedDurata", submittedDurata);
            ctx.setVariable("submittedResponsabile", submittedResponsabile);

            // 2. Dati per i Form WP e Task (Filtriamo i progetti con stato "CREATO")
            List<Project> allProjects=projectDAO.findAllProjects(user.getID());
            List<Project> activeProjects=allProjects.stream()
                    .filter(p -> "CREATO".equals(p.getStato()))
                    .toList();
            ctx.setVariable("activeProjects", activeProjects);
            
            // 3. Caricamento dinamico o ripristinato dei WP per il form del Task
            List<WorkPackage> availableWPs=null;
            if(idProgettoStr!=null && !idProgettoStr.isEmpty())
            {
                int idProgetto=Integer.parseInt(idProgettoStr);
                availableWPs=workPackageDAO.findWPsByProject(idProgetto);
            }
            
            //Variabili Form WP salvate
            ctx.setVariable("submittedProgetto", submittedProgetto);
            ctx.setVariable("submittedNomeWP", submittedNomeWP);
            ctx.setVariable("submittedMeseInizioWP", submittedMeseInizioWP);
            ctx.setVariable("submittedMeseFineWP", submittedMeseFineWP);

            //Variabili Form Task salvate
            ctx.setVariable("selectedProgettoId", idProgettoStr);
            ctx.setVariable("availableWPs", availableWPs);
            ctx.setVariable("submittedWP", submittedWP);
            ctx.setVariable("submittedNomeTask", submittedNomeTask);
            ctx.setVariable("submittedDescrizione", submittedDescrizione);
            ctx.setVariable("submittedMeseInizioTask", submittedMeseInizioTask);
            ctx.setVariable("submittedMeseFineTask", submittedMeseFineTask);

            //Contesto globale ed errore unificato
            ctx.setVariable("user", user);
            ctx.setVariable("errorMsg", errorMsg);

            templateEngine.process("admin", ctx, response.getWriter());
            
        } 
        catch(SQLException | NumberFormatException e)
        {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento della dashboard amministratore");
        }
    }
    
    @Override
    public void destroy()
    {
        if(connection != null)
            try { connection.close(); }
            catch(SQLException ignored) {}
    }
}