package it.polimi.tiw.progetto2025.daos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import it.polimi.tiw.progetto2025.beans.Task;

/**
 * Data Access Object (DAO) dedicato alla gestione della persistenza dei Task
 * e delle relative allocazioni orarie (ore previste e ore lavorate).
 * Fornisce metodi ottimizzati per ridurre l'interazione con il database
 * e supporta transazioni ACID per le operazioni di scrittura.
 */
public class TaskDAO 
{
	private Connection connection;

    public TaskDAO(Connection connection) 
    {
        this.connection=connection;
    }

    /**
     * Crea un nuovo task e associa il numero di ore di default (pari a 0) per ogni 
     * mese compreso nell'intervallo di esecuzione specificato. 
     * L'intera operazione è eseguita all'interno di una transazione atomica.
     * @param idWp identificativo del work package a cui appartiene il task
     * @param titolo titolo del task
     * @param descrizione descrizione testuale del task
     * @param meseInizio mese iniziale di esecuzione del task (espresso come intero)
     * @param meseFine mese finale di esecuzione del task (espresso come intero)
     * @throws SQLException se si verifica un errore durante l'esecuzione delle query SQL o nella gestione della transazione
     */
    public void createTask(int idWp, String titolo, String descrizione, int meseInizio, int meseFine) throws SQLException 
    {
    	String select="SELECT MAX(numeroOrdine) FROM "+MyDAO.TASK_TABLE+" WHERE idWP=?";
        String queryTask="INSERT INTO "+MyDAO.TASK_TABLE+" (idWp, titolo, descrizione, numeroOrdine) VALUES (?, ?, ?, ?)";
        String queryMesi="INSERT INTO "+MyDAO.ORE_PREVISTE_TABLE+" (idTask, mese, ore) VALUES (?, ?, 0)";
        
        int nuovoOrdine=1;
        try(PreparedStatement pstatement=connection.prepareStatement(select)) 
        {
            pstatement.setInt(1, idWp);
            
            try (ResultSet rs=pstatement.executeQuery()) 
            {
                if(rs.next()) nuovoOrdine=rs.getInt(1)+1;
            }
        }
    	
        
        boolean oldAutoCommit=connection.getAutoCommit();
        
        try 
        {
            connection.setAutoCommit(false);

            int idTask=-1;
            try(PreparedStatement pstmt=connection.prepareStatement(queryTask, java.sql.Statement.RETURN_GENERATED_KEYS)) 
            {
                pstmt.setInt(1, idWp);
                pstmt.setString(2, titolo);
                pstmt.setString(3, descrizione);
                pstmt.setInt(4, nuovoOrdine);
                pstmt.executeUpdate();

                try(ResultSet generatedKeys=pstmt.getGeneratedKeys()) 
                {
                    if(generatedKeys.next()) idTask=generatedKeys.getInt(1);
                    else throw new SQLException("Creazione Task fallita, nessun ID generato.");
                }
            }

            try(PreparedStatement pstmtMesi=connection.prepareStatement(queryMesi)) 
            {
                for(int i=meseInizio; i<=meseFine; i++) 
                {
                    pstmtMesi.setInt(1, idTask);
                    pstmtMesi.setInt(2, i);
                    pstmtMesi.addBatch();
                }
                
                pstmtMesi.executeBatch();
            }

            connection.commit();
        } 
        catch (SQLException e) 
        {
            connection.rollback();
            throw e;
        } 
        finally 
        {
            connection.setAutoCommit(oldAutoCommit);
        }
    }
    
