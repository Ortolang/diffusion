$(function () {
	var oTable;
    $('#ortolangTab a[href="#search"]').tab('show');
    $('#ortolangTab a[href="#browse"]').on('show', function (e) {
    	oTable = $('#entriesTable').dataTable( {
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
        } );
    });
    $("#newDigitalObjectForm").validate({
        submitHandler: function(form) {
        	var formData = new FormData(form);
        	$.ajax({
                url: "rest/core/object",
                type: 'POST',
                data:  formData,
                mimeType:"multipart/form-data",
                contentType: false,
                cache: false,
                async: false,
                processData:false,
                success: function(data, textStatus, jqXHR) {
                	$("#messageModalTitle").text("Objet déposé avec succès");
                    $("#messageModalBody").text("Félicitation, votre objet a été déposé et est enregistré sous la clé : xxxx. Vous pouvez désormais le trouver dans le registre. Merci pour votre participation !!");
                    $('#messageModal').modal('show');
                    $("#newDigitalObjectForm").reset();
                    $('#ortolangTab a[href="#browse"]').tab('show');
                },
                error: function(jqXHR, textStatus, errorThrown) {
                	$("#messageModalTitle").text("Le dépôt a échoué");
                    $("#messageModalBody").text("Une erreur est survenue lors de votre dépôt : " + data.value);
                    $('#messageModal').modal('show');
                }         
            });
        }
    });
    $('input[id=file]').change(function() {
    	$('#inputFile').val($(this).val());
    });
});

