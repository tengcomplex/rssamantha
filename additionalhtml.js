// Array list of days.
var days = new Array('Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday');
 
// Array list of months.
var months = new Array('January','February','March','April','May','June','July','August','September','October','November','December');

function startTime()
{
	var today = new Date();
	document.getElementById('txtdate').innerHTML = days[today.getDay()] + ", "+months[today.getMonth()]+" "+zeroPre(today.getDate())+", "+(fourdigits(today.getYear()));
	document.getElementById('txttime').innerHTML = zeroPre(today.getHours())+":"+zeroPre(today.getMinutes())+":"+zeroPre(today.getSeconds());
	t=setTimeout(function(){startTime()}, 1000);
}

function zeroPre(i)
{
	return (i < 10) ? i = "0" + i : i;
}

// Calculate four digit year.
function fourdigits(number) 
{
    return (number < 1000) ? number + 1900 : number;
}

var counter = 100;

function countDown()
{	t = setTimeout(function(){countDown()},1000);
	document.getElementById('txtcountdown').innerHTML = "Next reload in "+counter+"s";
	counter -= 1;
}