    /**
     * Cerca i task associati ad un determinato work package.
     * @param idWp identificativo del work package di riferimento
     * @return lista ordinata di task contenuti nel work package, completi di ore previste e lavorate
     * @throws SQLException se si verifica un errore di accesso al database
     */
    public List<Task> findTasksByWP(int idWp) throws SQLException 
    {
        Map<Integer, Task> taskMap=new LinkedHashMap<>();
        
        String queryTasks="SELECT idTask, idWp, titolo, descrizione, numeroOrdine FROM "+MyDAO.TASK_TABLE+" WHERE idWp=? ORDER BY numeroOrdine ASC";
        try(PreparedStatement pstmt=connection.prepareStatement(queryTasks)) 
        {
            pstmt.setInt(1, idWp);
            try(ResultSet rs=pstmt.executeQuery()) 
            {
                while(rs.next()) 
                {
                    int idTask=rs.getInt("idTask");
                    Task t=new Task();
                    t.setId(idTask);
                    t.setIdWp(rs.getInt("idWp"));
                    t.setNomeTask(rs.getString("titolo"));
                    t.setDescrizione(rs.getString("descrizione"));
                    t.setNumeroOrdine(rs.getInt("numeroOrdine"));
                    taskMap.put(idTask, t);
                }
            }
        }

        if(taskMap.isEmpty()) return new ArrayList<>();

        String queryOrePreviste="SELECT op.idTask, op.mese, op.ore FROM "+MyDAO.ORE_PREVISTE_TABLE+" op "+
        		"JOIN "+MyDAO.TASK_TABLE+" t ON op.idTask=t.idTask WHERE t.idWp=? ORDER BY op.mese ASC";
        try(PreparedStatement pstmt=connection.prepareStatement(queryOrePreviste)) 
        {
            pstmt.setInt(1, idWp);
            try(ResultSet rs=pstmt.executeQuery()) 
            {
                while(rs.next()) 
                {
                    Task t=taskMap.get(rs.getInt("idTask"));
                    if(t!=null) t.getOrePreviste().put(rs.getInt("mese"), rs.getInt("ore"));
                }
            }
        }

        String queryOreLavorate="SELECT ol.idTask, ol.mese, SUM(ol.ore) AS totale_ore FROM "+MyDAO.ORE_LAVORATE_TABLE+" ol "+
                                  "JOIN "+MyDAO.TASK_TABLE+" t ON ol.idTask=t.idTask WHERE t.idWp=? GROUP BY ol.idTask, ol.mese";
        try(PreparedStatement pstmt=connection.prepareStatement(queryOreLavorate)) 
        {
            pstmt.setInt(1, idWp);
            try(ResultSet rs=pstmt.executeQuery()) 
            {
                while(rs.next()) 
                {
                    Task t=taskMap.get(rs.getInt("idTask"));
                    if(t!=null) t.getOreLavorate().put(rs.getInt("mese"), rs.getInt("totale_ore"));
                }
            }
        }

        return new ArrayList<>(taskMap.values());
    }

    /**
     * Cerca i collaboratori associati ad un determinato task, escludendo i duplicati.
     * @param idTask identificativo del task di riferimento
     * @return lista di ID univoci dei collaboratori che partecipano al task
     * @throws SQLException se si verifica un errore di accesso al database
     */
	public List<Integer> findCollaboratorsByTask(int idTask) throws SQLException 
	{
		List<Integer> ids=new ArrayList<>();
		
		String query="SELECT idCollaboratore FROM "+MyDAO.ORE_LAVORATE_TABLE+" WHERE idTask=?";
		try(PreparedStatement pstmt=connection.prepareStatement(query)) 
        {
            pstmt.setInt(1, idTask);
            try(ResultSet rs=pstmt.executeQuery()) 
            {
                while(rs.next()) 
                    ids.add(rs.getInt("idCollaboratore"));

            }
        }
		
		return ids;
	}

	/**
     * Recupera le ore previste associate ad un determinato task, raggruppate per mese.
     * @param idTask identificativo del task di riferimento
     * @return una mappa che associa a ciascun mese le relative ore pianificate
     * @throws SQLException se si verifica un errore di accesso al database
     */
	public Map<Integer, Integer> findPlannedHoursByTask(int idTask) throws SQLException 
	{	
		Map<Integer, Integer> ore=new HashMap<>();
		
		String query="SELECT mese, ore FROM "+MyDAO.ORE_PREVISTE_TABLE+" WHERE idTask=? ORDER BY mese ASC";
		try(PreparedStatement pstmt=connection.prepareStatement(query)) 
        {
            pstmt.setInt(1, idTask);
            try(ResultSet rs=pstmt.executeQuery()) 
            {
                while(rs.next()) 
                	ore.put(rs.getInt("mese"), rs.getInt("ore"));
            }
        }
		
		return ore;
	}

