package io.github.MigadaTang.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.github.MigadaTang.Attribute;
import io.github.MigadaTang.Relationship;
import io.github.MigadaTang.RelationshipEdge;

import java.io.IOException;
import java.util.List;

public class RelationshipSerializer extends JsonSerializer<Relationship> {
    private boolean isRenderFormat;

    public RelationshipSerializer(boolean isRenderFormat) {
        this.isRenderFormat = isRenderFormat;
    }

    @Override
    public void serialize(
            Relationship relationship, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {

        jgen.writeStartObject();

        if (isRenderFormat) {
            jgen.writeNumberField("id", relationship.getID());
        }


        jgen.writeStringField("name", relationship.getName());
        List<Attribute> attributeList = relationship.getAttributeList();
        if (isRenderFormat || (attributeList != null && attributeList.size() != 0)) {
            jgen.writeObjectField("attributeList", attributeList);
        }
        List<RelationshipEdge> edgeList = relationship.getEdgeList();
        if (isRenderFormat || (edgeList != null && edgeList.size() != 0)) {
            jgen.writeObjectField("edgeList", edgeList);
        }
        if (relationship.getLayoutInfo() != null) {
            jgen.writeObjectField("layoutInfo", relationship.getLayoutInfo());
        }

        jgen.writeEndObject();
    }
}