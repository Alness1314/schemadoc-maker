package com.alness.schemadoc.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColumnInfo {
    private String columName;   // <- tal cual lo pediste
    private String dataType;
    private boolean allowNulls;
}
