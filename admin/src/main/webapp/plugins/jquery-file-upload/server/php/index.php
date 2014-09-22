
<?php
/*
 * jQuery File Upload Plugin PHP Example 5.14
 * https://github.com/blueimp/jQuery-File-Upload
 *
 * Copyright 2010, Sebastian Tschan
 * https://blueimp.net
 *
 * Licensed under the MIT license:
 * http://www.opensource.org/licenses/MIT
 */

/* Delete File after certain time, here 2 minutes (demo purpose) */
$path = 'files/';   
// Open the directory  
if ($handle = opendir($path))  
{  
    while (false !== ($file = readdir($handle)))  
    {  
        // Check the file we're doing is actually a file  
        if (is_file($path.$file))  
        {  
            // Check if the file is older than X days old  
            if (filemtime($path.$file) < ( time() - ( 2*60 ) ) )  
            {  
                // Do the deletion  
                unlink($path.$file);  
            }  
        }  
    }  
}  

error_reporting(E_ALL | E_STRICT);
require('UploadHandler.php');
$upload_handler = new UploadHandler();
