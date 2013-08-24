<?php
echo "fetchweather\n";
//header('content-type: text/html; charset=utf-8');

$city = "dortmund";
$str  = array('Accept-Language: '.$_SERVER["HTTP_ACCEPT_LANGUAGE"]);
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, "http://www.google.com/ig/api?weather=dortmund");
curl_setopt($ch, CURLOPT_PROXY,"localhost:8118");
curl_setopt($ch, CURLOPT_HTTPHEADER, $str);
curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 4);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, TRUE);
$ret = curl_exec($ch);
if(curl_exec($ch) === false)
{
  echo 'Curl-Fehler: ' . curl_error($ch);
}
else
{
  echo 'Operation ohne Fehler vollständig ausgeführt';
}
curl_close($ch);
echo $ret."\n";
$weather = simplexml_load_string(utf8_encode($ret));
$weather = $weather->weather;
echo $weather;
echo $weather->current_conditions->humidity["data"]."\n";

?>