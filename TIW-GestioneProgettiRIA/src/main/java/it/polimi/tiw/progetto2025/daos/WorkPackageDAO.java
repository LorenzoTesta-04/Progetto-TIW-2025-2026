package it.polimi.tiw.progetto2025.daos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import it.polimi.tiw.progetto2025.beans.Task;
import it.polimi.tiw.progetto2025.beans.WorkPackage;

/**
 * Data Access Object (DAO) dedicato alla gestione della persistenza dei Work Package (WP).
 * Offre metodi per la creazione, la ricerca puntuale e il recupero filtrato in base allo stato del progetto 
 * o al coinvolgimento dei singoli collaboratori operativi.
 */
public class WorkPackageDAO 
{
	private Connection connection;

    public WorkPackageDAO(Connection connection) 
    {
        this.connection=connection;
    }

    /**
     * Crea un nuovo Work Package agganciato a un determinato progetto e ne definisce l'intervallo temporale.
     * @param idProgetto identificativo del progetto padre a cui associare il WP
     * @param titolo titolo descrittivo del Work Package
     * @param meseInizio mese iniziale di validità del WP
     * @param meseFine mese finale di validità del WP
     * @throws SQLException se si verifica un errore durante l'esecuzione dell'istruzione di INSERT
     */
    public void createWorkPackage(int idProgetto, String titolo, int meseInizio, int meseFine) throws SQLException 
    {
    	int nuovoOrdine=1;
    	String select="SELECT MAX(numeroOrdine) FROM "+MyDAO.WORK_PACKAGE_TABLE+" WHERE idProgetto=?";
   
    	try(PreparedStatement pstatement=connection.prepareStatement(select)) 
        {
            pstatement.setInt(1, idProgetto);
            
            try (ResultSet rs=pstatement.executeQuery()) 
            {
                if(rs.next()) nuovoOrdine=rs.getInt(1)+1;
            }
        }
    	
    	
        String query="INSERT INTO "+MyDAO.WORK_PACKAGE_TABLE+" (idProgetto, numeroOrdine, titolo, meseInizio, meseFine) VALUES (?, ?, ?, ?, ?)";
        try(PreparedStatement pstatement=connection.prepareStatement(query)) 
        {
            pstatement.setInt(1, idProgetto);
            pstatement.setInt(2, nuovoOrdine);
            pstatement.setString(3, titolo);
            pstatement.setInt(4, meseInizio);
            pstatement.setInt(5, meseFine);
            
            pstatement.executeUpdate();
        }
    }
    
    /**
     * Recupera tutti i Work Package associati a progetti creati da un determinato utente, 
     * a patto che il progetto si trovi in uno stato modificabile ('CREATO' o 'ASSEGNATO').
     * @param idCreatore identificativo del creatore dei progetti
     * @return lista di Work Package disponibili, ordinati per ID progetto e ID WP crescenti
     * @throws SQLException se si verifica un errore di accesso al database
     */
    public List<WorkPackage> findAllAvailableWPs(int idCreatore) throws SQLException
    {
    	List<WorkPackage> list=new ArrayList<>();
        String query="SELECT idWP, wp.idProgetto, titolo, meseInizio, meseFine, numeroOrdine "+
                       "FROM "+MyDAO.WORK_PACKAGE_TABLE+" wp "+
                       "JOIN "+MyDAO.PROJECT_TABLE+" p ON wp.idProgetto=p.id "+
                       "WHERE idCreatore=? AND (p.stato='CREATO' OR p.stato='ASSEGNATO') "+
                       "ORDER BY idProgetto, numeroOrdine ASC";

        try(PreparedStatement pstmt=connection.prepareStatement(query)) 
        {
            pstmt.setInt(1, idCreatore);
            try (ResultSet rs=pstmt.executeQuery()) 
            {
                while (rs.next()) 
                {
                    WorkPackage wp=new WorkPackage();
                    wp.setCodiceWP(rs.getInt("idWP"));
                    wp.setIdProgetto(rs.getInt("idProgetto"));
                    wp.setTitolo(rs.getString("titolo"));
                    wp.setMeseInizio(rs.getInt("meseInizio"));
                    wp.setMeseFine(rs.getInt("meseFine"));
                    wp.setNumeroOrdine(rs.getInt("numeroOrdine"));
                    list.add(wp);
                }
            }
        }
        return list;
    }
    
