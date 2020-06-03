const fields = new WeakMap();

window.setFieldState = (field, activeUser) => {
    let nameTag = fields.get(field);
    if (!activeUser) {
        if (nameTag) {
            nameTag.textContent = "";
            nameTag.style.display = "none";
            Object.assign(field.style, {
                borderRadius: '',
                backgroundColor: '',
                boxShadow: ''
            });
        }
    } else {
        if (!nameTag) {
            nameTag = document.createElement('div');

            nameTag.setAttribute('part', 'collaboration-name-tag');
            // TODO: Use a badge?
            nameTag.setAttribute('theme', 'badge primary');
            // TODO: How to make these modifiable. Need to make a WC of nameTag?
            // Or should you register styles for text-field to style the tag?
            Object.assign(nameTag.style, {
                position: 'absolute',
                top: '3px',
                right: '3px',
                padding: '0 2px',
                visibility: 'hidden',
                borderRadius: '2px',
                pointerEvents: 'none',
                backgroundColor: 'var(--lumo-primary-text-color)',
                color: 'var(--lumo-primary-contrast-color)'
            });
            field.shadowRoot.appendChild(nameTag);
        
            field.addEventListener('mouseover', () => nameTag.style.visibility = 'visible');
            field.addEventListener('mouseout', () => nameTag.style.visibility = 'hidden');

            fields.set(field, nameTag);
        }

        Object.assign(field.style, {
            // TODO: Not nice, have to create a stacking context / position the host to place the tag properly
            transform: 'translateZ(0)',
            borderRadius: '1px',
            backgroundColor: 'var(--lumo-primary-color-10pct)',
            boxShadow: '0px 0px 0px 3px var(--lumo-primary-color-10pct)'
        });

        nameTag.style.display = "block";
        nameTag.textContent = activeUser;
    }
}
  