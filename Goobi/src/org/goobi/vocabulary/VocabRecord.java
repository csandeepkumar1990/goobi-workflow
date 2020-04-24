package org.goobi.vocabulary;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.goobi.beans.DatabaseObject;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data

@NoArgsConstructor
@XmlRootElement
public class VocabRecord implements DatabaseObject {
    private Integer id;
    private Integer vocabularyId;
    private List<Field> fields;

    @JsonIgnore
    private boolean valid = true;

    public VocabRecord(Integer id, Integer vocabularyId, List<Field> fields) {
        this.id = id;
        this.vocabularyId = vocabularyId;
        this.fields = fields;
    }

    @JsonIgnore
    public String getTitle() {
        for (Field field : fields) {
            if (field.getDefinition().isMainEntry()) {
                return field.getValue();
            }
        }
        return "";
    }

    @Override
    public void lazyLoad() {
    }

}