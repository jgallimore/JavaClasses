package com.genexus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

import com.genexus.db.Namespace;
import com.genexus.db.UserInformation;
import com.genexus.platform.INativeFunctions;
import com.genexus.platform.NativeFunctions;
import com.genexus.util.ReorgSubmitThreadPool;

public abstract class GXReorganization/*extends Applet*/
{
	// static JTable Table;
	// static MyTableModel myTableModel;

   	//private Frame m_Frame;
	protected File reorganizationFlag;
	protected boolean working = false;
	protected boolean autoCommit = true;
	int handle;

	private static final String ReoFlagGen = "REORGPGM.GEN";
	protected static final String ReoFlagExp = "REORGPGM.EXP";
	
	public abstract void execute();
	public abstract String getPackageDir();

	protected static Messages msg;
	protected ModelContext context;

	public GXReorganization(Class gxCfg)
	{
		ApplicationContext.getInstance().setReorganization(true);
		Application.init(gxCfg);
		ServerPreferences.fileName = "reorg.cfg";

		context = new ModelContext(gxCfg);
		handle = Application.getConnectionManager().createUserInformation(Namespace.getNamespace(context.getNAME_SPACE())).getHandle();

		msg = Application.getConnectionManager().getUserInformation(handle).getLocalUtil().getMessages();
		
	
	}
	
	public static String getMainDBName(ModelContext context, int handle)
	{
		UserInformation ui = Application.getConnectionManager().getUserInformation(handle);
		return ui.getNamespace().getDataSource("DEFAULT").jdbcDBName;
	}

	protected void cleanup()
	{
		try
		{
			//if	(gui)
				//m_Frame.dispose();
		}
		catch (Exception e)
		{
		}

		//Application.exitApplet();
	}
	protected int getHandle()
	{
		return handle;
	}

	protected String getPath()
	{
		//if	(Application.getApplet() != null)
		//{
		//	String codeBase = Application.getApplet().getCodeBase().toString();
		//	return codeBase.substring(codeBase.indexOf('/') + 1).replace('/', '\\');
		//}

		return "";
	}

	private static boolean gui   = false;
	private static boolean force = false;
	private static boolean recordcount = false;
	private static boolean ignoreresume = false;
	private static boolean noprecheck = false;
	private static boolean notexecute = false;

	private void processParameters(String args[])
	{
		int i = 0;
		while (i < args.length)
		{
			if	(args[i].startsWith("-"))
			{
				if	(args[i].toLowerCase().startsWith("-nogui"))
				{
					gui = false;
				}
				if	(args[i].toLowerCase().startsWith("-force"))
				{
					force = true;
				}
				if (args[i].toLowerCase().startsWith("-recordcount"))
				{
					recordcount = true;
				}
				if (args[i].toLowerCase().startsWith("-ignoreresume"))
				{
					ignoreresume = true;
				}
				if (args[i].toLowerCase().startsWith("-noverifydatabaseschema"))
				{
					noprecheck = true;
				}				
				if (args[i].toLowerCase().startsWith("-donotexecute"))
				{
					notexecute = true;
				}				
			}
			else
			{
				break;
			}
			i++;
		}
	}
	
	public static boolean getRecordCount()
	{
		return recordcount;
	}
	
	public static void printRecordCount(String tableName, int recordCount)
	{
		if (!executingResume)
		{
			addMsg(msg.getMessage("GXM_table_recordcount", new Object[] { tableName, Integer.valueOf(recordCount) }));
			//addMsg("Table " + tableName + " has " + recordCount + " records.");
		}
	}	

