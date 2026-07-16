package it.polimi.tiw.progetto2025.daos;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import it.polimi.tiw.progetto2025.beans.Project;
import it.polimi.tiw.progetto2025.beans.WorkPackage;

/**
 * Data Access Object (DAO) dedicato alla gestione della persistenza dei Progetti.
 * Fornisce metodi per la creazione, il recupero filtrato in base ai ruoli (creatore, responsabile, 
 * collaboratore) e la verifica dei requisiti per il cambio di stato del ciclo di vita del progetto.
 */
public class ProjectDAO 
{
    private Connection connection;

    public ProjectDAO(Connection connection) 
    {
        this.connection=connection;
    }

    /**
     * Recupera tutti i progetti creati da uno specifico utente (generalmente un amministratore).
     * Include le informazioni anagrafiche del responsabile del progetto.
     * @param idCreatore identificativo dell'utente che ha creato i progetti
     * @return lista di oggetti Project popolati, ordinati per ID crescente
     * @throws SQLException se si verifica un errore di accesso al database
     */
    public List<Project> findAllProjects(int idCreatore) throws SQLException 
    {
        List<Project> list=new ArrayList<>();
        String query="SELECT p.id, p.nomeProgetto, p.durata, p.stato, u.nome, u.cognome "+
                       "FROM "+MyDAO.PROJECT_TABLE+" p JOIN "+MyDAO.USER_TABLE+" u ON p.idResponsabile=u.id "+
                       "WHERE p.idCreatore=? "+
                       "ORDER BY p.id ASC";
        
        try(PreparedStatement pstmt=connection.prepareStatement(query)) 
        {
            pstmt.setInt(1, idCreatore);
            try(ResultSet rs=pstmt.executeQuery()) 
            {
                while (rs.next()) 
                {
                    Project p=new Project();
                    p.setId(rs.getInt("id"));
                    p.setNomeProgetto(rs.getString("nomeProgetto"));
                    p.setDurata(rs.getInt("durata"));
                    p.setStato(Project.projectState.valueOf(rs.getString("stato").toUpperCase().trim()));
                    p.setResponsabile(rs.getString("nome")+" "+rs.getString("cognome"));
                    list.add(p);
                }
            }
        }
        
        return list;
    }

    //Crea un nuovo progetto inserendolo nel DB
    public void createProject(String name, int durata, int idResponsabile, int idCreatore) throws SQLException 
    {
        String query="INSERT INTO "+MyDAO.PROJECT_TABLE+"(nomeProgetto, durata, idResponsabile, idCreatore) VALUES(?, ?, ?, ?)";
        
        try(PreparedStatement pstmt=connection.prepareStatement(query)) 
        {
            pstmt.setString(1, name);
            pstmt.setInt(2, durata);
            pstmt.setInt(3, idResponsabile);
            pstmt.setInt(4, idCreatore);
            pstmt.executeUpdate();
        }
    }    
    
