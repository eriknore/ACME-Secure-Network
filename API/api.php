<?php
/**
 * Database definations
 */
define ('db_host', '127.0.0.1');
define ('db_name', 'api');
define ('db_user', 'root');
define ('db_pass', 'bnss2015');
/*
Connect to DB
*/
function db_connect() {
	$db = @mysqli_connect(db_host,db_user, db_pass, db_name);   
 	if(!$db) {
 		die('Could not connect to MySQL: ' .mysqli_connect_error());    
 	    	return FALSE;
    	}
     return $db;
}
/*
Close connection to DB
*/
function db_close($db){
        mysqli_close($db);
}
/*
Check _SERVER for SSL
*/
function isSSL(){
	if($_SERVER['SSL_CLIENT_I_DN'] == "/C=SE/O=ACME Scandinavia/OU=ACME Certificate Authority/CN=ACME TLS CA" && $_SERVER['SSL_CLIENT_VERIFY'] == "SUCCESS"){
		return TRUE;
	}else{
		return FALSE;
	}
}

/*
Get users and public keys
*/
$user = $_SERVER['SSL_CLIENT_S_DN_CN'];
$REQ = $_REQUEST['req_status'];


if($REQ == 1 && isSSL()) {
 /* Should not be root, but a user account. */
        $connection = db_connect();
	
        if($connection){
        $res = mysqli_query($connection, "select * from users"); // WHERE NOT name = '$user'");
        $response = array();
        while ($row = mysqli_fetch_assoc($res)){
                $response[] = $row;
        }
        print(json_encode($response));
        db_close($connection);
	}
/*
Send file to server and store info in DB
*/
}elseif($REQ == 2 && isSSL()){
	if(isset($_POST['upload']) && $_FILES['file']['size'] > 0){
       	$connection = db_connect();
                if($connection){
			$to = $_POST['name'];
			$sym_key = $_POST['sym_key'];
			$fileName = $_FILES['file']['name'];
			$tmpName  = $_FILES['file']['tmp_name'];
			$fileSize = $_FILES['file']['size'];
			$fp = fopen($tmpName, 'r');
			$content = fread($fp, fileSize($tmpName));
			$content = addslashes($content);
			fclose($fp);
			echo "FileName gotten is: " .$fileName;
			$sql ="INSERT INTO transfers (to_user, sym_key, size, filename, file) VALUES ('$to', '$sym_key', '$fileSize', '$fileName', '$content')";
			$res = mysqli_query($connection, $sql);
			if ( !$res ) {
				//  printf("error: %s\n", mysqli_error($connection));
				echo 'fail';
			}else { echo 'done';}
			db_close($connection);
       		}else{
               		echo "Sorry, there was an error uploading your file.";
        }
	}else{
	echo "Something wrong with either POST[upload] or the _FILES['file']";
}
/*
Files waiting for download and upload to App.
*/
}elseif($REQ == 3 &&isSSL()){
	$connection = db_connect();
	if($connection){
		$res = mysqli_query($connection, "select * from transfers WHERE to_user = '$user'");
		if (!$res) {
    			printf("Error: %s\n", mysqli_error($connection));
 			exit();
		}
	        $response = array();
        while ($row=mysqli_fetch_array($res)){
		$row_array['id'] = $row['id'];
		$row_array['filename'] = $row['filename'];
		$row_array['size'] = $row['size'];
		$row_array['sym_key'] = $row['sym_key'];
	        array_push($response, $row_array);
	}
        print(json_encode($response));
	}
	db_close($connection);
}elseif($REQ == 4 && isSSL()){

	$connection = db_connect();
        if($connection){
		$id = mysqli_real_escape_string($connection, $_POST['id']);
		$res = mysqli_query($connection, "select file FROM transfers WHERE id = '$id' AND to_user = '$user'");
                if($res){
			while($row = mysqli_fetch_array($res)){
			print(json_encode($row));
			if(!mysqli_query($connection, "DELETE FROM transfers WHERE id = '$id' AND to_user = '$user'")){
				print(mysqli_error($connection));
			}
			
		}
		}else{
		print(mysqli_error($connection));
		}
  	db_close($connection);
	}
}else{
	echo "req_status = $REQ";
	echo "user = $user";
	echo "isSSL " .isSSL();
}

?>
 
