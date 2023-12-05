package com.huachuan.entity;

import java.io.Serial;
import java.io.Serializable;

public class ReturnInfo implements Serializable {

   @Serial
   private static final long serialVersionUID = -5029786906349949434L;
   public Class type;
   public Object value;
}
