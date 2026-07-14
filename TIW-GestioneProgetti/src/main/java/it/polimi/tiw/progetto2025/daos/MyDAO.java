package it.polimi.tiw.progetto2025.daos;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import it.polimi.tiw.progetto2025.beans.exceptions.DBException;

public interface MyDAO {
	
	public final String DB_URL="jdbc:mysql://127.0.0.1:3306/tiw-project";
	public final String DB_DRIVER="com.mysql.cj.jdbc.Driver";
	public final String DB_NAME="`tiw-project`";
	public final String DB_USER="root";
	public final String DB_PASS="";

	public final String USER_TABLE=DB_NAME+".`utenti`";
	public final String PROJECT_TABLE=DB_NAME+".`progetti`";
	public final String WORK_PACKAGE_TABLE=DB_NAME+".`work_packages`";
	public final String TASK_TABLE=DB_NAME+".`task`";
	public final String ORE_PREVISTE_TABLE=DB_NAME+".`ore_previste`";
	public final String ORE_LAVORATE_TABLE=DB_NAME+".`ore_lavorate`";
	
	public static void closeQuery (ResultSet result, PreparedStatement pstatement) throws DBException
	{
		if(result!=null)
			try
			{
				result.close();
			}
			catch (Exception e)
			{
				throw new DBException();
			}
		
		MyDAO.closeQuery(pstatement);	
	}
	
	public static void closeQuery (PreparedStatement pstmt) throws DBException
	{
		if(pstmt!=null)
			try
			{
				pstmt.close();
			}
			catch (Exception e)
			{
					throw new DBException();
			}
	}
}
