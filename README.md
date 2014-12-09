sftpLoad
========

upload file to remote server using sftp

fileProcess.java: processing video file:
    1) split a video file into n blocks, each block has predefined size
    2) append horizontal and vertical checksum data at the end of each block for error detecting
    3) merge n block into one file
    4) using mp4parser for mp4 file format checking (aspectjrt, isoparser)
    	
stpTransfer.java: Transfer file between local computer and remote server:
    1) using sftp for secure file transmission
    2) using Apache Commons VFS to provide a single API for accessing file systems (e.g. SFTP)
