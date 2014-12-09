sftpLoad
========

upload file to remote server using sftp

fileProcess.java: processing video file:
    	split a video file into n blocks, each block has predefined size
      append horizontal and vertical checksum data at the end of each block for error detecting
    	merge n block into one file
    	using mp4parser for mp4 file format checking (aspectjrt, isoparser)
    	
stpTransfer.java: Transfer file between local computer and remote server:
    	using sftp for secure file transmission
      using Apache Commons VFS to provide a single API for accessing file systems (e.g. SFTP)