	/*
	Button ok;
	private void showUI()
	{
		m_Frame = new Frame(doReorganization ? msg.getMessage("GXM_gxdbm_reorg") : msg.getMessage("GXM_gxdbm_cpymdl"));

		m_Frame.setBackground(new Color(172, 172, 172));
		//m_Frame.setForeground(new Color(255, 255, 255));

		GXutil.MainInsets = m_Frame.getInsets();

		m_Frame.setSize( 470, 370);

		Dimension dim  = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		Dimension fdim = m_Frame.getSize();

		m_Frame.setLocation((dim.width - fdim.width) / 2,(dim.height - fdim.height) /2);

		m_Frame.add(this, BorderLayout.CENTER);
		setLayout(new BorderLayout(20, 20));
		
		Panel top   = new Panel();
		Panel left  = new Panel();
		Panel right = new Panel();
		ok   = new Button(PrivateUtilities.removeMnemonicKey(msg.getMessage("GXM_button_ok")));
		
		add(ok   , BorderLayout.SOUTH);
		ok.setEnabled(false);  
		ok.addActionListener(new GXAction());

		add(top  , BorderLayout.NORTH);
		add(right, BorderLayout.EAST);
		add(left , BorderLayout.WEST);
			
		myTableModel = new MyTableModel();
		
		Table = new JTable(myTableModel);
		Table.setShowGrid(false);
		JScrollPane scrollpane = new JScrollPane(Table);
		Table.setPreferredScrollableViewportSize(new Dimension(450, 250));
		Table.setDefaultRenderer(Label.class, new LabelRenderer());
		
		add(scrollpane, BorderLayout.CENTER);
		
		m_Frame.setVisible(true);
		m_Frame.addWindowListener(new WPWindowListener());	

	}
	*/
	
	boolean doReorganization;
	public void executeReorg(String args[], boolean isCreateDataBase)
	{
		createDataBase = isCreateDataBase;
		processParameters(args);
		if (notexecute)
		{
			addMsg(msg.getMessage("GXM_dbnotreorg"));
		}
		else
		{
			if	(!gui)
				ApplicationContext.getInstance().setMsgsToUI(false);

			executeReorg();
		}
	}

	protected void executeReorg()
	{
		reorganizationFlag = new File(getPath() + ReoFlagGen );

		doReorganization = Application.getClientContext().getClientPreferences().getCS_REORGJAVA();
      
		// Esto es para que los procs de reorg no terminen con System.exit
      	Application.realMainProgram = this;

		//if (gui)
			//showUI();

		if	(!force)
		{
			returnValue = true;
			NativeFunctions.getInstance().executeWithPermissions(new ReorgEnabled1(), INativeFunctions.FILE_READ);

			if	(!returnValue)
				return;
		}

		if (!gui || force)
		{
			if (doReorganization)
			{
				if (!recordcount)
				{
					beginResume();
					if (!inavlidResumeVersion)
					{
						try
						{
							processExternalScript("beforeReorganizationScript.txt");
						}
						catch(Exception ex)
						{
						}
					}
				}
				if(!inavlidResumeVersion)
				{
					try
					{
						execute();
					}
					catch(GXRuntimeException e)
					{
						e.printStackTrace();
					}
				}
			}
			else
			{
				addMsg(msg.getMessage("GXM_dbnotreorg"));
				addMsg(msg.getMessage("GXM_reorgpref"));
			}

			ReorgSubmitThreadPool.waitForEnd();
			if	(success() && !ReorgSubmitThreadPool.hasAnyError())
			{
				if (!recordcount)
				{
					deleteResumeFile();
					try
					{
						processExternalScript("afterReorganizationScript.txt");
					}
					catch(Exception ex)
					{
					}
					addMsg(msg.getMessage("GXM_reorgsuccess"));
				}
				Application.commit(context, getHandle(), "DEFAULT", "GXReorganization");
			}
			else
			{
				addMsg(msg.getMessage("GXM_reorgnotsuccess"));
				if (!gui)
					System.exit(1);
			}

			//if	(gui)
				//ok.setEnabled(true);
			//else
				//System.exit(0);
		}
		else
		{
			cleanup();
		}
	}

	class ReorgEnabled1 implements Runnable
	{
		public void run()
		{
			
			if	(!reorganizationFlag.exists())
			{
				msg(msg.getMessage("GXM_noreorg"));
				returnValue = false;
				cleanup();
				return;
			}
		}
	}


	private void msg(String text)
	{
			System.out.println("! " + text);
	}

	class ProcessFiles implements Runnable
	{
		public void run()
		{
			if (new File(getPath() + ReoFlagExp).exists())
			{
				new File(getPath() + ReoFlagExp).delete();
			}

			if	(!reorganizationFlag.renameTo(new File(getPath() + "REORGPGM.EXP")))
			{
				addMsg(msg.getMessage("GXM_reorgrenre"));
				returnValue = false;
				return;
			}
    	}
	}

