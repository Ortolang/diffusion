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
	$('#test a[href="#consulter"]').tab('show');
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
                          { "mData": "locked", "sClass": "center", "bSortable": false },
                          { "mData": "hidden", "sClass": "center", "bSortable": false },
                          { "mData": "deleted", "sClass": "center", "bSortable": false },
                          { "mData": "state", "sClass": "center", "bSortable": false },
                          { "mData": "view", "sClass": "center", "bSortable": false }
                      ],
            "aoColumnDefs": [
                             { "bVisible": false,  "aTargets": [ 5 ] },
                             { "bVisible": false,  "aTargets": [ 6 ] },
                             { "bVisible": false,  "aTargets": [ 7 ] },
                             { "bVisible": false,  "aTargets": [ 8 ] },
                             { "mRender": function(data, type, row ) { 
                            	 return row[5] + ' ' + row[6] + ' ' + row[7]; 
                            	 }, "aTargets": [ 9 ] }
                            ],
	});

	// Get the file name in order to set the object name
	$("#uploadFileModal #file").change(function() {
		var filename = $(this).val().split('\\').pop();
		$("#uploadFileModal #name").val(filename);
    });
	
	$('#createFolderButton').click(function() {
		// Create a collection
		var urlCoreCollection = "rest/core/collection";
		var urlCoreReference = "rest/core/reference";
		var params = $('#createFolderForm').serialize();
		var folderName = params['name'];
		
		$.ajax({
			type: "POST",
			url: urlCoreCollection,
			data: params,
			success: function(msg, textStatus, xhr){
				var pathToNewCollection = xhr.getResponseHeader('Location').split('/');
				//console.log("Location : "+pathToNewCollection);

				// And create a dynamic reference on the new collection
				if(pathToNewCollection.length>0) {
					var keyCollection = pathToNewCollection[pathToNewCollection.length-1];
	
					$.ajax({
						type: "POST",
						url: urlCoreReference,
						data: "name="+folderName+"&target="+keyCollection,
						success: function(msg){
//							alert( "Dossier créé");
							
							location.reload();
						}
					});
				}
			}
		});
		$('#createCollectionModal').modal('hide');
		
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

