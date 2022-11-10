package com.ic.er.entity;

import com.ic.er.common.AttributeConnectObjType;
import com.ic.er.common.DataType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttributeDO {
    private Long ID;
    private Long belongObjId;
    private AttributeConnectObjType belongObjType;
    private Long schemaID;
    private String name;
    private DataType dataType;
    private Boolean isPrimary;
    private Boolean nullable;
    private Integer aimPort;
    private Integer isDelete;
    private Date gmtCreate;
    private Date gmtModified;

    public AttributeDO(Long ID) {
        this.ID = ID;
    }

    public AttributeDO(Long belongObjId, AttributeConnectObjType belongObjType, Long schemaID) {
        this.belongObjId = belongObjId;
        this.schemaID = schemaID;
    }

}
