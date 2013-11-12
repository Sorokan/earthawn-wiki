
$(function() {
	var editMode = false;
	var federFollowMouse = function(e) {
		$('#feder').css({'top': e.clientY-88+$(window).scrollTop(), 'left': e.clientX+5});
	}
	function setEditMode(on,e) {
		editMode = on;
		var content = jQuery('#content');
		var menu = jQuery('#menu');
		var editButton = jQuery('#edit-button');
		if (editMode) {
			$("body").on('mousemove.feder',federFollowMouse);
			$("#feder").show();
			federFollowMouse(e);
			jQuery('#save-button').show();
			editButton.addClass('active');
			content.aloha();
			menu.aloha();
			window.onbeforeunload = function() {
			    return 'Achtung - Ihre Änderungen gehen verloren!';
			};
			$('#content_meta').css('display','block');
			$('#content_meta .markieroptionen').each(function() {
				$(this).scrollTop($('.markierung:nth-child(1)',this).offset().top);
			});
			$('#page_markierungen').addClass('edit');
		} else {
			$("#feder").hide();
			$("body").off('mousemove.feder');
			editButton.removeClass('active');
			content.mahalo();
			content.removeClass();
			menu.mahalo();
			menu.removeClass();
			$.post("/save",{ content: $('#content').html(), menu: $('#menu').html()});
			window.onbeforeunload = null;
			$('X').hide();
			$('#content_meta').css('display','none');
			$('#page_markierungen').removeClass('edit');
		}
	}
	$('#edit-button').click(function(e) {
		setEditMode(!editMode,e);
	});
	$('#content').dblclick(function(e) {
		if (!editMode) {
			setEditMode(true,e);
		}
	});
	$('#page_title .text').click(function() {
		var label = $(this);
		label.hide();
		var edit = $('#text_edit');
		edit.val(label.text());
		edit.show();
		edit.focus();
		$('.left_mark').hide();
		$('.right_mark').hide();
	});
	var saveTitle = function() {
		var label = $('#page_title .text');
		var edit = $('#text_edit');
		edit.hide();
		label.text(edit.val());
		label.show();
		$('.left_mark').show();
		$('.right_mark').show();
	}
	$('#text_edit').keypress(function(event) {
		if ( event.which == 13 ) {
			event.preventDefault();
			saveTitle();
		}
	});
	$('#text_edit').focusout(function(event) {
		saveTitle();
	});
	$('.markierung').click(function(event){
		selected = $('.markierung_selected',$(this).parents('.markieroptionen'));
		selected.empty();
		selected.append($('img',this).clone());
		$(this).parent().children().show();
		$(this).hide();
	});
});
/*
Aloha.ready(function() {
  Aloha.jQuery('#content').aloha();
});
*/