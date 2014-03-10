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

$(document).ready(function() {

	var urlPart = window.location.hash;
	var urlPartSplit = urlPart.split('/');
	//alert(urlPartSplit);
	var tabName = "undefined";
	if(urlPartSplit!=null && urlPartSplit.length>0)
		tabName = urlPartSplit[0];

	var ooName = "undefined";
	if(urlPartSplit!=null && urlPartSplit.length>1)
		ooName = urlPartSplit[1];
	console.log("Ortolang object : "+ooName);
	if(tabName!=null && tabName!="undefined" && tabName!="null" && tabName!="") {
		//alert("tabName == "+tabName);
		$('#test a[href="'+tabName+'"]').tab('show');
		
		if(tabName=="#myspace" && ooName!="undefined") {
			getFolder(ooName);
		}
		
	} else {
		console.log("tabName == null");
		$('#test a').tab('show');
	}
	
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

	// Mon espace
	// Accéder à un dossier/fichier : .../index.html#myspace/{clé}
	// Si dossier Alors tableau avec liste des dossiers/fichiers
	// Si fichier Alors tableau avec les propriétés du fichier + onglet metadonnées + bouton télécharger
	// Accéder à la fiche d'un dossier/fichier : .../index.html#myspace/{clé_dossier}/properties

	// Get the file name in order to set the object name
	$("#uploadFileModal #file").change(function() {
		var filename = $(this).val().split('\\').pop();
		$("#uploadFileModal #name").val(filename);
    });
	
	$('#createFolderButton').click(function() {
		// Create a folder
		var urlFsFolder = "rest/fs/folders";
		var params = $('#createFolderForm').serialize();
		var folderName = params['name'];
		
		$.ajax({
			type: "POST",
			url: urlFsFolder,
			data: params,
			success: function(msg, textStatus, xhr){
				location.reload();
			}
		});
		$('#createFolderModal').modal('hide');
		
	});

	$('#uploadFileButton').click(function() {
		// Create a collection
		var urlCoreObject = "rest/core/object";
		var urlCoreReference = "rest/core/reference";
		var params = $('#uploadFileForm').serialize();
		var fileName = params['name'];
		
		var formData = new FormData(document.getElementById("uploadFileForm"));
		
		$.ajax({
			type: "POST",
			url: urlCoreObject,
			mimeType:"multipart/form-data",
			processData:false,
		    contentType:false,
			//contentType: 'multipart/form-data',
		    cache: false,
//			data: params,
			data: formData,
            async: false,
			success: function(msg, textStatus, xhr){
				var pathToNewObject = xhr.getResponseHeader('Location').split('/');
				//console.log("Location : "+pathToNewCollection);

				// And create a dynamic reference on the new collection
				if(pathToNewObject.length>0) {
					var keyObject = pathToNewObject[pathToNewObject.length-1];
	
					$.ajax({
						type: "POST",
						url: urlCoreReference,
						data: "name="+fileName+"&target="+keyObject,
						success: function(msg){
							location.reload();
						}
					});
				}
			},
			error: function(xhr, textStatus, errorThrown) {
				alert(errorThrown);
			}
		});
		$('#uploadFileModal').modal('hide');
		
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
});

function getFolder(key) {
	var urlFsFolder = "rest/fs/folders/"+key;
	$.ajax({
		type: "GET",
		url: urlFsFolder,
		success: function(msg, textStatus, xhr){
			console.log(msg);
		}
	});
	
}
