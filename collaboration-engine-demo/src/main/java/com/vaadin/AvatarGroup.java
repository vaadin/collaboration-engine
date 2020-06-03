package com.vaadin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.internal.JsonSerializer;

@Tag("vcf-avatar-group")
@NpmPackage(value = "@vaadin-component-factory/vcf-avatar-group", version = "1.0.2")
@JsModule("@vaadin-component-factory/vcf-avatar-group/theme/lumo/vcf-avatar-group.js")
public class AvatarGroup extends Component {

    public static class AvatarGroupItem {
        private String name;
        private String abbr;
        private String image;

        public AvatarGroupItem() {
        }

        public AvatarGroupItem(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAbbr() {
            return abbr;
        }

        public void setAbbr(String abbr) {
            this.abbr = abbr;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }
    }

    private List<AvatarGroupItem> items = new ArrayList<>();

    public AvatarGroup() {
    }

    public List<AvatarGroupItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void setItems(List<AvatarGroupItem> items) {
        Objects.requireNonNull(items, "Cannot set null items to AvatarGroup");
        this.items = items;
        getElement().setPropertyJson("items", JsonSerializer.toJson(items));
    }
}
