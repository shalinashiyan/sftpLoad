/* ----------------------------------------------------------------------------
 * Copyright 2014 Ideabytes Inc.
 * 
 * @File: fileProcess.java
 *
 * Description:
 *   processing video file:
 *   	split a video file into n blocks, each block has predefined size
 *      append horizontal and vertical checksum data at the end of each block for error detecting
 *   	merge n block into one file
 *   	using mp4parser for mp4 file format checking (aspectjrt, isoparser)
 *      
 *
 * Author: Shalina (Shiyan) Hu
 * Date:   Nov. 27, 2014
 * ----------------------------------------------------------------------------
 */

import java.io.*;
import java.util.Arrays;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.MovieBox;

public class fileProcess {
	
	public final int rUnit = 1024;
	public final int sizeOfFiles = 1024 * rUnit;  // 1MB size of each trunk file
	
	public String blockPath;
	public String origFile;
	
	public fileProcess (String file){
		origFile = file;			
	}
	
	public boolean checkFileType (File f) throws IOException {
		
		boolean flag = true;
		String fileName = f.getName();
		String fileExt = fileName.substring(fileName.lastIndexOf(".")+1);
       
		if (fileExt.equals("mp4")) {
    	   
    	   IsoFile isoFile = new IsoFile(f.getPath());
    	   MovieBox mbox = isoFile.getMovieBox();
   		
   		   if (mbox == null)
   			   flag = false;
   		     	   
		}
    	else
    	   flag = false;
		
		if (flag)
			System.err.println(f.getPath() + " is in mp4 format " + flag);
		else
			System.err.println(f.getPath() + " is not in mp4 format " + flag);
		
		return flag;
	}
	
    public void splitFile(File f) throws IOException {
    	
    	BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
        
    	FileOutputStream out;     
        	        
    	String name = f.getName();    		
    	String fileName = name.replaceFirst("[.][^.]+$", ""); 		// Get file name
    	String fileExt = name.substring(name.lastIndexOf(".")+1);	// Get file extension    		                    
    		
    	// Create a folder to store all parts
    	File dir = new File(f.getParent()+"/"+fileName);
    	dir.mkdir();
    		
    	blockPath = dir.getPath();
        
    	int partCounter = 1;    
        
    	byte[] buffer = new byte[sizeOfFiles];
    	byte[] checkSum;
    		
    	int tmp = 0;
    	byte hSum;
    	int i, j, rIdx, cIdx;
    	while ((tmp = bis.read(buffer)) > 0) {
            
            // Create file name for ith block
    		File newFile=new File(blockPath+"/"+fileName+"."+String.format("%05d", partCounter++)+"."+ fileExt);
    		newFile.createNewFile();
            
        	rIdx = (int) Math.ceil(tmp / (double) rUnit);
        	cIdx = (int) tmp % rUnit; 
        	
        	if (tmp < sizeOfFiles)
        		checkSum = new byte [rIdx+rUnit+4];
        	else
        		checkSum = new byte [rIdx+rUnit];
            	            	
            for (i = 0; i < rIdx; i++){
            	hSum = 0;
            	for (j = 0; j < rUnit; j++){
            		if(i*rUnit+j < tmp){
            			hSum += buffer[i*rUnit+j];	            				
            			checkSum[j]+=buffer[i*rUnit+j]; // Add vertical check sum for error detecting
            		}
            	}
            		
            	checkSum[rUnit+i] = hSum;      // Add horizontal check sum for error detecting
            }
            	
            if(tmp < sizeOfFiles){
            	checkSum[rUnit+rIdx] = (byte) (cIdx & 0xFF);
            	checkSum[rUnit+rIdx+1] = (byte) ((cIdx>>8) & 0xFF);
            	checkSum[rUnit+rIdx+2] = (byte) (cIdx & 0xFF);
            	checkSum[rUnit+rIdx+3] = (byte) ((cIdx>>8) & 0xFF);
            		
            }
//            System.out.println(newFile.getName()+" "+tmp+" "+rIdx+" "+cIdx+" "+rUnit+" "+checkSum.length);	            		            	
            out = new FileOutputStream(newFile);
            out.write(buffer,0,tmp);
            out.write(checkSum);
            out.close();	            	
    	}
    	  		        	
    }
    
