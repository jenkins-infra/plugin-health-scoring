import {DataTable} from 'simple-datatables';
import 'simple-datatables/src/css/style.css';

export function setupDataTable(elementId, option = {}) {
  const mergedOpt = {
    ...{
      perPageSelect: [10, 25, 50],
    },
    ...option
  }
  new DataTable(elementId, mergedOpt);
}
