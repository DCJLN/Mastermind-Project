// colors: RED BLUE YELLOW GREEN WHITE BLACK
var buttons = [];
var colors = ["red", "blue", "yellow", "green", "white", "black"];
var row = 0;
var gameOver = false;

function cycleColor(buttonID){
	el = document.getElementById(buttonID);
	el.classList.remove(colors[el.color]);
	el.color = (el.color+1)%6;
	el.classList.add(colors[el.color]);
}

function submitCombination(){
	if(gameOver){
		gameOver = false;
		row = 0;
		for(i = 0; i < 12; i++){
			for(j = 0; j < 4; j++){
				document.getElementById("guessdot"+i.toString(16)+j).classList.remove("red");
				document.getElementById("guessdot"+i.toString(16)+j).classList.remove("blue");
				document.getElementById("guessdot"+i.toString(16)+j).classList.remove("yellow");
				document.getElementById("guessdot"+i.toString(16)+j).classList.remove("green");
				document.getElementById("guessdot"+i.toString(16)+j).classList.remove("white");
				document.getElementById("guessdot"+i.toString(16)+j).classList.remove("black");
			}
			for(j = 0; j < 4; j++){
				document.getElementById("tipdot"+i.toString(16)+j).classList.remove("correct");
				document.getElementById("tipdot"+i.toString(16)+j).classList.remove("misplaced");
			}
		}
	}

	var buttons = [];
	for(i = 1; i<= 4; i++)
		buttons.push(document.getElementById("button"+i));

	var url = "http://localhost:8019/eval?";
	for(i = 1; i <= 4; i++)
		url+=buttons[i-1].color+"-";
	url = url.slice(0,-1);
	console.log(url);
	httpGet(url, processResponse);

	for(col = 0; col < 4; col++){
		guessModification("guessdot"+(11-row).toString(16)+col, "button"+(col+1))
	}
	row++;
}

function guessModification(guessDotId, buttonID){
	guess = document.getElementById(guessDotId)
	button = document.getElementById(buttonID)
	guess.classList.add(button.classList[1]);
}

window.onload = function(){
	for(i = 1; i<= 4; i++)
		buttons.push(document.getElementById("button"+i));
	for(b of buttons)
		b.color = colors.indexOf(b.classList[1]);
	for(i = 0; i < 12; i++)
		if(colors.includes(document.getElementById("guessdot"+i.toString(16)+0).classList[1]))
			row++;
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
	console.log("correct: "+correct)
	console.log("misplaced: "+misplaced)
	tipModification(correct, misplaced)
	if(correct == 4){
		gameOver = true;
		alert("You won! Sumbit a new combination to start a new game.");
	}
	else if(row == 12){
		gameOver = true;
		alert("You lost. Sumbit a new combination to start a new game.");
	}
}

function tipModification(correct, misplaced){
	nbCorrect = correct;
	nbMisplaced = misplaced;
	hexaRow = (12-row).toString(16);

	for(i = 0; i < 4; i++){
		tip = document.getElementById("tipdot"+hexaRow+i)
		if(nbCorrect > 0){
			tip.classList.add("correct");
			nbCorrect--;
		}
		else if(nbMisplaced > 0){
			tip.classList.add("misplaced");
			nbMisplaced--;
		}
	}
}