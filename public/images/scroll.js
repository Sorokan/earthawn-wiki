var NS4 = (document.layers) ? 1 : 0;
var IE4 = (document.all) ? 1 : 0;
var IE5 = (document.getElementById && IE4) ? 1 : 0;
var NS6 = (document.getElementById && !IE4) ? 1 : 0;
//alert("NS4: "+NS4+"\nNS6: "+NS6+"\nIE4: "+IE4+"\nIE5: "+IE5);

var layerRef = "";
var styleSwitch = "";
var visibleVar = "";
var hideVar = "";
var styleTop = "";
var posiHeight = "";

function init(){
		// fuer Netscape
        if (NS4) {
			layerRef="document.layers";
			styleSwitch="";
			visibleVar="show";
			hideVar="hide";
			styleTop=".top"
			posiHeight=".clip.height"
		// fuer Netscape 6
        }
        else if (NS6) {
			styleSwitch=".style";
			visibleVar="visible";
			hideVar="hidden";
			styleTop=".style.top"
			posiHeight=".offsetHeight"
        }
		// fuer InternetExplorer
		else{
			layerRef="document.all";
			styleSwitch=".style";
			visibleVar="visible";
			hideVar="hidden";
			styleTop=".style.pixelTop"
			posiHeight=".offsetHeight"
		}; 
}


function find_textlayer(layername){
		if (NS4) {
			eval(textLayer = "document.layers[\'viewlayer\'].document.layers[\'"+layername+"\']");
		} else if (NS6) {
			textLayer = document.getElementById(layername);
		} else {
			eval(textLayer = layerRef+"[\'"+layername+"\']");
		}
}

var activetext = "textlayer";
var wdh=null;
var dir="up";

function get_posTarget(activetext){
var obj;
		find_textlayer(activetext);
		pos_max=10;
		if (NS6) {
			obj = textLayer;
			pos_aktuell = parseInt(obj.offsetHeight);
		} else
			pos_aktuell=parseInt(eval(textLayer+""+posiHeight));
		posTarget=pos_max-pos_aktuell;
}
function scroll(dir){
var obj;
		find_textlayer(activetext);
		if (NS6) {
			obj = textLayer.style;
			posiTop = parseInt(obj.top);
		} else
			posiTop = parseInt(eval(textLayer+""+styleTop));
//		alert(posiTop);
		// Scrollen nach oben weg
		if(dir=='up'){
			get_posTarget(activetext);
//			alert("posiTop: "+posiTop+"\nposTarget: "+posTarget);
			if (posiTop > posTarget){
				posiTop -= 5;
				if (NS6)
					obj.top = posiTop;
				else
					eval(textLayer+""+styleTop+"=posiTop")
			}
		}
		// Scrollen nach unten weg
		if(dir=='down'){
			posTarget=0;
			if (posiTop < posTarget){
				posiTop += 5;
				if (NS6)
					obj.top = posiTop;
				else
					eval(textLayer+""+styleTop+"=posiTop")
			}
		}
		if (wdh==null){
			wdh=setInterval("scroll(\'"+dir+"\')",5);
		}
		return false;
}