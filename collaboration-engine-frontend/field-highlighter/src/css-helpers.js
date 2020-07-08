const isShadyCSS = window.ShadyCSS && !window.ShadyCSS.nativeCss;

const isShadyDOM = window.ShadyDOM && window.ShadyDOM.inUse;

export const setCustomProperty = (node, prop, value) => {
  if (isShadyCSS) {
    window.ShadyCSS.styleSubtree(node, {
      [prop]: value
    });
  } else {
    node.style.setProperty(prop, value);
  }
}

let stylesMap = {};

export const applyShadyStyle = (node, css) => {
  if (isShadyDOM) {
    const tag = node.tagName.toLowerCase();
    // create style once per class
    if (!stylesMap[tag]) {
      const style = document.createElement('style');
      const cssText = css.replace(/:host\((.+)\)/, `${tag}$1`)
      style.textContent = cssText;
      style.setAttribute('scope', tag);
      stylesMap[tag] = style;
      document.head.appendChild(style);
    }
  } else {
    const style = document.createElement('style');
    style.textContent = css;
    node.shadowRoot.appendChild(style);
  }
}