	/**
     * Recupera le informazioni complete di un task dato il suo identificativo,
     * includendo sia la mappa delle ore previste che quella delle ore lavorate complessive.
     * @param idTask identificativo del task da cercare
     * @return l'oggetto Task popolato, oppure {@code null} se il task non esiste
     * @throws SQLException se si verifica un errore di accesso al database
     */
	public Task findTaskById(int idTask) throws SQLException 
	{
	    Task t=null;
	    
	    String query="SELECT idTask, idWp, titolo, descrizione, numeroOrdine FROM "+MyDAO.TASK_TABLE+" WHERE idTask=?";
	    try(PreparedStatement pstmt=connection.prepareStatement(query)) 
	    {
	        pstmt.setInt(1, idTask);
	        try(ResultSet rs=pstmt.executeQuery()) 
	        {
	            if(rs.next()) 
	            {
	                t=new Task();
	                t.setId(rs.getInt("idTask"));
	                t.setIdWp(rs.getInt("idWp"));
	                t.setNomeTask(rs.getString("titolo"));
	                t.setDescrizione(rs.getString("descrizione"));
	                t.setNumeroOrdine(rs.getInt("numeroOrdine"));
	            }
	        }
	    }
	    
	    if(t==null) return null;
	    
	    query="SELECT mese, ore FROM "+MyDAO.ORE_PREVISTE_TABLE+" WHERE idTask=? ORDER BY mese ASC";
	    try(PreparedStatement pstmt=connection.prepareStatement(query)) 
	    {
	        pstmt.setInt(1, idTask);
	        try(ResultSet rs=pstmt.executeQuery()) 
	        {
	            while(rs.next())
	                t.getOrePreviste().put(rs.getInt("mese"), rs.getInt("ore"));
	        }
	    }
	    

	    query="SELECT mese, SUM(ore) AS totale_ore FROM "+MyDAO.ORE_LAVORATE_TABLE+" WHERE idTask=? GROUP BY mese";
	    try(PreparedStatement pstmt=connection.prepareStatement(query)) 
	    {
	        pstmt.setInt(1, idTask);
	        try(ResultSet rs=pstmt.executeQuery())
	        {
	            while(rs.next())
	                t.getOreLavorate().put(rs.getInt("mese"), rs.getInt("totale_ore"));
	        }
	    }
	    
	    return t;
	}
	
	/**
     * Recupera le informazioni del task dato il suo identificativo, filtrando e 
     * isolando esclusivamente le ore lavorate dallo specifico collaboratore passato come parametro.
     * @param idTask identificativo del task da cercare
     * @param idCollaboratore identificativo del collaboratore di cui mappare le ore
     * @return l'oggetto Task popolato con i dati del collaboratore, oppure {@code null} se il task non esiste
     * @throws SQLException se si verifica un errore di accesso al database
     */
	public Task findTaskByIdWithCollaborator(int idTask, int idCollaboratore) throws SQLException 
	{
	    Task t=null;
	    
	    String query="SELECT idTask, idWp, titolo, descrizione, numeroOrdine FROM "+MyDAO.TASK_TABLE+" WHERE idTask=? ORDER BY numeroOrdine ASC";
	    try(PreparedStatement pstmt=connection.prepareStatement(query)) 
	    {
	        pstmt.setInt(1, idTask);
	        try(ResultSet rs=pstmt.executeQuery()) 
	        {
	            if(rs.next()) 
	            {
	                t=new Task();
	                t.setId(rs.getInt("idTask"));
	                t.setIdWp(rs.getInt("idWp"));
	                t.setNomeTask(rs.getString("titolo"));
	                t.setDescrizione(rs.getString("descrizione"));
	                t.setNumeroOrdine(rs.getInt("numeroOrdine"));
	            }
	        }
	    }
	    
	    if(t==null) return null;
	    
	    query="SELECT mese, ore FROM "+MyDAO.ORE_PREVISTE_TABLE+" WHERE idTask=? ORDER BY mese ASC";
	    try(PreparedStatement pstmt=connection.prepareStatement(query)) 
	    {
	        pstmt.setInt(1, idTask);
	        try(ResultSet rs=pstmt.executeQuery()) 
	        {
	            while(rs.next())
	                t.getOrePreviste().put(rs.getInt("mese"), rs.getInt("ore"));
	        }
	    }
	    

	    query="SELECT mese, ore FROM "+MyDAO.ORE_LAVORATE_TABLE+" WHERE idTask=? AND idCollaboratore=? GROUP BY mese";
	    try(PreparedStatement pstmt=connection.prepareStatement(query)) 
	    {
	        pstmt.setInt(1, idTask);
	        pstmt.setInt(2, idCollaboratore);
	        try(ResultSet rs=pstmt.executeQuery())
	        {
	            while(rs.next())
	                t.getOreLavorate().put(rs.getInt("mese"), rs.getInt("ore"));
	        }
	    }
	    
	    return t;
	}
	
