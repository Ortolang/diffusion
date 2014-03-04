$(function() {
    $('#side-menu').metisMenu();
});

$(function() {
    $(window).bind("load resize", function() {
        console.log($(this).width());
        if ($(this).width() < 768) {
            $('div.sidebar-collapse').addClass('collapse');
        } else {
            $('div.sidebar-collapse').removeClass('collapse');
        }
    });
});

$(document).ready(function() { 
	console.log("yala");
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
	
	$('#createCollectionButton').click(function() {
		$('#createCollectionForm').submit();
	});
	

	$('#createReferenceButton').click(function() {
		$('#createReferenceForm').submit();
	});
});

