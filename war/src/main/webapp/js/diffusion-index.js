$(function() {
    $('#side-menu').metisMenu();
});

$(function() {
    $(window).bind("load resize", function() {
        //console.log($(this).width());
        if ($(this).width() < 768) {
            $('div.sidebar-collapse').addClass('collapse');
        } else {
            $('div.sidebar-collapse').removeClass('collapse');
        }
    });
});

var current_dir="undefined";

$(document).ready(function() {

	var urlPart = window.location.hash;
	var urlPartSplit = urlPart.split('/');
	//alert(urlPartSplit);
	var tabName = "undefined";
	if(urlPartSplit!=null && urlPartSplit.length>0)
		tabName = urlPartSplit[0];

	var $a = $('#side-menu a').first();
	if(tabName!=null && tabName!="undefined" && tabName!="null" && tabName!="") {
		$a = $('#side-menu a[href="'+tabName+'"]');
	} else {
		console.log("[INIT] Select default (no tab selected)");
		tabName = $a.attr('href');
	}
	
	initTabView();
	
	console.log("[INIT] View tab "+$a.attr('href'));
//	$('#test a[href="'+tabName+'"]').tab('show');
	$a.tab('show');
	
//	showTab(tabName, urlPartSplit);
	
	initMySpaceTabView();
	initRegistryTabView();
});

/**
 * Show a specific tab.
 * @param tabName
 */
function showTab(tabName) {
	if(tabName=="#myspace") {
		showMySpaceTab();
	} else if(tabName=="#registry") {
		showRegistryTab();
	}
}

function showRegistryTab() {


}

function showMySpaceTab() {

	var urlPart = window.location.hash;
	var urlPartSplit = urlPart.split('/');
	//alert(urlPartSplit);
//	var tabName = "undefined";
//	if(urlPartSplit!=null && urlPartSplit.length>0)
//		tabName = urlPartSplit[0];

	if(urlPartSplit!=null && urlPartSplit.length>1)
		current_dir = urlPartSplit[1];
	else
		current_dir = "undefined";
	
	console.log("[showMySpaceTab] Selected ortolang object is "+current_dir);
	if(current_dir!="undefined") {
//		current_dir = ooName;
		getFolder(current_dir);
	}
}

var $fsEntriesTable = "undefined";

function getFolder(key) {
	/*
	var urlFsFolder = "rest/fs/folders/"+key;
	$.ajax({
		type: "GET",
		url: urlFsFolder,
		success: function(msg, textStatus, xhr){
			console.log(msg);
		}
	});
	*/

	if($fsEntriesTable=="undefined") {
		$fsEntriesTable = $('#fsEntriesTable').dataTable( {
            "bProcessing": true,
            "bServerSide": true,
            "bFilter": false,
            "sAjaxSource": "./rest/fs/folders/"+key+"/elements",
            "aoColumns": [
                          { "mData": "name", "sClass": "center", "bSortable": false },
//                          { "mData": "key", "sClass": "center", "bSortable": false },
//                          { "mData": "service", "sClass": "center", "bSortable": false},
                          { "mData": "type", "sClass": "center", "bSortable": false },
                          { "mData": "owner", "sClass": "center", "bSortable": false },
//                          { "mData": "creationDate", "sClass": "center", "bSortable": false },
                          { "mData": "modificationDate", "sClass": "center", "bSortable": false },
//                          { "mData": "state", "sClass": "center", "bSortable": false },
//                          { "mData": "view", "sClass": "center", "bSortable": false }
                      ],
		});
	} else {
		$fsEntriesTable.fnReloadAjax();
	}
	

}

function initTabView() {

	$('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
		  console.log("[initTabView] activated tab : "+e.target); // activated tab
		  //e.relatedTarget; // previous tab
		  
		  var tabName = e.target.hash;
		  showTab(tabName);
		});
	

}