	/**
     * Aggiorna integralmente l'allocazione temporale e di staff di un determinato task.
     * Rimuove le allocazioni e pianificazioni precedenti e inserisce le nuove configurazioni.
     * L'intera catena di eliminazione e inserimento a batch è protetta da una transazione SQL.
     * @param idTask identificativo del task da aggiornare
     * @param idCollaboratori lista degli ID dei collaboratori da associare al task
     * @param meseOreMap mappa contenente l'accoppiamento Mese-Ore Previste pianificate per il task
     * @throws SQLException se avviene un errore nella cancellazione, nell'esecuzione dei batch o se è necessario un rollback
     */
	public void updateTaskAllocation(int idTask, List<Integer> idCollaboratori, Map<Integer, Integer> meseOreMap) throws SQLException 
	{
        String deleteLavorate="DELETE FROM "+MyDAO.ORE_LAVORATE_TABLE+" WHERE idTask=?";
        String deletePreviste="DELETE FROM "+MyDAO.ORE_PREVISTE_TABLE+" WHERE idTask=?";
        String insertPreviste="INSERT INTO "+MyDAO.ORE_PREVISTE_TABLE+" (idTask, mese, ore) VALUES (?, ?, ?)";
        String insertLavorate="INSERT INTO "+MyDAO.ORE_LAVORATE_TABLE+" (idTask, idCollaboratore, mese, ore) VALUES (?, ?, ?, 0)";

        boolean oldAutoCommit=connection.getAutoCommit();
        try 
        {
            connection.setAutoCommit(false);

            try(PreparedStatement ps=connection.prepareStatement(deleteLavorate)) 
            {
                ps.setInt(1, idTask);
                ps.executeUpdate();
            }

            try(PreparedStatement ps=connection.prepareStatement(deletePreviste)) 
            {
                ps.setInt(1, idTask);
                ps.executeUpdate();
            }

            try(PreparedStatement ps=connection.prepareStatement(insertPreviste)) 
            {
                for(Map.Entry<Integer, Integer> entry:meseOreMap.entrySet()) 
                {
                    ps.setInt(1, idTask);
                    ps.setInt(2, entry.getKey());
                    ps.setInt(3, entry.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try(PreparedStatement ps=connection.prepareStatement(insertLavorate)) 
            {
                for(Integer id:idCollaboratori) 
                {
                    for(Integer mese:meseOreMap.keySet()) 
                    {
                        ps.setInt(1, idTask);
                        ps.setInt(2, id);
                        ps.setInt(3, mese);
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }

            connection.commit();
        } 
        catch (SQLException e) 
        {
            connection.rollback();
            throw e;
        } 
        finally 
        {
            connection.setAutoCommit(oldAutoCommit);
        }
    }
	
	/**
     * Recupera tutti i task appartenenti a un Work Package nei quali è coinvolto 
     * uno specifico collaboratore.
     * @param idWp identificativo del work package entro cui cercare
     * @param idCollaboratore identificativo del collaboratore assegnato ai task
     * @return lista di oggetti Task compilati con le ore lavorate dal singolo utente e le ore previste globali
     * @throws SQLException se si verifica un errore durante l'esecuzione dei JOIN batch
     */
	public List<Task> findTaskWithCollaboratorInWp(int idWp, int idCollaboratore) throws SQLException 
	{
        Map<Integer, Task> taskMap=new LinkedHashMap<>();

        String queryTasks="SELECT DISTINCT t.idTask, t.titolo, t.descrizione, t.idWp, t.numeroOrdine FROM "+MyDAO.TASK_TABLE+" t "+
                             "JOIN "+MyDAO.ORE_LAVORATE_TABLE+" ol ON ol.idTask=t.idTask WHERE t.idWp=? AND ol.idCollaboratore=?";
        try(PreparedStatement pstmt=connection.prepareStatement(queryTasks)) 
        {
            pstmt.setInt(1, idWp);
            pstmt.setInt(2, idCollaboratore);
            try(ResultSet rs=pstmt.executeQuery()) 
            {
                while(rs.next()) 
                {
                    int idTask=rs.getInt("idTask");
                    Task t=new Task();
                    t.setId(idTask);
                    t.setIdWp(rs.getInt("idWp"));
                    t.setNomeTask(rs.getString("titolo"));
                    t.setDescrizione(rs.getString("descrizione"));
                    t.setNumeroOrdine(rs.getInt("numeroOrdine"));
                    taskMap.put(idTask, t);
                }
            }
        }

        if(taskMap.isEmpty()) return new ArrayList<>();

        String queryOrePreviste="SELECT op.idTask, op.mese, op.ore FROM "+MyDAO.ORE_PREVISTE_TABLE+" op "+
                                  "JOIN "+MyDAO.TASK_TABLE+" t ON op.idTask=t.idTask WHERE t.idWp=? ORDER BY op.mese ASC";
        try(PreparedStatement pstmt=connection.prepareStatement(queryOrePreviste)) 
        {
            pstmt.setInt(1, idWp);
            try(ResultSet rs=pstmt.executeQuery()) 
            {
                while(rs.next()) 
                {
                    Task t=taskMap.get(rs.getInt("idTask"));
                    if(t!=null) t.getOrePreviste().put(rs.getInt("mese"), rs.getInt("ore"));
                }
            }
        }

        String queryOreLavorate="SELECT ol.idTask, ol.mese, ol.ore FROM "+MyDAO.ORE_LAVORATE_TABLE+" ol "+
        		"JOIN "+MyDAO.TASK_TABLE+" t ON ol.idTask=t.idTask WHERE t.idWp=? AND ol.idCollaboratore=? ORDER BY ol.mese ASC";
        try(PreparedStatement pstmt=connection.prepareStatement(queryOreLavorate)) 
        {
            pstmt.setInt(1, idWp);
            pstmt.setInt(2, idCollaboratore);
            try(ResultSet rs=pstmt.executeQuery()) 
            {
                while(rs.next()) 
                {
                    Task t=taskMap.get(rs.getInt("idTask"));
                    if(t!=null) t.getOreLavorate().put(rs.getInt("mese"), rs.getInt("ore"));
                }
            }
        }

        return new ArrayList<>(taskMap.values());
    }

	/**
     * Aggiorna puntualmente il quantitativo di ore lavorate inserite da un singolo collaboratore
     * per un determinato task in un mese specifico.
     * @param idTask identificativo del task di riferimento
     * @param idCollaboratore identificativo del collaboratore che ha lavorato
     * @param mese il mese di riferimento della prestazione oraria
     * @param oreLavorate il nuovo valore numerico di ore da registrare
     * @throws SQLException se si verifica un errore durante l'esecuzione dell'istruzione di UPDATE
     */
	public void updateHours(int idTask, int idCollaboratore, int mese, int oreLavorate) throws SQLException 
	{
		String query="UPDATE "+MyDAO.ORE_LAVORATE_TABLE+" SET ore=? WHERE idTask=? AND idCollaboratore=? AND mese=?";
		
		try(PreparedStatement pstmt=connection.prepareStatement(query)) 
        {
        	pstmt.setInt(1, oreLavorate);
        	pstmt.setInt(2, idTask);
        	pstmt.setInt(3, idCollaboratore);
        	pstmt.setInt(4, mese);
        	
        	pstmt.executeUpdate();
        }		
	}
	
    /**
     * Verifica che un task sia valido per l'assegnazione di un progetto.
     * @param id identificativo del task da validare
     * @return {@code true} se è valido, {@code else} altrimenti
     * @throws SQLException se si verifica un errore
     */
	public boolean isAssignable(int id) throws SQLException 
	{
		Task t=findTaskById(id);
		
		 if(t==null) 
			 return false;
		
		 List<Integer> collaborators=findCollaboratorsByTask(t.getId());
		 
		 if(collaborators==null || collaborators.isEmpty())
			 return false;
		 
		 if(t.getOrePreviste()==null || t.getOrePreviste().isEmpty())
			 return false;
		 
		return t.getOrePreviste().entrySet().stream()
	            .allMatch(entry -> entry.getKey()>=t.getMeseInizio() 
                && entry.getKey()<=t.getMeseFine() 
                && entry.getValue()>0);
	}
}