import { defineCustomElements } from "ionicons/dist/loader";
defineCustomElements(window, {
  resourcesUrl: '/',
});

$(document).ready( function () {
    $('#myTable').DataTable();
} );
