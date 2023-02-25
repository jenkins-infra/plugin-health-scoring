import { defineCustomElements } from "ionicons/dist/loader";
defineCustomElements(window, {
  resourcesUrl: '/',
});

import $ from 'jquery';
import dt from 'datatables.net';
$(document).ready( function () {
  $('#probes-table').DataTable(
    {"order":[[3, 'asc']]}
  );
} );