    /**
     * Trova un singolo Work Package partendo dal suo codice identificativo univoco.
     * @param idWP identificativo univoco del Work Package da cercare
     * @return l'oggetto WorkPackage popolato, oppure {@code null} se non viene trovata alcuna corrispondenza
     * @throws SQLException se si verifica un errore di accesso al database
     */
    public WorkPackage findWPById(int idWP) throws SQLException 
    {
        String query="SELECT idWP, idProgetto, titolo, meseInizio, meseFine, numeroOrdine FROM "+MyDAO.WORK_PACKAGE_TABLE+" WHERE idWP=?";
        try(PreparedStatement pstmt=connection.prepareStatement(query)) 
        {
            pstmt.setInt(1, idWP);
            try(ResultSet rs=pstmt.executeQuery()) 
            {
                if(rs.next()) 
                {
                    WorkPackage wp=new WorkPackage();
                    wp.setCodiceWP(rs.getInt("idWP"));
                    wp.setIdProgetto(rs.getInt("idProgetto"));
                    wp.setTitolo(rs.getString("titolo"));
                    wp.setMeseInizio(rs.getInt("meseInizio"));
                    wp.setMeseFine(rs.getInt("meseFine"));
                    wp.setNumeroOrdine(rs.getInt("numeroOrdine"));
                    return wp;
                }
            }
        }
        return null;
    }
    
    /**
     * Estrae la lista completa di tutti i Work Package strutturati all'interno di un determinato progetto.
     * @param idProgetto identificativo del progetto di riferimento
     * @return lista di Work Package appartenenti al progetto specificato
     * @throws SQLException se si verifica un errore di accesso al database
     */
    public List<WorkPackage> findWPsByProject(int idProgetto) throws SQLException 
    {
    	List<WorkPackage> list=new ArrayList<>();	
        String query="SELECT idWP, titolo, meseInizio, meseFine, numeroOrdine FROM "+MyDAO.WORK_PACKAGE_TABLE+" WHERE idProgetto=? ORDER BY numeroOrdine";
        
        try(PreparedStatement pstmt=connection.prepareStatement(query)) 
        {
        	pstmt.setInt(1, idProgetto);
        	ResultSet rs=pstmt.executeQuery();
        	
            while(rs.next()) 
            {
        		WorkPackage wp=new WorkPackage();
                wp.setCodiceWP(rs.getInt("idWP"));
                wp.setTitolo(rs.getString("titolo"));
                wp.setMeseInizio(rs.getInt("meseInizio"));
                wp.setMeseFine(rs.getInt("meseFine"));
                wp.setNumeroOrdine(rs.getInt("numeroOrdine"));
                
                list.add(wp);
            }
        }
        return list;
    }
    
    /**
     * Cerca tutti i Work Package di un determinato progetto nei quali uno specifico collaboratore risulta operativo.
     * @param idProgetto identificativo del progetto entro cui effettuare la ricerca
     * @param idCollaboratore identificativo del collaboratore assegnato ai task del WP
     * @return lista di oggetti WorkPackage distinti associati al collaboratore
     * @throws SQLException se si verifica un errore durante l'esecuzione della JOIN a tre vie
     */
    public List<WorkPackage> findWPsWithCollaboratorInProject(int idProgetto, int idCollaboratore) throws SQLException 
    {
    	List<WorkPackage> list=new ArrayList<>();	
    	String query="SELECT DISTINCT wp.idWP, wp.titolo, wp.meseInizio, wp.meseFine, wp.numeroOrdine "+
                "FROM "+MyDAO.WORK_PACKAGE_TABLE+" wp "+
                "JOIN "+MyDAO.TASK_TABLE+" t ON t.idWp=wp.idWP "+
                "JOIN "+MyDAO.ORE_LAVORATE_TABLE+" ol ON ol.idTask=t.idTask "+
                "WHERE wp.idProgetto=? AND ol.idCollaboratore=?";
                		
        try(PreparedStatement pstmt=connection.prepareStatement(query)) 
        {
        	pstmt.setInt(1, idProgetto);
        	pstmt.setInt(2, idCollaboratore);
        	ResultSet rs=pstmt.executeQuery();
        	
            while(rs.next()) 
            {
        		WorkPackage wp=new WorkPackage();
                wp.setCodiceWP(rs.getInt("idWP"));
                wp.setTitolo(rs.getString("titolo"));
                wp.setMeseInizio(rs.getInt("meseInizio"));
                wp.setMeseFine(rs.getInt("meseFine"));
                wp.setNumeroOrdine(rs.getInt("numeroOrdine"));
                list.add(wp);
            }
        }
        return list;
    }

    /**
     * Verifica che l'intero wp sia valido per l'assegnazione di un progetto.
     * @param codiceWP identificativo del WP da validare
     * @return {@code true} se è valido, {@code else} altrimenti
     * @throws SQLException se si verifica un errore
     */
	public boolean isAssignable(int codiceWP) throws SQLException 
	{
		WorkPackage wp=findWPById(codiceWP);
		
		 if(wp==null) return false;
		 
		 TaskDAO tDAO=new TaskDAO(connection);
		 List<Task> tasks=tDAO.findTasksByWP(codiceWP);
		 
		 if(tasks==null || tasks.isEmpty()) 
			 return false;
		 
		 for(Task t:tasks)
        	if(!tDAO.isAssignable(t.getId()))
        		return false;
		
		return true;
	} 
}