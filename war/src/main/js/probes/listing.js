import $ from 'jquery';
import dt from 'datatables.net';
$(document).ready( function () {
    $('#probes-table').DataTable(
      /**
       * default: sort by probe's execution order
       */
      {"order":[[3, 'asc']]}
    );
} );