    public boolean validateChkSum (File f, byte[] buffer, int length) {
        		        
        boolean noError = true;
        int dataLength;
        int cIdx1 = 0, cIdx2 = 0, rIdx = 0;
        
        if(length != (sizeOfFiles + 1024 + rUnit)){
        	cIdx1 = ((buffer[length-3] & 0xFF) << 8 ) | (buffer[length-4] & 0xFF);      // Convert byte to integer
        	cIdx2 = ((buffer[length-1]& 0xFF)<<8 ) | (buffer[length-2] & 0xFF); // 
        		
        	if(cIdx1 != cIdx2){
        		System.err.println("Error in file "+f.getName()+" Please retransfer!");
        		noError = false;
        	}
        	else{
        		if(cIdx1==0)
        			rIdx = (length - rUnit)/(rUnit + 1);
        		else
        			rIdx = (length - rUnit - cIdx1 -1) / (rUnit +1);
        	}
        }
        else{
        	rIdx = 1024;
        	cIdx1 = 0;
        }
        	
        dataLength = rIdx * rUnit + cIdx1;
        if(cIdx1 != 0)
        	rIdx++;
        	
        if(noError){
        	byte hSum;
            byte [] vSum = new byte [rUnit];
            	
        	for (int i = 0; i < rIdx; i++){
        		hSum = 0;
        		for (int j = 0; j < rUnit; j++){
        			if ((i*rUnit+j) < dataLength){
        				hSum += buffer[i*rUnit+j]; // Add horizontal check sum
        				vSum[j]+=buffer[i*rUnit+j];  // Add vertical check sum for error detecting         			
        			}
        		}
        		
        		if (hSum != buffer[dataLength+rUnit+i]){    // Check horizontal checksum
        			noError = false;
        			System.err.println("Error in file "+f.getName()+" Please retransfer!");
        			break;
        		}
        	}
        		
        	for(int i = 0; i < rUnit; i++){
        		if(vSum[i] != buffer[dataLength+i]){
        			noError = false;
        			System.err.println("Error in file "+f.getName()+" Please retransfer!");
        			break;
        		}
        	}
        		
        	}
    	return noError;
    }
    
    public int calDataLength (byte[] buffer, int length) {
    	
    	int cIdx, rIdx, dataLength;
    	if (length != (sizeOfFiles + 1024 + rUnit)){
    		cIdx = ((buffer[length-3] & 0xFF) << 8 ) | (buffer[length-4] & 0xFF); 
	        if(cIdx==0)
	            rIdx = (length - rUnit)/(rUnit + 1);
	        else
	            rIdx = (length - rUnit - cIdx -1) / (rUnit +1);
	            			
	    }
	    else{
	        rIdx = 1024;
	        cIdx = 0;
	    }
	            		
	    dataLength = rIdx * rUnit + cIdx;
	    return dataLength;        		
    	 	        	    	
    }
    
    public void joinFiles (File f, String dir) throws IOException {
    	// Extract all file names in the folder
    	File folder = new File(dir);
    	File[] listOfFiles = folder.listFiles();
    	Arrays.sort(listOfFiles);
    	
        FileInputStream fis;	        
        FileOutputStream fos = new FileOutputStream (f);
        int tmp=0, dataLength=0;	
        byte[] buffer;  
        for (File file : listOfFiles) {
        	
            fis = new FileInputStream (file);
            buffer = new byte [(int) file.length()];
            if ((tmp = fis.read(buffer)) > 0 ){
            	
            	if(validateChkSum(file, buffer, (int) file.length())){
            		dataLength = calDataLength (buffer, tmp);
            		fos.write(buffer, 0, dataLength);
            		fis.close();
            	}
            }
            
    	 }
        fos.close();
    }

}
