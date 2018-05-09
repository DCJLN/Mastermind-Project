var participantsField = document.getElementById("Containertorepeat"),
	form = document.getElementsByTagName("form")[0],
	nbline = 11,
	i;
for(i=0; i<nbline; i++){
  var clone = participantsField.cloneNode(true);
  form.appendChild(clone);
}




// colors: RED BLUE YELLOW GREEN WHITE BLACK
var colors = [
"rgb(231, 76, 60)",
"rgb(52, 152, 219)",
"rgb(241, 196, 15)",
"rgb(46, 204, 113)",
"white",
"black"];

function cycleColor(buttonID){
	el = document.getElementById(buttonID);
	el.color = (el.color+1)%6;
	el.style.backgroundColor = colors[el.color];
}

function submitCombination(){
	var buttons = [];
	for(i = 1; i<= 4; i++)
		buttons.push(document.getElementById("button"+i));

	var url = "http://localhost:8019/eval?";
	for(i = 1; i <= 4; i++)
		url+=buttons[i-1].color+"-";
	url = url.slice(0,-1);
	console.log(url);
	httpGet(url, processResponse);
}

window.onload = function(){
	var buttons = [];
	for(i = 1; i<= 4; i++)
		buttons.push(document.getElementById("button"+i));
	for(b of buttons)
		b.color = colors.indexOf(b.style.backgroundColor);
}

function httpGet(url, callback){
    var xmlHttp = new XMLHttpRequest();
    xmlHttp.onreadystatechange = function() {
        if (xmlHttp.readyState == 4)
            callback(this);
    }
    xmlHttp.open("GET", url, true); // true for asynchronous
    xmlHttp.send(null);
}

function processResponse(request){
	var correct = request.getResponseHeader("Correct");
	var misplaced = request.getResponseHeader("Misplaced");
	console.log("correct: "+correct);
	console.log("misplaced: "+misplaced);
}