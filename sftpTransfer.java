/* ----------------------------------------------------------------------------
 * Copyright 2014 Ideabytes Inc.
 * 
 * @File: stpTransfer.java
 *
 * Description:
 *   Transfer file between local computer and remote server:
 *   	using sftp for secure file transmission
 *      using Apache Commons VFS to provide a single API for accessing file systems (e.g. SFTP)
 *      
 *
 * Author: Shalina (Shiyan) Hu
 * Date:   Dec. 1, 2014
 * ----------------------------------------------------------------------------
 */


import java.io.File;
import java.io.IOException;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;

public class sftpTransfer {
	
	public fileProcess fileToTrans; 
	
	private String hostName;
	private String portNumber;
	private String userName;
	private String passWord;
	private String localFilePath;
	private String remoteFilePath;	
	
	public sftpTransfer (String hName, String nPort, String uName, String pWord) {
		hostName = hName;
		portNumber = nPort;
		userName = uName;
		passWord = pWord;
	}
	
	// Establish sftp connection
	public static String createConnection(String hostName, String port, 
			String username, String password, String remoteFilePath) {
	//return "sftp://" + username + ":" + password + "@" + hostName + ":2210/" + remoteFilePath;
		return "sftp://" + username + ":" + password + "@" + hostName + ":" + port + "/" + remoteFilePath;
	}
	
	// Setup default SFTP configuration
	public static FileSystemOptions createDefaultOptions() throws FileSystemException {
		
		// Create SFTP options
	    FileSystemOptions opts = new FileSystemOptions();

	    // SSH Key checking
	    SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(opts, "no");

	    // Root directory set to user home
	    SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(opts, true);

	    // Timeout is count by Milliseconds
	    SftpFileSystemConfigBuilder.getInstance().setTimeout(opts, 10000);

	    return opts;
	}
	
	// Check  if remote file exist 
	public static boolean exist(String hostName, String port, String username,
			String password, String remoteFilePath) {
		
		StandardFileSystemManager manager = new StandardFileSystemManager();

	    try {
	    	manager.init();

	        // Create remote object
	        FileObject remoteFile = manager.resolveFile(
	        		createConnection(hostName, port, username, password,
	                remoteFilePath), createDefaultOptions());

	        System.out.println("File exist: " + remoteFile.exists());

	        	return remoteFile.exists();
	    } catch (Exception e) {
	    	throw new RuntimeException(e);
	    } finally {
	    	manager.close();
	    }
	}
	
	// Create a new folder in remote server
	public static void createFolder (String hostName, String port, String username,
			String password, String remoteFilePath){
		
		StandardFileSystemManager manager = new StandardFileSystemManager();

	    try {
	    	manager.init();

	        // Create remote object
	        FileObject remoteFile = manager.resolveFile(
	        		createConnection(hostName, port, username, password,
	                remoteFilePath), createDefaultOptions());

	        remoteFile.createFolder();
	        System.out.println("folder created");
	        	
	    } catch (Exception e) {
	    	throw new RuntimeException(e);
	    } finally {
	    	manager.close();
	    }

	}
	
	// Upload a file to the remote server
	public static void upLoad(String hostName, String port, String username,
            String password, String localFilePath, String remoteFilePath) {

		File file = new File(localFilePath);
		if (!file.exists())
            throw new RuntimeException("Error. Local file not found -- " + file.getPath());

		StandardFileSystemManager manager = new StandardFileSystemManager();

		try {
            	manager.init();

            	// Create local file object
            	FileObject localFile = manager.resolveFile(file.getAbsolutePath());

            	// Create remote file object
            	FileObject remoteFile = manager.resolveFile(
            			createConnection(hostName, port, username, password,
                        remoteFilePath), createDefaultOptions());

            	// Copy local file to sftp server
            	remoteFile.copyFrom(localFile, Selectors.SELECT_SELF);

            	System.out.println("File upload success");
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
            manager.close();
		}
	}
	
