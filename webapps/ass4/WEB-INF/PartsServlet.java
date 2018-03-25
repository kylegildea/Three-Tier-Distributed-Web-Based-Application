/*  Name: Kyle Gildea     
Course: CNT 4714 – Spring 2017 – Project Four      
Assignment title:  Developing A Three-Tier Distributed Web-Based Application      
Date:  April 24, 2017 */

import java.sql.*;
import java.lang.*;
import java.io.PrintWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.RequestDispatcher;

public class PartsServlet extends HttpServlet 
{
	private Connection connection;
	private Statement statement;
	private ResultSet resultSet;
	private ResultSetMetaData metaData;
	private int numberOfRows;
	private String query;
	String message="";
	private boolean connectedToDatabase = false;

	// set up database connection and create SQL statement
	public void init( ServletConfig config ) throws ServletException
	{
		super.init(config);
		// attempt database connection and create Statement
		try 
		{
			//Class.forName( config.getInitParameter( "databaseDriver" ) );
			//connection = DriverManager.getConnection( 
			//config.getInitParameter( "databaseName" ),
			//config.getInitParameter( "username" ),
			//config.getInitParameter( "password" ) );


			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection("jdbc:mysql://localhost:3310/project4", "root", "root" );
			connectedToDatabase = true;
			// create Statement to query database
			statement = connection.createStatement();

		} // end try
		// for any exception throw an UnavailableException to 
		// indicate that the servlet is not currently available
		catch ( Exception exception ) 
		{
			exception.printStackTrace();
			throw new UnavailableException( exception.getMessage() );
		} // end catch
	}  // end method init 

	// process survey response
	protected void doPost( HttpServletRequest request,
			HttpServletResponse response )
					throws ServletException, IOException
	{
		query = request.getParameter( "userSQL" );	 
		HttpSession session = request.getSession();
		StringBuilder output = new StringBuilder(); 
		StringBuilder firstLetter = new StringBuilder();
		if(query.equals(""))
		{
			query = "select * from suppliers";
		}
		firstLetter.append(query.charAt(0));
		if(firstLetter.toString().equalsIgnoreCase("s"))
		{

			if ( !connectedToDatabase ) 
				throw new IllegalStateException( "Not Connected to Database" );

			// specify query and execute it
			try
			{
				resultSet = statement.executeQuery( query );

				// obtain meta data for ResultSet
				metaData = resultSet.getMetaData();
				
				// determine number of rows in ResultSet
				resultSet.last();                   // move to last row
				numberOfRows = resultSet.getRow();  // get row number     
				// set query and execute it
				output.append("<tr>");
				for(int i = 0; i < getColumnCount(); i++)
				{

					output.append("<th>");
					output.append(getColumnName(i));
					output.append("</th>");
				}
				output.append("</tr>");
				for(int row = 0; row < getRowCount(); row++)
				{
					output.append("<tr>");
					for(int col = 0; col < getColumnCount(); col++)
					{
						output.append("<td>");
						if(getColumnClass(col).toString() == "String")
						{
							output.append((String)getValueAt(row, col));
						}
						else
						{
							output.append(getValueAt(row, col));
						}
						output.append("</td>");
					}
					output.append("</tr>");
				}
				message = output.toString();
			}catch(Exception ex)
			{
				message = ex.getMessage();
			}
		}
		else
		{
			try
			{
				int res;
				// ensure database connection is available
				if ( !connectedToDatabase ) 
					throw new IllegalStateException( "Not Connected to Database" );
				query.toLowerCase();
				// specify query and execute it
				if(query.contains("insert") || query.contains("update"))
				{
					Statement checkStatement = connection.createStatement();
					checkStatement.executeUpdate("drop table if exists beforeShipments;");
					checkStatement.executeUpdate("create table beforeShipments like shipments;");
					checkStatement.executeUpdate("insert into beforeShipments select * from shipments;");

					res = statement.executeUpdate( query );

					int rowsUpdated = checkStatement.executeUpdate("update suppliers set status = status + 5 where snum in (select distinct snum from shipments left join beforeShipments using (snum, pnum,jnum,quantity) where beforeShipments.snum is null and quantity > 100 )");
					if(rowsUpdated > 0)
					{
						message = "Business Logic Detected, Updating Supplier Status<br/><br/> " + rowsUpdated + " rows updated";
					}
					else
					{
						message = "Update Successful";
					}
				}
				else
				{
					res = statement.executeUpdate( query );
					message = " ";
				}
			}catch(Exception ex)
			{
				message = ex.getMessage();
			}
		}
		session.setAttribute("message", message); 
		session.setAttribute("userSQL", query);
		RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/index.jsp");
		dispatcher.forward(request, response);
	} // end method doPost

