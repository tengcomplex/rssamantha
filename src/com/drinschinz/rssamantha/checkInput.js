<script type="text/javascript">

var intRegex = /^\d+$/;

function isInt(ii)
{
    return intRegex.test(ii);
}

var dtRegex = /^\d{4}\-\d{2}\-\d{2}\ \d{2}\:\d{2}\:\d{2}$/;

function isValidDateTime(dt)
{
	if(!dt || !dtRegex.test(dt))
	{
		return false;
	}	
	dt = dt.toString();
    // split
 	date = dt.substr(0,10).split("-");
	time = dt.substr(11,19).split(":");
    // leading zeros
	date[2] = parseInt(date[2],10);	
	date[1] = parseInt(date[1],10)-1;//month
	time[0] = parseInt(time[0],10);
	time[1] = parseInt(time[1],10);	
	time[2] = parseInt(time[2],10);		
    // create and check
	var cdt = new Date(date[0],date[1],date[2],time[0],time[1],time[2],0);
    //compare
	return cdt.getDate() == date[2] && cdt.getMonth() == date[1] && cdt.getFullYear() == date[0] && cdt.getHours() == time[0] && cdt.getMinutes() == time[1] && cdt.getSeconds() == time[2];
}

function checkInput() 
{  
	var ni = document.generate.numitems.value;
	if(ni.toUpperCase() != "ALL" && !isInt(ni)) 
    {
        alert("Input \"Number of items\" must be \"ALL\" or an integer.");
        document.generate.numitems.focus();
        return false;
    }
	var ct = document.generate.cutoff.value;
	if(ct != "" && ct.toUpperCase() != "TODAY" && !isInt(ct) && !isValidDateTime(ct)) 
    {
        alert("Input \"Cutoff\" must be empty, \"TODAY\", \"yyyy-mm-dd hh:mm:ss\" or milliseconds since epoch.");
        document.generate.cutoff.focus();
        return false;
    }
	var rf = document.generate.refresh.value;
	if(rf != "" && !isInt(rf)) 
    {
        alert("Input \"Refresh\" must be an integer.");
        document.generate.refresh.focus();
        return false;
    }
    return true;
}
</script>
