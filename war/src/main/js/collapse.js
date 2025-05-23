const updateCollapseIcon = (container, target) => {
  if (target.classList.contains('show')) {
    container.setAttribute('name', 'chevron-collapse');
  } else {
    container.setAttribute('name', 'chevron-expand');
  }
}

export function collapse(elementClass = 'collapse') {
  const triggers = document.getElementsByClassName(elementClass)
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
}
