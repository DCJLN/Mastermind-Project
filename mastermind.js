/*for(i = 0; i < 12; i++){
							
	var newMainDiv = document.createElement("div")
	newMainDiv.setAttribute("class", "guess-tip-container")
	newMainDiv.setAttribute("id", "containertorepeat"+i)
	
	var newGuessDiv = document.createElement("div")
	newGuessDiv.setAttribute("class", "guess-container")
	newGuessDiv.setAttribute("id", "guess-cont"+i)
	
	var newTipDiv = document.createElement("div")
	newTipDiv.setAttribute("class", "tip-container")
	newTipDiv.setAttribute("id", "tip-cont"+i)

	document.getElementsByTagName("form")[0].appendChild(newMainDiv)
	document.getElementById("containertorepeat"+i).appendChild(newGuessDiv)
	document.getElementById("containertorepeat"+i).appendChild(newTipDiv)

	for(j = 0; j < 4; j++){
		var newSpan = document.createElement("span")
		newSpan.setAttribute("class", "guess-dot")
		newSpan.setAttribute("id", "guessdot"+i+j)
		document.getElementById("guess-cont"+i).appendChild(newSpan)
	}

	for(k = 0; k < 4; k++){
		var newSpan = document.createElement("span")
		newSpan.setAttribute("class", "tip-dot")
		newSpan.setAttribute("id", "tipdot"+i+k)
		document.getElementById("tip-cont"+i).appendChild(newSpan)
	}
}*/



// colors: RED BLUE YELLOW GREEN WHITE BLACK
var buttons = [];
var colors = ["red", "blue", "yellow", "green", "white", "black"];

function cycleColor(buttonID){
	el = document.getElementById(buttonID);
	el.classList.remove(colors[el.color]);
	el.color = (el.color+1)%6;
	el.classList.add(colors[el.color]);
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
	for(i = 1; i<= 4; i++)
		buttons.push(document.getElementById("button"+i));
	for(b of buttons)
		b.color = colors.indexOf(b.classList[1]);
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