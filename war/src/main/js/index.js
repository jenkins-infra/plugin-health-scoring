import {defineCustomElements} from "ionicons/dist/loader";

defineCustomElements(window, {
  resourcesUrl: '/',
});


const updateCollapseIcon = (container, target) => {
  if (target.classList.contains('show')) {
    container.setAttribute('name', 'chevron-collapse');
  } else {
    container.setAttribute('name', 'chevron-expand');
  }
}

const triggers = document.getElementsByClassName('collapse')
for (const trigger of triggers) {
  const iconContainers = trigger.getElementsByTagName('ion-icon');
  const target = document.getElementById(trigger.getAttribute('data-target'));
  if (target != null) {
    for (const container of iconContainers) {
      updateCollapseIcon(container, target)
    }
    trigger.addEventListener('click', e => {
      e.preventDefault();
      target.classList.toggle('show');
      for (const container of iconContainers) {
        updateCollapseIcon(container, target)
      }
    });
  }
}