	// Download a file from remote server 
	public static void downLoad(String hostName, String port, String username,
	                String password, String localFilePath, String remoteFilePath) {

	        StandardFileSystemManager manager = new StandardFileSystemManager();

	        String downloadFilePath = localFilePath.substring(0,
	                localFilePath.lastIndexOf("."))
	                + "_downlaod_from_sftp"
	                + localFilePath.substring(localFilePath.lastIndexOf("."),
	                localFilePath.length());
	                
	        File file = new File (downloadFilePath);
	                
	        try {
	                manager.init();

	                // Create local file object
	                FileObject localFile = manager.resolveFile(file.getAbsolutePath());
	                
	                // Create remote file object
	                FileObject remoteFile = manager.resolveFile(
	                		createConnection(hostName, port, username, password,
	                        remoteFilePath), createDefaultOptions());

	                // Copy local file to sftp server
	                localFile.copyFrom(remoteFile, Selectors.SELECT_SELF);

	                System.out.println("File download success");
	        } catch (Exception e) {
	        	throw new RuntimeException(e);
	        } finally {
	            manager.close();
	        }
	}
	
	// Delete file in remote system:
	public static void deleteFile(String hostName, String port, String username,
	                String password, String remoteFilePath) {
	        StandardFileSystemManager manager = new StandardFileSystemManager();

	        try {
	                manager.init();

	                // Create remote object
	                FileObject remoteFile = manager.resolveFile(
	                		createConnection(hostName, port, username, password,
	                        remoteFilePath), createDefaultOptions());

	                if (remoteFile.exists()) {
	                	remoteFile.delete();
	                    System.out.println("Delete remote file success  ---" + remoteFilePath);
	                }
	        } catch (Exception e) {
	        	throw new RuntimeException(e);
	        } finally {
	        	manager.close();
	        }
	}
	
	// Break a video file into many blocks and then upload each block to remote server
	public void uploadFiles(String localFile, String remotePath) throws IOException{		
		
		localFilePath = localFile;
		remoteFilePath = remotePath;
		
		fileToTrans = new fileProcess (localFilePath);
		
		File fis = new File (localFilePath);
		
		if(fileToTrans.checkFileType(fis)){
		
			fileToTrans.splitFile(fis); 
		
			String blockPath = fileToTrans.blockPath;      // Extract path for split files
		
			// Create a dir in remote server
			String remoteFileDir = remoteFilePath + "/" + blockPath.substring(blockPath.lastIndexOf("/")+1);
			createFolder(hostName, portNumber, userName, passWord, remoteFileDir);
		
			// Extract all file names in the folder
			File folder = new File(fileToTrans.blockPath);
			File[] listOfFiles = folder.listFiles();
    	  	
			for (File file : listOfFiles) {
				String remoteFile = remoteFileDir + "/" + file.getName();
				upLoad(hostName, portNumber, userName, passWord, file.getPath(), remoteFile);   		
			}
		}
		else
			System.err.println(fis.getPath() + " is not in MP4 format.");
		
	}
	
	public void downloadFiles() throws IOException {
		
	}
	
    public static void main(String[] args) throws IOException {
        String hostName = "olab.ideabytes.com";
        String port = "2210";
        String username = "vbox";
        String password = "Jump4Life";
        String localFile = "test/Tours.mp4";
        String remoteFilePath = "test";
        
        sftpTransfer fileTrans = new sftpTransfer (hostName, port, username, password);
        
        fileTrans.uploadFiles(localFile, remoteFilePath);
//       fileTrans.upLoad(hostName, port, username, password, localFile, remoteFilePath);
 //       exist(hostName, port, username, password, remoteFilePath);
 //       downLoad(hostName, port, username, password, localFilePath, remoteFilePath);
  //      deleteFile(hostName, port, username, password, remoteFilePath);
        
    }

}
