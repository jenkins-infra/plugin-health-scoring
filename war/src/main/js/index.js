import {defineCustomElements} from "ionicons/dist/loader";
import { DataTable } from "simple-datatables";

defineCustomElements(window, {
  resourcesUrl: '/',
});

const probesTable = document.getElementById('probes-table');
if (probesTable !== null) {
    new DataTable("#probes-table", {
      perPageSelect: [10, 25, 50],
      columns: [
        { select: 1, sortable: false },
      ]
    });
}

const updateCollapseIcon = (container, target) => {
  if (target.classList.contains('show')) {
    container.setAttribute('name', 'chevron-collapse');
  } else {
    container.setAttribute('name', 'chevron-expand');
  }
}

const triggers = document.getElementsByClassName('collapse')
for (const trigger of triggers) {
  const iconContainer = trigger.querySelector('ion-icon[data-collapse]');
  const target = document.getElementById(trigger.getAttribute('data-target'));
  if (target != null) {
    updateCollapseIcon(iconContainer, target)
    trigger.addEventListener('click', e => {
      e.preventDefault();
      target.classList.toggle('show');
      updateCollapseIcon(iconContainer, target)
    });
  }
}
