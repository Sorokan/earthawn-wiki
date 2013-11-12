define([
	'aloha',
	'jquery',
	'aloha/plugin',
	'ui/ui',
	'ui/toggleButton',	
	'ui/floating',
	'css!../../../../plugins/custom/initialcaps/css/initialcaps.css'
], function (Aloha,
	$,
	Plugin,
	Ui,
	ToggleButton,
	Floating
	) {

	'use strict';
     
    return Plugin.create('initialcaps', {
    	
        defaults: {
            value: 10
        },
        
        init: function () {
        	this.buttonNames = ['_button1','_button2'];
        	this.classNames = ['initial-caps-1','initial-caps-2'];
        	this.iconCssNames = ['aloha-icon-initialcaps-1','aloha-icon-initialcaps-2'];
        	for ( var i = 0; i < this.classNames.length; i++) {
            	this.createButton(i);
			}
        	var that = this;
			Aloha.bind('aloha-selection-changed', function (evt, selection, originalEvent) {
				for ( var i = 0; i < that.classNames.length; i++) {
					var on = selection && $(selection.getCommonAncestorContainer()).hasClass(that.classNames[i]);
					that[that.buttonNames[i]].setState(on);
				}					
			});
        },
        
        createButton: function (index) {
        	var that = this;

        	that[that.buttonNames[index]] = Ui.adopt("colorPicker", ToggleButton, {
				tooltip: "Initiale",
				icon: that.iconCssNames[index],				
				scope: 'Aloha.continuoustext',
				click: function () {
					var startContainer = Aloha.Selection.rangeObject.startContainer;
					var p = $(startContainer).parents('p');
		        	for ( var i = 0; i < that.classNames.length; i++) {
		        		if (i==index) {
		        			p.toggleClass(that.classNames[i]);		        			
							that[that.buttonNames[i]].setState(p.hasClass(that.classNames[i]));
		        		} else {		        			
		        			p.removeClass(that.classNames[i]);
							that[that.buttonNames[i]].setState(false);
		        		}
					}					
				}
			});
    	}

    });
});