	// close SQL statements and database when servlet terminates
	public void destroy()
	{
		// attempt to close statements and database connection
		try 
		{
			statement.close();
			connection.close();
		} // end try
		// handle database exceptions by returning error to client
		catch( Exception sqlException ) 
		{
			sqlException.printStackTrace();
		} // end catch
	} // end method destroy

	public void setQuery( String query ) 
			throws SQLException, IllegalStateException 
	{
		// ensure database connection is available
		if ( !connectedToDatabase ) 
			throw new IllegalStateException( "Not Connected to Database" );

		// specify query and execute it
		resultSet = statement.executeQuery( query );

		// obtain meta data for ResultSet
		metaData = resultSet.getMetaData();

		// determine number of rows in ResultSet
		resultSet.last();                   // move to last row
		numberOfRows = resultSet.getRow();  // get row number      

	} // end method setQuery

	// set new database update-query string
	public void setUpdate( String query ) 
			throws SQLException, IllegalStateException 
	{
		int res;
		// ensure database connection is available
		if ( !connectedToDatabase ) 
			throw new IllegalStateException( "Not Connected to Database" );

		// specify query and execute it
		res = statement.executeUpdate( query );
	} // end method setUpdate

	public Class getColumnClass( int column ) throws IllegalStateException
	{
		// ensure database connection is available
		if ( !connectedToDatabase ) 
			throw new IllegalStateException( "Not Connected to Database" );

		// determine Java class of column
		try 
		{
			String className = metaData.getColumnClassName( column + 1 );

			// return Class object that represents className
			return Class.forName( className );
		} // end try
		catch ( Exception exception ) 
		{
			exception.printStackTrace();
		} // end catch

		return Object.class; // if problems occur above, assume type Object
	} // end method getColumnClass
	
	// get number of columns in ResultSet
	public int getColumnCount() throws IllegalStateException
	{   
		// ensure database connection is available
		if ( !connectedToDatabase ) 
			throw new IllegalStateException( "Not Connected to Database" );

		// determine number of columns
		try 
		{
			return metaData.getColumnCount(); 
		} // end try
		catch ( SQLException sqlException ) 
		{
			sqlException.printStackTrace();
		} // end catch

		return 0; // if problems occur above, return 0 for number of columns
	} // end method getColumnCount

	// get name of a particular column in ResultSet
	public String getColumnName( int column ) throws IllegalStateException
	{    
		// ensure database connection is available
		if ( !connectedToDatabase ) 
			throw new IllegalStateException( "Not Connected to Database" );

		// determine column name
		try 
		{
			return metaData.getColumnName( column + 1 );  
		} // end try
		catch ( SQLException sqlException ) 
		{
			sqlException.printStackTrace();
		} // end catch

		return ""; // if problems, return empty string for column name
	} // end method getColumnName

	// return number of rows in ResultSet
	public int getRowCount() throws IllegalStateException
	{      
		// ensure database connection is available
		if ( !connectedToDatabase ) 
			throw new IllegalStateException( "Not Connected to Database" );

		return numberOfRows;
	} // end method getRowCount

	// obtain value in particular row and column
	public Object getValueAt( int row, int column ) 
			throws IllegalStateException
	{
		// ensure database connection is available
		if ( !connectedToDatabase ) 
			throw new IllegalStateException( "Not Connected to Database" );

		// obtain a value at specified ResultSet row and column
		try 
		{
			resultSet.next();  /* fixes a bug in MySQL/Java with date format */
			resultSet.absolute( row + 1 );
			return resultSet.getObject( column + 1 );
		} // end try
		catch ( SQLException sqlException ) 
		{
			sqlException.printStackTrace();
		} // end catch

		return ""; // if problems, return empty string object
	} // end method getValueAt

} // end class SurveyServlet

