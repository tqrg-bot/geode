/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Utils.java
 *
 * Created on March 11, 2005, 12:34 PM
 */

package com.gemstone.gemfire.cache.query;

import java.util.Collection;
import java.util.Iterator;

/**
 *
 * @author vaibhav
 */
public class Utils {
  public static String printResult(Object r){
    StringBuffer sb = new StringBuffer();
    sb.append("Search Results\n");
    if(r == null){
      sb.append("Result is NULL");
      return sb.toString();
    } else if(r == QueryService.UNDEFINED){
      sb.append("Result is UNDEFINED");
      return sb.toString();
    }
    sb.append("Type = "+r.getClass().getName()).append("\n");
    if(r instanceof Collection){
      sb.append("Size = "+((Collection)r).size()+"\n");
      int cnt = 1 ;
      Iterator iter = ((Collection)r).iterator();
      while(iter.hasNext()){
        Object value = iter.next();
        sb.append((cnt++)+" type = "+value.getClass().getName()).append("\n");
        sb.append("  "+value+"\n");
      }
    } else
      sb.append(r);
    return sb.toString();
  }
}
