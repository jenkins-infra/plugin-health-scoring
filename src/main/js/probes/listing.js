import $ from 'jquery';
import dt from 'datatables.net';

$(document).ready( function () {
  $('#probes-table').DataTable(
    /**
     * Sort according to the executionOrder of the probes by default, which is the 3rd column in the table
     */
    {"order":[[3, 'asc']]}
  );
} );