function initRegistryTabView() {

	$('#entriesTable').dataTable( {
            "bProcessing": true,
            "bServerSide": true,
            "bFilter": false,
            "sAjaxSource": "./rest/catalog/entries",
            "aoColumns": [
                          { "mData": "key", "sClass": "center", "bSortable": false },
                          { "mData": "service", "sClass": "center", "bSortable": false},
                          { "mData": "type", "sClass": "center", "bSortable": false },
                          { "mData": "owner", "sClass": "center", "bSortable": false },
                          { "mData": "creationDate", "sClass": "center", "bSortable": false },
                          { "mData": "modificationDate", "sClass": "center", "bSortable": false },
                          { "mData": "state", "sClass": "center", "bSortable": false },
                          { "mData": "view", "sClass": "center", "bSortable": false }
                      ],
            "aoColumnDefs": [ 
                          { "bVisible": false,  "aTargets": [ 5 ] } 
            		  ],
	});
	
	$('#createCollectionButton').click(function() {
//		$('#createCollectionForm').submit();
		var url = $('#createCollectionForm').attr('action');
		var params = $('#createCollectionForm').serialize();
		$.ajax({
			type: "POST",
			url: url,
			data: params,
			success: function(msg){
				//alert( "Collection créée !");
				location.reload();
			}
		});
		$('#createCollectionModal').modal('hide');
	});
	

	$('#createReferenceButton').click(function() {
		$('#createReferenceForm').submit();
	});

	$('#createMetadataButton').click(function() {
		$('#createMetadataForm').submit();
	});
}

function initMySpaceTabView() {

	// Mon espace
	// Accéder à un dossier/fichier : .../index.html#myspace/{clé}
	// Si dossier Alors tableau avec liste des dossiers/fichiers
	// Si fichier Alors tableau avec les propriétés du fichier + onglet metadonnées + bouton télécharger
	// Accéder à la fiche d'un dossier/fichier : .../index.html#myspace/{clé_dossier}/properties

	$('#createFolderButton').click(function() {
		// Create a folder
		var urlFsFolder = "rest/fs/folders";
		var params = $('#createFolderForm').serialize();
//		var folderName = params['name'];
		
		$.ajax({
			type: "POST",
			url: urlFsFolder,
			data: params,
			success: function(msg, textStatus, xhr){
				if(current_dir!="undefined") {
					var pathToNewObject = xhr.getResponseHeader('Location').split('/');
					//console.log("Location : "+pathToNewCollection);

					// Update the folder to add the new collection
					if(pathToNewObject.length>0) {
						var keyObject = pathToNewObject[pathToNewObject.length-1];
						var urlFsFolderElements = "rest/fs/folders/"+current_dir+"/elements";
		
						$.ajax({
							type: "POST",
							url: urlFsFolderElements,
							data: "element="+keyObject,
							success: function(msg){
								location.reload();
							}
						});
					}
				} else {
					location.reload();
				}
			}
		});
		$('#createFolderModal').modal('hide');
		
	});

	// Get the file name in order to set the object name
	$("#uploadFileModal #file").change(function() {
		var filename = $(this).val().split('\\').pop();
		$("#uploadFileModal #name").val(filename);
    });
	
	$('#uploadFileButton').click(function() {
		// Create a collection
		var urlFsFile = "rest/fs/files";
//		var params = $('#uploadFileForm').serialize();
//		var fileName = params['name'];
		
		var formData = new FormData(document.getElementById("uploadFileForm"));
		
		$.ajax({
			type: "POST",
			url: urlFsFile,
			mimeType:"multipart/form-data",
			processData:false,
		    contentType:false,
			//contentType: 'multipart/form-data',
		    cache: false,
//			data: params,
			data: formData,
            async: false,
			success: function(msg, textStatus, xhr){
				if(current_dir!="undefined") {
					var pathToNewObject = xhr.getResponseHeader('Location').split('/');
					//console.log("Location : "+pathToNewCollection);
	
					// And create a dynamic reference on the new collection
					if(pathToNewObject.length>0) {
						var keyObject = pathToNewObject[pathToNewObject.length-1];
						var urlFsFolderElements = "rest/fs/folders/"+current_dir+"/elements";
						
						$.ajax({
							type: "POST",
							url: urlFsFolderElements,
							data: "element="+keyObject,
							success: function(msg){
								location.reload();
							}
						});
					}
				} else {
					location.reload();
				}
			},
			error: function(xhr, textStatus, errorThrown) {
				alert(errorThrown);
			}
		});
		$('#uploadFileModal').modal('hide');
		
	});
	
}