	protected boolean returnValue;
	public boolean success()	
	{
		if (checkError)
		{
			addMsg(msg.getMessage("GXM_error_in_schema_verification"));
			//addMsg("An error was found in the database schema verification process.");
			addMsg(errorMessage);
			deleteResumeFile();
			return false;
		}
		
		if (inavlidResumeVersion)
		{
			return false;
		}
		
		returnValue = true;

		if	(!force)
		{
			//addMsg(msg.getMessage("reorgupdgxdb"));

			NativeFunctions.getInstance().executeWithPermissions(new ProcessFiles(), INativeFunctions.FILE_ALL);
		}

		return returnValue;
	}

	public static void addMsg(String msg)
	{
		if (gui /* && Table != null */)
		{
			//Label label = new Label(msg);
			//Table.setValueAt(label, Table.getRowCount() +1, 0); 
		}
		else
		{
			AndroidLog.info(msg);
		}
	}
	
	public static void addMsg(int index, String msg)
	{
		if	(gui)
		{			
			//Label label = new Label(msg);
			//Table.setValueAt(label, index -1, 0); 
		}
		else
		{
			AndroidLog.info(msg);
		}
	}
	
	public static void replaceMsg(int index, String msg)
	{
		if	(gui)
		{
			//Label label = new Label(msg);
			//Table.setValueAt(label, index -1, 0); 
		}
		else
		{
			AndroidLog.info(msg);
		}
	}		

	/*
	class GXAction implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			cleanup();	
		}
	}
	
	class MyTableModel extends AbstractTableModel 
	{
		private Vector data = new Vector();
		private String[] columnNames = {""};		
				
		public int getColumnCount() 
		{ 
			return 1; 
		}
			
		public int getRowCount() 
		{ 
			return data.size();
		}
		
        public String getColumnName(int col) {
            return columnNames[col];
        }
		
		public Class getColumnClass(int c) 
		{
			return getValueAt(0, c).getClass();
		}
		
			
		public Object getValueAt(int row, int col) 
		{
			return (Object)data.elementAt(row); 
		}
		
		public void setValueAt(Object value, int row, int col) 
		{
			if (data.size() <= row)
			{
				//data.addElement((Label)value);
				//fireTableRowsInserted(row, row); 
			}
			else
			{
				//data.setElementAt((Label)value, row);
				//fireTableRowsUpdated(row, row); 
			}
		}		
	}
	
	class LabelRenderer extends JLabel implements TableCellRenderer 
	{
		public LabelRenderer() 
		{
			setOpaque(true);
		}

		public Component getTableCellRendererComponent(JTable table, Object label, boolean isSelected, boolean hasFocus, int row, int column)
		{
			String msg = ((Label)label).getText();
			Color newColor;
			if (msg.endsWith("ENDED"))
			{
				newColor = new Color(0, 170, 0);
			}
			else
			{
				if (msg.endsWith("STARTED"))
				{
					newColor = new Color(255, 170, 130);
				}
				else
				{
					if (msg.endsWith("FAILED"))
					{
						newColor = Color.red;
					}
					else
					{
						if (msg.indexOf(" WAITING FOR ") != -1)
						{
							newColor = new Color(232, 232, 0);
						}
						else
						{
							newColor = Color.white;
						}
					}
				}
			}			
			setBackground(newColor);
			setText(msg);
	        return this;
		}
	}
	*/
	
   private void processExternalScript(String fileName) throws Exception
   {
	   try
	   {
		   BufferedReader input = new BufferedReader( new FileReader(fileName) );
		   String line = readSentence(input);
		   while (!line.equals(""))
		   {
			   if (!executedBefore(line))
			   {
				   addMsg(msg.getMessage("GXM_executing", new Object[] { line }));
				   //addMsg("Executing " + line);
				   ExecuteDirectSQL.executeWithThrow(context, getHandle(), "DEFAULT", line) ;
			   }
			   line = readSentence(input);
		   }
		   
	   }
	   catch(FileNotFoundException ex)
	   {
	   }
   }
   