    /**
     * Individua un singolo progetto partendo dal suo identificativo univoco.
     * @param idProgetto identificativo del progetto da cercare
     * @return l'oggetto Project mappato, oppure {@code null} se non viene trovato alcuna corrispondenza
     * @throws SQLException se si verifica un errore di accesso al database
     */
    public Project findProjectById(int idProgetto) throws SQLException 
    {
        String query="SELECT id, nomeProgetto, stato, durata, idResponsabile FROM "+MyDAO.PROJECT_TABLE+" WHERE id=?";
        
        try(PreparedStatement pstmt=connection.prepareStatement(query)) 
        {
            pstmt.setInt(1, idProgetto);
            try(ResultSet rs=pstmt.executeQuery()) 
            {
                if(rs.next())
                {
                    Project p=new Project();
                    p.setId(rs.getInt("id"));
                    p.setNomeProgetto(rs.getString("nomeProgetto"));
                    p.setStato(Project.projectState.valueOf(rs.getString("stato").toUpperCase().trim()));
                    p.setDurata(rs.getInt("durata"));
                    p.setIdResponsabile(rs.getInt("idResponsabile"));
                    return p;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Recupera l'elenco dei progetti assegnati alla supervisione di un determinato Project Manager.
     * 
     * @param idManager identificativo del manager/responsabile
     * @return lista dei progetti associati, ordinati dall'ultimo creato al primo (ID decrescente)
     * @throws SQLException se si verifica un errore di accesso al database
     */
    public List<Project> findProjectsByManager(int idManager) throws SQLException 
    {
        List<Project> list=new ArrayList<>();
        String query="SELECT id, nomeProgetto, durata, stato FROM "+MyDAO.PROJECT_TABLE+" WHERE idResponsabile=? ORDER BY id DESC";
        
        try(PreparedStatement pstmt=connection.prepareStatement(query)) 
        {
            pstmt.setInt(1, idManager);
            try(ResultSet rs=pstmt.executeQuery()) 
            {
                while (rs.next()) 
                {
                    Project p=new Project();
                    p.setId(rs.getInt("id"));
                    p.setNomeProgetto(rs.getString("nomeProgetto"));
                    p.setDurata(rs.getInt("durata"));
                    p.setStato(Project.projectState.valueOf(rs.getString("stato").toUpperCase().trim()));
                    list.add(p);
                }
            }
        }
        
        return list;
    }
    
    /**
     * Cerca tutti i progetti in cui un determinato utente figura come collaboratore operativo,
     * ovvero ha ore registrate all'interno di almeno uno dei task del progetto.
     * @param idCollaboratore identificativo del collaboratore da cercare
     * @return lista di progetti distinti nei quali il collaboratore è coinvolto
     * @throws SQLException se si verifica un errore durante l'esecuzione della JOIN multilivello
     */
    public List<Project> findProjectByCollaborator(int idCollaboratore) throws SQLException 
    {
        List<Project> list=new ArrayList<>();

        String query="SELECT DISTINCT p.* "+
                       "FROM "+MyDAO.PROJECT_TABLE+" p "+
                       "JOIN "+MyDAO.WORK_PACKAGE_TABLE+" wp ON p.id=wp.idProgetto "+
                       "JOIN "+MyDAO.TASK_TABLE+" t ON wp.idWp=t.idWp "+
                       "JOIN "+MyDAO.ORE_LAVORATE_TABLE+" ol ON t.idTask=ol.idTask "+
                       "WHERE ol.idCollaboratore=?";
        
        try(PreparedStatement pstmt=connection.prepareStatement(query)) 
        {
            pstmt.setInt(1, idCollaboratore);
            try(ResultSet rs=pstmt.executeQuery()) 
            {
                while (rs.next()) 
                {
                    Project p=new Project();
                    p.setId(rs.getInt("id"));
                    p.setIdResponsabile(rs.getInt("idResponsabile"));
                    p.setNomeProgetto(rs.getString("nomeProgetto"));
                    p.setDurata(rs.getInt("durata"));
                    p.setStato(Project.projectState.valueOf(rs.getString("stato").toUpperCase().trim()));
                    list.add(p);
                }
            }
        }
        
        return list;
    }
    
    /**
     * Verifica se un progetto possiede i requisiti minimi per poter essere assegnato/avviato.
     * Un progetto è considerato assegnabile se e solo se:
     * 1) Ha almeno un task associato ai suoi Work Package.
     * 2) Tutti i suoi task hanno almeno un collaboratore associato e hanno ore previste pianificate (maggiori di zero).
     * @param idProgetto identificativo del progetto da controllare
     * @return {@code true} se soddisfa tutti i requisiti di assegnabilità, {@code false} altrimenti
     * @throws SQLException se si verifica un errore durante il conteggio o il controllo dei vincoli sul DB
     */
    public boolean isAssignable(int idProgetto) throws SQLException 
    {        
        Project p=findProjectById(idProgetto);
        
        if(p==null) return false;

        WorkPackageDAO wpDAO=new WorkPackageDAO(connection);
        List<WorkPackage> wps=wpDAO.findWPsByProject(idProgetto);
        
        if(wps==null || wps.isEmpty()) 
        	return false;
        
        for(WorkPackage wp:wps)
        	if(!wpDAO.isAssignable(wp.getCodiceWP()))
        		return false;
        
        return true;
    }

    /**
     * Aggiorna lo stato corrente del ciclo di vita di un progetto.
     * @param idProgetto identificativo del progetto da aggiornare
     * @param nuovoStato stringa corrispondente al valore letterale del nuovo stato
     * @throws SQLException se si verifica un errore durante l'esecuzione del comando UPDATE
     */
    public void updateProjectState(int idProgetto, Project.projectState nuovoStato) throws SQLException 
    {
        String query="UPDATE "+MyDAO.PROJECT_TABLE+" SET stato=? WHERE id=?";
        try(PreparedStatement pstmt=connection.prepareStatement(query)) 
        {
            pstmt.setString(1, nuovoStato.toString());
            pstmt.setInt(2, idProgetto);
            pstmt.executeUpdate();
        }
    }
}