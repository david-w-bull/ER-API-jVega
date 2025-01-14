package io.github.MigadaTang;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.github.MigadaTang.common.AttributeType;
import io.github.MigadaTang.common.Cardinality;
import io.github.MigadaTang.common.DataType;
import io.github.MigadaTang.common.EntityType;
import io.github.MigadaTang.exception.ERException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SchemaDeserializer extends StdDeserializer<Schema> {

    private Map<String, Entity> entityNameMap = new HashMap<>();
    private Map<String, Relationship> relationshipNameMap = new HashMap<>();


    public SchemaDeserializer() {
        this(null);
    }

    public SchemaDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Schema deserialize(JsonParser jp, DeserializationContext ctxt)
            throws ERException, IOException {

        JsonNode schemaJSONNode = jp.getCodec().readTree(jp);
        String schemaName = schemaJSONNode.get("name").textValue();
        Schema schema = ER.createSchema(schemaName);

        parseEntityList(schema, schemaJSONNode.get("entityList"));
        prepareEmptyRelationship(schema, schemaJSONNode.get("relationshipList"));
        parseRelationshipList(schema, schemaJSONNode.get("relationshipList"));

        return schema;
    }

    private void parseEntityList(Schema schema, JsonNode entityList) {
        if (entityList == null) {
            return;
        }
        // first round of iteration: create all strong entities, because subset and weak entities rely on these strong entities
        for (JsonNode entityJSONNode : entityList) {
            parseEntityNodeWithEntityType(schema, entityJSONNode, EntityType.STRONG);
        }
        // second round create subset and weak entities
        for (JsonNode entityJSONNode : entityList) {
            parseEntityNodeWithEntityType(schema, entityJSONNode, EntityType.WEAK, EntityType.SUBSET);
        }
    }

    private void parseEntityNodeWithEntityType(Schema schema, JsonNode entityJSONNode, EntityType... types) {
        String entityName = entityJSONNode.get("name").textValue();
        EntityType entityType = EntityType.getFromValue(entityJSONNode.get("entityType").textValue());
        JsonNode belongStrongEntity = entityJSONNode.get("belongStrongEntity");
        String strongEntityName = null;
        if (belongStrongEntity != null) {
            strongEntityName = belongStrongEntity.textValue();
        }
        Entity entity = null;
        for (EntityType type : types) {
            if (type == entityType) {
                switch (entityType) {
                    case STRONG: {
                        entity = schema.addEntity(entityName);
                        break;
                    }
                    case WEAK: {
                        entity = schema.addEntity(entityName, EntityType.WEAK);
                        break;
                    }
                    case SUBSET: {
                        entity = schema.addSubset(entityName, entityNameMap.get(strongEntityName));
                        break;
                    }
                    default: {
                        throw new ERException(String.format("deserialize schema fail: fail to create entity: %s", entityName));
                    }
                }
            }
        }
        if (entity == null) {
            return;
        }
        JsonNode aimPortNode = entityJSONNode.get("aimPort");
        if (aimPortNode != null) {
            entity.updateAimPort(aimPortNode.intValue());
        }
        JsonNode layoutInfoJSONNode = entityJSONNode.get("layoutInfo");
        if (layoutInfoJSONNode != null) {
            entity.updateLayoutInfo(layoutInfoJSONNode.get("layoutX").asDouble(), layoutInfoJSONNode.get("layoutY").asDouble());
        }
        JsonNode attributeList = entityJSONNode.get("attributeList");
        if (attributeList != null) {
            for (JsonNode attributeJSONNode : attributeList) {
                String attributeName = attributeJSONNode.get("name").textValue();
                DataType attributeDataType = DataType.valueOf(attributeJSONNode.get("dataType").textValue());
                Boolean attributeIsPrimary = attributeJSONNode.get("isPrimary").booleanValue();
                AttributeType attributeType = AttributeType.valueOf(attributeJSONNode.get("attributeType").textValue());
                Attribute attribute = entity.addAttribute(attributeName, attributeDataType, attributeIsPrimary, attributeType);
                JsonNode attributeAimPortNode = attributeJSONNode.get("aimPort");
                if (attributeAimPortNode != null) {
                    attribute.updateAimPort(attributeAimPortNode.intValue());
                }
                JsonNode attributeLayoutInfoNode = attributeJSONNode.get("layoutInfo");
                if (attributeLayoutInfoNode != null) {
                    attribute.updateLayoutInfo(attributeLayoutInfoNode.get("layoutX").asDouble(), attributeLayoutInfoNode.get("layoutY").asDouble());
                }
            }
        }
        entityNameMap.put(entityName, entity);
    }

    private void prepareEmptyRelationship(Schema schema, JsonNode relationshipList) {
        if (relationshipList == null) {
            return;
        }
        for (JsonNode relationshipJSONNode : relationshipList) {
            String relationshipName = relationshipJSONNode.get("name").textValue();
            Relationship relationship = schema.createEmptyRelationship(relationshipName);
            relationshipNameMap.put(relationshipName, relationship);
        }
    }

    private void parseRelationshipList(Schema schema, JsonNode relationshipList) {
        if (relationshipList == null) {
            return;
        }
        for (JsonNode relationshipJSONNode : relationshipList) {
            String relationshipName = relationshipJSONNode.get("name").textValue();
            Relationship relationship = relationshipNameMap.get(relationshipName);
            JsonNode edgeList = relationshipJSONNode.get("edgeList");
            if (edgeList == null || edgeList.size() == 0) {
                throw new ERException(String.format("deserialize schema fail edgeList of relationship: %s cannot be empty", relationshipName));
            }
            for (JsonNode edgeJsonNode : edgeList) {
                JsonNode entityNode = edgeJsonNode.get("entity");
                JsonNode relationshipNode = edgeJsonNode.get("relationship");
                Cardinality cardinality = Cardinality.getFromValue(edgeJsonNode.get("cardinality").textValue());
                JsonNode isKeyNode = edgeJsonNode.get("isKey");
                boolean isKey = false;
                if (isKeyNode != null) {
                    isKey = isKeyNode.booleanValue();
                }
                ERConnectableObj target = null;
                if (entityNode != null) {
                    target = entityNameMap.get(entityNode.textValue());
                } else if (relationshipNode != null) {
                    target = relationshipNameMap.get(relationshipNode.textValue());
                } else {
                    throw new ERException("missing entity or relationship in the edge");
                }
                RelationshipEdge edge = relationship.linkObj(target, cardinality, isKey);
                int portAtRelationship = -1, portAtBelongObj = -1;
                JsonNode portAtRelationshipNode = edgeJsonNode.get("portAtRelationship");
                if (portAtRelationshipNode != null) {
                    portAtRelationship = portAtRelationshipNode.intValue();
                }
                JsonNode portAtBelongObjNode = edgeJsonNode.get("portAtBelongObj");
                if (portAtBelongObjNode != null) {
                    portAtBelongObj = portAtBelongObjNode.intValue();
                }
                if(edge != null) {   // edited dwb 20230705
                    edge.updatePorts(portAtRelationship, portAtBelongObj);
                }
            }

            JsonNode attributeList = relationshipJSONNode.get("attributeList");
            if (attributeList != null) {
                for (JsonNode attributeJSONNode : attributeList) {
                    String attributeName = attributeJSONNode.get("name").textValue();
                    DataType attributeDataType = DataType.valueOf(attributeJSONNode.get("dataType").textValue());
                    AttributeType attributeType = AttributeType.valueOf(attributeJSONNode.get("attributeType").textValue());
                    Attribute attribute = relationship.addAttribute(attributeName, attributeDataType, attributeType);
                    JsonNode aimPortNode = attributeJSONNode.get("aimPort");
                    if (aimPortNode != null) {
                        attribute.updateAimPort(aimPortNode.intValue());
                    }
                    JsonNode layoutInfoJSONNode = attributeJSONNode.get("layoutInfo");
                    if (layoutInfoJSONNode != null) {
                        attribute.updateLayoutInfo(layoutInfoJSONNode.get("layoutX").asDouble(), layoutInfoJSONNode.get("layoutY").asDouble());
                    }
                }
            }
            JsonNode layoutInfoJSONNode = relationshipJSONNode.get("layoutInfo");
            if (layoutInfoJSONNode != null) {
                relationship.updateLayoutInfo(layoutInfoJSONNode.get("layoutX").asDouble(), layoutInfoJSONNode.get("layoutY").asDouble());
            }
        }
    }
}