	private String readSentence(BufferedReader is) throws Exception
	{
		StringBuffer result = new StringBuffer();
		boolean inLiteral = false;
		int caracter = is.read();
		while (caracter != ';' || inLiteral)
		{
			if (caracter == -1)
			{
				return "";
			}			
			if (caracter != (char)13 && caracter != (char)10)
			{
				if (caracter == '"')
				{
					inLiteral = !inLiteral;
				}
				result.append( (char) caracter );
			}
			caracter = is.read();
		}
		return result.toString();
	}
	
	//Implementaciones para la retoma
	private static final String resumeFileName = "resumereorg.txt";
	private static Vector executedStatements = new Vector();
	private static boolean executingResume = false;
	private boolean inavlidResumeVersion = false;
	private static boolean createDataBase = false;
	
	public static void setCreateDataBase()
	{
		createDataBase = true;
	}
	
	private void beginResume()
	{
		try
		{
			if (createDataBase || ignoreresume)
			{
				try
				{
					new File(resumeFileName).delete();
				}
				catch(Exception e)
				{
				}				
			}
			
			FileReader reader = new FileReader(resumeFileName);
			BufferedReader input = new BufferedReader(reader);
			String statement = input.readLine();
			if (statement != null)
			{
				if (!statement.equals(Application.getClientContext().getClientPreferences().getREORG_TIME_STAMP()))
				{
					inavlidResumeVersion = true;
					addMsg(msg.getMessage("GXM_lastreorg_failed1"));
					//addMsg("The last reorganization has failed and you are trying to execute a different reorganization.");
					addMsg(msg.getMessage("GXM_lastreorg_failed2"));
					//addMsg("Unexpected errors may occur if you don't try to finalize previous reorganization before running this one.");
					addMsg(msg.getMessage("GXM_lastreorg_failed3"));
					//addMsg("If you want to run this reorganization anyway, use 'ignoreresume' parameter.");
					return;
				}				
			}
			while (statement != null)
			{
				executedStatements.addElement(statement);
				statement = input.readLine();
			}
			input.close();
			reader.close();
			executingResume = true;
		}
		catch(FileNotFoundException fnfe)
		{
		}
		catch(IOException ioe)
		{
		}
		finally
		{
			serializeExecutedStatements();
		}
	}
	
	public static boolean executedBefore(String statement)
	{
		if (executingResume)
		{
			return executedStatements.contains(statement);
		}
		
		return false;
	}
	
	//static Object lock = new Object();
	
	public static void addExecutedStatement(String statement)
	{
		/*
		if (!recordcount) //Si estoy con recordCount entonces no tengo que poner nada en el archivo de retoma.
		{
			synchronized (lock)
			{
				try
				{
					output.write(statement);
					output.write(newline);
					output.flush();
				}
				catch(IOException ioe)
				{
				}		
			}
		}
		*/
	}
		
	//private static FileWriter output;
	private static final String newline = CommonUtil.newLine( );
	
	private static void serializeExecutedStatements()
	{
		/*
		try
		{
			output = new FileWriter(resumeFileName, true);
			if (!executingResume)
			{
				output.write(Application.getClientContext().getClientPreferences().getREORG_TIME_STAMP());
				output.write(newline);
				output.flush();				
			}
		}
		catch(IOException ioe)
		{
		}
		*/
	}
	
	private void deleteResumeFile()
	{
		/*
		try
		{
			output.close();
			new File(resumeFileName).delete();
		}
		catch(Exception e)
		{
		}
		*/
	}
	
	//Implementaciones para el precheck
	private static boolean checkError = false;
	private static String errorMessage;
	
	public static void setCheckError(String checkErrorMessage)
	{
		errorMessage = checkErrorMessage;
		checkError = true;
	}
	
	public static boolean isResumeMode()
	{
		return executingResume;
	}
	
	public static boolean mustRunCheck()
	{
		return(!executingResume && !noprecheck);
	}
	
	public static String getSchemaName()
	{
		String schema;
		String section = Application.getClientPreferences().getNAME_SPACE() + "|DEFAULT";
		schema = Application.getClientPreferences().getIniFile().getProperty(section, "CS_SCHEMA", "");
		if (schema.equals(""))
			return Application.getClientPreferences().getIniFile().getPropertyEncrypted(section, "USER_ID","");
		else
			return schema;
	}
	
}



